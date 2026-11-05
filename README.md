# Secure REST API with Spring Boot and JWT

A small learning project for applying the [`secure-rest-api-spring-jwt` skill](.agents/skills/secure-rest-api-spring-jwt/SKILL.md) to a real Spring Boot REST API. [`AGENTS.md`](AGENTS.md) gives Codex sessions repository context and explains when to load the skill. The backend now uses JWT authentication, refresh-token rotation, logout/revocation, and role-based authorization.

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
2. **`01-project-initialization`** — the original unauthenticated Java/Spring Boot API.
3. **`02-jwt-auth-with-skill`** — the secured learning stage. Authentication is implemented in this branch.

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

When authentication is added, the skill suggests `auth/`, `security/`, `user/`, and `token/` packages. The actual backend has been created on `01-project-initialization`; the secured branch will extend it.

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
- BookReading in the initial unauthenticated stage: `userId`, `bookId`, `status`, optionally `progressPercent` and `notes`. In the secured stage, the owner is taken from the access token's `sub`, so request bodies contain only `bookId`, `status`, and optional progress/notes fields.

The initial API should validate input and return sensible HTTP statuses, while leaving authentication and authorization out. In the secured iteration, we can add registration/login/refresh/logout, a current-user endpoint, user-owned book readings, and an admin-only catalog endpoint to exercise `200`, `401`, and `403` behavior from the skill.

## Agent and skill instructions

- [`AGENTS.md`](AGENTS.md) provides project context and tells future sessions when to read the skill.
- [`.agents/skills/secure-rest-api-spring-jwt/SKILL.md`](.agents/skills/secure-rest-api-spring-jwt/SKILL.md) is the detailed implementation guide for securing the API. Its central model is:

- short-lived signed access JWT accepted by the API;
- ID token for client identity display, never accepted as API credentials;
- opaque refresh token stored as a hash and rotated on use;
- refresh-token family revocation on logout or detected reuse;
- BCrypt password hashing, stateless Spring Security, and role-based protected endpoints.

See the skill checklist for implementation and verification details. Never commit a real signing secret; configure it through an environment variable.

## Run the backend

Requirements: Java 17+ and Maven 3.6.3+. Spring Boot 3.5.16 is used to stay on the Spring Boot 3 line expected by the JWT learning skill; Swagger UI is provided through springdoc.

```powershell
cd backend
mvn spring-boot:run
```

Swagger UI: `http://localhost:8080/swagger-ui.html`  
OpenAPI document: `http://localhost:8080/v3/api-docs`

CSV files are created on first use under `./data` (relative to the process working directory). Set `CSV_DATA_DIR` to choose another directory. For local development, copy `backend/.env.example` to `backend/.env` and set `JWT_SECRET` to a Base64 encoded key of at least 256 bits. Spring Boot imports this optional properties file automatically, so run `mvn spring-boot:run` from `backend/`. Generate a value with `$bytes = [byte[]]::new(32); [Security.Cryptography.RandomNumberGenerator]::Fill($bytes); [Convert]::ToBase64String($bytes)`. Keep it private and stable while running the app. Register accounts through `/auth/register`; legacy fixture users have no password and cannot log in until provisioned.

### Authentication and authorization

- `POST /auth/register` accepts `{ "name", "email", "password" }`, creates a USER account with a BCrypt password hash, and returns access, ID, and refresh tokens.
- `POST /auth/login` accepts `{ "email", "password" }` and returns the same token set; failures return a generic `401`.
- `POST /auth/refresh` accepts `{ "refreshToken" }`, rotates it, and revokes its family if a used token is replayed.
- `POST /auth/logout` accepts `{ "refreshToken" }` and revokes its family. Access JWTs remain valid for at most 15 minutes.
- Use only `accessToken` as `Authorization: Bearer <token>`. The ID token is for client identity display; the opaque refresh token is only for refresh/logout.
- `/auth/me` requires an access token. `/api/book-readings/**` requires authentication and is scoped to the signed-in user. The API takes the reading owner from the access token's `sub` claim; `POST` and `PUT` bodies contain no `userId` field.
- `GET /api/books/**` is available to USER and ADMIN roles; book write operations (`POST`, `PUT`, `DELETE`) and `/api/users/**` and `/api/admin/stats` require ADMIN. Public registration never grants ADMIN. An operator must provision an administrator by adding an existing BCrypt hash and `ADMIN` role to `users.csv`; never store a plaintext password there.
- Swagger and OpenAPI endpoints remain public. Refresh state is stored in `refresh-tokens.csv` as SHA-256 hashes.

### Sample data

Starter CSV fixtures are included in `backend/data/`: two synthetic users, three books, and four book-reading records. Start the app from the `backend/` directory to load these files using the default storage path. Stable UUIDs link each reading to its user and book. API writes modify the fixture files; restore them with `git restore backend/data`.

The sample users have no passwords in this unauthenticated stage. When the JWT skill is applied, preserve the user IDs and reading relationships while adding BCrypt password hashes and roles. Never add plaintext passwords to the CSV files.
