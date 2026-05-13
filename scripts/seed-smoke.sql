SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

TRUNCATE TABLE prescription;
TRUNCATE TABLE incident;
TRUNCATE TABLE adverse_reaction;
TRUNCATE TABLE warning_override;
TRUNCATE TABLE patient_condition;
TRUNCATE TABLE appointment;
TRUNCATE TABLE change_requests;
TRUNCATE TABLE patients;
TRUNCATE TABLE medication;
TRUNCATE TABLE `condition`;
TRUNCATE TABLE clinics;
TRUNCATE TABLE users;

SET FOREIGN_KEY_CHECKS = 1;

INSERT INTO users (username, password_hash, role) VALUES
('clinical1', 'secret', 'Clinical'),
('reception1', 'secret', 'Receptionist'),
('records1', 'secret', 'Medical_Records');

INSERT INTO clinics (name, location_type) VALUES
('Demo Clinic', 'Hospital'),
('Central Medical Clinic', 'Urban'),
('North Outreach Centre', 'Community');

INSERT INTO patients (
    first_name, last_name, address,
    homeless, risk_status, deceased, self_harm_history
) VALUES
('Jane', 'Doe', '1 Test St', 0, NULL, 0, 0),
('Jane', 'Smith', NULL, 1, 'HIGH', 0, 1);

INSERT INTO medication (name) VALUES
('Aspirin'),
('Paracetamol'),
('Ibuprofen'),
('Amoxicillin');

INSERT INTO `condition` (name) VALUES
('Depression'),
('Schizophrenia'),
('Asthma');

INSERT INTO appointment (
    patient_id, clinic_id, staff_id,
    appointment_date, type, status, records_updated
) VALUES
(1, 1, 1, '2026-01-15', 'Drop-in', 'Missed', 0),
(1, 1, 1, '2026-01-10', 'Checkup', 'COMPLETED', 1),
(2, 2, 2, '2026-01-12', 'Drop-in', 'OPEN', 0);

INSERT INTO prescription (
    appointment_id, medication_id, prescriber_id,
    issue_date, repeat_presc
) VALUES
(1, 1, 1, '2026-01-15', 0),
(2, 2, 1, '2026-01-10', 1),
(3, 3, 1, '2026-01-12', 0);

INSERT INTO patient_condition (
    patient_id, condition_id, appointment_id,
    diagnosis_date, notes
) VALUES
(1, 1, 1, '2025-10-01', 'Stable condition'),
(2, 3, 2, '2026-01-12', 'Uses inhaler regularly');

INSERT INTO adverse_reaction (
    patient_id, medication_id, description
) VALUES
(1, 2, 'Developed rash after medication'),
(2, 3, 'Severe nausea reported');

INSERT INTO warning_override (
    prescriber_id, medication_id,
    warning_details, override_date
) VALUES
(1, 2, 'Override due to clinical necessity', '2026-01-10');

INSERT INTO incident (
    patient_id, type, description, incident_date
) VALUES
(2, 'SELF_HARM', 'Patient reported self-harm thoughts', '2026-01-11'),
(1, 'FALL', 'Patient slipped in waiting room', '2026-01-09');

INSERT INTO comment (
    patient_id, clinician_id,
    free_form_text, comment_date
) VALUES
(1, 1, 'Patient recovering well', '2026-01-10'),
(2, 2, 'Patient requires follow-up appointment', '2026-01-12');

INSERT INTO change_requests (
    raw_patient_data,
    requested_changes,
    status
) VALUES
(
    '{"first_name":"John","last_name":"Doe"}',
    'Update address to 45 New Street',
    'PENDING'
),
(
    '{"first_name":"Jane","last_name":"Smith"}',
    'Correct risk status from HIGH to MEDIUM',
    'APPROVED'
);