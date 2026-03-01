# eRetain - Employee Retention & Resource Management Platform

A comprehensive multi-tier microservices web application built with **Java Spring Boot** and **PostgreSQL** for managing company structure, projects, resource allocation, timesheets, and reporting.

## Architecture Overview

```
                    ┌─────────────────┐
                    │   UI Service     │       ┌──────────────────┐
                    │  Thymeleaf (9000)│──────►│  Azure OpenAI    │
                    └────────┬────────┘       │  (GPT-4o Chat)   │
                             │                └──────────────────┘
                    ┌────────┴────────┐
                    │   API Gateway    │
                    │    (Port 8080)   │
                    └────────┬────────┘
                             │
                    ┌────────┴────────┐
                    │ Discovery Server │
                    │  Eureka (8761)   │
                    └────────┬────────┘
                             │
        ┌────────────────────┼────────────────────┐
        │                    │                     │
   ┌────┴────┐    ┌─────────┴────────┐    ┌──────┴──────┐
   │  Auth   │    │    Company       │    │   Project   │
   │ Service │    │   Structure      │    │   Service   │
   │ (8081)  │    │   Service (8082) │    │   (8083)    │
   └─────────┘    └──────────────────┘    └─────────────┘
        │                    │                     │
   ┌────┴────┐    ┌─────────┴────────┐    ┌──────┴──────┐
   │Allocat- │    │   Timesheet      │    │  Reporting  │
   │  ion    │    │    Service       │    │   Service   │
   │ (8084)  │    │   (8085)         │    │   (8086)    │
   └─────────┘    └──────────────────┘    └─────────────┘
        │                    │                     │
        └────────────────────┼─────────────────────┘
                             │
                    ┌────────┴────────┐
                    │   PostgreSQL    │
                    │   (Port 5432)   │
                    └─────────────────┘
```

## Technology Stack

| Component | Technology | Version |
|-----------|-----------|---------|
| Language | Java | 17 |
| Framework | Spring Boot | 3.2.3 |
| Cloud | Spring Cloud | 2023.0.0 |
| Database | PostgreSQL | 16 |
| Service Discovery | Netflix Eureka | - |
| API Gateway | Spring Cloud Gateway | - |
| Security | Spring Security + JWT | - |
| API Docs | SpringDoc OpenAPI | 2.3.0 |
| UI Framework | Thymeleaf + Bootstrap | 5.3.2 |
| AI Integration | Azure OpenAI (GPT-4o) | 2025-01-01-preview |
| Build Tool | Maven | 3.9+ |
| Containerization | Docker | - |

## Personas & Access Control

| Persona | Access Level |
|---------|-------------|
| **Administrator** | Full access to all modules. Can manage users, company structure, projects, allocations, timesheets, reports, bulk upload projects, and **AI Chat Assistant**. |
| **PMO** | Can manage projects, allocations, approve timesheets, view all reports, bulk upload projects, and **AI Chat Assistant**. **No access to Company Structure.** |
| **Employee** | Can view own allocations, fill timesheets for allocated projects, view own reports. |

## Microservices

### 1. Discovery Server (Port 8761)
Netflix Eureka service registry for service discovery.

### 2. API Gateway (Port 8080)
Spring Cloud Gateway with JWT authentication filter. Routes all requests to backend services.

### 3. Auth Service (Port 8081)
- User management (CRUD)
- JWT authentication (login/register)
- Role-based access control
- User access management
- Role rate card management

### 4. Company Structure Service (Port 8082)
- Business Unit → Unit → Delivery Unit hierarchy
- Full CRUD for all organizational entities

### 5. Project Service (Port 8083)
- Project management (create, update, status changes)
- Project schedule/phase management
- **Bulk Upload** — CSV-based bulk project creation with multi-format date parsing
- Project types: FIXED_PRICE, TIME_AND_MATERIAL, INTERNAL, SUPPORT

### 6. Allocation Service (Port 8084)
- Employee-to-project resource allocation
- **Business Rule: Max 8 hours/day, 40 hours/week per employee**
- Allocation statuses: PROPOSED, ACTIVE, COMPLETED, CANCELLED
- Detailed daily allocation tracking

### 7. Timesheet Service (Port 8085)
- Weekly timesheet creation and submission
- Timesheet entry management (per project, per day)
- Approval workflow: DRAFT → SUBMITTED → APPROVED/REJECTED
- 8 hours/day and 40 hours/week validation

### 8. Reporting Service (Port 8086)
- Project reports with allocation summaries
- Employee utilization reports
- Timesheet summary reports
- Allocation analysis
- Cross-service data aggregation via WebClient

### 9. UI Service (Port 9000)
- Thymeleaf-based web frontend with Bootstrap 5
- Dashboard with role-specific views
- Project, allocation, and timesheet management pages
- Report views (projects, allocations, timesheets, utilization)
- User management (Admin only)
- **Project Bulk Upload** (Admin & PMO only) — CSV upload with template download and results reporting
- **AI Chat Assistant** (Admin & PMO only) — powered by Azure OpenAI GPT-4o

## AI Chat Assistant

The platform includes an **AI-powered chat assistant** accessible to Administrator and PMO users. It uses **Azure OpenAI GPT-4o** with function calling to query real eRetain data in natural language.

### Capabilities
- **Project queries** — "Show me all active projects", "What are the details of project DA001?"
- **Allocation queries** — "Who is allocated to what project?", "Show allocations for employee 3"
- **Timesheet queries** — "Show all submitted timesheets", "What are the timesheet details for this week?"
- **Utilization queries** — "What's the utilization rate for employee 3?"
- **Cross-data analysis** — Combines data from multiple services to answer complex questions

### How It Works
1. User sends a natural language question via the chat UI
2. Azure OpenAI analyzes the question and decides which eRetain APIs to call
3. The system executes the API calls using **12 predefined functions**:
   - `get_all_projects`, `get_project_by_id`
   - `get_all_allocations`, `get_allocations_by_project`, `get_allocations_by_employee`
   - `get_all_timesheets`, `get_timesheets_by_employee`
   - `get_utilization_report`
   - `get_employee_names`
   - `get_projects_report`, `get_allocations_report`, `get_timesheets_report`
4. Real data is sent back to Azure OpenAI, which generates a formatted response
5. Responses include markdown tables, lists, and structured data

### Access
- Floating robot icon (bottom-right) on every page for Admin/PMO users
- Dedicated chat page at `/chat`
- Conversation history maintained per session with auto-trimming

### Configuration
Set the following environment variables (or update `application.yml`):
```
AZURE_OPENAI_ENDPOINT=https://your-endpoint.openai.azure.com/
AZURE_OPENAI_API_KEY=your-api-key
AZURE_OPENAI_DEPLOYMENT=gpt-4o
AZURE_OPENAI_API_VERSION=2025-01-01-preview
```

## Databases

Each service uses its own PostgreSQL database:

| Service | Database |
|---------|----------|
| Auth Service | `eretain_auth` |
| Company Service | `eretain_company` |
| Project Service | `eretain_project` |
| Allocation Service | `eretain_allocation` |
| Timesheet Service | `eretain_timesheet` |
| Reporting Service | `eretain_reporting` |
| UI Service | — (no database, frontend only) |

## Getting Started

### Prerequisites
- Java 17+
- Maven 3.9+
- PostgreSQL 16+ (or Docker)
- Docker & Docker Compose (optional)

### Option 1: Run with Docker Compose

```bash
# Build all services
mvn clean package -DskipTests

# Start everything
docker-compose up -d

# Check status
docker-compose ps
```

### Option 2: Run Locally

1. **Create databases:**
```sql
CREATE DATABASE eretain_auth;
CREATE DATABASE eretain_company;
CREATE DATABASE eretain_project;
CREATE DATABASE eretain_allocation;
CREATE DATABASE eretain_timesheet;
CREATE DATABASE eretain_reporting;
```

2. **Build the project:**
```bash
mvn clean install -DskipTests
```

3. **Start services in order:**
```bash
# 1. Discovery Server
cd eretain-discovery-server && mvn spring-boot:run

# 2. API Gateway
cd eretain-api-gateway && mvn spring-boot:run

# 3. Auth Service
cd eretain-auth-service && mvn spring-boot:run

# 4. Company Service
cd eretain-company-service && mvn spring-boot:run

# 5. Project Service
cd eretain-project-service && mvn spring-boot:run

# 6. Allocation Service
cd eretain-allocation-service && mvn spring-boot:run

# 7. Timesheet Service
cd eretain-timesheet-service && mvn spring-boot:run

# 8. Reporting Service
cd eretain-reporting-service && mvn spring-boot:run
```

## API Documentation

Each service exposes Swagger UI:

| Service | Swagger URL |
|---------|-------------|
| Auth | http://localhost:8081/swagger-ui.html |
| Company | http://localhost:8082/swagger-ui.html |
| Project | http://localhost:8083/swagger-ui.html |
| Allocation | http://localhost:8084/swagger-ui.html |
| Timesheet | http://localhost:8085/swagger-ui.html |
| Reporting | http://localhost:8086/swagger-ui.html |
| Eureka Dashboard | http://localhost:8761 |
| **UI Application** | **http://localhost:9000** |

## API Endpoints Summary

### Auth Service (`/api/auth`)
| Method | Endpoint | Description | Access |
|--------|----------|-------------|--------|
| POST | `/api/auth/login` | User login | Public |
| POST | `/api/auth/register` | Register user | Public |
| GET | `/api/auth/me` | Current user info | Authenticated |
| GET | `/api/auth/users` | List all users | ADMIN, PMO |
| PUT | `/api/auth/users/{id}` | Update user | ADMIN, PMO |
| GET | `/api/auth/rate-cards` | List rate cards | ADMIN |

### Company Service (`/api/company`)
| Method | Endpoint | Description | Access |
|--------|----------|-------------|--------|
| GET/POST | `/api/company/business-units` | Business units | ADMIN, PMO |
| GET/POST | `/api/company/units` | Units | ADMIN, PMO |
| GET/POST | `/api/company/delivery-units` | Delivery units | ADMIN, PMO |

### Project Service (`/api/projects`)
| Method | Endpoint | Description | Access |
|--------|----------|-------------|--------|
| GET | `/api/projects` | List projects | ALL |
| POST | `/api/projects` | Create project | ADMIN, PMO |
| PUT | `/api/projects/{id}` | Update project | ADMIN, PMO |
| DELETE | `/api/projects/{id}` | Delete project | ADMIN, PMO |
| GET/POST | `/api/projects/{id}/schedules` | Project schedules | ADMIN, PMO |
| GET | `/api/projects/bulk-upload/template` | Download CSV template | ADMIN, PMO |
| POST | `/api/projects/bulk-upload` | Bulk upload projects (CSV) | ADMIN, PMO |

### Allocation Service (`/api/allocations`)
| Method | Endpoint | Description | Access |
|--------|----------|-------------|--------|
| GET | `/api/allocations/employee/{id}` | Employee allocations | ADMIN, PMO, Self |
| GET | `/api/allocations/project/{id}` | Project allocations | ADMIN, PMO |
| POST | `/api/allocations` | Create allocation | ADMIN, PMO |
| PUT | `/api/allocations/{id}` | Update allocation | ADMIN, PMO |
| DELETE | `/api/allocations/{id}` | Delete allocation | ADMIN, PMO |

### Timesheet Service (`/api/timesheets`)
| Method | Endpoint | Description | Access |
|--------|----------|-------------|--------|
| GET | `/api/timesheets/employee/{id}` | Employee timesheets | ADMIN, PMO, Self |
| POST | `/api/timesheets` | Create timesheet | ALL |
| POST | `/api/timesheets/{id}/submit` | Submit timesheet | ALL |
| POST | `/api/timesheets/{id}/review` | Approve/Reject | ADMIN, PMO |
| POST | `/api/timesheets/entries` | Add time entry | ALL |

### Reporting Service (`/api/reports`)
| Method | Endpoint | Description | Access |
|--------|----------|-------------|--------|
| GET | `/api/reports/projects` | Project report | ADMIN, PMO |
| GET | `/api/reports/allocations` | Allocation report | ADMIN, PMO |
| GET | `/api/reports/my-allocations` | Own allocations | ALL |
| GET | `/api/reports/timesheets` | Timesheet report | ADMIN, PMO |
| GET | `/api/reports/my-timesheets` | Own timesheets | ALL |
| GET | `/api/reports/utilization/{id}` | Utilization report | ADMIN, PMO |
| GET | `/api/reports/my-utilization` | Own utilization | ALL |

## Default Credentials

| User | Username | Password | Role |
|------|----------|----------|------|
| Administrator | admin | Admin@123 | ADMINISTRATOR |
| PMO Manager | pmo_user | Pmo@123 | PMO |
| Employee | employee_user | Emp@123 | EMPLOYEE |

> **Note:** Passwords in seed data use BCrypt encoding. Update them in production.

## Project Structure

```
eRetain/
├── pom.xml                          # Parent POM
├── docker-compose.yml               # Docker orchestration
├── db-init/                         # Database init scripts
├── eretain-common/                  # Shared library
│   └── src/main/java/com/eretain/common/
│       ├── config/                  # AuditEntity
│       ├── dto/                     # ApiResponse, PagedResponse
│       ├── enums/                   # Role, ProjectStatus, etc.
│       ├── exception/               # Global exception handling
│       └── security/                # JWT, SecurityConfig
├── eretain-discovery-server/        # Eureka Server
├── eretain-api-gateway/             # API Gateway
├── eretain-auth-service/            # Authentication & Users
├── eretain-company-service/         # Company Structure
├── eretain-project-service/         # Projects
├── eretain-allocation-service/      # Resource Allocation
├── eretain-timesheet-service/       # Timesheets
├── eretain-reporting-service/       # Reports & Analytics
└── eretain-ui/                      # Thymeleaf Web Frontend
    └── src/main/java/com/eretain/ui/
        ├── config/                  # SecurityConfig, WebClientConfig
        ├── controller/              # Page controllers + ChatController
        ├── security/                # JWT filter, UserPrincipal
        └── service/                 # ApiService, ChatService (Azure OpenAI)
```

## Key Business Rules

1. **Allocation Limits:** An employee's total allocation across all projects cannot exceed **8 hours/day** and **40 hours/week**.
2. **Timesheet Validation:** Timesheet entries follow the same 8 hrs/day and 40 hrs/week limits.
3. **Timesheet Workflow:** DRAFT → SUBMITTED → APPROVED or REJECTED. Rejected timesheets can be resubmitted.
4. **Soft Delete:** All entities use `isActive` flag for soft deletion.
5. **Audit Trail:** All entities track `createdAt`, `updatedAt`, `createdBy`, `updatedBy`.
6. **AI Chat:** Uses Azure OpenAI function calling to query real backend data — responses are always based on live eRetain data, not hallucinated.
7. **Bulk Upload:** CSV-based project bulk upload with multi-format date parsing, per-record validation, and detailed success/failure reporting.
8. **PMO Access:** PMO users do not have access to Company Structure management — this is restricted to Administrators only.
7. **Bulk Upload:** CSV-based project bulk upload with multi-format date parsing, per-record validation, and detailed success/failure reporting.
8. **PMO Access:** PMO users do not have access to Company Structure management — this is restricted to Administrators only.

## Screenshots

### Dashboard
Role-specific dashboard with project summaries, allocation counts, and recent timesheets.

### AI Chat Assistant
Natural language interface to query projects, allocations, timesheets, and utilization data. Available via the floating robot icon (bottom-right) for Admin and PMO users.

## License

This project is proprietary. All rights reserved.
