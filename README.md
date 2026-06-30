# Rental Management System

Ứng dụng quản lý phòng trọ theo hướng Modular Monolith Backend + React SPA + PostgreSQL.

## Stack

Backend:
- Java 21, Spring Boot 3.5.15, Maven
- Spring Web MVC REST, Spring Data JPA, Hibernate
- PostgreSQL 16, Flyway
- Spring Security + JWT
- Jakarta Validation, springdoc-openapi/Swagger, Actuator
- JUnit 5, Mockito, MockMvc, Spring Security Test, Testcontainers PostgreSQL

Frontend:
- Node.js 24 LTS, React 19, TypeScript, Vite
- Tailwind CSS 4, shadcn-style local UI components
- React Router, TanStack Query, Axios
- React Hook Form, Zod, Lucide React, date-fns
- Vitest, React Testing Library, Playwright

## Structure

```text
rental-management/
├── backend/
│   └── src/main/java/com/example/rental/
│       ├── common/
│       ├── admin/
│       ├── auth/
│       ├── user/
│       ├── property/
│       ├── tenant/
│       ├── tenantportal/
│       ├── contract/
│       ├── billing/
│       ├── payment/
│       ├── maintenance/
│       └── dashboard/
├── frontend/
│   └── src/
│       ├── app/
│       ├── components/ui/
│       ├── features/
│       ├── lib/
│       └── pages/
├── docs/
├── docker-compose.yml
└── .env.example
```

## Run Locally

1. Start PostgreSQL:

```bash
docker compose up -d postgres
```

2. Start backend:

```bash
cd backend
mvnw.cmd spring-boot:run
```

Backend URLs:
- API: `http://localhost:8080/api`
- Swagger: `http://localhost:8080/swagger-ui.html`
- Health: `http://localhost:8080/actuator/health`

3. Start frontend:

```bash
cd frontend
npm.cmd install
npm.cmd run dev
```

Frontend URL: `http://localhost:5173`

The backend uses `backend/mvnw.cmd`, so Maven does not need to be installed globally.

Demo accounts after Flyway migrations run:

| Role | Email | Password | Notes |
| --- | --- | --- | --- |
| LANDLORD | `demo@rental.local` | `Password123!` | Main account with seeded property, rooms, tenant, contract, invoice, and maintenance data |
| ADMIN | `admin@rental.local` | `Password123!` | Auth/role test account |
| TENANT | `tenant@rental.local` | `Password123!` | Auth/role test account linked to Demo Tenant |

## First Flow

The implemented landlord workflow is:

```text
login
  -> create/update property and room
  -> create/update tenant
  -> create/end contract
  -> record monthly electricity and water readings
  -> create invoice with rent and utility items
  -> record one or more payments
  -> process maintenance requests
```

## MVP Feature Status

| Area | Landlord | Tenant | Admin |
| --- | --- | --- | --- |
| Authentication and role protection | Login/register/logout | Login/logout | Login/logout |
| Properties and rooms | Create, list, edit, deactivate | View rented room | System totals |
| Tenants | Create, list, edit, rental history | View own profile | Account list |
| Contracts | Create and end | View own active contract | System totals |
| Utility readings | Create, edit, usage and cost calculation | View own readings | - |
| Invoices | Create, auto-fill rent/utilities, cancel unpaid invoice | View own invoices | System totals |
| Payments | Record partial/full payments and history | View own payments | - |
| Maintenance | View and update status/resolution | Submit and track requests | Pending total |
| Account administration | - | - | Lock and unlock accounts |

All business APIs verify record ownership. Direct navigation to a route outside the current role is also redirected by the React application.

## Tests

Backend:

```bash
cd backend
mvnw.cmd test
```

Frontend:

```bash
cd frontend
npm.cmd run test
npm.cmd run test:e2e
```

Backend integration tests use Testcontainers PostgreSQL. Playwright is configured for Chromium, Firefox, WebKit, and a Pixel 7 mobile viewport. Docker Desktop must be running because Testcontainers starts a real PostgreSQL 16 instance.

## Notes

- Redis and Quartz are intentionally not added yet.
- File attachment tables and Cloudinary/S3 integration are deferred until the maintenance or contract attachment workflow needs them.
- Database migration `V1__init_core_schema.sql` creates the 13 core MVP tables described in `docs/database-schema.md`.
- Temporary residence, recurring service configuration, dedicated deposit accounting, notifications, asset inventory, and advanced financial reporting belong to the post-MVP roadmap and are not represented as completed features.

## ⚠️ Note: 
This branch uses AI-assisted development to create a sample prototype of the project. The purpose of this branch is to explore ideas, test implementation approaches, and support the learning process.
