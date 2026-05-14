SET NAMES utf8mb4;

CREATE TABLE users (
    user_id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(512) NOT NULL,
    role VARCHAR(64) NOT NULL,

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    INDEX idx_users_role (role)
);

CREATE TABLE clinics (
    clinic_id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    location_type VARCHAR(64),

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    INDEX idx_clinics_name (name)
);

CREATE TABLE patients (
    patient_id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    first_name VARCHAR(255) NOT NULL,
    last_name VARCHAR(255) NOT NULL,
    address VARCHAR(512),

    homeless BOOLEAN NOT NULL DEFAULT FALSE,
    risk_status VARCHAR(64),
    deceased BOOLEAN NOT NULL DEFAULT FALSE,
    self_harm_history BOOLEAN NOT NULL DEFAULT FALSE,

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    INDEX idx_patients_name (last_name, first_name),
    INDEX idx_patients_risk (risk_status)
);

CREATE TABLE medication (
    medication_id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    INDEX idx_medication_name (name)
);

CREATE TABLE `condition` (
    condition_id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
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

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_appt_patient
        FOREIGN KEY (patient_id) REFERENCES patients(patient_id)
        ON DELETE CASCADE
        ON UPDATE CASCADE,

    CONSTRAINT fk_appt_clinic
        FOREIGN KEY (clinic_id) REFERENCES clinics(clinic_id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE,

    CONSTRAINT fk_appt_staff
        FOREIGN KEY (staff_id) REFERENCES users(user_id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE,

    INDEX idx_appt_patient (patient_id),
    INDEX idx_appt_clinic (clinic_id),
    INDEX idx_appt_date (appointment_date),
    INDEX idx_appt_status (status)
);

CREATE TABLE patient_condition (
    patient_condition_id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    patient_id INT NOT NULL,
    condition_id INT NOT NULL,
    appointment_id INT,

    diagnosis_date DATE,
    notes TEXT,

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_pc_patient
        FOREIGN KEY (patient_id) REFERENCES patients(patient_id)
        ON DELETE CASCADE,

    CONSTRAINT fk_pc_condition
        FOREIGN KEY (condition_id) REFERENCES `condition`(condition_id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_pc_appointment
        FOREIGN KEY (appointment_id) REFERENCES appointment(appointment_id)
        ON DELETE SET NULL,

    UNIQUE KEY uq_patient_condition (patient_id, condition_id, appointment_id),

    INDEX idx_pc_patient (patient_id)
);


CREATE TABLE change_requests (
    request_id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    raw_patient_data TEXT,
    requested_changes TEXT,

    status VARCHAR(32) NOT NULL DEFAULT 'PENDING',

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    INDEX idx_change_status (status)
);

CREATE TABLE adverse_reaction (
    reaction_id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    patient_id INT NOT NULL,
    medication_id INT NOT NULL,

    description VARCHAR(1024),

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_ar_patient
        FOREIGN KEY (patient_id) REFERENCES patients(patient_id)
        ON DELETE CASCADE,

    CONSTRAINT fk_ar_med
        FOREIGN KEY (medication_id) REFERENCES medication(medication_id)
        ON DELETE RESTRICT,

    INDEX idx_ar_patient (patient_id)
);

CREATE TABLE warning_override (
    override_id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    prescriber_id INT NOT NULL,
    medication_id INT NOT NULL,

    warning_details VARCHAR(2048),
    override_date DATE,

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_wo_prescriber
        FOREIGN KEY (prescriber_id) REFERENCES users(user_id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_wo_med
        FOREIGN KEY (medication_id) REFERENCES medication(medication_id)
        ON DELETE RESTRICT,

    INDEX idx_wo_med (medication_id)
);

CREATE TABLE prescription (
    prescription_id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    appointment_id INT NOT NULL,
    medication_id INT NOT NULL,
    prescriber_id INT NOT NULL,

    issue_date DATE,
    repeat_presc BOOLEAN NOT NULL DEFAULT FALSE,

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_rx_appt
        FOREIGN KEY (appointment_id) REFERENCES appointment(appointment_id)
        ON DELETE CASCADE,

    CONSTRAINT fk_rx_med
        FOREIGN KEY (medication_id) REFERENCES medication(medication_id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_rx_prescriber
        FOREIGN KEY (prescriber_id) REFERENCES users(user_id)
        ON DELETE RESTRICT,

    INDEX idx_rx_appt (appointment_id),
    INDEX idx_rx_med (medication_id)
);

CREATE TABLE incident (
    incident_id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    patient_id INT NOT NULL,

    type VARCHAR(64) NOT NULL,
    description VARCHAR(2048),
    incident_date DATE,

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_inc_patient
        FOREIGN KEY (patient_id) REFERENCES patients(patient_id)
        ON DELETE CASCADE,

    INDEX idx_inc_patient (patient_id),
    INDEX idx_inc_date (incident_date)
);

CREATE TABLE comment (
    comment_id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    patient_id INT NOT NULL,
    clinician_id INT NOT NULL,

    free_form_text TEXT NOT NULL,
    comment_date DATE NOT NULL,

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_comment_patient
        FOREIGN KEY (patient_id) REFERENCES patients(patient_id)
        ON DELETE CASCADE,

    CONSTRAINT fk_comment_clinician
        FOREIGN KEY (clinician_id) REFERENCES users(user_id)
        ON DELETE RESTRICT,

    INDEX idx_comment_patient (patient_id),
    INDEX idx_comment_date (comment_date)
);