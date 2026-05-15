let API = 'http://localhost:8080';
let auth = { user: '', pass: '', role: '', userId: 1 };

function updateApiBase(v) {
  API = v.replace(/\/+$/, '');
  const el = document.getElementById('api-url-input');
  if (el) el.value = API;
}

const b64 = s => btoa(unescape(encodeURIComponent(s)));
const authHeader = () => 'Basic ' + b64(auth.user + ':' + auth.pass);

async function apiFetch(path, opts = {}) {
  const res = await fetch(API + path, {
    ...opts,
    headers: { Authorization: authHeader(), 'Content-Type': 'application/json', ...(opts.headers || {}) }
  });
  if (res.status === 204) return null;
  const txt = await res.text();
  let data; try { data = JSON.parse(txt); } catch { data = txt; }
  if (!res.ok) throw { status: res.status, data };
  return data;
}

function toast(msg, type = 'success') {
  const icons = { success: '✓', error: '✕', warn: '!' };
  const el = document.createElement('div');
  el.className = `toast-item toast-${type}`;
  el.innerHTML = `<span>${icons[type]}</span> ${msg}`;
  document.getElementById('toast').appendChild(el);
  setTimeout(() => el.remove(), 3500);
}

function closeModal(id) { document.getElementById(id).classList.remove('open'); }

async function doLogin() {
  const user = document.getElementById('login-user').value.trim();
  const pass = document.getElementById('login-pass').value;
  const apiVal = document.getElementById('login-api').value.trim();
  if (!user || !pass) { showLoginError('Enter username and password.'); return; }
  updateApiBase(apiVal);
  const btn = document.getElementById('login-btn');
  btn.innerHTML = '<div class="spinner"></div> Signing in…'; btn.disabled = true;
  try {
    auth = { user, pass, role: '', userId: 1 };
    const probes = [
      { key: 'patients',      path: '/patients' },
      { key: 'appointments',  path: '/appointments/pending-records' },
      { key: 'reports',       path: '/reports/patients-per-clinic' },
    ];
    let results;
    try {
      results = await Promise.all(probes.map(p =>
        fetch(API + p.path, { headers: { Authorization: authHeader() } })
          .then(r => ({ key: p.key, status: r.status }))
          .catch(() => ({ key: p.key, status: 0 }))
      ));
    } catch (e) {
      showLoginError('Cannot reach server at ' + API + '. Is the backend running?');
      btn.innerHTML = 'Sign In'; btn.disabled = false; return;
    }
    if (results.some(r => r.status === 401)) {
      showLoginError('Invalid credentials.'); btn.innerHTML = 'Sign In'; btn.disabled = false; return;
    }
    if (results.every(r => r.status === 0)) {
      showLoginError('Cannot reach server at ' + API + '. Is the backend running?');
      btn.innerHTML = 'Sign In'; btn.disabled = false; return;
    }
    const s = {};
    results.forEach(r => s[r.key] = r.status);
    let found = null;
	if (s.reports === 200) {
	found = 'Medical_Records';
	} else if (s.appointments === 200) {
	found = 'Receptionist';
	} else if (s.patients === 200) {
	found = 'Clinical';
	}
    if (!found) { showLoginError('Could not determine role. Contact your administrator.'); btn.innerHTML = 'Sign In'; btn.disabled = false; return; }
    auth = { user, pass, role: found, userId: 1 };
    sessionStorage.setItem('mhis_auth', JSON.stringify({ user: auth.user, pass: auth.pass, role: auth.role, userId: auth.userId, api: API }));
    const roleMap = {
      'Clinical': 'clinical.html',
      'Receptionist': 'receptionist.html',
      'Medical_Records': 'records.html',
    };
    window.location.href = roleMap[found];
  } catch (e) { showLoginError('Connection error.'); btn.innerHTML = 'Sign In'; btn.disabled = false; }
}

function showLoginError(msg) {
  const el = document.getElementById('login-error');
  el.textContent = msg;
  el.style.display = 'block';
}

function requireAuth(expectedRole) {
  const raw = sessionStorage.getItem('mhis_auth');
  if (!raw) { window.location.href = 'login.html'; return false; }
  const saved = JSON.parse(raw);
  if (expectedRole && saved.role !== expectedRole) { window.location.href = 'login.html'; return false; }
  auth = saved;
  API = saved.api || API;
  return true;
}

function logout() {
  sessionStorage.removeItem('mhis_auth');
  window.location.href = 'login.html';
}

function initShell() {
  const apiInput = document.getElementById('api-url-input');
  if (apiInput) apiInput.value = API;
  document.getElementById('user-display').textContent = auth.user;
  document.getElementById('user-role-display').textContent = auth.role.replace('_', ' ');
  document.getElementById('user-avatar').textContent = auth.user[0].toUpperCase();
}

function showPage(id) {
  document.querySelectorAll('.page').forEach(p => p.classList.remove('active'));
  document.getElementById(id).classList.add('active');
}

function showDashboard(extraHtml) {
  document.querySelectorAll('.page').forEach(p => p.classList.remove('active'));
  document.getElementById('page-dashboard').classList.add('active');
  const h = new Date().getHours();
  document.getElementById('dash-greeting').textContent =
    (h < 12 ? 'Good morning' : h < 17 ? 'Good afternoon' : 'Good evening') + ', ' + auth.user + '.';
  const bc = auth.role === 'Clinical' ? 'badge-clinical' : auth.role === 'Receptionist' ? 'badge-receptionist' : 'badge-records';
    let html = `<div class="card-grid" style="max-width:800px;margin:0 auto;">
    <div class="card"><div class="card-title">Your Role</div>
      <div style="font-size:22px;font-family:var(--font-head);font-style:italic;margin-bottom:8px;">${auth.role.replace('_', ' ')}</div>
      <span class="role-badge ${bc}">${auth.role}</span>
      <p style="margin-top:12px;font-size:12px;color:var(--ink-3);">Logged in as <strong>${auth.user}</strong></p></div>
    <div class="card"><div class="card-title">Quick Actions</div><div style="display:flex;flex-direction:column;gap:8px;">${extraHtml || ''}</div></div>
  </div>`;
  document.getElementById('dashboard-content').innerHTML = html;
}	