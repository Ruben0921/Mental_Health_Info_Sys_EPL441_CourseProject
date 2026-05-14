let allPatients = [];
let editingPatientId = null;
let currentPatientAppointments = [];
let selectedApptId = null;
let allMedications = [];
let allConditions = [];
let patientAdverseReactions = [];
let modalCtx = { apptId: null, patientId: null, existingPrescription: null, existingCondition: null };

function initClinical() {
  if (!requireAuth('Clinical')) return;
  initShell();
  buildClinicalNav();
  showDashboard(`<button class="btn btn-secondary btn-full" onclick="showPatientsPage()">👤 Patient Records</button>`);
  loadMedications();
  loadConditions();
}

function buildClinicalNav() {
  const nav = document.getElementById('main-nav');
  nav.innerHTML = '';
  [
    { label: '⊞ Dashboard', action: () => showDashboard(`<button class="btn btn-secondary btn-full" onclick="showPatientsPage()">👤 Patient Records</button>`) },
    { label: '👤 Patients',  action: showPatientsPage },
  ].forEach(item => {
    const btn = document.createElement('button');
    btn.className = 'nav-btn';
    btn.textContent = item.label;
    btn.onclick = () => {
      document.querySelectorAll('.nav-btn').forEach(b => b.classList.remove('active'));
      btn.classList.add('active');
      item.action();
    };
    nav.appendChild(btn);
  });
  nav.firstChild.classList.add('active');
}

function showPatientsPage() {
  showPage('page-patients');
  loadPatients();
}

/* Patients */
async function loadPatients() {
  document.getElementById('patients-table-wrap').innerHTML = '<div class="empty"><div class="spinner spinner-dark" style="margin:0 auto;"></div></div>';
  try {
    allPatients = await apiFetch('/patients');
    renderPatientsTable(allPatients);
  } catch (e) {
    document.getElementById('patients-table-wrap').innerHTML = `<div class="empty"><div class="empty-icon">⚠️</div><h4>Could not load patients</h4><p>${e.status === 403 ? 'Access denied.' : 'Server error — is the backend running?'}</p></div>`;
  }
}

function filterPatients() {
  const q = document.getElementById('patient-search').value.toLowerCase();
  renderPatientsTable(allPatients.filter(p =>
    p.firstName?.toLowerCase().includes(q) ||
    p.lastName?.toLowerCase().includes(q) ||
    String(p.patientId).includes(q)
  ));
}

function renderPatientsTable(patients) {
  if (!patients.length) { document.getElementById('patients-table-wrap').innerHTML = '<div class="empty"><div class="empty-icon">👤</div><h4>No patients found</h4></div>'; return; }
  let html = `<div class="table-wrap"><table><thead><tr><th>ID</th><th>Name</th><th>Address</th><th>Risk</th><th>Flags</th><th>Actions</th></tr></thead><tbody>`;
  patients.forEach(p => {
    const rc = p.selfHarmHistory ? 'flag-row' : '';
    const rsc = p.riskStatus ? `status-${p.riskStatus.toLowerCase()}` : '';
    html += `<tr class="${rc}">
      <td><span style="font-family:var(--font-mono);color:var(--ink-3);">#${p.patientId}</span></td>
      <td><strong>${p.firstName} ${p.lastName}</strong>${p.selfHarmHistory ? '<br><span class="alert-selfharm">⚠ Self-harm</span>' : ''}${p.deceased ? '<br><span class="badge-deceased">🔒 Deceased</span>' : ''}</td>
      <td>${p.homeless ? '<em style="color:var(--amber);">🏠 Homeless</em>' : (p.address || '—')}</td>
      <td>${p.riskStatus ? `<span class="status ${rsc}">${p.riskStatus}</span>` : '—'}</td>
      <td>${p.homeless ? '<span class="status status-pending" style="font-size:10px;">Homeless</span> ' : ''}${p.deceased ? '<span class="status status-missed" style="font-size:10px;">Deceased</span>' : ''}</td>
      <td><button class="btn btn-secondary" style="padding:5px 12px;font-size:12px;" onclick="openPatientDetail(${p.patientId})">Open</button></td>
    </tr>`;
  });
  html += '</tbody></table></div>';
  document.getElementById('patients-table-wrap').innerHTML = html;
}

async function openPatientDetail(id) {
  editingPatientId = id;
  const p = allPatients.find(x => x.patientId === id);
  if (!p) return;

  document.getElementById('detail-heading').textContent = `${p.firstName} ${p.lastName}`;
  document.getElementById('detail-subheading').textContent = `Patient ID #${p.patientId}`;
  document.getElementById('detail-selfharm-alert').style.display = p.selfHarmHistory ? 'flex' : 'none';
  document.getElementById('detail-deceased-alert').style.display = p.deceased ? 'flex' : 'none';
  document.getElementById('pe-firstName').value = p.firstName || '';
  document.getElementById('pe-lastName').value = p.lastName || '';
  document.getElementById('pe-address').value = p.address || '';
  document.getElementById('pe-riskStatus').value = p.riskStatus || '';
  document.getElementById('pe-homeless').checked = !!p.homeless;
  document.getElementById('pe-selfHarmHistory').checked = !!p.selfHarmHistory;
  document.getElementById('pe-deceased').checked = !!p.deceased;

  const locked = !!p.deceased;
  ['pe-firstName', 'pe-lastName', 'pe-address', 'pe-riskStatus', 'pe-homeless', 'pe-selfHarmHistory', 'pe-deceased']
    .forEach(fid => document.getElementById(fid).disabled = locked);
  document.getElementById('save-patient-btn').disabled = locked;

  patientAdverseReactions = [];
  document.getElementById('adverse-reactions-list').innerHTML = '<div class="empty" style="padding:20px 0;"><div class="spinner spinner-dark" style="margin:0 auto;"></div></div>';

	try {
	patientAdverseReactions = await apiFetch(`/patients/${id}/adverse-reactions`);
	renderAdverseReactions();
	} catch (e) {
	document.getElementById('adverse-reactions-list').innerHTML = '<div class="empty" style="padding:20px 0;"><p style="font-size:13px;color:var(--red);">Failed to load adverse reactions.</p></div>';
	}

	// Populate the adverse reaction medication dropdown
	const arSel = document.getElementById('ar-medicationId');
	if (arSel) {
	arSel.innerHTML = '<option value="">— Select medication —</option>';
	allMedications.forEach(m => {
		const o = document.createElement('option');
		o.value = m.medicationId;
		o.textContent = m.name;
		arSel.appendChild(o);
	});
	}

  switchPatientTab('record', document.getElementById('ptab-record'));
  showPage('page-patient-detail');
  document.querySelectorAll('.nav-btn').forEach(b => b.classList.remove('active'));
}

function switchPatientTab(tab, btn) {
  document.querySelectorAll('.tabs .tab').forEach(t => t.classList.remove('active'));
  btn.classList.add('active');
  document.getElementById('ptab-content-record').style.display = tab === 'record' ? '' : 'none';
  document.getElementById('ptab-content-consultations').style.display = tab === 'consultations' ? '' : 'none';
  document.getElementById('ptab-content-comments').style.display = tab === 'comments' ? '' : 'none';
  if (tab === 'consultations') loadPatientAppointments();
  if (tab === 'comments') loadComments();
}

async function savePatient() {
  if (!editingPatientId) return;
  const body = {
    patientId: editingPatientId,
    firstName: document.getElementById('pe-firstName').value,
    lastName: document.getElementById('pe-lastName').value,
    address: document.getElementById('pe-address').value,
    riskStatus: document.getElementById('pe-riskStatus').value || null,
    homeless: document.getElementById('pe-homeless').checked,
    selfHarmHistory: document.getElementById('pe-selfHarmHistory').checked,
    deceased: document.getElementById('pe-deceased').checked,
  };
  try {
    await apiFetch(`/patients/${editingPatientId}`, { method: 'PUT', body: JSON.stringify(body) });
    toast('Patient record saved.', 'success');
    allPatients = await apiFetch('/patients');
    const upd = allPatients.find(x => x.patientId === editingPatientId);
    if (upd?.selfHarmHistory) document.getElementById('detail-selfharm-alert').style.display = 'flex';
    if (upd?.deceased) {
      document.getElementById('detail-deceased-alert').style.display = 'flex';
      ['pe-firstName', 'pe-lastName', 'pe-address', 'pe-riskStatus', 'pe-homeless', 'pe-selfHarmHistory', 'pe-deceased']
        .forEach(fid => document.getElementById(fid).disabled = true);
      document.getElementById('save-patient-btn').disabled = true;
    }
  } catch (e) { toast('Save failed: ' + (e.data?.error || e.status), 'error'); }
}

async function loadMedications() { try { allMedications = await apiFetch('/clinical/medications'); } catch {} }
async function loadConditions()  { try { allConditions  = await apiFetch('/clinical/conditions');  } catch {} }

/* Consultations  */
async function loadPatientAppointments() {
  if (!editingPatientId) return;
  document.getElementById('patient-appt-list').innerHTML = '<div class="empty" style="padding:24px;"><div class="spinner spinner-dark" style="margin:0 auto;"></div></div>';
  resetConsultPanel();
  try {
    currentPatientAppointments = await apiFetch(`/clinical/patients/${editingPatientId}/appointments`);
    renderApptList(currentPatientAppointments);
  } catch (e) {
    document.getElementById('patient-appt-list').innerHTML = `<div class="empty" style="padding:24px;"><div class="empty-icon">📅</div><h4>No appointments</h4><p>${e.status === 404 ? 'None found.' : 'Error: ' + e.status}</p></div>`;
  }
}

function resetConsultPanel() {
  document.getElementById('consultation-detail-panel').innerHTML = `
    <div class="card" style="text-align:center;padding:48px 24px;border:2px dashed var(--paper-3);">
      <div style="font-size:40px;margin-bottom:12px;">🩺</div>
      <div style="font-family:var(--font-head);font-style:italic;font-size:20px;color:var(--ink-2);">Select a consultation</div>
      <p style="font-size:13px;color:var(--ink-3);margin-top:6px;">Click an appointment to view and edit its clinical details.</p>
    </div>`;
}

function renderApptList(appts) {
  if (!appts.length) {
    document.getElementById('patient-appt-list').innerHTML = '<div class="empty" style="padding:24px;"><div class="empty-icon">📅</div><h4>No consultations</h4><p>No appointments on record.</p></div>';
    return;
  }

    const sorted = [...appts].sort((a, b) =>
    	new Date(b.appointmentDate) - new Date(a.appointmentDate)
  	);

  document.getElementById('patient-appt-list').innerHTML = '<div class="appt-list-panel">' + sorted.map(a => {
    const sc = `status-${(a.status || '').toLowerCase()}`;
    return `<div class="appt-list-item" id="appt-item-${a.appointmentId}" onclick="openConsultDetail(${a.appointmentId})">
      <div class="appt-item-date">${a.appointmentDate || 'Unknown date'}</div>
      <div class="appt-item-sub">${a.type || ''} · Clinic #${a.clinicId}</div>
      <div class="appt-item-badges">
        <span class="status ${sc}">${a.status || '—'}</span>
        ${!a.recordsUpdated ? '<span class="pending-flag">⚑ Pending Update</span>' : ''}
      </div>
    </div>`;
  }).join('') + '</div>';
}

async function openConsultDetail(apptId) {
  selectedApptId = apptId;
  document.querySelectorAll('.appt-list-item').forEach(el => el.classList.remove('selected'));
  const sel = document.getElementById('appt-item-' + apptId);
  if (sel) sel.classList.add('selected');

  const sorted = [...currentPatientAppointments].sort((a, b) =>
    new Date(b.appointmentDate) - new Date(a.appointmentDate)
  );
  const appt = sorted.find(a => a.appointmentId === apptId);
  const idx = sorted.findIndex(a => a.appointmentId === apptId);
  const olderAppts = sorted.slice(idx + 1);

  document.getElementById('consultation-detail-panel').innerHTML = '<div class="card"><div class="empty" style="padding:32px;"><div class="spinner spinner-dark" style="margin:0 auto;"></div></div></div>';

  let [prescription, condition, prevPrescription, prevCondition] = [null, null, null, null];
  try { prescription = await apiFetch(`/clinical/appointments/${apptId}/prescription`); } catch {}
  try { condition    = await apiFetch(`/clinical/appointments/${apptId}/condition`);    } catch {}

  // Fetch previous appointment's data if this one has no condition/prescription
  if (olderAppts.length) {
    const prevId = olderAppts[0].appointmentId;
    if (!prescription) try { prevPrescription = await apiFetch(`/clinical/appointments/${prevId}/prescription`); } catch {}
    if (!condition)    try { prevCondition    = await apiFetch(`/clinical/appointments/${prevId}/condition`);    } catch {}
  }

  renderConsultPanel(appt, prescription || prevPrescription, condition || prevCondition);
  openConsultModal(appt, prescription, condition, olderAppts, prevPrescription, prevCondition);
}

function renderConsultPanel(appt, rx, cond) {
  const sc = `status-${(appt.status || '').toLowerCase()}`;
  document.getElementById('consultation-detail-panel').innerHTML = `
    <div class="card" style="padding:0;overflow:hidden;">
      <div class="consult-header" style="border-radius:10px 10px 0 0;">
        <div style="font-size:15px;font-family:var(--font-head);font-style:italic;">${appt.appointmentDate || 'Unknown'}</div>
        <div style="font-size:12px;opacity:.75;margin-top:2px;">${appt.type || ''}</div>
      </div>
      <div style="padding:16px 20px;">
        <div style="display:grid;grid-template-columns:1fr 1fr;gap:14px;margin-bottom:14px;">
          <div>
            <div style="font-size:11px;font-weight:700;font-family:var(--font-mono);color:var(--ink-3);text-transform:uppercase;letter-spacing:.08em;margin-bottom:4px;">Condition</div>
            <div style="font-size:13px;color:var(--ink-2);">${cond?.conditionName || '<em style="color:var(--ink-3);">Not recorded</em>'}</div>
          </div>
          <div>
            <div style="font-size:11px;font-weight:700;font-family:var(--font-mono);color:var(--ink-3);text-transform:uppercase;letter-spacing:.08em;margin-bottom:4px;">Medication</div>
            <div style="font-size:13px;color:var(--ink-2);">${rx?.medicationName || '<em style="color:var(--ink-3);">None</em>'}${rx?.repeat ? ' <span style="font-size:10px;background:var(--blue-lt);color:var(--accent);border-radius:3px;padding:1px 5px;font-family:var(--font-mono);">REPEAT</span>' : ''}</div>
          </div>
        </div>
        <span class="status ${sc}">${appt.status || '—'}</span>
        ${!appt.recordsUpdated ? '<span class="pending-flag" style="margin-left:6px;">⚑ Records Pending</span>' : ''}
        ${cond?.notes ? `<div style="font-size:12px;color:var(--ink-3);font-style:italic;border-top:1px solid var(--paper-3);padding-top:10px;margin-top:12px;">${cond.notes}</div>` : ''}
        <button class="btn btn-primary" style="width:100%;margin-top:14px;justify-content:center;" onclick="openConsultDetail(${appt.appointmentId})">✏️ Open Full Editor</button>
      </div>
    </div>`;
}

function openConsultModal(appt, rx, cond, olderAppts, prevRx, prevCond) {
  document.getElementById('cm-title').textContent = `Consultation — ${appt.appointmentDate || 'Unknown'}`;
  document.getElementById('cm-meta').textContent = `${appt.type || ''} · Clinic #${appt.clinicId} · Staff #${appt.staffId}`;

  const prevBanner = document.getElementById('cm-prev-banner');
  if (olderAppts.length) {
    prevBanner.style.display = 'flex';
    const prev = olderAppts[0];
    document.getElementById('cm-prev-text').innerHTML = `<strong>Previous consultation: ${prev.appointmentDate}</strong> — ${prev.status}${!prev.recordsUpdated ? ' <span class="pending-flag" style="font-size:10px;">⚑ Pending</span>' : ''}`;
  } else { prevBanner.style.display = 'none'; }

  const dropinWarn = document.getElementById('cm-dropin-warn');
  if (appt.type === 'Drop-in' && olderAppts.length) {
    dropinWarn.style.display = 'flex';
    document.getElementById('cm-last-visit').textContent = olderAppts[0].appointmentDate;
  } else { dropinWarn.style.display = 'none'; }

  const condSel = document.getElementById('cm-conditionId');
  condSel.innerHTML = '<option value="">— Select condition —</option>';
  allConditions.forEach(c => { const o = document.createElement('option'); o.value = c.conditionId; o.textContent = c.name; condSel.appendChild(o); });

  const effectiveCond = cond || prevCond;
  condSel.value = effectiveCond?.conditionId || '';
  document.getElementById('cm-diagnosisDate').value = cond?.diagnosisDate || appt.appointmentDate || '';
  document.getElementById('cm-conditionNotes').value = cond?.notes || '';
  document.getElementById('cm-prev-condition-label').textContent =
    cond?.conditionName ? `Current: ${cond.conditionName}` :
    prevCond?.conditionName ? `Previous: ${prevCond.conditionName}` :
    olderAppts.length ? 'No condition recorded yet' : 'First consultation';

  const medSel = document.getElementById('cm-medicationId');
  medSel.innerHTML = '<option value="">— None —</option>';
  allMedications.forEach(m => { const o = document.createElement('option'); o.value = m.medicationId; o.textContent = m.name; medSel.appendChild(o); });

  const effectiveRx = rx || prevRx;
  medSel.value = effectiveRx?.medicationId || '';
  document.getElementById('cm-issueDate').value = rx?.issueDate || appt.appointmentDate || '';
  document.getElementById('cm-repeat').checked = !!effectiveRx?.repeat;
  document.getElementById('cm-prev-rx-label').textContent =
    rx?.medicationName ? `Current: ${rx.medicationName}` :
    prevRx?.medicationName ? `Previous: ${prevRx.medicationName}` : 'No prescription recorded';

  const histEl = document.getElementById('cm-history-list');
  if (!olderAppts.length) { histEl.innerHTML = '<p style="font-size:13px;color:var(--ink-3);">This is the earliest recorded consultation for this patient.</p>'; }
  else {
    histEl.innerHTML = olderAppts.map(a => `
      <div class="history-row">
        <div class="history-row-date">${a.appointmentDate} · ${a.type}</div>
        <div class="history-row-detail">
          <span class="status status-${(a.status || '').toLowerCase()}" style="font-size:10px;">${a.status}</span>
          ${!a.recordsUpdated ? '<span class="pending-flag" style="font-size:10px;margin-left:6px;">⚑ Records pending</span>' : ''}
          · Clinic #${a.clinicId}
        </div>
      </div>`).join('');
  }

  modalCtx = { apptId: appt.appointmentId, patientId: editingPatientId, existingPrescription: rx, existingCondition: cond, prevPrescription: prevRx, prevCondition: prevCond };
  document.getElementById('consult-modal').classList.add('open');
  checkAllergyWarning();
}

function checkAllergyWarning() {
  const medId = parseInt(document.getElementById('cm-medicationId').value);
  const warn = document.getElementById('cm-allergy-warn');
  if (!medId || !patientAdverseReactions.length) { warn.style.display = 'none'; return; }
  const r = patientAdverseReactions.find(x => x.medicationId === medId);
  if (r) { warn.style.display = 'flex'; document.getElementById('cm-allergy-detail').textContent = r.description || 'Known reaction on file.'; }
  else { warn.style.display = 'none'; }
}

async function saveConsultation() {
  const { apptId, patientId } = modalCtx;
  const conditionId   = parseInt(document.getElementById('cm-conditionId').value) || 0;
  const medicationId  = parseInt(document.getElementById('cm-medicationId').value) || 0;
  const diagnosisDate = document.getElementById('cm-diagnosisDate').value;
  const conditionNotes = document.getElementById('cm-conditionNotes').value;
  const issueDate     = document.getElementById('cm-issueDate').value;
  const repeat        = document.getElementById('cm-repeat').checked;
  let saved = false;

  if (conditionId) {
    try {
      await apiFetch(`/clinical/appointments/${apptId}/condition`, {
        method: 'PUT',
        body: JSON.stringify({ patientId, conditionId, appointmentId: apptId, diagnosisDate: diagnosisDate || null, notes: conditionNotes || null })
      });
      saved = true;
    } catch (e) { toast('Failed to save condition: ' + (e.data?.error || e.status), 'error'); return; }
  }

  if (medicationId) {
    try {
      await apiFetch(`/clinical/appointments/${apptId}/prescription`, {
        method: 'POST',
        body: JSON.stringify({ appointmentId: apptId, medicationId, prescriberId: auth.userId || 1, issueDate: issueDate || null, repeat })
      });
      saved = true;
    } catch (e) { toast('Failed to save prescription: ' + (e.data?.error || e.status), 'error'); return; }
  }

  if (saved) {
    try {
      await apiFetch(`/appointments/${apptId}/update`, {
        method: 'PUT',
        body: JSON.stringify({ value: true })
      });
    } catch (e) { toast('Could not mark records as updated: ' + (e.data?.error || e.status), 'error'); }

    toast('Consultation saved successfully.', 'success');
    closeModal('consult-modal');
    loadPatientAppointments();
  } else {
    toast('Select a condition and/or medication before saving.', 'warn');
  }
}

/* Comments */
async function loadComments() {
  if (!editingPatientId) return;
  document.getElementById('comments-list').innerHTML = '<div class="empty"><div class="spinner spinner-dark" style="margin:0 auto;"></div></div>';
  try {
    const comments = await apiFetch(`/clinical/patients/${editingPatientId}/comments`);
    if (!comments.length) { document.getElementById('comments-list').innerHTML = '<div class="empty"><div class="empty-icon">💬</div><h4>No comments yet</h4><p>Add the first clinical note below.</p></div>'; return; }
    document.getElementById('comments-list').innerHTML = comments.map(c => `
      <div class="comment-card">
        <div class="comment-author">👤 ${c.clinicianName || 'Clinician #' + c.clinicianId}</div>
        <div class="comment-text">${c.freeFormText}</div>
        <div class="comment-date">${c.commentDate || ''}</div>
      </div>`).join('');
  } catch (e) { document.getElementById('comments-list').innerHTML = `<div class="empty"><p>Could not load comments.</p></div>`; }
}

async function addComment() {
  const text = document.getElementById('new-comment-text').value.trim();
  if (!text) { toast('Enter a comment.', 'warn'); return; }
  try {
    await apiFetch(`/clinical/patients/${editingPatientId}/comments`, {
      method: 'POST',
      body: JSON.stringify({ patientId: editingPatientId, clinicianId: auth.userId || 1, freeFormText: text, commentDate: new Date().toISOString().split('T')[0] })
    });
    document.getElementById('new-comment-text').value = '';
    toast('Comment added.', 'success');
    loadComments();
  } catch (e) { toast('Failed: ' + (e.data?.error || e.status), 'error'); }
}

function openNewPatientModal() {
  document.getElementById('np-firstName').value = '';
  document.getElementById('np-lastName').value = '';
  document.getElementById('np-address').value = '';
  document.getElementById('np-riskStatus').value = '';
  document.getElementById('np-homeless').checked = false;
  document.getElementById('np-selfHarmHistory').checked = false;
  document.getElementById('new-patient-modal').classList.add('open');
}

async function createPatient() {
  const firstName = document.getElementById('np-firstName').value.trim();
  const lastName  = document.getElementById('np-lastName').value.trim();
  if (!firstName || !lastName) { toast('First and last name are required.', 'warn'); return; }

  try {
    const patient = await apiFetch('/patients', {
      method: 'POST',
      body: JSON.stringify({
        firstName,
        lastName,
        address:         document.getElementById('np-address').value.trim() || null,
        riskStatus:      document.getElementById('np-riskStatus').value || null,
        homeless:        document.getElementById('np-homeless').checked,
        selfHarmHistory: document.getElementById('np-selfHarmHistory').checked,
        deceased:        false
      })
    });
    closeModal('new-patient-modal');
    toast(`Patient ${firstName} ${lastName} created.`, 'success');
    await loadPatients();
    openPatientDetail(patient.patientId); // jump straight to the new record
  } catch (e) { toast('Failed to create patient: ' + (e.data?.error || e.status), 'error'); }
}

async function addAdverseReaction() {
  const medicationId = parseInt(document.getElementById('ar-medicationId').value);
  const description  = document.getElementById('ar-description').value.trim();
  if (!medicationId) { toast('Select a medication.', 'warn'); return; }
  if (patientAdverseReactions.find(r => r.medicationId === medicationId)) {
    toast('Reaction for this medication already recorded.', 'warn'); return;
  }
  try {
    await apiFetch(`/patients/${editingPatientId}/adverse-reactions`, {
      method: 'POST',
      body: JSON.stringify({ patientId: editingPatientId, medicationId, description: description || null })
    });
    patientAdverseReactions = await apiFetch(`/patients/${editingPatientId}/adverse-reactions`);
    renderAdverseReactions();
    document.getElementById('ar-medicationId').value = '';
    document.getElementById('ar-description').value = '';
    toast('Adverse reaction recorded.', 'success');
  } catch (e) { toast('Failed to add reaction: ' + (e.data?.error || e.status), 'error'); }
}

async function removeAdverseReaction(reactionId) {
  try {
    await apiFetch(`/patients/${editingPatientId}/adverse-reactions/${reactionId}`, { method: 'DELETE' });
    patientAdverseReactions = patientAdverseReactions.filter(r => r.reactionId !== reactionId);
    renderAdverseReactions();
    toast('Adverse reaction removed.', 'success');
  } catch (e) { toast('Failed to remove reaction: ' + (e.data?.error || e.status), 'error'); }
}

function renderAdverseReactions() {
  const el = document.getElementById('adverse-reactions-list');
  if (!patientAdverseReactions.length) {
    el.innerHTML = '<div class="empty" style="padding:20px 0;"><p style="font-size:13px;">No known adverse reactions on file.</p></div>';
    return;
  }
  el.innerHTML = patientAdverseReactions.map(r => `
    <div style="display:flex;align-items:flex-start;justify-content:space-between;padding:10px 0;border-bottom:1px solid var(--paper-3);">
      <div>
        <div style="font-weight:600;font-size:13px;color:var(--ink-1);">💊 ${r.medicationName}</div>
        <div style="font-size:12px;color:var(--ink-3);margin-top:3px;">${r.description || 'No description recorded.'}</div>
      </div>
      <button class="btn btn-secondary" style="font-size:11px;padding:4px 10px;color:var(--red);" onclick="removeAdverseReaction(${r.reactionId})">✕ Remove</button>
    </div>`).join('');
}