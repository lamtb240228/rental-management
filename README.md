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
│       ├── auth/
│       ├── user/
│       ├── property/
│       ├── tenant/
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

The implemented first workflow is:

```text
register/login -> create property -> create room -> view room list
```

The backend also includes APIs for tenants, contracts, utility readings, invoices, payments, maintenance requests, and dashboard summary.

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

Backend integration tests use Testcontainers PostgreSQL. Playwright is configured for Chromium, Firefox, and WebKit.

## Notes

- Redis and Quartz are intentionally not added yet.
- File attachment tables and Cloudinary/S3 integration are deferred until the maintenance or contract attachment workflow needs them.
- Database migration `V1__init_core_schema.sql` creates the 13 core MVP tables described in `docs/database-schema.md`.

## ⚠️ **Note:** This project uses AI-assisted development to create the initial prototype and accelerate the development process. All architecture, code reviews, testing, and final implementation decisions are manually verified and refined by the author.

