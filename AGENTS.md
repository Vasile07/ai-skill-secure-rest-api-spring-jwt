# Repository guidance

## Project context

- This repository is a learning project for adding JWT security to a small Java REST API.
- The backend is a Spring Boot 3 application in `backend/`, currently on Java 17.
- The demo domain is a Reading Tracker: `User` has many `BookReading` records; each `BookReading` references one `Book` by `bookId`. `Book` owns its title and author.
- Persistence is through CSV-backed repositories under the configured data directory. Keep HTTP controllers, service logic, repository interfaces, and CSV storage separated.
- Starter CSV fixtures are in `backend/data/`; they use stable UUIDs and synthetic `.test` email addresses. API writes modify these tracked files.
- The API on `01-project-initialization` is intentionally unauthenticated. User records currently contain name and email only.

## Skill for authentication work

- The repository skill is [`secure-rest-api-spring-jwt`](.agents/skills/secure-rest-api-spring-jwt/SKILL.md).
- Before changing authentication, JWT, Spring Security, password handling, refresh-token behavior, or authorization, read that skill and follow its checklist.
- Treat the skill as the detailed implementation guide; use this file and the README for project-specific context.
- The `02-jwt-auth-with-skill` branch is the place for the skill application. It starts from `01-project-initialization`; until the user asks to apply the skill, leave authentication unimplemented.
- When the skill is applied, adapt its user and refresh-token persistence requirements to this project’s repository and CSV design. Never store plaintext passwords or raw refresh tokens, and never commit real secrets or credentials.
- Preserve the starter users' IDs and related book-reading rows when extending the user schema with BCrypt hashes and roles; fixtures must never contain plaintext demo passwords.
- Preserve the public API contract where possible. Any changes needed to support registration, login, refresh, logout, `/api/me`, and role-protected endpoints should be reflected in the README and Swagger description.

## Working conventions

- Keep the unauthenticated and secured learning stages clear in Git history and documentation.
- Use validation and clear HTTP status codes for API errors.
- Keep CSV headers stable, preserve UUID relationships, and correctly quote CSV values.
- Do not add credentials, signing keys, or personal data to tracked files.
