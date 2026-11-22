# Employee Leave Management System (LeaveHub)

Built a full-stack Employee Leave Management System with React.js, Spring Boot, and PostgreSQL (Neon Serverless). Features secure authentication and role-based access: employees can apply, edit, cancel, and track leave requests, while admins filter and approve or reject applications via protected REST APIs.

---

## Technologies Used

### Frontend
- **Framework:** React.js 19
- **Bundler & Dev Server:** Vite 8
- **HTTP Client:** Axios (centralized configuration with auto JWT injection)
- **Styling:** CSS3 (modern responsive design system)
- **Linter & Test:** Oxlint & Node.js native test runner

### Backend
- **Framework:** Spring Boot 3.4.2
- **Language:** Java 17 / 21
- **Security:** Spring Security 6 (Stateless JWT, HttpOnly Cookies, BCrypt)
- **ORM & Data Access:** Spring Data JPA / Hibernate
- **Database Driver:** PostgreSQL JDBC Driver

### Database
- **Database:** PostgreSQL (Neon Cloud Serverless PostgreSQL with TLS)
- **Connection Pool:** HikariCP (optimized bounded pool with keep-alive ping)

---

## Key Features

### Employee Features
- **User Authentication:** Registration with input validation (alphabetical-only name enforcement), login, and session persistence.
- **Interactive Leave Application:** Apply for Casual, Sick, Earned, or Other leaves with live working days calculation (excluding Saturdays and Sundays).
- **Assigned Approver Selection:** Select reviewing administrators (**Manideep Sir**, **Sushil Sir**, **Gaurav Sir**).
- **History & Tracking:** Tabular view of all submissions with filter by status and category.
- **Edit & Cancel:** Ability to modify or cancel pending applications prior to review.
- **User Profile Modal:** Dedicated modal displaying user details, role, and capabilities.

### Admin Features
- **Admin Command Center:** Real-time metrics counters for Total, Pending, Approved, and Rejected requests.
- **Organization-Wide Oversight:** Filter by Status, Approver, and Leave Category with multi-field search.
- **Approval Actions:** Modal confirmation to approve or reject leave applications.
- **Dual View Support:** Administrators can toggle between Admin View and Personal Employee View.

---

## Project Structure

```text
Employee-Leave-Management-System/
├── backend/                  # Spring Boot application
│   ├── src/main/java/        # Controllers, Services, Repositories, Entities, Config
│   ├── src/main/resources/   # application.properties
│   └── pom.xml               # Maven configuration
├── frontend/                 # React 19 application
│   ├── src/components/       # UI Components (ApplyLeave, MyLeaveRequests, AdminLeaveManagement, UserProfileModal)
│   ├── src/utils/            # Utilities (dateUtils, working days calculation)
│   └── package.json          # Vite & dependencies
├── database/                 # SQL schemas (PostgreSQL migration & sample data)
├── .env                      # Environment variables
└── docker-compose.yml        # Docker configuration
```

---

## How to Run the Project

### Prerequisites
- Node.js 18+ & npm
- Java 17+ & Maven
- Active Internet connection (for Neon PostgreSQL) or local PostgreSQL/MySQL

### 1. Backend Setup
1. Configure database credentials in `.env` or `backend/src/main/resources/application.properties`.
2. In the `backend` directory, run:
   ```bash
   mvn clean spring-boot:run
   ```
3. Backend starts on `http://localhost:8080`.

### 2. Frontend Setup
1. In the `frontend` directory, install dependencies and run:
   ```bash
   npm install
   npm run dev
   ```
2. Frontend runs on `http://localhost:5173`.

### 3. Run Automated Tests
- **Frontend Tests:**
  ```bash
  cd frontend
  npm test
  npm run lint
  npm run build
  ```
- **Backend Tests:**
  ```bash
  cd backend
  mvn test
  ```
