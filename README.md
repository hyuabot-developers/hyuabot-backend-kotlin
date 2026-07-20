# HYUabot Backend (Kotlin)

A Spring Boot GraphQL API backend for [HYUabot](https://github.com/hyuabot-developers), providing campus information for Hanyang University — including shuttle buses, public transit, dining, academic calendar, and more.

## Tech Stack

| Category          | Technology                                 |
|-------------------|--------------------------------------------|
| Language          | Kotlin 2.x                                 |
| Framework         | Spring Boot 4.0                            |
| API               | Spring GraphQL + Netflix DGS 11            |
| Database          | PostgreSQL + Spring Data JPA (Hibernate 7) |
| Cache             | Redis (Lettuce)                            |
| Security          | Spring Security + JJWT 0.13                |
| Config Encryption | Jasypt Spring Boot 4                       |
| Build             | Gradle Kotlin DSL                          |
| Runtime           | Java 21                                    |

## Features

- **Shuttle Bus** — Routes, stops, timetables, holidays, and periods with complex filtering (by stop, period, weekday, time, date)
- **City Bus** — Routes, stops, timetables, and realtime departure information
- **Subway** — Station info, routes, timetables, and realtime arrival data
- **Commute Shuttle** — Dormitory commute routes and schedules
- **Cafeteria** — Campus dining hall menus with meal type and pricing
- **Buildings & Rooms** — Campus building locations and classroom info
- **Academic Calendar** — Events and categories with version tracking
- **Notices** — Multi-language campus announcements
- **Reading Rooms** — Library seat availability
- **Contacts** — Campus phonebook
- **Authentication** — JWT access/refresh tokens with Redis-backed invalidation

## Prerequisites

- Java 21+
- PostgreSQL
- Redis
- Runtime environment variables for database, Redis, Jasypt, and JWT configuration

## Configuration

The application uses `application.properties` for base configuration. Sensitive values are injected through runtime environment variables and are not packaged in the JAR or container image. Jasypt remains enabled for any `ENC(...)` values supplied through the Spring environment.

Required environment variables / secrets:

| Variable            | Description                |
|---------------------|----------------------------|
| `SPRING_DATASOURCE_URL`      | PostgreSQL JDBC URL        |
| `SPRING_DATASOURCE_USERNAME` | Database username          |
| `SPRING_DATASOURCE_PASSWORD` | Database password          |
| `SPRING_DATA_REDIS_HOST`     | Redis host                 |
| `SPRING_DATA_REDIS_PORT`     | Redis port                 |
| `JASYPT_ENCRYPTOR_PASSWORD`  | Jasypt decryption password |
| `JWT_SECRET`                 | JWT signing secret         |

Pass all sensitive configuration at startup. For example:

```bash
JASYPT_ENCRYPTOR_PASSWORD=<password> \
JWT_SECRET=<jwt-secret> \
java -jar app.jar
```

## Getting Started

```bash
# Clone the repository
git clone https://github.com/hyuabot-developers/hyuabot-backend-kotlin.git
cd hyuabot-backend-kotlin

# Run the application
./gradlew bootRun
```

The server starts at `http://localhost:8080`.

- GraphQL endpoint: `http://localhost:8080/graphql`
- Swagger UI: `http://localhost:8080/swagger-ui/index.html`

## Development

### Code Style

This project enforces [ktlint](https://github.com/pinterest/ktlint) for Kotlin code style.

```bash
# Check code style
./gradlew ktlintCheck

# Auto-format
./gradlew ktlintFormat
```

### Testing

```bash
# Run all tests
./gradlew clean test

# Run tests with coverage report
./gradlew clean test jacocoTestReport

# Verify coverage thresholds
./gradlew jacocoTestCoverageVerification
```

Coverage reports are generated at `build/reports/jacoco/test/html/index.html`.

### Build

```bash
# Build JAR (skip tests)
./gradlew build -x test
```

## Docker

```bash
# Build image
docker build -t hyuabot-backend-kotlin .

# Run container (example with required configuration)
docker run -p 8080:8080 \
  -e JASYPT_ENCRYPTOR_PASSWORD=<jasypt_password> \
  -e SPRING_DATASOURCE_URL=jdbc:postgresql://<db-host>:5432/<db-name> \
  -e SPRING_DATASOURCE_USERNAME=<db-username> \
  -e SPRING_DATASOURCE_PASSWORD=<db-password> \
  -e SPRING_DATA_REDIS_HOST=<redis-host> \
  -e SPRING_DATA_REDIS_PORT=<redis-port> \
  -e JWT_SECRET=<jwt-secret> \
  hyuabot-backend-kotlin
```

The Dockerfile uses a multi-stage build with `eclipse-temurin:21-jre-alpine` as the runtime image.

## CI/CD

| Workflow | Trigger | Actions |
|---|---|---|
| `code-check.yml` | Push / PR | ktlint check, tests, JaCoCo coverage |
| `deploy.yml` | Merged PR / manual | Docker build & push, Kubernetes rolling restart |

Deployment targets a Kubernetes cluster under the `hyuabot` namespace.

## Project Structure

```
src/main/kotlin/app/hyuabot/backend/
├── auth/              # Authentication (signup, login, token refresh)
├── bus/               # City bus routes, stops, timetables, realtime
├── building/          # Campus buildings and rooms
├── cafeteria/         # Dining halls and menus
├── calendar/          # Academic calendar
├── campus/            # Campus information
├── commuteShuttle/    # Commute shuttle routes and timetables
├── config/            # App configuration (Swagger, Redis, Jasypt)
├── contact/           # Campus phonebook
├── database/          # JPA entities and repositories
├── notice/            # Campus announcements
├── readingRoom/       # Library seat availability
├── security/          # JWT provider and Spring Security config
├── shuttle/           # Shuttle routes, stops, timetables, periods
├── subway/            # Subway stations, routes, timetables, realtime
└── utility/           # Helpers (timezone, date utilities)
```

## GraphQL API

The API is GraphQL-first. Main query types:

| Query         | Description                           |
|---------------|---------------------------------------|
| `shuttle`     | Shuttle bus timetables with filtering |
| `bus`         | City bus routes and arrivals          |
| `subway`      | Subway station info and arrivals      |
| `commute`     | Commute shuttle schedules             |
| `cafeteria`   | Dining hall menus                     |
| `building`    | Campus buildings and rooms            |
| `calendar`    | Academic calendar events              |
| `notices`     | Campus announcements                  |
| `readingRoom` | Library seat availability             |
| `phonebook`   | Contact directory                     |

Custom scalars: `Date`, `DateTime`, `LocalTime`

## License

This project is maintained by the [hyuabot-developers](https://github.com/hyuabot-developers) organization.
