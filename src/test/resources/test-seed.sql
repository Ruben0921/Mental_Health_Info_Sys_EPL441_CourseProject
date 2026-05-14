INSERT INTO users (user_id, username, password_hash, role)
VALUES (1, 'test_staff', 'hash', 'CLINICIAN');

INSERT INTO clinics (clinic_id, name)
VALUES (1, 'Test Clinic');

INSERT INTO patients (patient_id, first_name, last_name)
VALUES (1, 'Alice', 'Smith'), (2, 'Bob', 'Jones');