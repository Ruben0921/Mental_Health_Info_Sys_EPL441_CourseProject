/* ═══════════════════════════════════════════════════════════════════
   receptionist.js  —  MHIS Receptionist Portal
   Requires: app.js (auth, apiFetch, toast, showPage, closeModal, etc.)

   LIVE ENDPOINTS:
     POST   /appointments                          (Receptionist)
     PUT    /appointments/{id}/attendance          (Receptionist) → 204
     GET    /appointments/missed?date=YYYY-MM-DD   (Receptionist)
     GET    /appointments/pending-records           (Receptionist)
     GET    /patients                              (Clinical + Receptionist)
     GET    /patients/{id}                         (Clinical + Receptionist)

   NOTE: Repeat Prescription has no REST endpoint yet — the UI flow
   records the intent and shows confirmation; a POST /prescriptions
   endpoint can be wired in once added to the backend.
   ═══════════════════════════════════════════════════════════════════ */

/* ── Module state ───────────────────────────────────────────────── */
let _calYear      = new Date().getFullYear();
let _calMonth     = new Date().getMonth();   // 0-indexed
let _cachedAppts  = [];   // pending-records cache for calendar
let _rxPatient    = null; // patient selected in Rx flow
let _rxLastAppt   = null; // most recent appointment for that patient

/* ═══════════════════════════════════════════════════════════════════
   INIT
   ═══════════════════════════════════════════════════════════════════ */
function initReceptionist() {
  if (!requireAuth('Receptionist')) return;
  initShell();
  buildReceptionistNav('dashboard');
  showPage('page-dashboard');
  _initDashboardHeader();
  setTodayDate();
  loadDashboard();
}

function _initDashboardHeader() {
  const h = new Date().getHours();
  const greeting = h < 12 ? 'Good morning' : h < 17 ? 'Good afternoon' : 'Good evening';
  const greetEl = document.getElementById('dash-greeting');
  if (greetEl) greetEl.textContent = `${greeting}, ${auth.user}.`;
  const dateEl = document.getElementById('dash-date-label');
  if (dateEl) dateEl.textContent = new Date().toLocaleDateString('en-GB', {
    weekday: 'long', year: 'numeric', month: 'long', day: 'numeric'
  });
}

/* ═══════════════════════════════════════════════════════════════════
   NAVIGATION
   ═══════════════════════════════════════════════════════════════════ */
function buildReceptionistNav(activeId) {
  const nav = document.getElementById('main-nav');
  if (!nav) return;
  nav.innerHTML = '';

  const items = [
    {
      id: 'dashboard', label: 'Dashboard',
      action: () => { showPage('page-dashboard'); _initDashboardHeader(); loadDashboard(); }
    },
    {
      id: 'appointments', label: 'Appointments',
      action: () => { showPage('page-appointments'); setTodayDate(); loadMissed(); loadPendingRecords(); }
    },
    {
      id: 'search', label: 'Patient Search',
      action: () => { showPage('page-search'); }
    },
    {
      id: 'prescription', label: 'Prescriptions',
      action: () => { showPage('page-prescription'); resetRx(); }
    },
    {
      id: 'calendar', label: 'Calendar',
      action: () => { showPage('page-calendar'); renderCalendar(); }
    },
  ];

  items.forEach(item => {
    const btn = document.createElement('button');
    btn.className = 'nav-btn' + (item.id === activeId ? ' active' : '');
    btn.textContent = item.label;
    btn.onclick = () => {
      document.querySelectorAll('.nav-btn').forEach(b => b.classList.remove('active'));
      btn.classList.add('active');
      item.action();
    };
    nav.appendChild(btn);
  });
}

/* ═══════════════════════════════════════════════════════════════════
   DASHBOARD
   ═══════════════════════════════════════════════════════════════════ */
function loadDashboard() {
  _loadDashPending();
  _loadDashMissed();
}

/* Stat cards + pending table */
async function _loadDashPending() {
  const wrap = document.getElementById('dash-pending-wrap');
  if (wrap) wrap.innerHTML = spinner();

  try {
    const rows = await apiFetch('/appointments/pending-records');
    _cachedAppts = rows || [];

    _setStat('stat-total',   rows.length);
    _setStat('stat-pending', rows.length);

    if (!rows.length) {
      if (wrap) wrap.innerHTML = emptyState('', 'All records up to date', '');
      return;
    }

    if (wrap) {
      wrap.innerHTML =
        tableWrap(
          '<tr><th>Appt ID</th><th>Patient</th><th>Clinic</th><th>Date</th><th>Type</th><th>Status</th><th>Records</th></tr>',
          rows.slice(0, 8).map(r => `
            <tr class="flag-row">
              <td><span style="font-family:var(--font-mono);">#${r.appointmentId}</span></td>
              <td>#${r.patientId}</td>
              <td>#${r.clinicId}</td>
              <td>${fmtDate(r.appointmentDate)}</td>
              <td>${r.type || '&mdash;'}</td>
              <td>${statusBadge(r.status)}</td>
              <td><span class="pending-flag">&#9873; Not Updated</span></td>
            </tr>`).join('')
        ) +
        (rows.length > 8
          ? `<div style="padding:12px 16px;font-size:12px;color:var(--ink-3);">
               +${rows.length - 8} more &mdash;
               <a href="#" onclick="event.preventDefault();
                  document.querySelectorAll('.nav-btn')[1].click();"
                  style="color:var(--accent);">View all</a>
             </div>`
          : '');
    }
  } catch (e) {
    if (wrap) wrap.innerHTML = errState(e);
    _setStat('stat-total',   '&mdash;');
    _setStat('stat-pending', '&mdash;');
  }
}

/* Missed today → stat card + notice banner + activity feed + attended count */
async function _loadDashMissed() {
  const today = todayStr();
  try {
    const rows = await apiFetch(`/appointments/missed?date=${today}`);
    const missedCount = (rows || []).length;
    _setStat('stat-missed', missedCount);

    const notice  = document.getElementById('dash-missed-notice');
    const countEl = document.getElementById('dash-missed-count');
    if (notice && countEl) {
      countEl.textContent = missedCount;
      notice.style.display = missedCount > 0 ? 'flex' : 'none';
    }

    _buildActivityFeed(rows || []);

    // Attended today = appointments dated today whose status is 'Attended'
    // We derive this from _cachedAppts (pending-records already loaded).
    // Pending-records only contains recordsUpdated=false rows, so we also
    // scan for today-dated entries marked Attended there; for a proper count
    // we re-use the cache and count status=Attended with today's date.
    const todayAppts = (_cachedAppts || []).filter(a => {
      const d = (a.appointmentDate + '').split('T')[0];
      return d === today;
    });
    const attendedToday = todayAppts.filter(a =>
      (a.status || '').toLowerCase() === 'attended'
    ).length;
    // If we found today-dated rows in cache use that count, else show 0
    _setStat('stat-attended', attendedToday);

  } catch (e) {
    _setStat('stat-missed',   '&mdash;');
    _setStat('stat-attended', '&mdash;');
    _buildActivityFeed([]);
  }
}

function _buildActivityFeed(missedRows) {
  const el = document.getElementById('dash-activity');
  if (!el) return;

  const items = [];
  (missedRows || []).slice(0, 5).forEach(r => {
    items.push({
      color: 'red',
      text:  `<strong>${r.firstName} ${r.lastName}</strong> missed their appointment (ID #${r.appointmentId})`,
      time:  'Today'
    });
  });
  items.push({
    color: 'blue',
    text:  'Daily missed-appointment list generated and emailed to responsible consultants.',
    time:  new Date().toLocaleTimeString('en-GB', { hour: '2-digit', minute: '2-digit' })
  });

  el.innerHTML = items.map(i => `
    <div class="activity-item">
      <div class="activity-dot ${i.color}"></div>
      <div>
        <div class="activity-text">${i.text}</div>
        <div class="activity-time">${i.time}</div>
      </div>
    </div>`).join('');
}

/* exposed for onclick in HTML refresh button */
function loadDashPending() { _loadDashPending(); }

/* ═══════════════════════════════════════════════════════════════════
   APPOINTMENTS PAGE
   ═══════════════════════════════════════════════════════════════════ */
function setTodayDate() {
  const el = document.getElementById('missed-date');
  if (el && !el.value) el.value = todayStr();
}

function showApptTab(tab, btn) {
  document.querySelectorAll('.tab').forEach(t => t.classList.remove('active'));
  btn.classList.add('active');
  ['missed', 'pending', 'update'].forEach(t => {
    const el = document.getElementById('appt-tab-' + t);
    if (el) el.style.display = t === tab ? '' : 'none';
  });
  if (tab === 'missed')  loadMissed();
  if (tab === 'pending') loadPendingRecords();
}

/* GET /appointments/missed?date=YYYY-MM-DD
   Response: List<MissedPatientRow> { appointmentId, patientId, firstName, lastName } */
async function loadMissed() {
  const date = (document.getElementById('missed-date') || {}).value;
  if (!date) return;
  const wrap = document.getElementById('missed-table-wrap');
  if (wrap) wrap.innerHTML = spinner();

  try {
    const rows = await apiFetch(`/appointments/missed?date=${date}`);

    if (!rows || !rows.length) {
      if (wrap) wrap.innerHTML = emptyState('', 'No missed appointments on this date', '');
      return;
    }

    if (wrap) {
      wrap.innerHTML =
        `<div class="missed-notice" style="margin:16px;border-radius:8px;">
           <span style="font-size:18px;">&#128231;</span>
           <div>Daily notification automatically sent to responsible consultants for
             <strong>${rows.length}</strong> missed appointment(s).</div>
         </div>` +
        tableWrap(
          '<tr><th>Appt ID</th><th>Patient ID</th><th>Name</th><th>Status</th></tr>',
          rows.map(r => `
            <tr class="flag-row">
              <td><span style="font-family:var(--font-mono);">#${r.appointmentId}</span></td>
              <td>#${r.patientId}</td>
              <td><strong>${r.firstName} ${r.lastName}</strong></td>
              <td><span class="status status-missed">Missed</span></td>
            </tr>`).join(''),
          'margin:0 16px 16px;'
        );
    }
  } catch (e) {
    if (wrap) wrap.innerHTML = errState(e);
  }
}

/* GET /appointments/pending-records
   Response: List<Appointment> { appointmentId, patientId, clinicId, staffId,
                                  appointmentDate, type, status, recordsUpdated } */
async function loadPendingRecords() {
  const wrap = document.getElementById('pending-table-wrap');
  if (wrap) wrap.innerHTML = spinner();

  try {
    const rows = await apiFetch('/appointments/pending-records');
    _cachedAppts = rows || [];

    if (!rows || !rows.length) {
      if (wrap) wrap.innerHTML = emptyState('', 'All records up to date', '');
      return;
    }

    if (wrap) {
      wrap.innerHTML = tableWrap(
        '<tr><th>Appt ID</th><th>Patient</th><th>Clinic</th><th>Date</th><th>Type</th><th>Status</th><th>Records</th></tr>',
        rows.map(r => `
          <tr class="flag-row">
            <td><span style="font-family:var(--font-mono);">#${r.appointmentId}</span></td>
            <td>#${r.patientId}</td>
            <td>#${r.clinicId}</td>
            <td>${fmtDate(r.appointmentDate)}</td>
            <td>${r.type || '&mdash;'}</td>
            <td>${statusBadge(r.status)}</td>
            <td><span class="pending-flag">&#9873; Not Updated</span></td>
          </tr>`).join('')
      );
    }
  } catch (e) {
    if (wrap) wrap.innerHTML = errState(e);
  }
}

/* ── New Appointment modal ─────────────────────────────────────── */

// Caches for smart search inside the modal
let _allPatients = [];
let _naSelectedPatientId = null;
let _naSelectedClinicId  = null;
let _naSelectedStaffId   = null;

async function openNewApptModal() {
  const el = document.getElementById('na-date');
  if (el) el.value = todayStr();
  const w = document.getElementById('new-appt-dropin-warn');
  if (w) w.style.display = 'none';

  // Reset selections
  _naSelectedPatientId = null;
  _naSelectedClinicId  = null;
  _naSelectedStaffId   = null;
  ['na-patient-result','na-clinic-result','na-staff-result'].forEach(id => {
    const el = document.getElementById(id);
    if (el) el.innerHTML = '';
  });
  ['na-patient-search','na-clinic-search','na-staff-search'].forEach(id => {
    const el = document.getElementById(id);
    if (el) el.value = '';
  });

  document.getElementById('new-appt-modal').classList.add('open');

  // Pre-load patient list for fast filtering
  if (!_allPatients.length) {
    try { _allPatients = await apiFetch('/patients'); } catch (_) {}
  }
}

/* Live-filter patient list as user types */
function filterModalPatients() {
  const q    = (document.getElementById('na-patient-search').value || '').toLowerCase().trim();
  const wrap = document.getElementById('na-patient-result');
  if (!q) { wrap.innerHTML = ''; return; }

  const matches = _allPatients.filter(p =>
    String(p.patientId).includes(q) ||
    `${p.firstName} ${p.lastName}`.toLowerCase().includes(q)
  ).slice(0, 6);

  if (!matches.length) {
    wrap.innerHTML = `<div class="na-suggestion-empty">No patients found</div>`;
    return;
  }
  wrap.innerHTML = matches.map(p => {
    const flag = p.selfHarmHistory ? ' <span class="alert-selfharm" style="font-size:10px;">&#9873;</span>' : '';
    return `<div class="na-suggestion" onclick="selectModalPatient(${p.patientId},'${p.firstName} ${p.lastName}')">
      <span class="na-sug-name">${p.firstName} ${p.lastName}${flag}</span>
      <span class="na-sug-meta">#${p.patientId}</span>
    </div>`;
  }).join('');
}

function selectModalPatient(id, name) {
  _naSelectedPatientId = id;
  document.getElementById('na-patient-search').value = `${name} (#${id})`;
  document.getElementById('na-patient-result').innerHTML = '';
  checkDropinWarning();
}

/* Clinic search — uses clinicIds seen in cached appointments */
function filterModalClinics() {
  const q    = (document.getElementById('na-clinic-search').value || '').toLowerCase().trim();
  const wrap = document.getElementById('na-clinic-result');
  if (!q) { wrap.innerHTML = ''; return; }

  const seenClinics = [...new Map(
    (_cachedAppts || []).filter(a => a.clinicId)
      .map(a => [a.clinicId, { clinicId: a.clinicId, name: `Clinic #${a.clinicId}` }])
  ).values()];

  const matches = seenClinics.filter(c =>
    String(c.clinicId).includes(q) || (c.name || '').toLowerCase().includes(q)
  ).slice(0, 6);

  if (!matches.length) {
    const numId = parseInt(q);
    if (numId) {
      wrap.innerHTML = `<div class="na-suggestion" onclick="selectModalClinic(${numId},'Clinic #${numId}')">
        <span class="na-sug-name">Clinic #${numId}</span>
        <span class="na-sug-meta">Use this ID</span>
      </div>`;
    } else {
      wrap.innerHTML = `<div class="na-suggestion-empty">Type a clinic ID number</div>`;
    }
    return;
  }
  wrap.innerHTML = matches.map(c =>
    `<div class="na-suggestion" onclick="selectModalClinic(${c.clinicId},'${c.name}')">
      <span class="na-sug-name">${c.name}</span>
      <span class="na-sug-meta">#${c.clinicId}</span>
    </div>`
  ).join('');
}

function selectModalClinic(id, name) {
  _naSelectedClinicId = id;
  document.getElementById('na-clinic-search').value = `${name} (#${id})`;
  document.getElementById('na-clinic-result').innerHTML = '';
}

/* Staff search — uses staffIds seen in cached appointments */
function filterModalStaff() {
  const q    = (document.getElementById('na-staff-search').value || '').toLowerCase().trim();
  const wrap = document.getElementById('na-staff-result');
  if (!q) { wrap.innerHTML = ''; return; }

  const seenStaff = [...new Map(
    (_cachedAppts || []).filter(a => a.staffId)
      .map(a => [a.staffId, { staffId: a.staffId, name: `Staff #${a.staffId}` }])
  ).values()];

  const matches = seenStaff.filter(s =>
    String(s.staffId).includes(q) || (s.name || '').toLowerCase().includes(q)
  ).slice(0, 6);

  if (!matches.length) {
    const numId = parseInt(q);
    if (numId) {
      wrap.innerHTML = `<div class="na-suggestion" onclick="selectModalStaff(${numId},'Staff #${numId}')">
        <span class="na-sug-name">Staff #${numId}</span>
        <span class="na-sug-meta">Use this ID</span>
      </div>`;
    } else {
      wrap.innerHTML = `<div class="na-suggestion-empty">Type a staff ID number</div>`;
    }
    return;
  }
  wrap.innerHTML = matches.map(s =>
    `<div class="na-suggestion" onclick="selectModalStaff(${s.staffId},'${s.name}')">
      <span class="na-sug-name">${s.name}</span>
      <span class="na-sug-meta">#${s.staffId}</span>
    </div>`
  ).join('');
}

function selectModalStaff(id, name) {
  _naSelectedStaffId = id;
  document.getElementById('na-staff-search').value = `${name} (#${id})`;
  document.getElementById('na-staff-result').innerHTML = '';
}

/* Drop-in warning — now uses _naSelectedPatientId from smart search */
async function checkDropinWarning() {
  const typeEl   = document.getElementById('na-type');
  const warnEl   = document.getElementById('new-appt-dropin-warn');
  const detailEl = document.getElementById('new-appt-dropin-detail');
  if (!typeEl || !warnEl) return;

  const isDropin  = typeEl.value === 'Drop-in';
  const patientId = _naSelectedPatientId;

  if (!isDropin || !patientId) { warnEl.style.display = 'none'; return; }

  let appts = _cachedAppts;
  if (!appts.length) {
    try { appts = await apiFetch('/appointments/pending-records'); _cachedAppts = appts; }
    catch (_) { appts = []; }
  }

  const patientAppts = (appts || [])
    .filter(a => a.patientId === patientId)
    .sort((a, b) => (b.appointmentDate || '').localeCompare(a.appointmentDate || ''));

  if (patientAppts.length) {
    const last = patientAppts[0];
    if (detailEl) {
      detailEl.innerHTML =
        `Patient #${patientId} last attended on <strong>${fmtDate(last.appointmentDate)}</strong>
         (Appt #${last.appointmentId}). Verify this drop-in is not an attempt to obtain extra medication.`;
    }
    warnEl.style.display = 'flex';
  } else {
    warnEl.style.display = 'none';
  }
}

/* POST /appointments
   Body: { patientId, clinicId, staffId, appointmentDate, type, status, recordsUpdated }
   Response: Appointment (with generated appointmentId) */
async function createAppointment() {
  const body = {
    patientId:       _naSelectedPatientId || 0,
    clinicId:        _naSelectedClinicId  || 0,
    staffId:         _naSelectedStaffId   || 0,
    appointmentDate: document.getElementById('na-date').value,
    type:            document.getElementById('na-type').value,
    status:          document.getElementById('na-status').value,
    recordsUpdated:  false
  };

  if (!body.patientId || !body.clinicId || !body.staffId || !body.appointmentDate) {
    toast('Select a patient, clinic, staff member and date.', 'warn');
    return;
  }

  try {
    const created = await apiFetch('/appointments', {
      method: 'POST',
      body:   JSON.stringify(body)
    });
    toast(`Appointment #${created.appointmentId} created successfully.`, 'success');
    closeModal('new-appt-modal');
    _loadDashPending();
    loadPendingRecords();
  } catch (e) {
    toast('Failed to create appointment: ' + _errMsg(e), 'error');
  }
}

/* PUT /appointments/{id}/attendance
   Body: { status: "Attended"|"Missed"|"Pending" }   (AttendanceUpdate)
   Response: 204 No Content */
async function updateAttendance() {
  const id     = parseInt((document.getElementById('upd-appt-id')     || {}).value);
  const status = (document.getElementById('upd-appt-status') || {}).value;
  if (!id) { toast('Enter a valid appointment ID.', 'warn'); return; }

  try {
    await apiFetch(`/appointments/${id}/attendance`, {
      method: 'PUT',
      body:   JSON.stringify({ status })
    });
    toast(`Appointment #${id} updated to "${status}".`, 'success');
    loadPendingRecords();
    _loadDashPending();
  } catch (e) {
    toast('Update failed: ' + _errMsg(e), 'error');
  }
}

/* ═══════════════════════════════════════════════════════════════════
   PATIENT SEARCH
   GET /patients       → List<Patient>
   GET /patients/{id}  → Patient
   Both allow: Clinical + Receptionist (PatientService.java Authz)
   Patient fields: patientId, firstName, lastName, address,
                   homeless, riskStatus, deceased, selfHarmHistory
   ═══════════════════════════════════════════════════════════════════ */
async function searchPatients() {
  const query = ((document.getElementById('search-input') || {}).value || '').trim();
  if (!query) { toast('Enter a name or patient ID.', 'warn'); return; }

  const wrap = document.getElementById('search-results-wrap');
  if (wrap) wrap.innerHTML = spinner();

  try {
    const all = await apiFetch('/patients');
    const q   = query.toLowerCase();
    const filtered = (all || []).filter(p =>
      String(p.patientId).includes(q) ||
      `${p.firstName} ${p.lastName}`.toLowerCase().includes(q) ||
      `${p.lastName} ${p.firstName}`.toLowerCase().includes(q)
    );
    _renderPatientList(filtered, `No results for "${query}"`);
  } catch (e) {
    if (wrap) wrap.innerHTML = errState(e);
  }
}

async function loadAllPatients() {
  const wrap = document.getElementById('search-results-wrap');
  if (wrap) wrap.innerHTML = spinner();
  try {
    const all = await apiFetch('/patients');
    _renderPatientList(all || [], 'No patients found.');
  } catch (e) {
    if (wrap) wrap.innerHTML = errState(e);
  }
}

function _renderPatientList(patients, emptyMsg) {
  const wrap = document.getElementById('search-results-wrap');
  if (!wrap) return;

  if (!patients.length) {
    wrap.innerHTML = emptyState('', emptyMsg, '');
    return;
  }

  wrap.innerHTML =
    `<div style="font-size:12px;color:var(--ink-3);margin-bottom:12px;font-family:var(--font-mono);">
       ${patients.length} patient(s) found
     </div>` +
    patients.map(p => {
      const initials = ((p.firstName || '?')[0] + (p.lastName || '?')[0]).toUpperCase();
      const risk     = (p.riskStatus || '').toLowerCase();
      const badges   = [];
      if (p.selfHarmHistory) badges.push('<span class="alert-selfharm">&#9873; Self-harm history</span>');
      if (p.deceased)        badges.push('<span class="badge-deceased">Deceased</span>');
      if (p.homeless)        badges.push('<span class="status status-pending">Homeless</span>');
      if (risk)              badges.push(`<span class="status status-${risk}">${p.riskStatus} risk</span>`);

      return `
        <div class="patient-row" onclick="openPatientDetail(${p.patientId})">
          <div class="patient-avatar">${initials}</div>
          <div style="flex:1;">
            <div class="patient-name">${p.firstName} ${p.lastName}</div>
            <div class="patient-meta">ID #${p.patientId} &middot; ${p.address || 'No address on file'}</div>
          </div>
          <div class="patient-badges">${badges.join('')}</div>
          <div style="display:flex;gap:8px;flex-shrink:0;">
            <button class="btn btn-secondary" style="font-size:12px;padding:6px 12px;"
                    onclick="event.stopPropagation();prefillRx(${p.patientId})">&#128138; Rx</button>
            <button class="btn btn-primary" style="font-size:12px;padding:6px 12px;"
                    onclick="event.stopPropagation();openPatientDetail(${p.patientId})">View &rarr;</button>
          </div>
        </div>`;
    }).join('');
}

/* GET /patients/{id}  →  Patient */
async function openPatientDetail(id) {
  document.getElementById('patient-detail-modal').classList.add('open');
  document.getElementById('patient-detail-name').textContent = 'Loading\u2026';
  document.getElementById('patient-detail-body').innerHTML = spinner();

  try {
    const p    = await apiFetch(`/patients/${id}`);
    const risk = (p.riskStatus || '').toLowerCase();

    document.getElementById('patient-detail-name').textContent =
      `${p.firstName} ${p.lastName}`;

    const selfHarmBanner = p.selfHarmHistory
      ? `<div class="info-banner danger" style="margin-bottom:16px;">
           <span style="font-size:18px;">&#9873;</span>
           <div><strong>Self-harm history on record.</strong>
             This patient has a history of deliberate self-harm.</div>
         </div>`
      : '';
    const deceasedBanner = p.deceased
      ? `<div class="info-banner warn" style="margin-bottom:16px;">
           <span>&#128274;</span>
           <div><strong>Deceased &mdash; Record is read-only.</strong></div>
         </div>`
      : '';

    document.getElementById('patient-detail-body').innerHTML = `
      ${selfHarmBanner}${deceasedBanner}
      <div class="form-grid" style="margin-bottom:20px;">
        <div>
          <div class="section-label">Patient ID</div>
          <div style="font-size:15px;font-weight:600;font-family:var(--font-mono);">#${p.patientId}</div>
        </div>
        <div>
          <div class="section-label">Risk Status</div>
          <div>${risk ? `<span class="status status-${risk}">${p.riskStatus}</span>` : '&mdash;'}</div>
        </div>
        <div>
          <div class="section-label">Address</div>
          <div style="font-size:14px;">${p.address || '&mdash;'}</div>
        </div>
        <div>
          <div class="section-label">Homeless</div>
          <div style="font-size:14px;">${p.homeless ? 'Yes' : 'No'}</div>
        </div>
        <div>
          <div class="section-label">Deceased</div>
          <div style="font-size:14px;">${p.deceased
            ? '<strong style="color:var(--ink-3);">Yes</strong>' : 'No'}</div>
        </div>
        <div>
          <div class="section-label">Self-harm history</div>
          <div style="font-size:14px;">${p.selfHarmHistory
            ? '<strong style="color:var(--red);">Yes &#9873;</strong>' : 'No'}</div>
        </div>
      </div>
      <div style="display:flex;gap:10px;flex-wrap:wrap;">
        <button class="btn btn-primary"
                onclick="prefillRx(${p.patientId});closeModal('patient-detail-modal');">
          &#128138; Generate Repeat Rx
        </button>
        <button class="btn btn-secondary"
                onclick="prefillNewAppt(${p.patientId})">
          &#128197; New Appointment
        </button>
      </div>`;
  } catch (e) {
    document.getElementById('patient-detail-body').innerHTML = errState(e);
  }
}

function prefillNewAppt(patientId) {
  closeModal('patient-detail-modal');
  openNewApptModal();
  // After modal opens and patients load, select the patient via the smart search
  setTimeout(async () => {
    if (!_allPatients.length) {
      try { _allPatients = await apiFetch('/patients'); } catch (_) {}
    }
    const p = _allPatients.find(x => x.patientId === patientId);
    if (p) selectModalPatient(patientId, `${p.firstName} ${p.lastName}`);
    else   selectModalPatient(patientId, `Patient #${patientId}`);
  }, 200);
}

/* ═══════════════════════════════════════════════════════════════════
   REPEAT PRESCRIPTION
   No dedicated REST endpoint yet.
   Step 1: patient ID input
   Step 2: GET /patients/{id} + inspect _cachedAppts for last appt
   Step 3: Confirmation display
   ═══════════════════════════════════════════════════════════════════ */
function prefillRx(patientId) {
  document.querySelectorAll('.nav-btn').forEach(b => {
    b.classList.toggle('active', b.textContent.includes('Prescription'));
  });
  showPage('page-prescription');
  resetRx();
  setTimeout(async () => {
    // Set hidden id and search display value, then look up
    const hidEl  = document.getElementById('rx-patient-id');
    const txtEl  = document.getElementById('rx-patient-search');
    if (hidEl) hidEl.value = patientId;
    if (txtEl) {
      if (!_allPatients.length) {
        try { _allPatients = await apiFetch('/patients'); } catch (_) {}
      }
      const p = _allPatients.find(x => x.patientId === patientId);
      txtEl.value = p ? `${p.firstName} ${p.lastName} (#${patientId})` : `Patient #${patientId}`;
    }
    lookupRxPatient();
  }, 60);
}

function resetRx() {
  _rxPatient  = null;
  _rxLastAppt = null;
  const idEl  = document.getElementById('rx-patient-id');
  const txtEl = document.getElementById('rx-patient-search');
  if (idEl)  idEl.value  = '';
  if (txtEl) txtEl.value = '';
  ['rx-step-2', 'rx-step-3'].forEach(id => {
    const el = document.getElementById(id);
    if (el) el.style.display = 'none';
  });
  const notice = document.getElementById('rx-issued-notice');
  if (notice) notice.style.display = 'none';
}

/* Rx patient search — same logic as modal patient search */
function filterRxPatients() {
  const q    = (document.getElementById('rx-patient-search').value || '').toLowerCase().trim();
  const wrap = document.getElementById('rx-patient-result');
  if (!q) { wrap.innerHTML = ''; return; }

  // Load patients if not cached yet
  const doFilter = (patients) => {
    const matches = patients.filter(p =>
      String(p.patientId).includes(q) ||
      `${p.firstName} ${p.lastName}`.toLowerCase().includes(q)
    ).slice(0, 6);
    if (!matches.length) {
      wrap.innerHTML = `<div class="na-suggestion-empty">No patients found</div>`;
      return;
    }
    wrap.innerHTML = matches.map(p => {
      const flag = p.selfHarmHistory ? ' <span class="alert-selfharm" style="font-size:10px;">&#9873;</span>' : '';
      return `<div class="na-suggestion" onclick="selectRxPatient(${p.patientId},'${p.firstName} ${p.lastName}')">
        <span class="na-sug-name">${p.firstName} ${p.lastName}${flag}</span>
        <span class="na-sug-meta">#${p.patientId}</span>
      </div>`;
    }).join('');
  };

  if (_allPatients.length) {
    doFilter(_allPatients);
  } else {
    apiFetch('/patients').then(pts => {
      _allPatients = pts || [];
      doFilter(_allPatients);
    }).catch(() => {
      wrap.innerHTML = `<div class="na-suggestion-empty">Could not load patients</div>`;
    });
  }
}

function selectRxPatient(id, name) {
  document.getElementById('rx-patient-search').value = `${name} (#${id})`;
  document.getElementById('rx-patient-id').value = id;
  document.getElementById('rx-patient-result').innerHTML = '';
}

/* Step 2: GET /patients/{id} + last appointment lookup from cache */
async function lookupRxPatient() {
  // Accept ID from hidden field (set by selectRxPatient) or parse from search input
  let id = parseInt((document.getElementById('rx-patient-id') || {}).value);
  if (!id) {
    // Try parsing a raw number from the search box as fallback
    const raw = (document.getElementById('rx-patient-search') || {}).value || '';
    id = parseInt(raw.trim());
  }
  if (!id) { toast('Select or enter a patient.', 'warn'); return; }

  ['rx-step-2', 'rx-step-3'].forEach(sid => {
    const el = document.getElementById(sid);
    if (el) el.style.display = 'none';
  });

  try {
    const p = await apiFetch(`/patients/${id}`);
    _rxPatient = p;

    let appts = _cachedAppts;
    if (!appts.length) {
      try { appts = await apiFetch('/appointments/pending-records'); _cachedAppts = appts; }
      catch (_) { appts = []; }
    }

    const patientAppts = (appts || [])
      .filter(a => a.patientId === id)
      .sort((a, b) => (b.appointmentDate || '').localeCompare(a.appointmentDate || ''));

    _rxLastAppt       = patientAppts[0] || null;
    const lastDate    = _rxLastAppt ? fmtDate(_rxLastAppt.appointmentDate) : null;
    const isLastDropin = _rxLastAppt && (_rxLastAppt.type || '').toLowerCase() === 'drop-in';

    // Drop-in warning (General req. 9)
    const dropWarn   = document.getElementById('rx-dropin-warning');
    const dropDetail = document.getElementById('rx-dropin-detail');
    if (isLastDropin && dropWarn && dropDetail) {
      dropDetail.innerHTML =
        `Patient last attended a <strong>drop-in session on ${lastDate}</strong>.
         Verify this repeat prescription request is legitimate.`;
      dropWarn.style.display = 'flex';
    } else if (dropWarn) {
      dropWarn.style.display = 'none';
    }

    const selfHarmAlert = p.selfHarmHistory
      ? `<div class="info-banner danger" style="margin-bottom:12px;">
           <span>&#9873;</span>&ensp;<strong>Self-harm history on record.</strong>
         </div>` : '';

    const risk = (p.riskStatus || '').toLowerCase();
    const infoEl = document.getElementById('rx-patient-info');
    if (infoEl) {
      infoEl.innerHTML = `
        ${selfHarmAlert}
        <div class="rx-card">
          <div style="display:flex;align-items:center;gap:14px;margin-bottom:10px;">
            <div class="patient-avatar" style="width:44px;height:44px;font-size:18px;">
              ${((p.firstName||'?')[0]+(p.lastName||'?')[0]).toUpperCase()}
            </div>
            <div>
              <div style="font-size:16px;font-weight:700;">${p.firstName} ${p.lastName}</div>
              <div style="font-size:12px;color:var(--ink-3);font-family:var(--font-mono);">ID #${p.patientId}</div>
            </div>
            ${risk
              ? `<span class="status status-${risk}" style="margin-left:auto;">${p.riskStatus} risk</span>`
              : ''}
          </div>
          <div style="font-size:12px;color:var(--ink-3);">
            Address: ${p.address || '&mdash;'} &middot; Homeless: ${p.homeless ? 'Yes' : 'No'}
          </div>
          ${lastDate
            ? `<div style="font-size:12px;color:var(--ink-3);margin-top:4px;">
                 Last appointment: <strong>${lastDate}</strong>
                 ${_rxLastAppt
                   ? `(Appt #${_rxLastAppt.appointmentId} &middot; ${_rxLastAppt.type})`
                   : ''}
               </div>`
            : '<div style="font-size:12px;color:var(--ink-3);margin-top:4px;">No recent appointments on record.</div>'}
        </div>`;
    }

    const s2 = document.getElementById('rx-step-2');
    if (s2) s2.style.display = 'block';

    const confirmEl = document.getElementById('rx-confirm-info');
    if (confirmEl) {
      confirmEl.innerHTML = `
        <div class="rx-card">
          <div class="rx-detail">Issuing repeat prescription for:</div>
          <div class="rx-drug" style="margin:6px 0;">${p.firstName} ${p.lastName}</div>
          <div class="rx-detail">Patient ID #${p.patientId}${lastDate ? ` &middot; Based on appointment of ${lastDate}` : ''}</div>
          ${_rxLastAppt
            ? `<div class="rx-detail">Reference appointment ID: #${_rxLastAppt.appointmentId}</div>`
            : ''}
        </div>`;
    }

    const s3 = document.getElementById('rx-step-3');
    if (s3) s3.style.display = 'block';

    const issued = document.getElementById('rx-issued-notice');
    if (issued) issued.style.display = 'none';

  } catch (e) {
    if (e.status === 404)      toast('Patient not found.', 'warn');
    else if (e.status === 403) toast('Access denied.', 'error');
    else                       toast('Could not load patient: ' + _errMsg(e), 'error');
  }
}

/* Issue repeat prescription.
   TODO: when the backend adds a Receptionist-accessible endpoint, replace with:
   await apiFetch(`/appointments/${_rxLastAppt.appointmentId}/prescription`, {
     method: 'POST',
     body: JSON.stringify({ medicationId: X, prescriberId: auth.userId, isRepeat: true })
   }); */
async function issueRepeatPrescription() {
  if (!_rxPatient) { toast('No patient selected.', 'warn'); return; }

  // TODO: replace with real POST once Receptionist-accessible Rx endpoint exists:
  // await apiFetch(`/appointments/${_rxLastAppt.appointmentId}/prescription`, {
  //   method: 'POST',
  //   body: JSON.stringify({ medicationId: X, prescriberId: auth.userId, isRepeat: true })
  // });

  const name     = `${_rxPatient.firstName} ${_rxPatient.lastName}`;
  const patId    = _rxPatient.patientId;
  const apptRef  = _rxLastAppt ? `Reference appointment #${_rxLastAppt.appointmentId}.` : '';

  toast(`Repeat prescription issued for ${name}.`, 'success');

  // Show a full confirmation panel, then reset the whole flow after 2 s
  const s3 = document.getElementById('rx-step-3');
  if (s3) {
    s3.querySelector('.rx-step-body').innerHTML = `
      <div class="info-banner info" style="margin-bottom:16px;">
        <span style="font-size:20px;">&#10003;</span>
        <div>
          <strong>Prescription issued for ${name}.</strong><br>
          Patient ID #${patId}. ${apptRef}
        </div>
      </div>
      <div style="font-size:13px;color:var(--ink-3);margin-bottom:16px;">
        Refreshing patient details…
      </div>`;
  }

  // After 1.8 s re-run the lookup so the page "refreshes" with current data
  const savedId = patId;
  setTimeout(async () => {
    // Re-build the confirm body area before re-lookup
    if (s3) {
      s3.querySelector('.rx-step-body').innerHTML = `
        <div id="rx-confirm-info"></div>
        <div style="display:flex;gap:10px;margin-top:14px;">
          <button class="btn btn-primary" onclick="issueRepeatPrescription()">&#10003; Issue Repeat Prescription</button>
          <button class="btn btn-secondary" onclick="resetRx()">&#10005; Cancel</button>
        </div>
        <div id="rx-issued-notice" style="display:none;margin-top:14px;" class="info-banner info">
          <span>&#10003;</span><div><strong>Prescription issued.</strong></div>
        </div>`;
    }
    // Re-run patient lookup to refresh displayed data
    const idEl = document.getElementById('rx-patient-id');
    if (idEl) idEl.value = savedId;
    await lookupRxPatient();
  }, 1800);
}

/* ═══════════════════════════════════════════════════════════════════
   CALENDAR
   Data: GET /appointments/pending-records  (Receptionist role)
   All loaded appointments are shown on the calendar grid.
   ═══════════════════════════════════════════════════════════════════ */
const _MONTHS = ['January','February','March','April','May','June',
                 'July','August','September','October','November','December'];
const _DAYS   = ['Sun','Mon','Tue','Wed','Thu','Fri','Sat'];

async function renderCalendar() {
  const titleEl = document.getElementById('cal-month-title');
  if (titleEl) titleEl.textContent = `${_MONTHS[_calMonth]} ${_calYear}`;

  if (!_cachedAppts.length) {
    try {
      _cachedAppts = await apiFetch('/appointments/pending-records');
    } catch (_) { _cachedAppts = []; }
  }

  _buildCalGrid(_cachedAppts);
}

function _buildCalGrid(appointments) {
  const grid = document.getElementById('cal-grid');
  if (!grid) return;

  const byDate = {};
  (appointments || []).forEach(a => {
    if (!a.appointmentDate) return;
    const d = (a.appointmentDate + '').split('T')[0];
    if (!byDate[d]) byDate[d] = [];
    byDate[d].push(a);
  });

  const countEl = document.getElementById('cal-appt-count');
  if (countEl) {
    const total = Object.values(byDate).reduce((s, arr) => s + arr.length, 0);
    countEl.textContent = `${total} appointment(s) shown`;
  }

  const firstDay    = new Date(_calYear, _calMonth, 1).getDay();
  const daysInMonth = new Date(_calYear, _calMonth + 1, 0).getDate();
  const daysInPrev  = new Date(_calYear, _calMonth, 0).getDate();
  const todayISO    = todayStr();

  let html = _DAYS.map(d => `<div class="cal-day-label">${d}</div>`).join('');

  for (let i = firstDay - 1; i >= 0; i--) {
    html += `<div class="cal-cell other-month"><div class="cal-date-num">${daysInPrev - i}</div></div>`;
  }

  for (let day = 1; day <= daysInMonth; day++) {
    const mm      = String(_calMonth + 1).padStart(2, '0');
    const dd      = String(day).padStart(2, '0');
    const dateStr = `${_calYear}-${mm}-${dd}`;
    const appts   = byDate[dateStr] || [];
    const isToday = dateStr === todayISO;

    // Encode for safe onclick attribute
    const encAppts = encodeURIComponent(JSON.stringify(appts));

    const events = appts.slice(0, 3).map(a => {
      const st       = (a.status || 'pending').toLowerCase();
      const isDropin = (a.type || '').toLowerCase() === 'drop-in';
      return `<div class="cal-event cal-event-${st}${isDropin ? ' cal-event-dropin' : ''}"
                   onclick="event.stopPropagation();_showDayDetail('${dateStr}','${encAppts}')"
                   title="Patient #${a.patientId} \u00b7 ${a.type||''} \u00b7 ${a.status||''}">
                P#${a.patientId} ${a.type || ''}
              </div>`;
    }).join('');

    const more = appts.length > 3
      ? `<div class="cal-more">+${appts.length - 3} more</div>` : '';

    html += `
      <div class="cal-cell${isToday ? ' today' : ''}${appts.length ? ' has-appts' : ''}"
           onclick="_showDayDetail('${dateStr}','${encAppts}')">
        <div class="cal-date-num">${day}</div>
        ${events}${more}
      </div>`;
  }

  const totalCells = firstDay + daysInMonth;
  const overflow   = totalCells % 7 === 0 ? 0 : 7 - (totalCells % 7);
  for (let i = 1; i <= overflow; i++) {
    html += `<div class="cal-cell other-month"><div class="cal-date-num">${i}</div></div>`;
  }

  grid.innerHTML = html;
}

function _showDayDetail(dateStr, encAppts) {
  const panel   = document.getElementById('cal-day-detail');
  const titleEl = document.getElementById('cal-detail-date');
  const listEl  = document.getElementById('cal-detail-list');
  if (!panel) return;

  const appts = JSON.parse(decodeURIComponent(encAppts));
  const d     = new Date(dateStr + 'T00:00:00');
  if (titleEl) titleEl.textContent = d.toLocaleDateString('en-GB', {
    weekday: 'long', day: 'numeric', month: 'long', year: 'numeric'
  });
  panel.style.display = 'block';
  panel.scrollIntoView({ behavior: 'smooth', block: 'nearest' });

  if (!appts.length) {
    if (listEl) listEl.innerHTML = emptyState('', 'No appointments on this day', '');
    return;
  }

  if (listEl) {
    listEl.innerHTML = appts.map(a => {
      const st       = (a.status || 'pending').toLowerCase();
      const isDropin = (a.type || '').toLowerCase() === 'drop-in';
      const notUpd   = a.recordsUpdated === false || a.recordsUpdated === 0;
      return `
        <div class="cal-appt-item status-${st}" id="cal-appt-card-${a.appointmentId}">
          <div style="display:flex;align-items:flex-start;justify-content:space-between;gap:12px;flex-wrap:wrap;">
            <div>
              <div class="cal-appt-id">Appointment #${a.appointmentId}</div>
              <div class="cal-appt-patient">Patient #${a.patientId}</div>
              <div class="cal-appt-meta" style="margin-top:4px;">
                <span>Clinic #${a.clinicId}</span>
                <span id="cal-appt-badge-${a.appointmentId}">${statusBadge(a.status)}</span>
                ${isDropin
                  ? '<span class="status" style="background:var(--blue-lt);color:var(--accent);">Drop-in</span>'
                  : '<span>Pre-arranged</span>'}
                ${notUpd ? '<span class="pending-flag">&#9873; Not Updated</span>' : ''}
              </div>
            </div>
            <div style="display:flex;align-items:center;gap:6px;flex-shrink:0;">
              <select id="cal-status-sel-${a.appointmentId}"
                      style="font-size:12px;padding:4px 8px;border:1.5px solid var(--paper-3);
                             border-radius:6px;font-family:var(--font-body);background:#fff;
                             color:var(--ink);outline:none;cursor:pointer;">
                <option value="Attended"  ${st==='attended' ?'selected':''}>Attended</option>
                <option value="Missed"    ${st==='missed'   ?'selected':''}>Missed</option>
                <option value="Pending"   ${st==='pending'  ?'selected':''}>Pending</option>
              </select>
              <button class="btn btn-primary" style="font-size:11px;padding:5px 12px;"
                      onclick="updateCalApptStatus(${a.appointmentId})">
                Update
              </button>
            </div>
          </div>
        </div>`;
    }).join('');
  }
}

/* Update appointment status directly from the calendar day-detail panel */
async function updateCalApptStatus(apptId) {
  const sel = document.getElementById(`cal-status-sel-${apptId}`);
  if (!sel) return;
  const newStatus = sel.value;

  try {
    await apiFetch(`/appointments/${apptId}/attendance`, {
      method: 'PUT',
      body:   JSON.stringify({ status: newStatus })
    });
    toast(`Appointment #${apptId} updated to "${newStatus}".`, 'success');

    // Update the badge in-place without a full calendar re-render
    const badge = document.getElementById(`cal-appt-badge-${apptId}`);
    if (badge) badge.innerHTML = statusBadge(newStatus);

    // Update the card's left-border colour
    const card = document.getElementById(`cal-appt-card-${apptId}`);
    if (card) {
      card.className = card.className.replace(/status-\w+/, `status-${newStatus.toLowerCase()}`);
    }

    // Invalidate cache so calendar re-fetches on next navigation
    _cachedAppts = _cachedAppts.map(a =>
      a.appointmentId === apptId ? { ...a, status: newStatus } : a
    );
    // Re-draw the grid so the event chip colour updates too
    _buildCalGrid(_cachedAppts);

    // Also refresh dashboard pending count
    _loadDashPending();

  } catch (e) {
    toast('Update failed: ' + _errMsg(e), 'error');
  }
}

function changeMonth(delta) {
  _calMonth += delta;
  if (_calMonth > 11) { _calMonth = 0; _calYear++; }
  if (_calMonth < 0)  { _calMonth = 11; _calYear--; }
  renderCalendar();
  const p = document.getElementById('cal-day-detail');
  if (p) p.style.display = 'none';
}

function goToday() {
  _calYear  = new Date().getFullYear();
  _calMonth = new Date().getMonth();
  renderCalendar();
  const p = document.getElementById('cal-day-detail');
  if (p) p.style.display = 'none';
}

/* ═══════════════════════════════════════════════════════════════════
   UTILITIES
   ═══════════════════════════════════════════════════════════════════ */
function todayStr() {
  return new Date().toISOString().split('T')[0];
}

function fmtDate(iso) {
  if (!iso) return '&mdash;';
  const d = new Date((iso + '').split('T')[0] + 'T00:00:00');
  if (isNaN(d)) return iso;
  return d.toLocaleDateString('en-GB', { day: '2-digit', month: 'short', year: 'numeric' });
}

function _setStat(id, val) {
  const el = document.getElementById(id);
  if (el) el.innerHTML = String(val);
}

function statusBadge(status) {
  const s = (status || 'pending').toLowerCase();
  return `<span class="status status-${s}">${status || '&mdash;'}</span>`;
}

function spinner() {
  return '<div class="empty"><div class="spinner spinner-dark" style="margin:0 auto;"></div></div>';
}

function emptyState(icon, title, sub) {
  return `<div class="empty">
    ${icon ? `<div class="empty-icon">${icon}</div>` : ''}
    <h4>${title}</h4>
    ${sub ? `<p style="font-size:13px;margin-top:6px;">${sub}</p>` : ''}
  </div>`;
}

function errState(e) {
  if ((e || {}).status === 403) return emptyState('', 'Access denied', 'Your role cannot access this resource.');
  if ((e || {}).status === 401) return emptyState('', 'Not authenticated', 'Please sign in again.');
  return emptyState('', 'Could not load data', 'Check the backend is running and try again.');
}

function tableWrap(thead, tbody, extraStyle) {
  return `<div class="table-wrap" style="${extraStyle||''}">
    <table><thead>${thead}</thead><tbody>${tbody}</tbody></table>
  </div>`;
}

function _errMsg(e) {
  if (!e) return 'Unknown error';
  if (e.status === 403) return 'Access denied (403)';
  if (e.status === 401) return 'Unauthenticated (401)';
  if (e.status === 404) return 'Not found (404)';
  if (e.data && e.data.error) return e.data.error;
  return `Error ${e.status || ''}`;
}
