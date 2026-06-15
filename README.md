# Employee Management System

A full-stack CRUD application for managing employee records, built with **Spring Boot 3** (backend) and **React + Vite** (frontend).

---

## Features

- List all employees in a sortable table
- Add a new employee with client- and server-side validation
- Edit an existing employee
- Delete an employee with a confirmation prompt
- Loading spinners and user-visible error messages
- Structured JSON error responses from the API

---

## Technology Stack

| Layer     | Technology                                  |
|-----------|---------------------------------------------|
| Backend   | Java 17, Spring Boot 3.2, Spring Data JPA   |
| Database  | MySQL 8                                     |
| Frontend  | React 18, Vite 5, React Router 6, Axios 1.6 |
| Styling   | Bootstrap 5.3                               |
| Build     | Maven (backend), npm (frontend)             |

---

## Repository Structure

```
employee-management-system/
├── backend/                          # Spring Boot application
│   ├── src/
│   │   ├── main/java/com/example/employeemanagement/
│   │   │   ├── EmployeeManagementApplication.java
│   │   │   ├── config/WebConfig.java          # CORS
│   │   │   ├── controller/EmployeeController.java
│   │   │   ├── dto/EmployeeDto.java
│   │   │   ├── entity/Employee.java
│   │   │   ├── exception/
│   │   │   │   ├── ErrorResponse.java
│   │   │   │   ├── GlobalExceptionHandler.java
│   │   │   │   └── ResourceNotFoundException.java
│   │   │   ├── mapper/EmployeeMapper.java
│   │   │   ├── repository/EmployeeRepository.java
│   │   │   └── service/
│   │   │       ├── EmployeeService.java
│   │   │       └── EmployeeServiceImpl.java
│   │   ├── main/resources/application.properties
│   │   └── test/…                    # Service + controller unit tests
│   ├── .env.example
│   ├── mvnw / mvnw.cmd
│   └── pom.xml
├── frontend/                         # React + Vite application
│   ├── src/
│   │   ├── components/               # Header, Footer
│   │   ├── pages/                    # EmployeeListPage, EmployeeFormPage, NotFoundPage
│   │   ├── services/employeeService.js
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
|-------------|----------------|------------------------------------|
| Java JDK    | 17             | `java -version`                    |
| Maven       | 3.9            | or use the included `./mvnw`       |
| Node.js     | 18             | `node -v`                          |
| npm         | 9              | bundled with Node                  |
| MySQL       | 8              | running locally or accessible      |

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

Hibernate will create the `employees` table automatically on first startup (`spring.jpa.hibernate.ddl-auto=update`).

---

## Backend Environment Variables

Spring Boot reads these from the shell environment (not from a `.env` file directly).

| Variable       | Default                                         | Description                |
|----------------|-------------------------------------------------|----------------------------|
| `DB_URL`       | `jdbc:mysql://localhost:3306/employee_management` | JDBC connection URL        |
| `DB_USERNAME`  | `root`                                          | Database username          |
| `DB_PASSWORD`  | *(empty)*                                       | Database password          |
| `JPA_DDL_AUTO` | `update`                                        | Hibernate DDL mode         |
| `FRONTEND_URL` | `http://localhost:5173`                         | Allowed CORS origin        |

See `backend/.env.example` for documentation and example values.

### Set variables — macOS / Linux

```bash
export DB_URL=jdbc:mysql://localhost:3306/employee_management
export DB_USERNAME=root
export DB_PASSWORD=your_password
export FRONTEND_URL=http://localhost:5173
```

### Set variables — Windows PowerShell

```powershell
$env:DB_URL     = "jdbc:mysql://localhost:3306/employee_management"
$env:DB_USERNAME = "root"
$env:DB_PASSWORD = "your_password"
$env:FRONTEND_URL = "http://localhost:5173"
```

---

## Frontend Environment Variables

Create `frontend/.env` (Git-ignored) from the example:

```bash
cp frontend/.env.example frontend/.env
```

| Variable            | Default                          | Description           |
|---------------------|----------------------------------|-----------------------|
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

The backend starts on **http://localhost:8080**.

---

## Frontend Startup

```bash
cd frontend
npm install
npm run dev
```

The frontend starts on **http://localhost:5173**.

---

## API Endpoints

| Method | Path                       | Description              | Success Code |
|--------|----------------------------|--------------------------|--------------|
| GET    | `/api/v1/employees`        | List all employees       | 200          |
| GET    | `/api/v1/employees/{id}`   | Get employee by ID       | 200          |
| POST   | `/api/v1/employees`        | Create employee          | 201          |
| PUT    | `/api/v1/employees/{id}`   | Update employee          | 200          |
| DELETE | `/api/v1/employees/{id}`   | Delete employee          | 204          |

### Error response shape

```json
{
  "status": 400,
  "message": "Validation failed",
  "timestamp": "2024-05-01T12:00:00",
  "fieldErrors": {
    "email": "Email must be a valid email address"
  }
}
```

---

## Running Tests

The test suite uses Mockito (service layer) and MockMvc (controller layer). **No running MySQL is required.**

```bash
cd backend
./mvnw test          # macOS / Linux
.\mvnw.cmd test      # Windows
mvn test             # if Maven is installed globally
```

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
npm run build
# Output: dist/  (serve with nginx, Apache, or any static host)
```

---

## Example curl Commands

```bash
# List all employees
curl http://localhost:8080/api/v1/employees

# Get one employee
curl http://localhost:8080/api/v1/employees/1

# Create an employee
curl -X POST http://localhost:8080/api/v1/employees \
  -H "Content-Type: application/json" \
  -d '{"firstName":"Jane","lastName":"Doe","email":"jane.doe@example.com"}'

# Update an employee
curl -X PUT http://localhost:8080/api/v1/employees/1 \
  -H "Content-Type: application/json" \
  -d '{"firstName":"Jane","lastName":"Smith","email":"jane.smith@example.com"}'

# Delete an employee
curl -X DELETE http://localhost:8080/api/v1/employees/1
```

---

## Configuring on Another Computer

1. Clone the repository.
2. Install Java 17, Node 18, and MySQL 8.
3. Create the `employee_management` database (see MySQL Setup above).
4. Set the four backend environment variables for your database.
5. Copy `frontend/.env.example` → `frontend/.env` and adjust `VITE_API_BASE_URL` if needed.
6. Start the backend, then the frontend.

---

## Troubleshooting

### MySQL connection failure (`Communications link failure`)
- Confirm MySQL is running: `mysql -u root -p`
- Check `DB_URL` — database name, host, and port must match.
- Ensure the database exists: `SHOW DATABASES LIKE 'employee_management';`

### Port 8080 already in use
```bash
# Find the process
lsof -i :8080          # macOS / Linux
netstat -ano | findstr :8080   # Windows

# Override the port
export SERVER_PORT=8081
./mvnw spring-boot:run
```
Then update `VITE_API_BASE_URL` in `frontend/.env` to use port 8081.

### Port 5173 already in use
Vite will automatically try the next available port (5174, 5175, …). Update `FRONTEND_URL` in the backend environment and restart both servers.

### CORS errors in the browser
- Check that `FRONTEND_URL` matches the exact origin shown in the browser address bar (scheme, host, and port).
- The backend allows the origin set in `FRONTEND_URL` (default `http://localhost:5173`).
- Re-export the variable and restart the backend.

### `VITE_API_BASE_URL` is undefined
- Make sure `frontend/.env` exists (the `.env.example` file is only a template).
- Restart `npm run dev` after creating or editing `.env`.
- All Vite env variables must start with `VITE_`.
