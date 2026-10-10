# FitCoach - Online Fitness Coaching Platform

Spring Boot 3 / Java 17 web app with role-based dashboards for **Admin**, **Coach** and **User**.

## Requirements
- JDK 17 or newer
- Maven 3.9+ (or open the folder in IntelliJ / VS Code / Eclipse and run `FitCoachApplication`)

## Run
```
mvn spring-boot:run
```
Open http://localhost:8080 (data is stored in `./data/fitcoach*.db`, H2 console at `/h2-console`,
JDBC URL `jdbc:h2:file:./data/fitcoach`, user `sa`, empty password).

## Demo accounts (seeded on first start)
| Role  | Email                | Password |
|-------|----------------------|----------|
| Admin | admin@fitcoach.com   | admin123 |
| Coach | coach@fitcoach.com   | coach123 |
| User  | user@fitcoach.com    | user123  |

New users can self-register (always the USER role); admins can create coaches/admins.

## Features
**Admin** - user CRUD with roles, approve/reject user-generated content, system settings
(site name, registration toggle, max plans per coach, announcement banner), content statistics chart, recent feedback.

**Coach** - create/delete workout plans, message users, client progress reports + chart,
plan analytics chart (enrolments vs. logged workouts), full interaction history.

**User** - browse/follow plans and view plan details, log workouts (duration, calories, weight),
progress chart + weekly goal bar, message coaches, profile management, workout history with achievements,
submit content for moderation, send feedback.

## Layout
```
src/main/java/com/fitcoach
  config/      security, demo data seeder, global model attributes
  model/       JPA entities
  repository/  Spring Data repositories
  service/     settings + cascading delete helpers
  controller/  Auth, Admin, Coach, User controllers
src/main/resources/templates   Thymeleaf pages (Chart.js loaded from CDN)
```
