# Secure REST API with Spring Boot and JWT

A small learning project for applying the [`secure-rest-api-spring-jwt` skill](SKILL.md) to a real Spring Boot REST API. The project will begin as an unauthenticated API, then gain JWT authentication, refresh-token rotation, logout/revocation, and role-based authorization.

## Demo theme: Reading Tracker

The demo is a personal reading tracker. It gives us a small, familiar domain with a user and two domain entities:

- **User** — account identity and role (added with the authentication work).
- **Book** — a title and author.
- **BookReading** — a user's registration of a book on their reading list, with status and progress (for example, `WANT_TO_READ`, `READING`, or `COMPLETED`). This is also commonly called a reading record or bookshelf entry.

The relationships are `User 1 → many BookReading` and `Book 1 → many BookReading`. Each `BookReading` belongs to exactly one user and one catalog book. This keeps the shared title and author in `Book` while allowing different users to track their own progress independently. The first unauthenticated iteration can expose CRUD endpoints without enforcing ownership; the JWT iteration can scope each user's book readings to their authenticated owner.

## Data storage

The backend will use CSV files for persistent storage in this learning project. Repository implementations will read and write the CSV files rather than using a database. We will keep the service and API layers separate from storage so the CSV repositories have clear responsibilities and can be replaced later if needed.

The initial files will be `users.csv`, `books.csv`, and `book-readings.csv`, with headers and stable IDs used for relationships. CSV parsing/writing must handle quoted fields and commas/newlines correctly. The demo will run as a single application instance; concurrent multi-process writes are outside this lightweight file-storage setup.

## Learning sequence

1. **Project foundation** — repository layout, this overview, and the skill checklist.
2. **`01-project-initialization`** — a Java/Spring Boot API with Swagger/OpenAPI and no authentication. We will agree on the package/file structure and endpoints before generating the backend.
3. **`02-jwt-auth-with-skill`** — branch from the completed initialization branch. The skill is applied here only when the learner is ready; this branch is intentionally not implementing authentication yet.

## Proposed backend layout

```text
backend/
├── pom.xml
└── src/
    ├── main/java/<base-package>/
    │   ├── ReadingTrackerApplication.java
    │   ├── user/                 # User model and CSV-backed repository
    │   ├── book/                 # Book model, repository, service, controller, DTOs
    │   ├── bookreading/          # BookReading model, repository, service, controller, DTOs
    │   ├── storage/csv/          # CSV parsing and persistence support
    │   └── common/               # shared errors and API configuration
    └── main/resources/
        └── application.yml
```

When authentication is added, the skill suggests `auth/`, `security/`, `user/`, and `token/` packages. We can decide the final package layout alongside the API contract before creating the backend.

## Candidate unauthenticated API

These are discussion proposals for branch `01-project-initialization`, not a finalized contract:

- `GET /api/books` and `GET /api/books/{id}` — browse the catalog.
- `POST /api/books`, `PUT /api/books/{id}`, `DELETE /api/books/{id}` — manage catalog entries.
- `GET /api/users` and `GET /api/users/{id}` — browse demo users.
- `POST /api/users`, `PUT /api/users/{id}`, `DELETE /api/users/{id}` — manage demo users. User records contain name and email only in this branch; password and authentication are introduced later.
- `GET /api/book-readings`, `GET /api/book-readings/{id}` — browse reading progress.
- `POST /api/book-readings`, `PUT /api/book-readings/{id}`, `DELETE /api/book-readings/{id}` — manage a user's book registrations and progress.
- Optional `GET /api/health` — a simple service check.

Suggested request fields:

- Book: `title`, `author`, optionally `isbn` and `publishedYear`.
- BookReading: `userId`, `bookId`, `status`, optionally `progressPercent` and `notes`.

The initial API should validate input and return sensible HTTP statuses, while leaving authentication and authorization out. In the secured iteration, we can add registration/login/refresh/logout, a current-user endpoint, user-owned book readings, and an admin-only catalog endpoint to exercise `200`, `401`, and `403` behavior from the skill.

## The skill

[`SKILL.md`](SKILL.md) is the implementation guide for securing the API. Its central model is:

- short-lived signed access JWT accepted by the API;
- ID token for client identity display, never accepted as API credentials;
- opaque refresh token stored as a hash and rotated on use;
- refresh-token family revocation on logout or detected reuse;
- BCrypt password hashing, stateless Spring Security, and role-based protected endpoints.

See the skill checklist for implementation and verification details. Never commit a real signing secret; configure it through an environment variable.

## Run

## Run the backend

Requirements: Java 17+ and Maven 3.6.3+. Spring Boot 3.5.16 is used to stay on the Spring Boot 3 line expected by the JWT learning skill; Swagger UI is provided through springdoc.

```powershell
cd backend
mvn spring-boot:run
```

Swagger UI: `http://localhost:8080/swagger-ui.html`  
OpenAPI document: `http://localhost:8080/v3/api-docs`

CSV files are created on first use under `./data` (relative to the process working directory). Set `CSV_DATA_DIR` to choose another directory. The API has no authentication on this branch by design.

### Sample data

Starter CSV fixtures are included in `backend/data/`: two synthetic users, three books, and four book-reading records. Start the app from the `backend/` directory to load these files using the default storage path. Stable UUIDs link each reading to its user and book. API writes modify the fixture files; restore them with `git restore backend/data`.

The sample users have no passwords in this unauthenticated stage. When the JWT skill is applied, preserve the user IDs and reading relationships while adding BCrypt password hashes and roles. Never add plaintext passwords to the CSV files.
