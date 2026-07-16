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

---

## Phase 3 — Employee Time Clock with Kafka Event Tracking

### Overview

Employees can clock in and clock out from the React frontend. Each clock action:

1. Is validated by the backend (no double clock-in, must be linked to an employee)
2. Creates or closes a `time_clock_sessions` record in MySQL
3. Publishes a Kafka event to `hr.timeclock.events`
4. Is consumed by a Kafka listener that writes a permanent `time_clock_event_logs` record

MANAGER, HR\_ADMIN, and SYSTEM\_ADMIN users can view the full time clock event log from the frontend. EMPLOYEE users can only see their own clock status and personal sessions.

```
EMPLOYEE clicks "Clock In" / "Clock Out"
    ↓  (POST /api/v1/time-clock/clock-in or /clock-out)
TimeClockService
    ↓  DB write committed (TimeClockSession OPEN → CLOSED)
    ↓  schedules after-commit callback
TimeClockEventProducer.publish()
    ↓
Kafka topic: hr.timeclock.events
    ↓
TimeClockEventConsumer.consume()
    ↓
TimeClockService.saveEventLog()
    ↓
time_clock_event_logs table (MySQL)
    ↓
GET /api/v1/time-clock/events  (MANAGER / HR_ADMIN / SYSTEM_ADMIN)
    ↓
Time Clock Records page (React)
```

---

### New Kafka Topic & Consumer Group

| Item | Value |
|------|-------|
| **Topic** | `hr.timeclock.events` |
| **Consumer group** | `hr-timeclock-event-consumer` |
| **Event types** | `EMPLOYEE_CLOCKED_IN`, `EMPLOYEE_CLOCKED_OUT` |

---

### New Backend API Endpoints

#### Employee-facing (any authenticated user with a linked employee record)

| Method | Path | Description | Success |
|--------|------|-------------|---------|
| GET | `/api/v1/time-clock/status` | Current clock status (clocked in / out, session details) | 200 |
| POST | `/api/v1/time-clock/clock-in` | Clock in — creates an OPEN session | 201 |
| POST | `/api/v1/time-clock/clock-out` | Clock out — closes the OPEN session | 200 |
| GET | `/api/v1/time-clock/my-sessions` | Paginated list of own time sessions | 200 |

The backend derives the employee identity from the JWT — the frontend never sends an `employeeId`.

#### Manager / Admin event log (MANAGER, HR_ADMIN, SYSTEM_ADMIN only)

| Method | Path | Description | Success |
|--------|------|-------------|---------|
| GET | `/api/v1/time-clock/events` | Paginated, filtered Kafka-consumed event log | 200 |

Optional query parameters for `/events`:

| Parameter | Example | Description |
|-----------|---------|-------------|
| `page` | `0` | Zero-based page number |
| `size` | `20` | Records per page |
| `employeeId` | `5` | Filter by employee |
| `eventType` | `EMPLOYEE_CLOCKED_IN` | Filter by event type |
| `startDate` | `2026-01-01T00:00:00Z` | Filter events from (ISO 8601) |
| `endDate` | `2026-12-31T23:59:59Z` | Filter events to (ISO 8601) |
| `userEmail` | `jane@example.com` | Filter by user email |

---

### New Frontend Routes

| Route | Page | Who can access |
|-------|------|----------------|
| `/time-clock` | My Time Clock | All authenticated users |
| `/time-clock/events` | Time Clock Records | MANAGER, HR\_ADMIN, SYSTEM\_ADMIN |

**Navigation bar additions:**
- **My Time Clock** — shown to all authenticated users
- **Time Clock Records** — shown only to MANAGER, HR\_ADMIN, SYSTEM\_ADMIN

---

### Role Permissions — Time Clock

| Capability | EMPLOYEE | MANAGER | HR\_ADMIN | SYSTEM\_ADMIN |
|------------|:--------:|:-------:|:---------:|:-------------:|
| View own clock status (`/status`) | ✅ | ✅ | ✅ | ✅ |
| Clock in / clock out | ✅ | ✅ | ✅ | ✅ |
| View own sessions (`/my-sessions`) | ✅ | ✅ | ✅ | ✅ |
| View all time clock events (`/events`) | ❌ | ✅* | ✅ | ✅ |

> \* **MANAGER visibility note:** MANAGER currently sees all employees' time clock events. In a future phase this should be narrowed to direct reports only, once a team/manager hierarchy is implemented.

---

### New Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `TIMECLOCK_EVENTS_TOPIC` | `hr.timeclock.events` | Kafka topic for time clock events |
| `TIMECLOCK_KAFKA_CONSUMER_GROUP` | `hr-timeclock-event-consumer` | Consumer group ID |

Add these to your IDE run configuration alongside the existing Kafka variables.

---

### New Database Tables (Flyway V5)

**`time_clock_sessions`** — one row per work session

| Column | Type | Description |
|--------|------|-------------|
| `id` | BIGINT | Primary key |
| `employee_id` | BIGINT | FK → employees.id |
| `user_id` | BIGINT | FK → users.id |
| `clock_in_time` | DATETIME(6) | When the employee clocked in |
| `clock_out_time` | DATETIME(6) | When the employee clocked out (null if OPEN) |
| `status` | VARCHAR(10) | `OPEN` or `CLOSED` |

**`time_clock_event_logs`** — Kafka-consumed event records (idempotent via `event_id` unique key)

| Column | Type | Description |
|--------|------|-------------|
| `event_id` | VARCHAR(36) | UUID (unique, prevents duplicates) |
| `event_type` | VARCHAR(30) | `EMPLOYEE_CLOCKED_IN` or `EMPLOYEE_CLOCKED_OUT` |
| `employee_id`, `employee_email`, `employee_full_name` | various | Employee context |
| `user_id`, `user_email` | various | Auth user context |
| `session_id` | BIGINT | Links back to `time_clock_sessions.id` |
| `metadata_json` | TEXT | For clock-out: includes `clockInTime`, `clockOutTime`, `durationMinutes` |

---

### Starting Kafka

Kafka must be running before the backend starts (same as Phase 2):

```bash
# From the project root
docker compose up -d

# Verify Kafka is healthy
docker ps

# View topic messages (after a clock-in or clock-out)
# Open: http://localhost:8085  → Topics → hr.timeclock.events
```

---

### Starting the Backend

```bash
cd backend
./mvnw spring-boot:run          # macOS / Linux
.\mvnw.cmd spring-boot:run      # Windows
```

The V5 Flyway migration (`V5__create_time_clock_tables.sql`) runs automatically on startup.

---

### Starting the Frontend

```bash
cd frontend
npm install
npm run dev
```

Navigate to `http://localhost:5173`. Log in as an EMPLOYEE to see the **My Time Clock** page. Log in as MANAGER or above to also see **Time Clock Records**.

---

### How to Clock In / Out

1. Log in as a user who has an **employee profile linked** to their account (set via the User Administration page).
2. Navigate to **My Time Clock** in the navbar.
3. If clocked out: click **Clock In**. The button changes to Clock Out after success.
4. If clocked in: click **Clock Out**. The session closes and duration is shown.
5. After a few seconds, navigate to **Time Clock Records** (as a MANAGER or above) to see the Kafka-consumed event appear in the log.

---

### Error Codes (Time Clock)

| `errorCode` | HTTP | Meaning |
|-------------|------|---------|
| `ALREADY_CLOCKED_IN` | 409 | Clock-in attempted while already clocked in |
| `NOT_CLOCKED_IN` | 409 | Clock-out attempted without an open session |
| `RESOURCE_NOT_FOUND` | 404 | User has no linked employee profile |
| `ACCESS_DENIED` | 403 | EMPLOYEE tried to access `/events` |
| `UNAUTHENTICATED` | 401 | No valid JWT provided |

---

### Troubleshooting — Time Clock

**Kafka not running — clock-in works but events don't appear in Time Clock Records:**
- Clock-in/out succeeds (DB write is committed first).
- The Kafka event is attempted after commit. If Kafka is down, only a log warning is emitted — the session is NOT rolled back.
- Start Kafka with `docker compose up -d` and perform a new clock action to generate fresh events.
- Events from when Kafka was down are permanently lost unless the transactional outbox pattern is implemented (see **Known Limitations**).

**Clock-in does not show in Time Clock Records:**
- The event travels through Kafka asynchronously. Wait 1–2 seconds and click **Refresh**.
- Check backend logs for `"Published EMPLOYEE_CLOCKED_IN event"` and `"Consuming EMPLOYEE_CLOCKED_IN event"`.
- Open Kafka UI at `http://localhost:8085` → Topics → `hr.timeclock.events` to verify the message arrived.

**Duplicate clock-in error (ALREADY\_CLOCKED\_IN):**
- You are already clocked in. Click **Clock Out** first.

**Clock-out without clock-in error (NOT\_CLOCKED\_IN):**
- There is no open session to close. Click **Clock In** first.

**401 Unauthorized on `/status`, `/clock-in`, or `/clock-out`:**
- Not logged in. Go to `/login`.

**403 Forbidden on `/time-clock/events`:**
- Your account's role is `EMPLOYEE`. Only MANAGER, HR\_ADMIN, and SYSTEM\_ADMIN can view the event log.

**Missing linked employee profile (404):**
- Your user account exists but no Employee record is linked to it.
- A SYSTEM\_ADMIN must go to User Administration → select your user → link an employee profile.

**Time zone display confusion:**
- All timestamps are stored as UTC in the backend (`Instant`).
- The frontend converts them to the browser's local timezone for display via `new Date(iso).toLocaleString()`.
- The exact display depends on your browser/OS locale settings.

---

### Known Limitations (Phase 3)

1. **MANAGER sees all time clock events** — there is no team/manager hierarchy yet. A future phase should add a `manager_id` or team concept and filter `/events` to show only direct reports' entries when the caller is a MANAGER.

2. **No guaranteed Kafka delivery** — events are published after the DB transaction commits. If Kafka is temporarily unavailable, the employee operation succeeds but the event is lost and will not appear in the time clock event log. A production-grade improvement would use the **transactional outbox pattern**: write the pending event to a dedicated DB table inside the same transaction, then relay it to Kafka via a separate process.

3. **No payroll calculation** — duration minutes are stored in event metadata but no totals, overtime rules, or pay calculations are implemented yet.

4. **No leave management, scheduling, or approval workflows** — out of scope for this phase.

---

## Phase 4 — Spring AI HR Knowledge Assistant

### Overview

HR_ADMIN/SYSTEM_ADMIN users upload official HR policy documents. The backend extracts,
chunks, and embeds their text into a vector store. Any authenticated employee can then
ask the HR Assistant a question, and it answers **strictly from that indexed content**
— citing sources — or says plainly that the available policy documents don't contain
enough information and recommends contacting HR. The assistant never invents policy,
never makes employment decisions, and treats uploaded documents as untrusted data, not
instructions (see **Prompt-injection protections** below).

```
HR_ADMIN uploads policy (POST /hr/policies, then /upload)
        ↓
DocumentStorageService (local disk — see Known Limitations)
        ↓
HR_ADMIN triggers /process
        ↓
DocumentTextExtractor (PDFBox / Apache POI / plain UTF-8)
        ↓
DocumentChunker (overlapping ~1000-char chunks, page-aware for PDF)
        ↓
EmbeddingModel (OpenAI-compatible, via Spring AI)
        ↓
VectorStore (SimpleVectorStore, in-memory)
        ↓
HR_ADMIN triggers /activate → document becomes searchable

Authenticated user asks a question (POST /ai/hr-assistant/chat)
        ↓
HrAssistantController → HrAssistantService
        ↓
HrPolicyKnowledgeService.retrieveRelevant() — active + READY documents only
        ↓
Spring AI ChatClient.prompt().system(context).user(question).call()
        ↓
Grounded answer + source citations
        ↓
AiConversation / AiMessage / AiResponseSource audit records (owner-scoped)
```

---

### Spring AI concepts used

| Concept | How it's used here |
|---|---|
| `ChatModel` / `ChatClient` | `OpenAiChatModel` wired manually in `config/ai/AiClientConfig`, wrapped by `ChatClient` for the fluent `prompt().system().user().call()` API |
| `EmbeddingModel` | `OpenAiEmbeddingModel`, same manually-built `OpenAiApi` client as the chat model |
| `VectorStore` | `SimpleVectorStore` (in-memory) built from the `EmbeddingModel` bean |
| `Document` | One per policy chunk — `id` is deterministic (`doc-{documentId}-chunk-{index}`), `metadata` carries `documentId`, `documentTitle`, `category`, `version`, `pageNumber`, `sectionName`, `chunkIndex`, `effectiveDate` |
| `SearchRequest` | `topK` / `similarityThreshold` from `AI_TOP_K` / `AI_SIMILARITY_THRESHOLD` |

**Provider abstraction note:** `AI_BASE_URL` can point at OpenAI itself or any
OpenAI-compatible endpoint (Ollama's compatibility mode, LM Studio, vLLM, a proxy).
`AI_PROVIDER` is accepted, validated, and surfaced via the status endpoint, but does
not select a different SDK — implementing true multi-SDK provider swapping was out of
scope for this phase (see **Known Limitations**).

---

### Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `AI_ENABLED` | `false` | Master switch. When `false`, the app still starts normally and AI endpoints return `503 AI_DISABLED` |
| `AI_PROVIDER` | `openai` | Informational/validated; see provider abstraction note above |
| `AI_API_KEY` | *(empty)* | API key for the chat/embedding provider. Required (with `AI_ENABLED=true`) for AI features to activate |
| `AI_MODEL` | `gpt-4o-mini` | Chat model name |
| `AI_BASE_URL` | `https://api.openai.com` | Chat/embedding API base URL |
| `AI_TEMPERATURE` | `0.2` | Chat sampling temperature |
| `AI_MAX_TOKENS` | `800` | Max tokens per chat completion |
| `AI_TOP_K` | `5` | Number of chunks retrieved per question |
| `AI_SIMILARITY_THRESHOLD` | `0.5` | Minimum similarity score for a retrieved chunk to be used |
| `HR_DOCUMENT_STORAGE_PATH` | `./data/hr-policy-documents` | Local disk path for uploaded originals (outside Git, outside `src`) |
| `HR_DOCUMENT_MAX_UPLOAD_BYTES` | `10485760` (10 MB) | Max upload size enforced by `LocalDocumentStorageService` |

None of these values, including the API key, are ever logged, returned in API
responses, or exposed via `/api/v1/system/ai/status` (see **Security decisions**).

---

### Supported document formats

| Format | Extractor | Notes |
|---|---|---|
| Plain text (`.txt`) | `PlainTextDocumentExtractor` | Read directly as UTF-8 |
| Markdown (`.md`) | `PlainTextDocumentExtractor` | Read directly as UTF-8, no Markdown parsing |
| PDF (`.pdf`) | `PdfDocumentExtractor` (Apache PDFBox) | Page-level text preserved for accurate `pageNumber` citations. Scanned/image-only PDFs are rejected (`DOCUMENT_TEXT_EMPTY`) — OCR is out of scope |
| Word (`.docx`) | `DocxDocumentExtractor` (Apache POI) | Whole-document text only — DOCX has no reliable page-boundary metadata without a layout engine |

Upload validation (`LocalDocumentStorageService`) rejects anything else, oversized
files, path-traversal filenames, and files whose content doesn't match their claimed
type (PDF/DOCX magic-byte check) — the client-supplied MIME type is never trusted
alone.

---

### Policy upload and processing flow

1. `POST /api/v1/hr/policies` — create draft metadata (`DRAFT`)
2. `POST /api/v1/hr/policies/{id}/upload` — attach the file (`UPLOADED`)
3. `POST /api/v1/hr/policies/{id}/process` — extract → chunk → embed → index (`READY` or `FAILED`); re-processing deletes and replaces the document's previous chunks, so it's always safe to re-run
4. `POST /api/v1/hr/policies/{id}/activate` — only allowed once `READY`; flips `active=true` and the document becomes searchable
5. `POST /api/v1/hr/policies/{id}/deactivate` — flips `active=false`, sets `INACTIVE`, and removes its chunks from the vector store (soft-deactivation only — the document row and its metadata are never deleted)

### RAG flow

1. `HrAssistantService.chat()` resolves/creates an `AiConversation` (ownership-checked if `conversationId` is supplied) and persists the user's question as an `AiMessage`
2. `HrPolicyKnowledgeService.retrieveRelevant()` queries the vector store and, as defense-in-depth, re-checks each result's owning document is still `active && READY` before returning it
3. A system prompt is built that clearly separates instructions from the retrieved context (marked as untrusted data) from the user's raw question, sent via `ChatClient.prompt().system(...).user(...)`
4. The answer is persisted as an `AiMessage` (`grounded` = whether any chunks were retrieved) with up to 5 `AiResponseSource` rows (short excerpts only, never full chunks/documents)

---

### Assistant permissions

| Capability | EMPLOYEE | MANAGER | HR\_ADMIN | SYSTEM\_ADMIN |
|---|:---:|:---:|:---:|:---:|
| Use the HR Assistant (`/chat`) | ✅ | ✅ | ✅ | ✅ |
| View own conversations | ✅ | ✅ | ✅ | ✅ |
| View another user's conversations | ❌ | ❌ | ❌ | ❌ |

> SYSTEM_ADMIN deliberately gets **no** override to read other users' conversation
> content — see **Security decisions**. `/api/v1/system/ai/status` gives SYSTEM_ADMIN
> aggregate operational data only (enabled flag, model name, active document count),
> never conversation content.

### Policy-management permissions

| Capability | EMPLOYEE | MANAGER | HR\_ADMIN | SYSTEM\_ADMIN |
|---|:---:|:---:|:---:|:---:|
| Create / update policy metadata | ❌ | ❌ | ✅ | ✅ |
| Upload / process / activate / deactivate | ❌ | ❌ | ✅ | ✅ |
| Download original file | ❌ | ❌ | ✅ | ✅ |
| View AI operational status (`/system/ai/status`) | ❌ | ❌ | ❌ | ✅ |

---

### New Backend API Endpoints

#### HR Assistant (any authenticated user)

| Method | Path | Description | Success |
|--------|------|-------------|---------|
| POST | `/api/v1/ai/hr-assistant/chat` | Ask a question; creates a conversation if `conversationId` is omitted | 200 |
| GET | `/api/v1/ai/hr-assistant/conversations` | Paginated list of the caller's own conversations | 200 |
| GET | `/api/v1/ai/hr-assistant/conversations/{id}` | Full conversation with messages + sources (owner only) | 200 |
| DELETE | `/api/v1/ai/hr-assistant/conversations/{id}` | Delete a conversation (owner only) | 204 |

#### HR Policy Administration (HR_ADMIN, SYSTEM_ADMIN only)

| Method | Path | Description | Success |
|--------|------|-------------|---------|
| GET | `/api/v1/hr/policies` | Paginated, filterable (`category`, `active`) list | 200 |
| GET | `/api/v1/hr/policies/{id}` | Get one document | 200 |
| POST | `/api/v1/hr/policies` | Create draft metadata | 201 |
| PUT | `/api/v1/hr/policies/{id}` | Update metadata | 200 |
| POST | `/api/v1/hr/policies/{id}/upload` | Upload/replace the file (multipart) | 200 |
| POST | `/api/v1/hr/policies/{id}/process` | Extract, chunk, embed, index | 200 |
| POST | `/api/v1/hr/policies/{id}/activate` | Make searchable | 200 |
| POST | `/api/v1/hr/policies/{id}/deactivate` | Remove from search + soft-deactivate | 200 |
| GET | `/api/v1/hr/policies/{id}/download` | Download the original file | 200 |

#### System AI Status (SYSTEM_ADMIN only)

| Method | Path | Description | Success |
|--------|------|-------------|---------|
| GET | `/api/v1/system/ai/status` | `{ enabled, providerConfigured, model, knowledgeBaseAvailable, activePolicyDocuments }` — no secrets | 200 |

---

### New Frontend Routes

| Route | Page | Who can access |
|-------|------|-----------------|
| `/ai/hr-assistant` | HR Assistant | All authenticated users |
| `/hr/policies` | HR Policy Documents | HR\_ADMIN, SYSTEM\_ADMIN |

**Navigation bar additions:**
- **HR Assistant** — shown to all authenticated users
- **Policy Documents** — shown only to HR\_ADMIN, SYSTEM\_ADMIN

---

### New Database Tables

**Flyway V6 — `hr_policy_documents`**

| Column | Type | Description |
|--------|------|-------------|
| `id` | BIGINT | Primary key |
| `title`, `description`, `category`, `version`, `effective_date` | various | Document metadata |
| `active` | BOOLEAN | Soft-deactivation flag |
| `processing_status` | VARCHAR(12) | `DRAFT` \| `UPLOADED` \| `PROCESSING` \| `READY` \| `FAILED` \| `INACTIVE` |
| `chunk_count` | INT | Number of chunks currently indexed, for deterministic replace-on-reindex |
| `original_file_name`, `content_type`, `storage_type`, `storage_location` | various | Storage metadata (never returned to clients as a raw path) |
| `uploaded_by_user_id`, `uploaded_by_email` | various | Audit fields |

**Flyway V7 — `ai_conversations`, `ai_messages`, `ai_response_sources`**

| Table | Key columns |
|---|---|
| `ai_conversations` | `user_id`, `user_email`, `created_at`, `updated_at` |
| `ai_messages` | `conversation_id` (FK, cascade delete), `role` (`USER`\|`ASSISTANT`\|`SYSTEM`), `content`, `grounded`, `model_name` |
| `ai_response_sources` | `message_id` (FK, cascade delete), `document_id`, `document_title`, `page_number`, `chunk_index`, `excerpt`, `similarity_score` |

No hidden provider reasoning, API keys, raw Authorization headers, or access/refresh
tokens are ever stored in these tables — only the visible question/answer text and
grounding metadata, for auditing.

---

### Local setup

```bash
cd backend
./mvnw spring-boot:run          # macOS / Linux
.\mvnw.cmd spring-boot:run      # Windows
```

The V6/V7 Flyway migrations run automatically. The app starts normally with no AI
configuration at all — the HR Assistant and policy-processing endpoints simply return
`503 AI_DISABLED` / `KNOWLEDGE_BASE_UNAVAILABLE` until you set:

```bash
AI_ENABLED=true
AI_API_KEY=sk-...
# Optional — defaults shown above
AI_MODEL=gpt-4o-mini
AI_BASE_URL=https://api.openai.com
```

```bash
cd frontend
npm install
npm run dev
```

Navigate to `http://localhost:5173`. **HR Assistant** is visible to any logged-in
user; **Policy Documents** is visible to HR\_ADMIN/SYSTEM\_ADMIN.

### AI-disabled behavior

With `AI_ENABLED=false` (the default) or a missing `AI_API_KEY`:
- The application context still starts — the conditional `ChatModel`/`EmbeddingModel`/
  `ChatClient`/`VectorStore` beans (`config/ai/AiClientConfig`, gated by
  `AiEnabledCondition`) simply aren't created.
- `POST /ai/hr-assistant/chat` returns `503 { "errorCode": "AI_DISABLED" }`.
- `POST /hr/policies/{id}/process` returns `503 { "errorCode": "KNOWLEDGE_BASE_UNAVAILABLE" }`.
- The frontend HR Assistant page shows a dedicated "currently unavailable" banner
  rather than a generic error.

---

### Testing

```bash
cd backend
./mvnw test
```

Covers, without ever making a real network call to an AI provider:
- **Unit tests** — `HrPolicyDocumentService`, `LocalDocumentStorageService` (path
  traversal, magic-byte, size-limit, extension validation via `@TempDir`),
  `PlainText`/`Pdf`/`DocxDocumentExtractor` (using real PDFBox/POI-generated
  fixtures), `DocumentChunker`, `HrPolicyKnowledgeService` (mocked `VectorStore` via
  `ObjectProvider`), `HrAssistantService` (mocked `ChatClient` fluent chain,
  AI-disabled path, provider-failure path, ownership rules)
- **Controller tests** — `@WebMvcTest` + the project's `MethodSecurityTestConfig`
  pattern for `HrPolicyController`, `HrAssistantController`, `SystemAiStatusController`
  — auth-required, role checks, validation, AI-disabled responses
- **Integration test** (`HrPolicyAssistantIntegrationTest`) — `@SpringBootTest` +
  real Spring Security filter chain + H2, with a `@TestConfiguration` stubbing
  `ChatModel`/`EmbeddingModel` (deterministic fake vectors, canned chat response) and
  a `@TempDir`-backed storage path: full upload → process → activate → grounded chat
  → source citation flow, cross-user conversation-access denial, and exclusion of a
  deactivated document's chunks from retrieval

```bash
cd frontend
npm install
npm run lint
npm run build
```

No frontend test framework exists yet in this project, so `lint` + `build` are the
frontend gate, consistent with earlier phases.

---

### Security decisions

- **AI beans are conditional, never required to boot** — `AiEnabledCondition` gates
  all Spring AI bean creation on `ai.enabled=true` and a non-blank `ai.api-key`, read
  directly from the `Environment` (conditions run before `@ConfigurationProperties`
  beans exist). Every AI-dependent service injects these via `ObjectProvider`/
  `Optional` and fails with a structured `AiUnavailableException` rather than an NPE.
- **Ownership-only conversation access, no admin override** — matches the "prefer
  privacy-preserving access" guidance. SYSTEM_ADMIN's operational visibility is
  limited to `/system/ai/status`'s aggregate counts.
- **Uploaded content is never trusted for validation** — extension allow-list +
  magic-byte sniffing (never the client-supplied Content-Type alone), server-generated
  storage filenames (rules out path traversal/collisions by construction), and a
  configurable upload size cap enforced in application code (with a generous
  `spring.servlet.multipart.max-file-size` ceiling as a second line of defense).
- **Secrets never leave the process** — `AI_API_KEY` is read from the environment,
  used only to construct the `OpenAiApi` client, and never logged, returned in a
  response body, or included in `/system/ai/status`.
- **Documents are never hard-deleted** — only `active`/`processing_status` change.
  Conversations *can* be hard-deleted by their owner (`DELETE /conversations/{id}`),
  which is a user-privacy action, not a policy-document one.

### Prompt-injection protections

Uploaded HR documents are treated as **untrusted content** end-to-end:
- The system prompt (`HrAssistantService.SYSTEM_PROMPT_TEMPLATE`) explicitly instructs
  the model to treat the "RETRIEVED HR POLICY CONTEXT" section as data, never as
  instructions — even if it contains phrases like "ignore previous instructions."
- System instructions, retrieved document context, and the user's raw question are
  structurally separated: instructions + context go in the `ChatClient` system role,
  the question goes in the user role — never concatenated into one blob.
- The assistant is explicitly instructed not to reveal its own system prompt, API
  keys, database contents, or internal implementation details, even if asked directly.
- No tool-calling, URL fetching, code execution, or filesystem/database access is
  wired into the model in this phase — it can only produce text.
- The model is instructed never to make employment decisions, evaluate performance, or
  give legal/medical/tax/immigration/financial conclusions — policy question-answering
  only.

---

### Known Limitations (Phase 4)

1. **Single-SDK provider abstraction** — `AI_PROVIDER` is accepted and surfaced but
   does not switch SDKs; only an OpenAI-compatible client is wired. `AI_BASE_URL`
   covers most self-hosted/compatible setups (Ollama, LM Studio, vLLM, proxies), but
   a provider requiring a genuinely different request/response shape (e.g. native
   Anthropic or Bedrock) is out of scope for this phase.
2. **In-memory vector store** — `SimpleVectorStore` does not persist across restarts.
   After a restart, previously-activated documents remain marked `active`/`READY` in
   the database but their chunks are gone from the (now-empty) vector store; an
   HR_ADMIN must re-run `/process` (and the retrieval-time defense-in-depth check
   means stale answers are never silently served — they just return no results until
   reprocessed). A production deployment should swap in a persistent vector database
   behind the same `VectorStore` interface.
3. **Local disk storage is temporary** — `DocumentStorageService`/
   `LocalDocumentStorageService` store originals on local disk
   (`HR_DOCUMENT_STORAGE_PATH`), Git-ignored and outside `src`. This is explicitly a
   placeholder; Amazon S3 (or another `DocumentStorageService` implementation) is the
   intended production replacement, and callers already depend only on the interface.
4. **No OCR** — scanned/image-only PDFs yield no extractable text and are rejected
   (`DOCUMENT_TEXT_EMPTY`) rather than silently producing an empty/wrong answer.
5. **DOCX has no page numbers** — citations for `.docx` sources omit `pageNumber`
   since DOCX has no reliable page-boundary metadata without a full layout engine.
6. **No streaming responses** — the chat endpoint is request/response only; Spring
   AI's streaming APIs (`ChatClient.stream()`) were not wired into this phase's
   frontend.
7. **Live AI provider connectivity was not exercised in this environment** — no
   AI credentials or Docker were available, so real OpenAI/Ollama calls could not be
   tested. All AI provider behavior is verified via mocked/stubbed `ChatModel`/
   `EmbeddingModel` beans (see **Testing**).
