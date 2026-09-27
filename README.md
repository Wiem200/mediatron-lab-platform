# MEDIATRON Laboratory Management System

Sprint 1 delivers the identity, security, and laboratory staff management baseline for MEDIATRON. The platform uses a decoupled client-server architecture with a Spring Boot backend and a React + Vite frontend.

## Sprint 1 Scope

### 1. Technical Infrastructure and Database
- PostgreSQL is configured through environment variables and supports a Neon-hosted deployment model.
- Flyway is enabled for database versioning and initializes the schema from `backend/Lab_Management/src/main/resources/db/migration`.
- Core Sprint 1 migrations create the security and staff management tables for users, roles, members, `phd_students`, and `member_research_axes`.

### 2. Backend Security and Business Logic
- Authentication is stateless and JWT-based.
- Passwords are hashed with BCrypt.
- Global CORS configuration allows the React development app to communicate with the Spring Boot API on localhost ports such as `5173` and `5179`.
- Self-registered users are created with `PENDING` status.
- Admin-created member accounts are created with `APPROVED` status.
- DTO mapping is used to expose member and PhD student data without leaking password fields.

### 3. Backend CRUD Endpoints
- `/api/auth/login` and `/api/auth/register` handle authentication flows.
- `/api/members/*` provides member listing and admin CRUD operations.
- `/api/phd-students/*` provides PhD student listing and admin CRUD operations.
- PhD students are linked to a lab member supervisor through a many-to-one relationship.

### 4. Frontend UI and UX
- The admin interface uses a dark blue and gold visual system aligned with the laboratory branding.
- Login persists the authenticated JWT payload in `localStorage`.
- Sidebar navigation is role-aware for admin, lab head, and member views.
- Protected routes block unauthenticated access to private screens.
- The laboratory directory provides tabbed management for members and PhD students.
- Member forms capture profile, contact, ORCID, biography, and scientific metadata.
- PhD student forms capture thesis, registration year, research team, and supervisor metadata.

### 5. Sprint 2 Readiness
- `MemberPublication` is prepared as a join entity with an `is_verified` flag for publication verification workflows.

## Project Structure

- `backend/Lab_Management`: Spring Boot API, security, business logic, and Flyway migrations.
- `frontend`: React + Vite application for authentication and administration.
- `ai_service`: auxiliary experimentation scripts.

## Local Setup

### Backend
1. Set the database and JWT environment variables as needed:
   - `SPRING_DATASOURCE_URL`
   - `SPRING_DATASOURCE_USERNAME`
   - `SPRING_DATASOURCE_PASSWORD`
   - `MEDIATRON_APP_JWT_SECRET`
2. Start the Spring Boot application from `backend/Lab_Management`.
3. Flyway will apply pending migrations automatically at startup.
4. The API runs on `http://localhost:8080` by default.

### Frontend
1. Open `frontend`.
2. Install dependencies with `npm install`.
3. Start the dev server with `npm run dev`.
4. Open the Vite URL shown in the terminal, typically `http://localhost:5173`.

## Notes

- The backend reads database configuration from environment variables, so the README no longer assumes a manually created local PostgreSQL database.
- Sprint 1 focuses on identity, approval-aware access control, and laboratory staff management. Publication workflows are partially scaffolded for the next sprint.
