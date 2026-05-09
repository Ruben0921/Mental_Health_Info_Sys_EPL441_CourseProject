CREATE TABLE users (
    user_id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(512) NOT NULL,
    role VARCHAR(64) NOT NULL
);

CREATE TABLE clinics (
    clinic_id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    location_type VARCHAR(64)
);

CREATE TABLE medication (
    medication_id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL
);

CREATE TABLE patients (
    patient_id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    first_name VARCHAR(255) NOT NULL,
    last_name VARCHAR(255) NOT NULL,
    address VARCHAR(512),
    homeless BOOLEAN NOT NULL DEFAULT FALSE,
    risk_status VARCHAR(64),
    deceased BOOLEAN NOT NULL DEFAULT FALSE,
    self_harm_history BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE appointment (
    appointment_id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    patient_id INT NOT NULL,
    clinic_id INT NOT NULL,
    staff_id INT NOT NULL,
    appointment_date DATE,
    type VARCHAR(64),
    status VARCHAR(64),
    records_updated BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_appt_patient FOREIGN KEY (patient_id) REFERENCES patients (patient_id),
    CONSTRAINT fk_appt_clinic FOREIGN KEY (clinic_id) REFERENCES clinics (clinic_id),
    CONSTRAINT fk_appt_staff FOREIGN KEY (staff_id) REFERENCES users (user_id)
);

CREATE TABLE change_requests (
    request_id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    raw_patient_data TEXT,
    requested_changes TEXT,
    status VARCHAR(32) NOT NULL DEFAULT 'Pending'
);

CREATE TABLE adverse_reaction (
    reaction_id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    patient_id INT NOT NULL,
    medication_id INT NOT NULL,
    description VARCHAR(1024),
    CONSTRAINT fk_ar_patient FOREIGN KEY (patient_id) REFERENCES patients (patient_id),
    CONSTRAINT fk_ar_med FOREIGN KEY (medication_id) REFERENCES medication (medication_id)
);

CREATE TABLE warning_override (
    override_id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    prescriber_id INT NOT NULL,
    medication_id INT NOT NULL,
    warning_details VARCHAR(2048),
    override_date DATE,
    CONSTRAINT fk_wo_med FOREIGN KEY (medication_id) REFERENCES medication (medication_id)
);

CREATE TABLE prescription (
    prescription_id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    appointment_id INT NOT NULL,
    medication_id INT NOT NULL,
    prescriber_id INT NOT NULL,
    issue_date DATE,
    repeat_presc BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_rx_appt FOREIGN KEY (appointment_id) REFERENCES appointment (appointment_id),
    CONSTRAINT fk_rx_med FOREIGN KEY (medication_id) REFERENCES medication (medication_id)
);

CREATE TABLE incident (
    incident_id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    patient_id INT NOT NULL,
    type VARCHAR(64) NOT NULL,
    description VARCHAR(2048),
    incident_date DATE,
    CONSTRAINT fk_inc_patient FOREIGN KEY (patient_id) REFERENCES patients (patient_id)
);
