# Employee Management System

A full-stack CRUD application for managing employee records, built with **Spring Boot 3** (backend) and **React + Vite** (frontend). Phase 1 adds JWT-based authentication, BCrypt password storage, and role-based access control (RBAC) on top of the original employee CRUD.

---

## Features

- Stateless JWT authentication (short-lived access token + rotating, revocable refresh token)
- BCrypt password hashing — no plaintext or reversibly-encrypted passwords anywhere
- Role-based access control with four roles: `EMPLOYEE`, `MANAGER`, `HR_ADMIN`, `SYSTEM_ADMIN`
- Strict separation between a user's login identity (`User`) and their HR record (`Employee`)
- Self-service profile: any authenticated user can view/update their own linked employee record
- HR/admin employee CRUD with soft deactivation (employees are deactivated, never hard-deleted)
- SYSTEM_ADMIN-only user administration: create users, enable/disable, change roles, link an employee record, revoke active sessions
- One-time initial administrator bootstrap via environment variables (no public registration endpoint)
- Consistent, structured JSON error responses with machine-readable error codes
- React route guards (authentication + role) and a role-aware navigation bar
- Documented frontend token-storage strategy, including its XSS tradeoff

---

## Technology Stack

| Layer        | Technology                                                          |
|--------------|----------------------------------------------------------------------|
| Backend      | Java 17, Spring Boot 3.2, Spring Security 6, Spring Data JPA        |
| Auth         | JWT (JJWT 0.12.6), BCrypt, DB-stored hashed rotating refresh tokens  |
| Migrations   | Flyway (MySQL dialect)                                              |
| Database     | MySQL 8 (H2 in-memory for backend tests)                            |
| Frontend     | React 18, Vite 5, React Router 6, Axios 1.6                         |
| Styling      | Bootstrap 5.3                                                       |
| Build        | Maven (backend), npm (frontend)                                     |

---

## Repository Structure

```
employee-management-system/
├── backend/                          # Spring Boot application
│   ├── src/
│   │   ├── main/java/com/example/employeemanagement/
│   │   │   ├── EmployeeManagementApplication.java
│   │   │   ├── auth/                          # AuthService, RefreshTokenService, AuthenticationController
│   │   │   ├── config/AdminBootstrapRunner.java
│   │   │   ├── controller/                    # EmployeeController, CurrentUserController, AdminUserController
│   │   │   ├── dto/                            # EmployeeDto, UserResponse, AuthenticationResponse, request DTOs
│   │   │   ├── entity/                         # Employee, User, Role, RoleName, RefreshToken
│   │   │   ├── exception/                      # ErrorResponse, GlobalExceptionHandler, business-rule exceptions
│   │   │   ├── mapper/                         # EmployeeMapper, UserMapper
│   │   │   ├── repository/
│   │   │   ├── security/                       # JwtService, JwtAuthenticationFilter, SecurityConfig,
│   │   │   │                                    #   CustomUserDetailsService, EmployeeAuthorizationService, …
│   │   │   └── service/                        # EmployeeService(Impl), UserService
│   │   ├── main/resources/
│   │   │   ├── application.properties
│   │   │   └── db/migration/                   # Flyway: V1 employees, V2 auth tables, V3 employees.active
│   │   └── test/…                              # Unit + MockMvc + full-stack authorization integration tests
│   ├── .env.example
│   ├── mvnw / mvnw.cmd
│   └── pom.xml
├── frontend/                         # React + Vite application
│   ├── src/
│   │   ├── components/               # Header, Footer, ProtectedRoute, RoleProtectedRoute
│   │   ├── constants/roles.js
│   │   ├── context/                  # AuthContext, AuthProvider
│   │   ├── hooks/useAuth.js
│   │   ├── pages/                    # LoginPage, AccessDeniedPage, ProfilePage, UserAdminPage,
│   │   │                             #   EmployeeListPage, EmployeeFormPage, NotFoundPage
│   │   ├── services/                 # httpClient (axios + interceptors), authService, userService, employeeService
│   │   ├── utils/tokenStorage.js
│   │   ├── styles/index.css
│   │   ├── App.jsx
│   │   └── main.jsx
│   ├── .env.example
│   ├── index.html
│   ├── package.json
│   └── vite.config.js
├── .gitignore
└── README.md
```

---

## Prerequisites

| Tool        | Minimum Version | Notes                              |
|-------------|------------------|-------------------------------------|
| Java JDK    | 17               | `java -version`                    |
| Maven       | 3.9              | or use the included `./mvnw`       |
| Node.js     | 18               | `node -v`                          |
| npm         | 9                | bundled with Node                  |
| MySQL       | 8                | running locally or accessible      |

---

## MySQL Database Setup

```sql
-- Connect to MySQL as root (or any user with CREATE privilege)
CREATE DATABASE employee_management CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- Optional: create a dedicated user
CREATE USER 'emp_user'@'localhost' IDENTIFIED BY 'change_me';
GRANT ALL PRIVILEGES ON employee_management.* TO 'emp_user'@'localhost';
FLUSH PRIVILEGES;
```

The schema is owned by **Flyway** migrations in `backend/src/main/resources/db/migration` (not Hibernate `ddl-auto`). On first startup against an empty database, Flyway creates everything: `employees`, `roles`, `users`, `user_roles`, `refresh_tokens`. Hibernate is set to `ddl-auto=validate` — it only checks the live schema matches the JPA entities and never alters it.

> If you are upgrading an existing pre-Phase-1 database that already has an `employees` table created by the old `ddl-auto=update` behavior, Flyway is configured to baseline at version 1 (`spring.flyway.baseline-on-migrate=true`, `spring.flyway.baseline-version=1`) so it adopts that table instead of trying to recreate it.

---

## Backend Environment Variables

Spring Boot reads these from the shell/IDE run-configuration environment (not from a `.env` file directly — `backend/.env.example` is a template you copy values from, not a file Spring Boot loads).

| Variable                 | Default                                            | Description                                                        |
|---------------------------|-----------------------------------------------------|----------------------------------------------------------------------|
| `DB_URL`                 | `jdbc:mysql://localhost:3306/employee_management`  | JDBC connection URL                                                 |
| `DB_USERNAME`             | `root`                                              | Database username                                                   |
| `DB_PASSWORD`             | *(empty)*                                           | Database password                                                   |
| `JPA_DDL_AUTO`            | `validate`                                          | Hibernate DDL mode — leave as `validate`; schema is owned by Flyway |
| `FRONTEND_URL`            | `http://localhost:5173`                             | Allowed CORS origin(s), comma-separated                             |
| `JWT_SECRET`              | dev-only insecure placeholder                       | HMAC signing key for JWTs — **must** be a long random secret (32+ bytes) outside local dev |
| `JWT_ACCESS_EXPIRATION`   | `900000` (15 minutes, in ms)                        | Access token lifetime                                                |
| `JWT_REFRESH_EXPIRATION`  | `604800000` (7 days, in ms)                         | Refresh token lifetime                                               |
| `JWT_ISSUER`              | `employee-management`                               | `iss` claim embedded in issued JWTs                                  |
| `INITIAL_ADMIN_EMAIL`     | *(empty — bootstrap skipped)*                        | One-time SYSTEM_ADMIN bootstrap email (see below)                    |
| `INITIAL_ADMIN_PASSWORD`  | *(empty — bootstrap skipped)*                        | One-time SYSTEM_ADMIN bootstrap password (see below)                 |

See `backend/.env.example` for the full annotated list.

### Set variables — macOS / Linux

```bash
export DB_URL=jdbc:mysql://localhost:3306/employee_management
export DB_USERNAME=root
export DB_PASSWORD=your_password
export FRONTEND_URL=http://localhost:5173
export JWT_SECRET=$(openssl rand -hex 32)
export INITIAL_ADMIN_EMAIL=admin@example.com
export INITIAL_ADMIN_PASSWORD=ChangeMe123!
```

### Set variables — Windows PowerShell

```powershell
$env:DB_URL = "jdbc:mysql://localhost:3306/employee_management"
$env:DB_USERNAME = "root"
$env:DB_PASSWORD = "your_password"
$env:FRONTEND_URL = "http://localhost:5173"
$env:JWT_SECRET = "a-long-random-32-plus-byte-secret"
$env:INITIAL_ADMIN_EMAIL = "admin@example.com"
$env:INITIAL_ADMIN_PASSWORD = "ChangeMe123!"
```

If you're running the backend from IntelliJ instead of the shell, set these same variables in **Run Configuration → Environment variables**.

---

## Initial Administrator Bootstrap

There is intentionally **no public registration endpoint** — every account after the first is created by a `SYSTEM_ADMIN` through `/api/v1/admin/users`. To get the very first administrator:

1. Set `INITIAL_ADMIN_EMAIL` and `INITIAL_ADMIN_PASSWORD` before starting the backend (see above).
2. Start the backend. On startup, `AdminBootstrapRunner` checks: if no `SYSTEM_ADMIN` exists yet *and* both variables are set, it creates one with a BCrypt-hashed password and logs `Created initial SYSTEM_ADMIN account for <email>` — the plaintext password is never logged.
3. If a `SYSTEM_ADMIN` already exists, or either variable is blank, the runner is a no-op and the app starts normally either way.
4. **Unset both variables after the first successful login.** Leaving them set is harmless (the runner keeps no-op'ing once an admin exists) but unnecessary.
5. Log in at the frontend's `/login` page with that email/password, then use the **User Administration** page (`/admin/users`) to create the rest of your users and assign roles.

---

## Roles & Permissions

| Capability                                   | EMPLOYEE | MANAGER | HR_ADMIN | SYSTEM_ADMIN |
|-----------------------------------------------|:--------:|:-------:|:--------:|:------------:|
| View/update own profile (`/employees/me`)     | ✅       | ✅      | ✅       | ✅           |
| List all employees                            | ❌       | ✅ *(temporary, see note)* | ✅ | ✅ |
| View any employee by ID                        | ❌ (own only) | ✅ | ✅ | ✅ |
| Create / update employees                      | ❌       | ❌      | ✅       | ✅           |
| Deactivate employees (soft delete)              | ❌       | ❌      | ✅       | ✅           |
| `/api/v1/users/me` (own auth identity)         | ✅       | ✅      | ✅       | ✅           |
| User administration (`/api/v1/admin/users/**`) | ❌       | ❌      | ❌       | ✅           |

> **MANAGER read access note:** `MANAGER` is granted full, org-wide read access to the employee list as a deliberate, documented temporary measure (see the comment on `EmployeeController.getAllEmployees`) — there is no manager/team-hierarchy concept yet. Narrow this to "direct reports only" once that hierarchy exists.

Authorization is enforced at two independent layers:
- **Request-matcher level** (`SecurityConfig`): `/api/v1/auth/**` is public, `/api/v1/admin/**` requires `SYSTEM_ADMIN`, everything else just requires authentication.
- **Method level** (`@PreAuthorize` + `@EnableMethodSecurity`): per-endpoint role checks, plus ownership checks via `EmployeeAuthorizationService.isSelf(...)` for "view my own record" cases.

---

## Authentication Flow

1. `POST /api/v1/auth/login` with `{ email, password }` → `AuthenticationResponse` (`accessToken`, `refreshToken`, `tokenType`, `expiresIn` (seconds), `user`).
2. The frontend stores both tokens (see **Frontend Token Storage** below) and attaches `Authorization: Bearer <accessToken>` to every subsequent API call.
3. When an access token expires, the next API call gets `401`. The frontend automatically calls `POST /api/v1/auth/refresh` with `{ refreshToken }`, receives a **new** access token *and* a **new, rotated** refresh token, stores both, and retries the original request once. If the refresh token is itself invalid/expired/revoked, the user is logged out client-side.
4. `POST /api/v1/auth/logout` with `{ refreshToken }` revokes that refresh token server-side (hashed at rest in the `refresh_tokens` table) and clears local tokens.
5. Refresh tokens are never accepted as query parameters — only in the JSON request body — and are rotated on every use (the old one is invalidated the moment a new one is issued).

### Frontend Token Storage

Tokens are kept in `localStorage` (see `frontend/src/utils/tokenStorage.js`), not an httpOnly cookie, since this project has no backend-set-cookie infrastructure. **This is a deliberate tradeoff, not an oversight:** any successful XSS on the frontend origin can read both tokens. It is mitigated by:
- short-lived access tokens (15 minutes by default),
- rotating, server-revocable refresh tokens (hashed at rest, never sent in URLs),
- React's default JSX escaping, which limits injection points.

It is **not** as strong as httpOnly cookies + `SameSite`. If this app ever needs to defend against a higher XSS risk profile, moving refresh-token storage to an httpOnly cookie (with the backend issuing `Set-Cookie`) is the recommended next step — it is out of scope for this phase.

---

## Frontend Environment Variables

Create `frontend/.env` (Git-ignored) from the example:

```bash
cp frontend/.env.example frontend/.env
```

| Variable            | Default                          | Description           |
|---------------------|-----------------------------------|------------------------|
| `VITE_API_BASE_URL` | `http://localhost:8080/api/v1`   | Backend API base URL  |

---

## Backend Startup

```bash
cd backend

# Export variables (see above), then:
./mvnw spring-boot:run          # macOS / Linux
.\mvnw.cmd spring-boot:run      # Windows

# Or with Maven installed globally:
mvn spring-boot:run
```

The backend starts on **http://localhost:8080**. Watch the startup log for Flyway's migration summary and (if configured) the admin-bootstrap line.

---

## Frontend Startup

```bash
cd frontend
npm install
npm run dev
```

The frontend starts on **http://localhost:5173** (or the next free port — if Vite picks a different one, update `FRONTEND_URL` on the backend to match).

---

## API Endpoints

### Auth (public — `/api/v1/auth/**`)

| Method | Path                   | Body                          | Description                          | Success |
|--------|------------------------|--------------------------------|----------------------------------------|---------|
| POST   | `/api/v1/auth/login`   | `{ email, password }`         | Authenticate, issue token pair        | 200     |
| POST   | `/api/v1/auth/refresh` | `{ refreshToken }`            | Rotate refresh token, issue new pair  | 200     |
| POST   | `/api/v1/auth/logout`  | `{ refreshToken }`            | Revoke the refresh token              | 204     |

### Current user (any authenticated user)

| Method | Path                  | Description                          | Success |
|--------|------------------------|----------------------------------------|---------|
| GET    | `/api/v1/users/me`     | Current user's auth identity + roles + linked employee summary | 200 |

### Employees

| Method | Path                          | Required role(s)                     | Description                          | Success |
|--------|--------------------------------|----------------------------------------|----------------------------------------|---------|
| GET    | `/api/v1/employees`            | `HR_ADMIN`, `SYSTEM_ADMIN`, `MANAGER` | List all employees                    | 200     |
| GET    | `/api/v1/employees/me`         | any authenticated user                 | Get own linked employee record        | 200     |
| PUT    | `/api/v1/employees/me`         | any authenticated user                 | Update own name fields (not email)    | 200     |
| GET    | `/api/v1/employees/{id}`       | `HR_ADMIN`/`SYSTEM_ADMIN`/`MANAGER`, or self | Get employee by ID              | 200     |
| POST   | `/api/v1/employees`            | `HR_ADMIN`, `SYSTEM_ADMIN`              | Create employee                       | 201     |
| PUT    | `/api/v1/employees/{id}`       | `HR_ADMIN`, `SYSTEM_ADMIN`              | Update employee                       | 200     |
| DELETE | `/api/v1/employees/{id}`       | `HR_ADMIN`, `SYSTEM_ADMIN`              | **Soft** deactivate (sets `active=false`) | 204 |

### Admin user management (`SYSTEM_ADMIN` only — `/api/v1/admin/users/**`)

| Method | Path                                       | Description                              | Success |
|--------|----------------------------------------------|---------------------------------------------|---------|
| GET    | `/api/v1/admin/users`                       | List all user accounts                     | 200     |
| GET    | `/api/v1/admin/users/{id}`                  | Get one user account                        | 200     |
| POST   | `/api/v1/admin/users`                       | Create user (email, password, roles, optional employeeId) | 201 |
| POST   | `/api/v1/admin/users/{id}/enable`           | Enable account                              | 200     |
| POST   | `/api/v1/admin/users/{id}/disable`          | Disable account                             | 200     |
| PUT    | `/api/v1/admin/users/{id}/roles`            | Replace role set (blocks removing the last `SYSTEM_ADMIN`) | 200 |
| PUT    | `/api/v1/admin/users/{id}/employee`         | Link an `Employee` record to this user      | 200     |
| POST   | `/api/v1/admin/users/{id}/revoke-tokens`    | Revoke all active refresh tokens for this user | 204  |

### Error response shape

```json
{
  "timestamp": "2026-06-16T12:00:00",
  "status": 409,
  "errorCode": "LAST_ADMIN_ROLE",
  "message": "Cannot remove the SYSTEM_ADMIN role from the last remaining administrator.",
  "path": "/api/v1/admin/users/3/roles",
  "fieldErrors": null
}
```

`fieldErrors` is populated (and `errorCode` is `VALIDATION_FAILED`) for Bean Validation failures; otherwise it is `null`. Known `errorCode` values: `RESOURCE_NOT_FOUND`, `DUPLICATE_EMAIL`, `EMPLOYEE_ALREADY_LINKED`, `LAST_ADMIN_ROLE`, `SELF_DISABLE_NOT_ALLOWED`, `UNKNOWN_ROLE`, `INVALID_REFRESH_TOKEN`, `ACCOUNT_DISABLED`, `INVALID_CREDENTIALS`, `VALIDATION_FAILED`, `UNAUTHENTICATED`, `ACCESS_DENIED`, `INTERNAL_ERROR`.

---

## Running Tests

The backend test suite (unit + `@WebMvcTest` + full-stack `@SpringBootTest` authorization integration tests) runs against **H2 in-memory** — **no running MySQL is required.**

```bash
cd backend
./mvnw test          # macOS / Linux
.\mvnw.cmd test      # Windows
mvn test             # if Maven is installed globally
```

The frontend has no test framework configured (none was introduced as part of this phase); rely on `npm run lint` and `npm run build` plus manual verification in the browser.

---

## Building for Production

### Backend JAR

```bash
cd backend
./mvnw package -DskipTests
# Output: target/employee-management-0.0.1-SNAPSHOT.jar
java -jar target/employee-management-0.0.1-SNAPSHOT.jar
```

### Frontend static bundle

```bash
cd frontend
npm run lint   # must be zero warnings/errors (max-warnings 0)
npm run build
# Output: dist/  (serve with nginx, Apache, or any static host)
```

---

## Example curl Commands

```bash
# Log in
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@example.com","password":"ChangeMe123!"}'

# Use the returned accessToken for everything else
TOKEN="paste-access-token-here"

# Current user
curl http://localhost:8080/api/v1/users/me -H "Authorization: Bearer $TOKEN"

# List employees (HR_ADMIN / SYSTEM_ADMIN / MANAGER only)
curl http://localhost:8080/api/v1/employees -H "Authorization: Bearer $TOKEN"

# Create an employee (HR_ADMIN / SYSTEM_ADMIN only)
curl -X POST http://localhost:8080/api/v1/employees \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"firstName":"Jane","lastName":"Doe","email":"jane.doe@example.com"}'

# Refresh tokens
curl -X POST http://localhost:8080/api/v1/auth/refresh \
  -H "Content-Type: application/json" \
  -d '{"refreshToken":"paste-refresh-token-here"}'

# Log out (revokes the refresh token)
curl -X POST http://localhost:8080/api/v1/auth/logout \
  -H "Content-Type: application/json" \
  -d '{"refreshToken":"paste-refresh-token-here"}'
```

---

## Configuring on Another Computer

1. Clone the repository.
2. Install Java 17, Node 18, and MySQL 8.
3. Create the `employee_management` database (see MySQL Setup above).
4. Set the backend environment variables, including a real `JWT_SECRET` and (for the first run only) `INITIAL_ADMIN_EMAIL`/`INITIAL_ADMIN_PASSWORD`.
5. Copy `frontend/.env.example` → `frontend/.env` and adjust `VITE_API_BASE_URL` if needed.
6. Start the backend, then the frontend, then log in at `/login` with the bootstrapped admin account.

---

## Troubleshooting

### MySQL connection failure (`Communications link failure`)
- Confirm MySQL is running: `mysql -u root -p`
- Check `DB_URL` — database name, host, and port must match.
- Ensure the database exists: `SHOW DATABASES LIKE 'employee_management';`

### Flyway error: table already exists / schema mismatch
- This usually means you're pointing at a pre-Phase-1 database that already has an `employees` table from the old `ddl-auto=update` behavior. The baseline settings in `application.properties` (`spring.flyway.baseline-on-migrate=true`, `baseline-version=1`) are meant to handle this automatically on a fresh adoption — if you still hit a mismatch, inspect the `flyway_schema_history` table before resorting to dropping anything.

### 401 `UNAUTHENTICATED` on every request
- Make sure you're sending `Authorization: Bearer <accessToken>` and that the token hasn't expired (default 15 minutes). The frontend's axios interceptor handles refreshing this automatically — a raw `curl` session will not.

### 403 Forbidden on an endpoint you expect to work
- Check the **Roles & Permissions** table above — the account's roles (visible via `GET /api/v1/users/me`) must include one of the roles required by that endpoint.

### Can't create the first user / no login works
- No `SYSTEM_ADMIN` exists yet and there's no public registration endpoint by design. Set `INITIAL_ADMIN_EMAIL`/`INITIAL_ADMIN_PASSWORD` and restart the backend (see **Initial Administrator Bootstrap**).

### Port 8080 already in use
```bash
lsof -i :8080                   # macOS / Linux
netstat -ano | findstr :8080    # Windows

export SERVER_PORT=8081
./mvnw spring-boot:run
```
Then update `VITE_API_BASE_URL` in `frontend/.env` to use port 8081.

### Port 5173 already in use
Vite will automatically try the next available port (5174, 5175, …). Update `FRONTEND_URL` in the backend environment and restart both servers.

### CORS errors in the browser
- Check that `FRONTEND_URL` matches the exact origin shown in the browser address bar (scheme, host, and port).
- Re-export the variable and restart the backend.

### `VITE_API_BASE_URL` is undefined
- Make sure `frontend/.env` exists (the `.env.example` file is only a template).
- Restart `npm run dev` after creating or editing `.env`.
- All Vite env variables must start with `VITE_`.

---

## Security Notes & Known Limitations

- **Token storage:** see **Frontend Token Storage** above — `localStorage`, not httpOnly cookies. Acceptable for this phase, documented tradeoff.
- **MANAGER read scope:** currently org-wide read access to all employees, intended to be narrowed to "direct reports" once a team/manager hierarchy is modeled — see the TODO comment on `EmployeeController.getAllEmployees`.
- **No public registration:** by design. All accounts originate from the one-time admin bootstrap or `SYSTEM_ADMIN`-created accounts.
- **Soft delete only:** employees are deactivated (`active=false`), never hard-deleted, preserving historical/audit data.
- **Last-admin protection:** the backend rejects any role change that would leave zero `SYSTEM_ADMIN` accounts (`LAST_ADMIN_ROLE` error), preventing total lockout.
- **Secrets:** `JWT_SECRET`, DB credentials, and admin bootstrap credentials are all environment-variable-driven with dev-only placeholders in `application.properties`; never commit real values — `backend/.env` and `frontend/.env` are both Git-ignored, only the `.env.example` templates are tracked.

---

## Phase 2 — Kafka-Powered HR Activity Pipeline

### Overview

Every time an HR-level user (HR\_ADMIN or SYSTEM\_ADMIN) manipulates employee records, the backend publishes a structured JSON event to a Kafka topic. A separate consumer within the same application reads that event and stores it as a permanent activity log in MySQL. SYSTEM\_ADMIN users can browse the full audit trail through the **System Activity** page in the frontend.

```
EmployeeController
    ↓  (HTTP request)
EmployeeService  ─── DB write committed ───▶  schedules after-commit callback
    ↓
EmployeeActivityEventProducer.publish()
    ↓
Kafka topic: hr.employee.events
    ↓
EmployeeActivityEventConsumer.consume()
    ↓
SystemActivityLogService.save()
    ↓
system_activity_logs table (MySQL)
    ↓
GET /api/v1/system/activity-logs  (SYSTEM_ADMIN only)
    ↓
System Activity page (React, SYSTEM_ADMIN only)
```

---

### Kafka Concepts Used in This Project

| Concept | Role in this app |
|---------|-----------------|
| **Producer** | `EmployeeActivityEventProducer` — publishes events after a successful employee operation |
| **Consumer** | `EmployeeActivityEventConsumer` — subscribes to the topic and persists activity logs |
| **Topic** | `hr.employee.events` — the message channel; auto-created on first publish |
| **Consumer group** | `hr-employee-activity-consumer` — tracks which messages the consumer has processed |
| **Event** | `EmployeeActivityEvent` — JSON DTO carrying who did what, to whom, and when |
| **Offset** | Kafka's per-partition cursor; `auto-offset-reset=earliest` means the consumer starts from the beginning of the topic if it has no saved offset |

---

### Local Kafka Setup (Docker)

Kafka runs via Docker Compose. No local Kafka installation is needed.

**Start Kafka and Kafka UI:**
```bash
docker compose up -d
```

**View live logs:**
```bash
docker compose logs -f kafka
```

**Stop everything:**
```bash
docker compose down
```

**Wipe data and start fresh:**
```bash
docker compose down -v
docker compose up -d
```

**Kafka UI** (view topics, messages, consumers in a browser):
```
http://localhost:8085
```

Ports used:
| Service | Port |
|---------|------|
| Kafka broker | `9092` |
| Kafka UI | `8085` |
| Backend API | `8080` |
| Frontend dev | `5173` |

---

### Kafka Environment Variables

Add to your IDE run configuration or shell environment:

| Variable | Default | Purpose |
|----------|---------|---------|
| `KAFKA_BOOTSTRAP_SERVERS` | `localhost:9092` | Kafka broker address |
| `EMPLOYEE_EVENTS_TOPIC` | `hr.employee.events` | Topic name for employee events |
| `KAFKA_CONSUMER_GROUP` | `hr-employee-activity-consumer` | Consumer group ID |

---

### Implemented Event Types

| `eventType` | Triggered by |
|-------------|-------------|
| `EMPLOYEE_CREATED` | `POST /api/v1/employees` |
| `EMPLOYEE_UPDATED` | `PUT /api/v1/employees/{id}` |
| `EMPLOYEE_DEACTIVATED` | `DELETE /api/v1/employees/{id}` (soft deactivation) |
| `EMPLOYEE_LINKED_TO_USER` | `PUT /api/v1/admin/users/{id}/employee` |

Events are **never** published for failed operations (validation errors, 401, 403, DB failures). Events contain no passwords, tokens, or secrets.

---

### Activity Log API

```
GET /api/v1/system/activity-logs
```

**Access:** SYSTEM\_ADMIN only. Returns 401 for unauthenticated, 403 for insufficient role.

**Query parameters:**

| Parameter | Default | Description |
|-----------|---------|-------------|
| `page` | `0` | Zero-based page number |
| `size` | `20` | Records per page |
| `eventType` | _(none)_ | Filter by event type (e.g. `EMPLOYEE_CREATED`) |

**Response shape (Spring Page):**
```json
{
  "content": [
    {
      "id": 1,
      "eventId": "uuid",
      "eventType": "EMPLOYEE_CREATED",
      "entityType": "EMPLOYEE",
      "entityId": 42,
      "actorEmail": "admin@company.com",
      "actorRoles": "HR_ADMIN",
      "message": "HR admin admin@company.com created employee Jane Smith.",
      "occurredAt": "2026-06-18T14:00:00Z",
      "consumedAt": "2026-06-18T14:00:01Z",
      "sourceTopic": "hr.employee.events"
    }
  ],
  "totalElements": 1,
  "totalPages": 1,
  "number": 0,
  "size": 20
}
```

---

### Known Limitation — Event Delivery

> **Events are published after the DB transaction commits (after-commit callback), but before the Kafka `send()` is confirmed.** If Kafka is temporarily unavailable, the employee operation succeeds but the event is silently lost — only a log warning is emitted. The application does **not** roll back the employee change.
>
> For production-grade guaranteed delivery the next improvement would be the **transactional outbox pattern**: write the event to a dedicated DB table inside the same transaction as the employee change, then have a separate relay process publish to Kafka and delete the row on confirmation.

---

### Troubleshooting Kafka

**Kafka not starting:**
```bash
docker compose logs kafka
docker compose down -v && docker compose up -d
```

**Cannot connect to `localhost:9092`:**
- Confirm `docker compose up -d` completed successfully
- Run `docker ps` and verify `kafka` container is `Up`
- Check `KAFKA_BOOTSTRAP_SERVERS` environment variable matches the broker address

**Topic not created / messages not appearing:**
- Topics are auto-created on first publish (`auto.create.topics.enable=true`)
- Open Kafka UI at `http://localhost:8085` → Topics → confirm `hr.employee.events` exists
- Perform any create/update/deactivate employee action and refresh

**Consumer not receiving messages:**
- Verify the backend started after Kafka was up
- Check backend logs for `"Published EMPLOYEE_* event"` and `"Consuming EMPLOYEE_* event"` lines
- In Kafka UI → Consumer Groups → `hr-employee-activity-consumer` → confirm lag is 0

**Activity log page is empty:**
- Confirm you are logged in as SYSTEM\_ADMIN
- Confirm at least one employee create/update/deactivate has been performed
- Check backend logs — if `"Failed to publish"` appears, Kafka was not reachable
- Refresh the page or use the **Refresh** button

**401 / 403 errors on the activity log page:**
- 401 → not logged in; go to `/login`
- 403 → logged in but not SYSTEM\_ADMIN; only SYSTEM\_ADMIN can view this page
