/* ═══════════════════════════════════════════════════════════════════
   records.js  —  MHIS Medical Records Portal
   Requires: app.js (auth, apiFetch, toast, showPage, closeModal, etc.)

   LIVE ENDPOINTS:
     GET  /patients                              (Clinical+Receptionist+Medical_Records)
     GET  /patients/{id}                         (same)
     GET  /reports/patients-per-clinic
     GET  /reports/prescription-stats
     GET  /reports/change-requests               (NEW)
     POST /reports/change-requests
     PUT  /reports/change-requests/{id}/status   (NEW)
     GET  /records/clinics                       (NEW)
     GET  /records/users                         (NEW)
     PUT  /records/patients/{id}/deceased        (NEW)
     GET  /records/audit                         (NEW)
   ═══════════════════════════════════════════════════════════════════ */

let _allPatientsCache  = [];
let _deceasedPatientId = null;
let _prData            = [];

/* ── Init ── */
function initRecords() {
  if (!requireAuth('Medical_Records')) return;
  initShell();
  buildRecordsNav('dashboard');
  showPage('page-dashboard');
  _initDashHeader();
  loadDashboard();
  _setWeeklyDefault();
  _setAuditDefaults();
}

function _initDashHeader() {
  const h = new Date().getHours();
  const g = h<12?'Good morning':h<17?'Good afternoon':'Good evening';
  const el = document.getElementById('dash-greeting');
  if (el) el.textContent = `${g}, ${auth.user}.`;
  const sub = document.getElementById('dash-date-sub');
  if (sub) sub.textContent = new Date().toLocaleDateString('en-GB',{weekday:'long',year:'numeric',month:'long',day:'numeric'});
}

/* ── Navigation ── */
function buildRecordsNav(activeId) {
  const nav = document.getElementById('main-nav');
  if (!nav) return;
  nav.innerHTML = '';
  const items = [
    {id:'dashboard',       label:'Dashboard',      fn:() => { showPage('page-dashboard');       _initDashHeader(); loadDashboard(); }},
    {id:'reports',         label:'Reports',         fn:() => { showPage('page-reports');         loadAllReports(); }},
    {id:'weekly',          label:'Weekly Report',   fn:() => { showPage('page-weekly'); _populateWeeklyClinics(); }},
    {id:'change-requests', label:'Change Requests', fn:() => { showPage('page-change-requests'); loadChangeRequests(); }},
    {id:'access',          label:'Access Control',  fn:() => { showPage('page-access');          loadAccessUsers(); }},
    {id:'patient-records', label:'Patient Records', fn:() => { showPage('page-patient-records'); }},
    {id:'audit',           label:'Audit Log',       fn:() => { showPage('page-audit');           loadAuditLog(); }},
  ];
  items.forEach(item => {
    const btn = document.createElement('button');
    btn.className = 'nav-btn' + (item.id===activeId?' active':'');
    btn.textContent = item.label;
    btn.onclick = () => {
      document.querySelectorAll('.nav-btn').forEach(b=>b.classList.remove('active'));
      btn.classList.add('active');
      item.fn();
    };
    nav.appendChild(btn);
  });
}

function navTo(id) {
  const map = {
    'dashboard':       ()=>{ showPage('page-dashboard');       _initDashHeader(); loadDashboard(); },
    'reports':         ()=>{ showPage('page-reports');         loadAllReports(); },
    'weekly':          ()=>{ showPage('page-weekly'); _populateWeeklyClinics(); },
    'change-requests': ()=>{ showPage('page-change-requests'); loadChangeRequests(); },
    'access':          ()=>{ showPage('page-access');          loadAccessUsers(); },
    'patient-records': ()=>{ showPage('page-patient-records'); },
    'audit':           ()=>{ showPage('page-audit');           loadAuditLog(); },
  };
  const ids = Object.keys(map);
  document.querySelectorAll('.nav-btn').forEach((b,i) => b.classList.toggle('active', ids[i]===id));
  if (map[id]) map[id]();
}

/* ── Dashboard ── */
async function loadDashboard() {
  try {
    const clinics = await apiFetch('/reports/patients-per-clinic');
    _setStat('ds-clinics',  clinics.length);
    _setStat('ds-patients', clinics.reduce((s,r)=>s+(r.patientCount||0),0));
  } catch(_) { _setStat('ds-clinics','—'); _setStat('ds-patients','—'); }

  try {
    const rx = await apiFetch('/reports/prescription-stats');
    _setStat('ds-meds', rx.length);
  } catch(_) { _setStat('ds-meds','—'); }

  try {
    const crs = await apiFetch('/reports/change-requests');
    _setStat('ds-cr', crs.filter(r=>r.status==='Pending').length);
  } catch(_) { _setStat('ds-cr','—'); }

  _buildDashActivity();
}

function _buildDashActivity() {
  const el = document.getElementById('dash-activity');
  if (!el) return;
  const now   = new Date().toLocaleTimeString('en-GB',{hour:'2-digit',minute:'2-digit'});
  const today = new Date().toLocaleDateString('en-GB');
  el.innerHTML = [
    {color:'blue',  text:`Session started by <strong>${auth.user}</strong> (Medical_Records).`, time:today},
    {color:'green', text:'Reports data loaded from central server.',                              time:now},
    {color:'amber', text:'Daily missed-appointment emails sent automatically.',                   time:now},
    {color:'blue',  text:'Transaction log updated with current session.',                         time:now},
  ].map(i=>`<div class="activity-item">
    <div class="activity-dot ${i.color}"></div>
    <div><div class="activity-text">${i.text}</div><div class="activity-time">${i.time}</div></div>
  </div>`).join('');
}

/* ── Reports ── */
async function loadAllReports() { loadClinicReport(); loadPrescriptionReport(); loadConditionReport(); }

async function loadClinicReport() {
  const el = document.getElementById('clinic-chart');
  if (el) el.innerHTML = _spinner();
  try {
    const data = await apiFetch('/reports/patients-per-clinic');
    _setStat('stat-clinic-count',  data.length);
    _setStat('stat-patient-total', data.reduce((s,r)=>s+(r.patientCount||0),0));
    _setStat('ds-clinics',  data.length);
    _setStat('ds-patients', data.reduce((s,r)=>s+(r.patientCount||0),0));
    if (!data.length) { if (el) el.innerHTML=_empty('No clinic data.'); return; }
    const max = Math.max(...data.map(r=>r.patientCount));
    if (el) el.innerHTML = '<div class="bar-chart-v2">'+data.map(r=>{
      const pct = max?Math.round(r.patientCount/max*100):0;
      return `<div class="bar-row-v2">
        <div class="bar-label-v2">${_esc(r.clinicName||'Clinic #'+r.clinicId)}</div>
        <div class="bar-track-v2"><div class="bar-fill-v2" style="width:${pct}%"><span>${r.patientCount}</span></div></div>
      </div>`;
    }).join('')+'</div>';
  } catch(e) {
    if (el) el.innerHTML=_errState(e);
    _setStat('stat-clinic-count','—'); _setStat('stat-patient-total','—');
  }
}

async function loadPrescriptionReport() {
  const el = document.getElementById('prescription-chart');
  if (el) el.innerHTML = _spinner();
  try {
    const data = await apiFetch('/reports/prescription-stats');
    _setStat('stat-med-count', data.length);
    _setStat('ds-meds', data.length);
    if (!data.length) { if (el) el.innerHTML=_empty('No prescription data.'); return; }
    const max = Math.max(...data.map(r=>r.prescriptionCount));
    if (el) el.innerHTML = '<div class="bar-chart-v2">'+data.map(r=>{
      const pct = max?Math.round(r.prescriptionCount/max*100):0;
      return `<div class="bar-row-v2">
        <div class="bar-label-v2">${_esc(r.medicationName)}</div>
        <div class="bar-track-v2"><div class="bar-fill-v2 green" style="width:${Math.max(pct,4)}%"><span>${r.prescriptionCount}</span></div></div>
      </div>`;
    }).join('')+'</div>';
  } catch(e) {
    if (el) el.innerHTML=_errState(e);
    _setStat('stat-med-count','—');
  }
}

async function loadConditionReport() {
  const el = document.getElementById('condition-chart');
  if (el) el.innerHTML = _spinner();
  try {
    const data = await apiFetch('/reports/condition-stats');
    // Only show conditions that have at least one patient
    const active = data.filter(r => r.patientCount > 0);
    _setStat('stat-condition-count', active.length);
    if (!active.length) { if (el) el.innerHTML = _empty('No condition data available.'); return; }
    const max = Math.max(...active.map(r => r.patientCount));
    if (el) el.innerHTML = '<div class="bar-chart-v2">' + active.map(r => {
      const pct = max ? Math.round(r.patientCount / max * 100) : 0;
      return `<div class="bar-row-v2">
        <div class="bar-label-v2" title="${_esc(r.conditionName)}">${_esc(r.conditionName)}</div>
        <div class="bar-track-v2"><div class="bar-fill-v2 amber" style="width:${Math.max(pct,4)}%"><span>${r.patientCount}</span></div></div>
      </div>`;
    }).join('') + '</div>';
  } catch(e) {
    if (el) el.innerHTML = _errState(e);
    _setStat('stat-condition-count','—');
  }
}

async function _populateWeeklyClinics() {
  const sel = document.getElementById('weekly-clinic-filter');
  if (!sel || sel.options.length > 1) return; // already populated
  try {
    const clinics = await apiFetch('/records/clinics');
    clinics.forEach(c => {
      const opt = document.createElement('option');
      opt.value = c.clinicId;
      opt.textContent = c.name;
      sel.appendChild(opt);
    });
  } catch(_) {}
}

/* ── Weekly Report ── */
function _setWeeklyDefault() {
  const el = document.getElementById('weekly-date');
  if (!el) return;
  const d=new Date(), day=d.getDay(), diff=d.getDate()-day+(day===0?-6:1);
  d.setDate(diff); el.value=d.toISOString().split('T')[0];
}

async function loadWeeklyReport() {
  const dateVal    = (document.getElementById('weekly-date')        ||{}).value;
  const clinicId   = (document.getElementById('weekly-clinic-filter')||{}).value || '0';
  const clinicName = clinicId === '0' ? 'All Clinics'
    : (document.getElementById('weekly-clinic-filter').selectedOptions[0]||{}).text || `Clinic #${clinicId}`;

  if (!dateVal) { toast('Select a date first.','warn'); return; }
  const wrap = document.getElementById('weekly-report-wrap');
  if (wrap) wrap.innerHTML = _spinner();

  try {
    // Compute Mon–Fri ISO strings for the selected week
    const base = new Date(dateVal+'T00:00:00'), dow = base.getDay();
    const mon  = new Date(base); mon.setDate(base.getDate() - (dow===0?6:dow-1));
    const days = Array.from({length:5}, (_,i) => {
      const d = new Date(mon); d.setDate(mon.getDate()+i);
      return {
        label: d.toLocaleDateString('en-GB',{weekday:'short',day:'numeric',month:'short'}),
        iso:   d.toISOString().split('T')[0]
      };
    });
    const weekFrom = days[0].iso;
    const weekTo   = days[4].iso;
    const cParam   = clinicId !== '0' ? `&clinicId=${clinicId}` : '';

    // Fetch all weekly data in parallel — all 3 new endpoints + clinic list
    const [allAppts, weekRx, weekConditions, clinicDetails] = await Promise.all([
      apiFetch(`/records/weekly?from=${weekFrom}&to=${weekTo}${cParam}`).catch(()=>[]),
      apiFetch(`/records/weekly/prescriptions?from=${weekFrom}&to=${weekTo}${cParam}`).catch(()=>[]),
      apiFetch(`/records/weekly/conditions?from=${weekFrom}&to=${weekTo}${cParam}`).catch(()=>[]),
      apiFetch('/records/clinics').catch(()=>[])
    ]);

    const nameMap = {};
    clinicDetails.forEach(c => { nameMap[c.clinicId] = c.name; });

    // Per-day counts from appointment data
    const dayCounts = days.map(d => {
      const dayAppts = (allAppts||[]).filter(a => (a.appointmentDate+'').split('T')[0] === d.iso);
      return {
        label:    d.label,
        iso:      d.iso,
        attended: dayAppts.filter(a=>(a.status||'').toLowerCase()==='attended').length,
        missed:   dayAppts.filter(a=>(a.status||'').toLowerCase()==='missed').length,
        dropin:   dayAppts.filter(a=>(a.type||'').toLowerCase()==='drop-in').length,
        total:    dayAppts.length
      };
    });

    const weekTotal    = dayCounts.reduce((s,d)=>s+d.total,0);
    const weekAttended = dayCounts.reduce((s,d)=>s+d.attended,0);
    const weekMissed   = dayCounts.reduce((s,d)=>s+d.missed,0);
    const weekDropin   = dayCounts.reduce((s,d)=>s+d.dropin,0);

    // Clinic breakdown for this week
    const clinicWeekMap = {};
    (allAppts||[]).forEach(a => {
      const n = nameMap[a.clinicId] || 'Clinic #'+a.clinicId;
      if (!clinicWeekMap[n]) clinicWeekMap[n] = 0;
      clinicWeekMap[n]++;
    });

    const noWeekData = weekTotal === 0;

    // ── Bar chart HTML helpers ──
    const barChart = (rows, valueKey, labelKey, colorClass='') => {
      if (!rows || !rows.length) return '<div style="font-size:12px;color:var(--ink-3);padding:8px 0;">No data for this period.</div>';
      const max = Math.max(...rows.map(r=>r[valueKey]));
      return '<div class="bar-chart-v2">'+rows.map(r=>{
        const pct = max ? Math.round(r[valueKey]/max*100) : 0;
        return `<div class="bar-row-v2">
          <div class="bar-label-v2" title="${_esc(r[labelKey])}">${_esc(r[labelKey])}</div>
          <div class="bar-track-v2"><div class="bar-fill-v2 ${colorClass}" style="width:${Math.max(pct,4)}%"><span>${r[valueKey]}</span></div></div>
        </div>`;
      }).join('')+'</div>';
    };

    if (wrap) wrap.innerHTML = `
      <div style="display:flex;align-items:center;justify-content:space-between;margin-bottom:20px;flex-wrap:wrap;gap:12px;">
        <div>
          <h3 style="font-family:var(--font-head);font-style:italic;font-size:22px;color:var(--ink);">
            ${_esc(clinicName)} &mdash; Week of ${days[0].label} &ndash; ${days[4].label}
          </h3>
          <div style="font-size:12px;color:var(--ink-3);font-family:var(--font-mono);margin-top:4px;">
            Generated ${new Date().toLocaleDateString('en-GB')} &middot; No individual patient identifiers (General req. 14)
          </div>
        </div>
        <button class="btn btn-secondary" onclick="window.print()">Print Report</button>
      </div>

      ${noWeekData ? `<div style="background:var(--amber-lt);border:1px solid #f5d88e;border-radius:8px;padding:12px 16px;margin-bottom:20px;font-size:13px;color:var(--amber);display:flex;gap:10px;">
        <span>&#9432;</span>
        <div>No appointments found for this week${clinicId!=='0'?' at '+_esc(clinicName):''}. Try a different week or check that seed data covers this period.</div>
      </div>` : ''}

      <div style="display:grid;grid-template-columns:repeat(4,1fr);gap:16px;margin-bottom:24px;">
        <div class="rec-stat blue"><div class="rec-stat-icon">&#128197;</div>
          <div class="rec-stat-value">${weekTotal}</div>
          <div class="rec-stat-label">Total Appointments</div><div class="rec-stat-sub">${weekFrom} &rarr; ${weekTo}</div></div>
        <div class="rec-stat green"><div class="rec-stat-icon">&#10003;</div>
          <div class="rec-stat-value">${weekAttended}</div>
          <div class="rec-stat-label">Attended</div><div class="rec-stat-sub">This week</div></div>
        <div class="rec-stat red"><div class="rec-stat-icon">&#10005;</div>
          <div class="rec-stat-value">${weekMissed}</div>
          <div class="rec-stat-label">Missed</div><div class="rec-stat-sub">This week</div></div>
        <div class="rec-stat amber"><div class="rec-stat-icon">&#8645;</div>
          <div class="rec-stat-value">${weekDropin}</div>
          <div class="rec-stat-label">Drop-in</div><div class="rec-stat-sub">This week</div></div>
      </div>

      <div class="card" style="margin-bottom:20px;">
        <div class="card-title">Daily Attendance &mdash; ${days[0].label} to ${days[4].label}</div>
        <div class="week-grid" style="margin-top:14px;">
          ${dayCounts.map(d=>`
            <div class="week-cell">
              <div class="week-day">${d.label}</div>
              <div class="week-count">${d.total}</div>
              <div class="week-sub">appointments</div>
              <div style="margin-top:8px;font-size:10px;color:var(--ink-3);font-family:var(--font-mono);line-height:2;">
                <div style="color:var(--green);">&#10003; ${d.attended} attended</div>
                <div style="color:var(--red);">&#10005; ${d.missed} missed</div>
                <div style="color:var(--accent);">&#8645; ${d.dropin} drop-in</div>
              </div>
            </div>`).join('')}
        </div>
      </div>

      <div style="display:grid;grid-template-columns:1fr 1fr;gap:20px;margin-bottom:20px;">
        ${clinicId === '0' ? `
        <div class="card">
          <div class="card-title">Patients by Clinic (This Week)</div>
          <div style="margin-top:12px;">
            ${Object.keys(clinicWeekMap).length
              ? barChart(Object.entries(clinicWeekMap).map(([name,cnt])=>({name,cnt})), 'cnt', 'name')
              : '<div style="font-size:12px;color:var(--ink-3);">No appointment data for this week.</div>'}
          </div>
        </div>` : `
        <div class="card">
          <div class="card-title">Appointment Breakdown &mdash; ${_esc(clinicName)}</div>
          <div style="margin-top:16px;display:flex;flex-direction:column;gap:12px;">
            ${[
              {label:'Attended',  val:weekAttended, color:'var(--green)'},
              {label:'Missed',    val:weekMissed,   color:'var(--red)'},
              {label:'Drop-in',   val:weekDropin,   color:'var(--accent)'},
              {label:'Pre-arranged', val:weekTotal-weekDropin, color:'var(--ink-3)'},
            ].map(r=>`<div class="bar-row-v2">
              <div class="bar-label-v2">${r.label}</div>
              <div class="bar-track-v2"><div class="bar-fill-v2" style="width:${weekTotal?Math.max(Math.round(r.val/weekTotal*100),r.val>0?4:0):0}%;background:${r.color}"><span>${r.val}</span></div></div>
            </div>`).join('')}
          </div>
        </div>`}

        <div class="card">
          <div class="card-title">Drug Prescriptions This Week</div>
          <div style="margin-top:12px;">
            ${barChart(weekRx, 'prescriptionCount', 'medicationName', 'green')}
          </div>
        </div>
      </div>

      <div class="card" style="margin-bottom:20px;">
        <div class="card-title">Conditions &mdash; Patients by Diagnosis (This Week)</div>
        <p style="font-size:12px;color:var(--ink-3);margin-bottom:12px;">Distinct patients with a recorded condition linked to an appointment in this period.</p>
        <div style="margin-top:4px;">
          ${barChart(weekConditions, 'patientCount', 'conditionName', 'amber')}
        </div>
      </div>

      <div class="card">
        <div class="card-title">Report Summary</div>
        <div style="display:grid;grid-template-columns:repeat(2,1fr);gap:16px;margin-top:12px;">
          <div><div class="section-label">Total Appointments This Week</div>
            <div style="font-size:28px;font-family:var(--font-head);color:var(--ink);">${weekTotal}</div></div>
          <div><div class="section-label">Attended</div>
            <div style="font-size:28px;font-family:var(--font-head);color:var(--green);">${weekAttended}</div></div>
          <div><div class="section-label">Missed</div>
            <div style="font-size:28px;font-family:var(--font-head);color:var(--red);">${weekMissed}</div></div>
          <div><div class="section-label">Prescriptions Issued</div>
            <div style="font-size:28px;font-family:var(--font-head);color:var(--accent);">${weekRx.reduce((s,r)=>s+r.prescriptionCount,0)}</div></div>
          <div><div class="section-label">Conditions Recorded</div>
            <div style="font-size:28px;font-family:var(--font-head);color:var(--ink);">${weekConditions.length}</div></div>
          <div><div class="section-label">Drop-in Sessions</div>
            <div style="font-size:28px;font-family:var(--font-head);color:var(--ink);">${weekDropin}</div></div>
        </div>
        <div style="margin-top:16px;padding:12px 14px;background:var(--paper-2);border-radius:8px;font-size:12px;color:var(--ink-3);">
          This report contains <strong>no individual patient identifiers</strong>. All figures are anonymised aggregates. (General req. 14)
        </div>
      </div>`;
  } catch(e) {
    if (wrap) wrap.innerHTML = _errState(e);
  }
}

/* ── Change Requests ── */
async function loadChangeRequests() {
  const el     = document.getElementById('cr-list');
  const notice = document.getElementById('cr-pending-notice');
  const noticeText = document.getElementById('cr-pending-text');
  if (el) el.innerHTML = _spinner();

  try {
    const crs     = await apiFetch('/reports/change-requests');
    const pending = crs.filter(r=>r.status==='Pending');
    if (notice&&noticeText) {
      notice.style.display = pending.length>0?'flex':'none';
      noticeText.textContent = `${pending.length} change request(s) pending review.`;
    }
    _setStat('ds-cr', pending.length);

    if (!crs.length) { if (el) el.innerHTML=_empty('No change requests on record.'); return; }

    if (el) el.innerHTML = crs.map(cr=>{
      const isPending = cr.status==='Pending';
      const stCls = cr.status==='Accepted'?'status-attended':cr.status==='Rejected'?'status-missed':'status-pending';
      return `<div class="cr-card">
        <div class="cr-card-header">
          <div>
            <span style="font-family:var(--font-mono);font-size:12px;color:var(--ink-3);">Request #${cr.requestId}</span>
            <span class="status ${stCls}" style="margin-left:10px;">${cr.status}</span>
          </div>
        </div>
        <div class="cr-card-body">
          <div class="cr-field"><label>Patient Data Snapshot</label><p>${_esc(cr.rawPatientData)}</p></div>
          <div class="cr-field"><label>Requested Changes</label><p>${_esc(cr.requestedChanges)}</p></div>
          <div style="font-size:11px;color:var(--ink-3);font-family:var(--font-mono);margin-top:6px;">Per General req. 17 — no patient_id FK stored on this record.</div>
          ${isPending?`<div class="cr-actions">
            <button class="btn btn-primary" style="font-size:12px;" onclick="updateCrStatus(${cr.requestId},'Accepted')">Accept</button>
            <button class="btn btn-danger"  style="font-size:12px;" onclick="updateCrStatus(${cr.requestId},'Rejected')">Reject</button>
          </div>`:''}
        </div>
      </div>`;
    }).join('');
  } catch(e) {
    if (el) el.innerHTML=_errState(e);
  }
}

async function updateCrStatus(id, status) {
  try {
    await apiFetch(`/reports/change-requests/${id}/status`,{method:'PUT',body:JSON.stringify({status})});
    toast(`Request #${id} ${status.toLowerCase()}.`,'success');
    loadChangeRequests(); loadDashboard();
  } catch(e) { toast('Failed: '+_errMsg(e),'error'); }
}

async function submitChangeRequest() {
  const raw     = (document.getElementById('cr-raw')    ||{value:''}).value.trim();
  const changes = (document.getElementById('cr-changes')||{value:''}).value.trim();
  if (!raw||!changes) { toast('Fill in both fields.','warn'); return; }
  try {
    const res = await apiFetch('/reports/change-requests',{method:'POST',body:JSON.stringify({rawPatientData:raw,requestedChanges:changes})});
    toast(`Change request #${res.requestId} submitted.`,'success');
    const r=document.getElementById('cr-raw');    if(r) r.value='';
    const c=document.getElementById('cr-changes');if(c) c.value='';
    loadChangeRequests();
  } catch(e) { toast('Failed: '+_errMsg(e),'error'); }
}

function openNewCrModal() {
  const m=document.getElementById('new-cr-modal'); if(m) m.classList.add('open');
}

async function submitModalChangeRequest() {
  const raw     = (document.getElementById('modal-cr-raw')    ||{value:''}).value.trim();
  const changes = (document.getElementById('modal-cr-changes')||{value:''}).value.trim();
  if (!raw||!changes) { toast('Fill in both fields.','warn'); return; }
  try {
    const res = await apiFetch('/reports/change-requests',{method:'POST',body:JSON.stringify({rawPatientData:raw,requestedChanges:changes})});
    toast(`Change request #${res.requestId} submitted.`,'success');
    closeModal('new-cr-modal');
    const mr=document.getElementById('modal-cr-raw');    if(mr) mr.value='';
    const mc=document.getElementById('modal-cr-changes');if(mc) mc.value='';
    loadChangeRequests();
  } catch(e) { toast('Failed: '+_errMsg(e),'error'); }
}

/* ── Access Control — GET /records/users ── */
async function loadAccessUsers() {
  const el = document.getElementById('access-user-list');
  if (el) el.innerHTML = _spinner();
  try {
    const users = await apiFetch('/records/users');
    if (!users.length) { if(el) el.innerHTML=_empty('No users found.'); return; }
    const rc = r => r==='Clinical'?'role-badge-clinical':r==='Receptionist'?'role-badge-receptionist':'role-badge-records';
    if (el) el.innerHTML = `
      <div class="table-wrap"><table>
        <thead><tr><th>User ID</th><th>Username</th><th>Role</th></tr></thead>
        <tbody>${users.map(u=>`
          <tr>
            <td style="font-family:var(--font-mono);font-size:12px;">#${u.userId}</td>
            <td style="font-family:var(--font-mono);">${_esc(u.username)}</td>
            <td><span class="role-badge ${rc(u.role)}">${u.role}</span></td>
          </tr>`).join('')}
        </tbody>
      </table></div>
      <div style="font-size:11px;color:var(--ink-3);margin-top:10px;font-family:var(--font-mono);">
        Passwords are not transmitted. Role access is enforced server-side via Authz.require() on every request.
      </div>`;
  } catch(e) {
    if (el) el.innerHTML = _errState(e);
  }
}

/* ── Patient Records — GET /patients (now allows Medical_Records) ── */
async function filterDeceasedSearch() {
  const q    = (document.getElementById('deceased-patient-search').value||'').toLowerCase().trim();
  const wrap = document.getElementById('deceased-patient-result');
  if (!wrap) return;
  if (!q) { wrap.style.display='none'; wrap.innerHTML=''; return; }

  if (!_allPatientsCache.length) {
    try { _allPatientsCache = await apiFetch('/patients'); } catch(_) { return; }
  }

  // Show ALL patients including deceased — Medical Records can VIEW deceased (just can't unlock)
  const matches = _allPatientsCache.filter(p =>
    String(p.patientId).includes(q) ||
    `${p.firstName} ${p.lastName}`.toLowerCase().includes(q)
  ).slice(0, 8);

  if (!matches.length) {
    wrap.innerHTML=`<div style="padding:10px 14px;font-size:12px;color:var(--ink-3);font-style:italic;">No patients found</div>`;
    wrap.style.display='block'; return;
  }

  wrap.innerHTML = matches.map(p=>{
    const lockIcon = p.deceased?' &#128274;':'';
    const sub      = p.deceased?' &middot; Deceased (read-only)':'';
    return `<div class="na-suggestion" onclick="selectDeceasedPatient(${p.patientId},'${_esc(p.firstName+' '+p.lastName)}',${!!p.deceased})">
      <span class="na-sug-name">${_esc(p.firstName+' '+p.lastName)}${lockIcon}</span>
      <span class="na-sug-meta">#${p.patientId}${sub}</span>
    </div>`;
  }).join('');
  wrap.style.display='block';
}

function selectDeceasedPatient(id, name, isDeceased) {
  _deceasedPatientId = id;
  const searchEl=document.getElementById('deceased-patient-search');
  if (searchEl) searchEl.value=`${name} (#${id})`;
  const wrap=document.getElementById('deceased-patient-result');
  if (wrap) { wrap.style.display='none'; wrap.innerHTML=''; }

  const infoEl=document.getElementById('deceased-selected-info');
  const nameEl=document.getElementById('deceased-selected-name');
  const metaEl=document.getElementById('deceased-selected-meta');
  if (infoEl) infoEl.style.display='block';
  if (nameEl) nameEl.textContent=name;
  if (metaEl) metaEl.textContent=isDeceased
    ? `Patient ID #${id} — Already deceased (record is read-only)`
    : `Patient ID #${id} — Living`;

  const btn=document.getElementById('deceased-lock-btn');
  if (btn) btn.style.display = isDeceased?'none':'inline-flex';

  const alreadyEl=document.getElementById('deceased-already-note');
  if (alreadyEl) alreadyEl.style.display = isDeceased?'flex':'none';

  const confirm=document.getElementById('deceased-confirm');
  if (confirm) confirm.style.display='none';
}

function lockDeceasedRecord() {
  if (!_deceasedPatientId) return;
  const name=(document.getElementById('deceased-selected-name')||{}).textContent||`Patient #${_deceasedPatientId}`;
  const mn=document.getElementById('lock-patient-name-modal');
  if (mn) mn.textContent=name;
  document.getElementById('patient-lock-confirm-modal').classList.add('open');
}

/* PUT /records/patients/{id}/deceased — Medical_Records only */
async function confirmLock() {
  closeModal('patient-lock-confirm-modal');
  const id=_deceasedPatientId; if (!id) return;
  try {
    const updated=await apiFetch(`/records/patients/${id}/deceased`,{method:'PUT'});
    toast(`Patient #${id} locked as deceased.`,'success');
    const confirm=document.getElementById('deceased-confirm');
    const ct=document.getElementById('deceased-confirm-text');
    if (confirm) confirm.style.display='flex';
    if (ct) ct.textContent=`Record for ${updated.firstName} ${updated.lastName} (ID #${id}) is now locked as deceased and read-only. (General req. 18)`;
    const btn=document.getElementById('deceased-lock-btn'); if(btn) btn.style.display='none';
    _allPatientsCache=_allPatientsCache.map(p=>p.patientId===id?{...p,deceased:true}:p);
    if (_prData.length) filterPatientRecords();
  } catch(e) { toast('Failed: '+_errMsg(e),'error'); }
}

async function loadPatientRecords() {
  const el=document.getElementById('pr-list');
  if (el) el.innerHTML=_spinner();
  try {
    _prData=await apiFetch('/patients');
    _allPatientsCache=_prData;
    _renderPatientRecords(_prData);
  } catch(e) { if(el) el.innerHTML=_errState(e); }
}

function filterPatientRecords() {
  const q=(document.getElementById('pr-search').value||'').toLowerCase().trim();
  if (!_prData.length) return;
  const filtered=!q?_prData:_prData.filter(p=>String(p.patientId).includes(q)||`${p.firstName} ${p.lastName}`.toLowerCase().includes(q));
  _renderPatientRecords(filtered);
}

function _renderPatientRecords(patients) {
  const el=document.getElementById('pr-list'); if(!el) return;
  if (!patients.length) { el.innerHTML=_empty('No patients found.'); return; }
  const total=patients.length, deceased=patients.filter(p=>p.deceased).length, sh=patients.filter(p=>p.selfHarmHistory).length;
  el.innerHTML=
    `<div style="font-size:12px;color:var(--ink-3);margin-bottom:10px;font-family:var(--font-mono);">
       ${total} patient(s) &middot; ${deceased} deceased &middot; ${sh} self-harm history
     </div>
     <div class="table-wrap"><table>
       <thead><tr><th>ID</th><th>Name</th><th>Risk</th><th>Flags</th><th>Address</th></tr></thead>
       <tbody>`+
    patients.map(p=>{
      const risk=(p.riskStatus||'').toLowerCase();
      const flags=[];
      if(p.selfHarmHistory) flags.push('<span class="alert-selfharm" style="font-size:11px;">&#9873; Self-harm</span>');
      if(p.deceased)        flags.push('<span class="status" style="background:#f0f0f0;color:#666;font-size:10px;">&#128274; Deceased</span>');
      if(p.homeless)        flags.push('<span class="status status-pending" style="font-size:10px;">Homeless</span>');
      return `<tr${p.deceased?' style="opacity:.55;"':''}${p.selfHarmHistory?' class="flag-row"':''}>
        <td><span style="font-family:var(--font-mono);font-size:12px;">#${p.patientId}</span></td>
        <td><strong>${_esc(p.firstName+' '+p.lastName)}</strong>${p.deceased?' <span style="font-size:10px;color:var(--ink-3);">(read-only)</span>':''}</td>
        <td>${risk?`<span class="status status-${risk}">${p.riskStatus}</span>`:'&mdash;'}</td>
        <td><div style="display:flex;gap:4px;flex-wrap:wrap;align-items:center;">${flags.join('')||'&mdash;'}</div></td>
        <td style="font-size:12px;color:var(--ink-3);">${_esc(p.address||'')||'&mdash;'}</td>
      </tr>`;
    }).join('')+
    '</tbody></table></div>';
}

/* ── Audit Log — GET /records/audit ── */
function _setAuditDefaults() {
  const today = new Date().toISOString().split('T')[0];
  const from  = document.getElementById('audit-from'); if(from) from.value = today;
  const to    = document.getElementById('audit-to');   if(to)   to.value   = today;
  // Populate clinic dropdown
  _populateAuditClinics();
}

async function _populateAuditClinics() {
  try {
    const clinics = await apiFetch('/records/clinics');
    const sel = document.getElementById('audit-clinic');
    if (!sel || !clinics.length) return;
    clinics.forEach(c => {
      const opt = document.createElement('option');
      opt.value = c.clinicId;
      opt.textContent = c.name;
      sel.appendChild(opt);
    });
  } catch(_) {}
}

async function loadAuditLog() {
  const wrap = document.getElementById('audit-log-wrap');
  if (wrap) wrap.innerHTML = _spinner();
  try {
    const fromVal    = (document.getElementById('audit-from')   ||{}).value || new Date().toISOString().split('T')[0];
    const toVal      = (document.getElementById('audit-to')     ||{}).value || new Date().toISOString().split('T')[0];
    const roleFilter = (document.getElementById('audit-role')   ||{}).value || '';
    const clinicId   = (document.getElementById('audit-clinic') ||{}).value || '0';
    const staffId    = (document.getElementById('audit-staff')  ||{}).value || '0';

    // Build query — clinic and staff filtering done server-side
    let url = `/records/audit?from=${fromVal}&to=${toVal}`;
    if (clinicId !== '0') url += `&clinicId=${clinicId}`;
    if (staffId  !== '0') url += `&staffId=${staffId}`;

    const appts = await apiFetch(url);

    // Fetch clinic names for display
    const clinics = await apiFetch('/records/clinics').catch(()=>[]);
    const nameMap = {};
    clinics.forEach(c => { nameMap[c.clinicId] = c.name; });

    // Build human-readable log entries from appointment records
    // Each appointment in the DB represents a real clinical transaction
    const entries = (appts||[]).map(a => {
      const clinicLabel = nameMap[a.clinicId] || `Clinic #${a.clinicId}`;
      const dateLabel   = a.appointmentDate ? (a.appointmentDate+'').split('T')[0] : 'Unknown date';
      const typeLabel   = a.type   || 'Unknown type';
      const statusLabel = a.status || 'Unknown status';
      const recLabel    = a.recordsUpdated ? 'Records updated' : 'Records pending update';

      // Determine action description based on status
      let action, color;
      if (statusLabel === 'Attended') {
        action = `Patient #${a.patientId} attended a ${typeLabel.toLowerCase()} appointment at ${clinicLabel}. ${recLabel}.`;
        color  = 'green';
      } else if (statusLabel === 'Missed') {
        action = `Patient #${a.patientId} missed a ${typeLabel.toLowerCase()} appointment at ${clinicLabel}. Consultant notified.`;
        color  = 'red';
      } else {
        action = `Appointment scheduled for Patient #${a.patientId} at ${clinicLabel} (${typeLabel}). Status: ${statusLabel}.`;
        color  = 'amber';
      }

      return {
        date:   dateLabel,
        user:   `Staff #${a.staffId}`,
        role:   'Clinical',
        action,
        color,
        apptId: a.appointmentId
      };
    });

    // Apply role filter
    const filtered = roleFilter ? entries.filter(e => e.role === roleFilter) : entries;

    if (!filtered.length) {
      if (wrap) wrap.innerHTML = `<div class="empty" style="padding:40px 0;">
        <h4>No transactions found</h4>
        <p style="font-size:13px;color:var(--ink-3);margin-top:6px;">
          No appointments recorded for the selected date range and filters.
          ${fromVal === toVal ? `Try extending the date range.` : ''}
        </p>
      </div>`;
      return;
    }

    if (wrap) wrap.innerHTML = `
      <div style="font-size:12px;color:var(--ink-3);margin-bottom:12px;font-family:var(--font-mono);">
        ${filtered.length} transaction(s) &middot; ${fromVal} to ${toVal}
        ${clinicId !== '0' ? ` &middot; ${nameMap[clinicId]||'Clinic #'+clinicId}` : ''}
        ${staffId  !== '0' ? ` &middot; Staff #${staffId}` : ''}
      </div>
      <div class="table-wrap">
        <table>
          <thead>
            <tr>
              <th style="width:100px;">Date</th>
              <th style="width:90px;">Staff</th>
              <th>Action</th>
              <th style="width:80px;">Appt ID</th>
              <th style="width:90px;">Role</th>
            </tr>
          </thead>
          <tbody>
            ${filtered.map(e => `
              <tr>
                <td style="font-family:var(--font-mono);font-size:11px;color:var(--ink-3);white-space:nowrap;">${e.date}</td>
                <td style="font-family:var(--font-mono);font-size:12px;font-weight:600;">${_esc(e.user)}</td>
                <td style="font-size:12px;color:var(--ink-2);">
                  <span style="display:inline-block;width:8px;height:8px;border-radius:50%;background:var(--${e.color});margin-right:6px;flex-shrink:0;"></span>
                  ${_esc(e.action)}
                </td>
                <td style="font-family:var(--font-mono);font-size:11px;color:var(--ink-3);">#${e.apptId}</td>
                <td><span class="role-badge role-badge-clinical">${e.role}</span></td>
              </tr>`).join('')}
          </tbody>
        </table>
      </div>`;
  } catch(e) {
    if (wrap) wrap.innerHTML = _errState(e);
  }
}
//   } catch(e) {
//     if(wrap) wrap.innerHTML=_errState(e);
//   }
// }

/* ── Utilities ── */
function _setStat(id,val){ const el=document.getElementById(id); if(el) el.innerHTML=String(val); }
function _spinner(){ return '<div class="empty"><div class="spinner spinner-dark" style="margin:0 auto;"></div></div>'; }
function _empty(msg){ return `<div class="empty"><h4>${msg}</h4></div>`; }
function _errState(e){
  if((e||{}).status===403) return _empty('Access denied — Medical Records role required.');
  if((e||{}).status===401) return _empty('Not authenticated — please sign in again.');
  return _empty('Could not load data. Check the backend is running and try again.');
}
function _errMsg(e){
  if(!e) return 'Unknown error';
  if(e.status===403) return 'Access denied (403)';
  if(e.status===401) return 'Unauthenticated (401)';
  if(e.status===404) return 'Not found (404)';
  if(e.data&&e.data.error) return e.data.error;
  return `Error ${e.status||''}`;
}
function _esc(str){ return (str||'').replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;').replace(/"/g,'&quot;'); }
