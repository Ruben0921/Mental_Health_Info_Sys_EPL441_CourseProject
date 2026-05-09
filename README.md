# Mental Health Information System

Java21 / Maven backend for mental health clinic workflows: JDBC data access, role-based REST APIs (Jersey + Grizzly), and JSON via Jackson. The domain covers patients, appointments, prescriptions, incidents, adverse reactions, and change requests for medical records.

## Requirements

- JDK 21  
- Maven 3.9+  
- MySQL 8 (for running the HTTP API against a real database)

## Quick start

```bash
mvn test
```

Tests use an in-memory H2 database (`src/test/resources`) and do not require MySQL. For running the HTTP API against MySQL, copying config, and smoke-testing with `curl`, see [How to test](#how-to-test).

## How to test

### 1. Automated tests (H2, no MySQL)

Runs the full JUnit suite against an in-memory database. No `config.properties` required for this step.

```bash
mvn test
```

- **Pass criteria:** Maven exits with code 0. JUnit XML and logs are under `target/surefire-reports/` (default Surefire output).
- **Scope:** DAOs, services, and models exercised with [`src/test/resources/schema.sql`](src/test/resources/schema.sql) and [`src/test/resources/config.properties`](src/test/resources/config.properties) (H2 in MySQL compatibility mode).

### 2. End-to-end smoke (MySQL + live REST API)

Use this when you want to verify JDBC against a real MySQL instance and the Grizzly/Jersey stack over HTTP.

**Prerequisites:** MySQL listening on the host/port in your JDBC URL (default `localhost:3306`), JDK 21+, Maven on `PATH`.

**a. Configuration (not committed; create locally)**

Copy the template and edit credentials:

```bash
cp src/main/resources/config.properties.example src/main/resources/config.properties
```

Set at least `db.url`, `db.username`, and `db.password`. Optional: `api.baseUri` (default `http://localhost:8080/`). The file [`config.properties.example`](src/main/resources/config.properties.example) lists the keys.

**b. Database and schema**

Create the database, apply DDL, and grant a dedicated user (example uses database `mental_health` and user `mhapp`; adjust names/passwords to match `config.properties`):

```bash
mysql -u root -p -e "CREATE DATABASE IF NOT EXISTS mental_health CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
mysql -u root -p mental_health < src/main/resources/schema.sql
mysql -u root -p -e "CREATE USER IF NOT EXISTS 'mhapp'@'localhost' IDENTIFIED BY 'your_secure_password'; GRANT ALL PRIVILEGES ON mental_health.* TO 'mhapp'@'localhost'; FLUSH PRIVILEGES;"
```

**c. Seed data**

`UserDAO` treats `users.password_hash` as **plain text** for local demos (not production-safe). You need at least one user per role you plan to call, and foreign-key data if you hit nested routes.

Example minimum seed (run in `mysql` against `mental_health` after updating passwords/usernames to match what you will pass in Basic Auth):

```sql
INSERT INTO clinics (name, location_type) VALUES ('Demo Clinic', 'Hospital');
INSERT INTO users (username, password_hash, role) VALUES
  ('clinical1', 'secret', 'Clinical'),
  ('reception1', 'secret', 'Receptionist'),
  ('records1', 'secret', 'Medical_Records');
INSERT INTO patients (first_name, last_name, address, homeless, deceased, self_harm_history)
  VALUES ('Jane', 'Doe', '1 Test St', 0, 0, 0);
INSERT INTO medication (name) VALUES ('Aspirin');
INSERT INTO appointment (patient_id, clinic_id, staff_id, appointment_date, type, status, records_updated)
  VALUES (1, 1, 1, '2026-01-15', 'Drop-in', 'Missed', 0);
INSERT INTO prescription (appointment_id, medication_id, prescriber_id, issue_date, repeat_presc)
  VALUES (1, 1, 1, '2026-01-15', 0);
```

**d. Start the API**

From the project root (classpath includes `mysql-connector-j`):

```bash
mvn -q compile org.codehaus.mojo:exec-maven-plugin:3.1.0:java -Dexec.mainClass=com.skillonnet.automation.Main
```

Leave this process running. Stop with Ctrl+C.

Alternatively, run [`com.skillonnet.automation.Main`](src/main/java/com/skillonnet/automation/Main.java) from your IDE with the same classpath dependencies.

**e. HTTP smoke checks (second terminal)**

All routes use **HTTP Basic Auth**. Role strings must match exactly: `Clinical`, `Receptionist`, or `Medical_Records`.

```bash
# Clinical — list patients
curl -s -w "\nHTTP %{http_code}\n" -u 'clinical1:secret' 'http://localhost:8080/patients'

# Receptionist — missed appointments on a date (must match seeded data)
curl -s -w "\nHTTP %{http_code}\n" -u 'reception1:secret' 'http://localhost:8080/appointments/missed?date=2026-01-15'

# Medical_Records — reporting
curl -s -w "\nHTTP %{http_code}\n" -u 'records1:secret' 'http://localhost:8080/reports/patients-per-clinic'
curl -s -w "\nHTTP %{http_code}\n" -u 'records1:secret' 'http://localhost:8080/reports/prescription-stats'
```

Expect HTTP **200** and JSON bodies when auth and data are valid. Without credentials you should see **401**.

| Role | Example routes |
|------|----------------|
| Clinical | `GET` / `PUT /patients`, `GET /patients/{id}` |
| Receptionist | `POST /appointments`, `PUT /appointments/{id}/attendance`, `GET /appointments/missed?date=YYYY-MM-DD`, `GET /appointments/pending-records` |
| Medical_Records | `GET /reports/patients-per-clinic`, `GET /reports/prescription-stats`, `POST /reports/change-requests` |

### 3. Full REST smoke (all endpoints)

Reset and load the deterministic smoke dataset (after schema is applied):

```bash
mysql -u YOUR_USER -p mental_health < scripts/seed-smoke.sql
```

Start the API (see step **d** above), then in another terminal from the project root:

```bash
chmod +x scripts/smoke-all-endpoints.sh   # once
./scripts/smoke-all-endpoints.sh
```

The script calls **every** route (Clinical, Receptionist, Medical_Records), including `POST /appointments` and `PUT .../attendance` using a newly created appointment id, then checks **401** (no credentials) and **403** (wrong role). Override defaults with environment variables if needed: `BASE_URL`, `CLINICAL_USER`, `CLINICAL_PASS`, `RECEPTION_USER`, `RECEPTION_PASS`, `RECORDS_USER`, `RECORDS_PASS`, `MISSED_DATE`, `NEW_APPT_DATE`.

## Data model

Conceptual entity-relationship view (some tables, e.g. `patient_condition` and `comment`, are modeled in code but not all are present in the shipped MySQL DDL—extend `schema.sql` if you need them in the database).

```mermaid
erDiagram
    USER ||--o{ APPOINTMENT : manages_or_attends
    USER ||--o{ PRESCRIPTION : issues
    USER ||--o{ WARNING_OVERRIDE : authorizes
    USER ||--o{ COMMENT : writes
    PATIENT ||--o{ APPOINTMENT : attends
    PATIENT ||--o{ PATIENT_CONDITION : has
    PATIENT ||--o{ INCIDENT : involved_in
    PATIENT ||--o{ ADVERSE_REACTION : suffers_from
    PATIENT ||--o{ COMMENT : receives
    CLINIC ||--o{ APPOINTMENT : hosts
    APPOINTMENT ||--o{ PRESCRIPTION : results_in
    CONDITION ||--o{ PATIENT_CONDITION : describes
    MEDICATION ||--o{ PRESCRIPTION : contains
    MEDICATION ||--o{ ADVERSE_REACTION : causes
    MEDICATION ||--o{ WARNING_OVERRIDE : triggers

    USER {
        int user_id PK
        string username
        string password_hash
        string role
    }

    PATIENT {
        int patient_id PK
        string first_name
        string last_name
        string address
        boolean is_homeless
        string risk_status
        boolean is_deceased
        boolean self_harm_history
    }

    CLINIC {
        int clinic_id PK
        string name
        string location_type
    }

    APPOINTMENT {
        int appointment_id PK
        int patient_id FK
        int clinic_id FK
        int staff_id FK
        date appointment_date
        string type
        string status
        boolean records_updated
    }

    CONDITION {
        int condition_id PK
        string name
    }

    PATIENT_CONDITION {
        int patient_id FK
        int condition_id FK
        date diagnosis_date
    }

    MEDICATION {
        int medication_id PK
        string name
    }

    PRESCRIPTION {
        int prescription_id PK
        int appointment_id FK
        int medication_id FK
        int prescriber_id FK
        date issue_date
        boolean is_repeat
    }

    INCIDENT {
        int incident_id PK
        int patient_id FK
        string type
        string description
        date incident_date
    }

    ADVERSE_REACTION {
        int reaction_id PK
        int patient_id FK
        int medication_id FK
        string description
    }

    WARNING_OVERRIDE {
        int override_id PK
        int prescriber_id FK
        int medication_id FK
        string warning_details
        date override_date
    }

    COMMENT {
        int comment_id PK
        int patient_id FK
        int clinician_id FK
        string free_form_text
        date comment_date
    }

    CHANGE_REQUEST {
        int request_id PK
        string raw_patient_data
        string requested_changes
        string status
    }
```

**Field notes (domain semantics)**

- **USER.role:** `Clinical`, `Receptionist`, `Medical_Records`.  
- **CLINIC.location_type:** e.g. Hospital, Health Centre.  
- **APPOINTMENT.type:** e.g. Drop-in, Pre-arranged. **APPOINTMENT.status:** e.g. Attended, Missed, Pending.  
- **INCIDENT.type:** e.g. Deliberate, Accidental.  
- **CHANGE_REQUEST.status:** e.g. Pending, Accepted, Rejected. Change requests store raw payloads only and do not reference `patient_id` in the implemented DAO.

## Frontend: which service to call for what data

The HTTP API is a single Jersey app: there is **no** path prefix beyond the host you set in `api.baseUri` (e.g. `http://localhost:8080/`). Every request must send **HTTP Basic Auth** with a user whose **role** matches the endpoint (see table below). JSON uses **camelCase** property names (Jackson).

| If the UI needs… | Call (method + path) | Backend class | Role required | Response (JSON shape) |
|------------------|----------------------|----------------|-----------------|------------------------|
| A list of all patients | `GET /patients` | [`PatientService`](src/main/java/com/skillonnet/automation/api/PatientService.java) | `Clinical` | Array of patient objects: `patientId`, `firstName`, `lastName`, `address`, `homeless`, `riskStatus`, `deceased`, `selfHarmHistory` |
| One patient’s details | `GET /patients/{id}` | same | `Clinical` | Single patient object (same fields as above) |
| Save edits to a patient | `PUT /patients/{id}` | same | `Clinical` | Updated patient object (body must be the same JSON shape; `patientId` in body must match `{id}` or be omitted/zero) |
| Create a new appointment | `POST /appointments` | [`AppointmentService`](src/main/java/com/skillonnet/automation/api/AppointmentService.java) | `Receptionist` | Appointment object including generated `appointmentId`, plus `patientId`, `clinicId`, `staffId`, `appointmentDate`, `type`, `status`, `recordsUpdated` |
| Mark attendance / status for an appointment | `PUT /appointments/{id}/attendance` | same | `Receptionist` | **204** empty body; send JSON `{"status":"Attended"}` (or `Missed`, etc.) |
| Who missed appointments on a given day | `GET /appointments/missed?date=YYYY-MM-DD` | same | `Receptionist` | Array of rows: `appointmentId`, `patientId`, `firstName`, `lastName` |
| Appointments still needing record updates | `GET /appointments/pending-records` | same | `Receptionist` | Array of appointments: `appointmentId`, `patientId`, `clinicId`, `staffId`, `appointmentDate`, `type`, `status`, `recordsUpdated` |
| Management: patients per clinic | `GET /reports/patients-per-clinic` | [`ReportingService`](src/main/java/com/skillonnet/automation/api/ReportingService.java) | `Medical_Records` | Array: `clinicId`, `clinicName`, `patientCount` |
| Management: prescription volume by drug | `GET /reports/prescription-stats` | same | `Medical_Records` | Array: `medicationId`, `medicationName`, `prescriptionCount` |
| Submit a patient data change request | `POST /reports/change-requests` | same | `Medical_Records` | **201** with JSON `{"requestId":…}` (class [`CreatedId`](src/main/java/com/skillonnet/automation/api/CreatedId.java)); request body: `rawPatientData`, `requestedChanges` (strings) |

**Auth from the browser:** send an `Authorization: Basic …` header (Base64 of `username:password`). Wrong or missing credentials → **401**; correct user but wrong role for that path → **403** (JSON `{"error":"forbidden"}`). Server/database errors on supported routes may return **500** with `{"error":"…"}`.

**Not exposed over HTTP today:** prescription warnings, overrides, incidents, and related logic live under [`ClinicalLogicService`](src/main/java/com/skillonnet/automation/service/ClinicalLogicService.java) and DAOs only—there is no REST surface for those yet. Extend with new resource classes if the frontend needs them.

## Project layout

| Area | Package / path |
|------|----------------|
| REST resources | `com.skillonnet.automation.api` |
| JDBC DAOs | `com.skillonnet.automation.dao` |
| Domain models | `com.skillonnet.automation.model` |
| Clinical rules (prescriptions / incidents) | `com.skillonnet.automation.service` |
| MySQL DDL | `src/main/resources/schema.sql` |

## License

This project is provided as-is for coursework or demonstration unless you add a separate license.
