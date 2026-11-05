---
name: secure-rest-api-spring-jwt
description: Secure a Java Spring Boot REST API with JWT authentication using the access token + ID token + refresh token model (OAuth2/OIDC style), including refresh token rotation, logout/revocation, BCrypt password hashing and role-based authorization. Use this skill whenever the user wants to add login, JWT, token refresh, Spring Security, protected endpoints, roles, or "secure my REST API" to a Spring Boot project, even if they only mention one of these pieces.
---

# Securing a Spring Boot REST API with JWT (access + ID + refresh tokens)

Working example: `https://github.com/Vasile07/ai-skill-secure-rest-api-spring-jwt`

## Goal

Implement stateless authentication where the client logs in once, receives three tokens, and uses them for different purposes. The access token is short-lived so a leak is low-impact; the refresh token lets the user stay logged in without re-entering a password.

| Token | Format | Lifetime | Who uses it | Purpose |
|---|---|---|---|---|
| `accessToken` | JWT (signed) | ~15 min | The API | Sent as `Authorization: Bearer ...`. Contains `sub` (userId), `roles`, `iat`, `exp`, `iss`. |
| `idToken` | JWT (signed) | ~15 min | The client (frontend) | Tells the client who logged in (name, email) so it avoids extra requests. **Never accepted by the API.** |
| `refreshToken` | Opaque random string | ~7 days | `/auth/refresh` only | Exchanged for a new token set. Stored hashed in the DB so it can be revoked. |

## Why these choices (read before coding)

- **JWTs are signed, not encrypted.** Anyone can read the payload, so never put passwords or secrets in them.
- **The refresh token is not a JWT** because a JWT cannot be invalidated before it expires. A random string stored in the database can be deleted, which is what makes logout real.
- **The ID token exists for the client.** If the API accepted it, a token meant for display could be used as a credential. Add a `token_use` claim (`access` or `id`) and reject anything that is not `access` in the API filter.
- **Rotation detects theft.** Every refresh issues a new refresh token and marks the old one as used. If a used token appears again, someone copied it, so revoke the whole token family.

## Project setup

Spring Boot 3.x, Java 17+. Dependencies: `spring-boot-starter-web`, `spring-boot-starter-security`, `spring-boot-starter-data-jpa`, `spring-boot-starter-validation`, `h2` (or PostgreSQL), and `io.jsonwebtoken:jjwt-api` / `jjwt-impl` / `jjwt-jackson` (0.12.x).

Configuration (`application.yml`). Never commit a real secret; read it from an environment variable.

```yaml
app:
  jwt:
    secret: ${JWT_SECRET}        # Base64, at least 256 bits
    issuer: secure-api
    access-ttl-minutes: 15
    refresh-ttl-days: 7
```

For local development with Spring Boot 3.1+, load a `.env` file directly through Spring Config Data instead of adding a dotenv dependency or custom startup code. Add `spring.config.import: optional:file:.env[.properties]` to `application.yml`; Spring then reads simple `KEY=value` lines as properties. Keep `.env` out of version control, provide a `.env.example` containing only placeholders, and run the app from the directory where `.env` is located. Validate that `JWT_SECRET` is set and is Base64-encoded with at least 256 bits before JWT key creation. Never place a real secret in `.env.example` or tracked configuration.

Suggested layout: `auth/` (controller, service, DTOs), `security/` (JwtService, JwtAuthFilter, SecurityConfig), `user/` (User, UserRepository), `token/` (RefreshToken, RefreshTokenRepository).

## Step 1: Users and password hashing

Store only a BCrypt hash. BCrypt adds a random salt and is deliberately slow, which makes brute-forcing leaked hashes expensive.

```java
@Bean
PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }
// on register: user.setPasswordHash(passwordEncoder.encode(request.password()));
```

## Step 2: Generate the access and ID tokens

```java
@Service
public class JwtService {
    private final SecretKey key;
    public JwtService(@Value("${app.jwt.secret}") String secret) {
        this.key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
    }

    public String createAccessToken(User u) {
        Instant now = Instant.now();
        return Jwts.builder()
            .subject(u.getId().toString())
            .issuer("secure-api")
            .claim("token_use", "access")
            .claim("roles", u.getRoles())              // e.g. ["USER"]
            .issuedAt(Date.from(now))
            .expiration(Date.from(now.plus(15, ChronoUnit.MINUTES)))
            .signWith(key).compact();
    }

    public String createIdToken(User u) {              // same shape, identity claims instead of roles
        Instant now = Instant.now();
        return Jwts.builder()
            .subject(u.getId().toString())
            .claim("token_use", "id")
            .claim("name", u.getName())
            .claim("email", u.getEmail())
            .issuedAt(Date.from(now))
            .expiration(Date.from(now.plus(15, ChronoUnit.MINUTES)))
            .signWith(key).compact();
    }

    public Claims parse(String token) {                // throws JwtException if invalid or expired
        return Jwts.parser().verifyWith(key).requireIssuer("secure-api")
                   .build().parseSignedClaims(token).getPayload();
    }
}
```

## Step 3: Refresh tokens (opaque, hashed, rotated)

Entity fields: `id`, `tokenHash`, `userId`, `familyId`, `expiresAt`, `used`, `revoked`.

```java
String generate() {                                    // 256 bits of randomness
    byte[] b = new byte[32];
    new SecureRandom().nextBytes(b);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(b);
}
// Store sha256(rawToken), never the raw value. Return the raw value to the client once.
```

Hash with SHA-256 (not BCrypt): the token is already high-entropy, and you need a fast lookup by hash.

## Step 4: Login, refresh with rotation, logout

```java
public record LoginResponse(String accessToken, String idToken, String refreshToken, long expiresIn) {}
```

- **`POST /auth/login`**: authenticate with `AuthenticationManager`, then create a new `familyId` and return a `LoginResponse`. On failure always return a generic `401 Invalid credentials`; never reveal whether the email exists.
- **`POST /auth/refresh`** (body: `{ "refreshToken": "..." }`):

```java
@Transactional(noRollbackFor = InvalidTokenException.class)   // so the family revocation is saved
public LoginResponse refresh(String raw) {
    RefreshToken t = repo.findByTokenHash(sha256(raw)).orElseThrow(InvalidTokenException::new);
    if (t.isUsed() || t.isRevoked()) {                 // reuse detected: assume theft
        repo.revokeFamily(t.getFamilyId());
        throw new InvalidTokenException();
    }
    if (t.getExpiresAt().isBefore(Instant.now())) throw new InvalidTokenException();
    t.setUsed(true);                                   // old token is now dead
    return issueTokens(t.getUser(), t.getFamilyId());  // new access + id + refresh, same family
}
```

- **`POST /auth/logout`**: revoke the refresh token's family. This is the answer to "how do you log out of a stateless JWT system?" The access token still works until it expires, which is why its lifetime is short.

## Step 5: Validate the access token on every request

```java
@Component
public class JwtAuthFilter extends OncePerRequestFilter {
    private final JwtService jwt;
    public JwtAuthFilter(JwtService jwt) { this.jwt = jwt; }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        String header = req.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            try {
                Claims c = jwt.parse(header.substring(7));
                if ("access".equals(c.get("token_use"))) {          // rejects ID tokens
                    List<String> roles = c.get("roles", List.class);
                    var authorities = roles.stream()
                        .map(r -> new SimpleGrantedAuthority("ROLE_" + r)).toList();
                    SecurityContextHolder.getContext().setAuthentication(
                        new UsernamePasswordAuthenticationToken(c.getSubject(), null, authorities));
                }
            } catch (JwtException | IllegalArgumentException ignored) { /* stays unauthenticated -> 401 */ }
        }
        chain.doFilter(req, res);
    }
}
```

## Step 6: Security configuration and roles

Authentication answers "who are you?"; authorization answers "what may you do?".

```java
@Bean
SecurityFilterChain chain(HttpSecurity http, JwtAuthFilter jwtFilter) throws Exception {
    return http
        .csrf(csrf -> csrf.disable())                  // safe here: no cookie-based auth, token is in a header
        .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(a -> a
            .requestMatchers("/auth/**").permitAll()
            .requestMatchers("/api/admin/**").hasRole("ADMIN")
            .anyRequest().authenticated())
        .exceptionHandling(e -> e.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
        .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
        .build();
}
```

Provide at least one endpoint per level so the example is testable: `GET /api/me` (any logged-in user) and `GET /api/admin/stats` (ADMIN only).

### Scope user-owned resources to the access token

For every endpoint that reads or changes data belonging to the current user, derive the user ID from the authenticated principal populated from the access token's `sub` claim (for example, `UUID.fromString(authentication.getName())`). Never accept the owner ID from a request body, query parameter, or client-controlled header. Do not include `userId` in create/update DTOs for user-owned resources. Pass the authenticated owner ID through the service layer, scope collection queries to it, and check ownership before returning, updating, or deleting a resource by ID. Return the same not-found response for a missing resource and a resource owned by someone else to avoid leaking its existence. Keep administrator operations explicit and separate from a user's own-resource operations.

### Swagger / OpenAPI Bearer authentication

When the project uses Springdoc, declaring an HTTP Bearer `@SecurityScheme` only defines the scheme and makes it available to OpenAPI; it does not tell Swagger UI which operations require it. Add `@SecurityRequirement(name = "bearerAuth")` to each protected controller class (or to each protected operation). Apply it to endpoints such as `/api/me`, admin routes, and other authenticated API controllers. Leave public registration, login, refresh, and logout operations without that requirement so Swagger UI does not send bearer credentials unnecessarily. Afterward, the Swagger UI **Authorize** dialog can attach the access token to protected “Try it out” requests. The security scheme name in `@SecurityRequirement` must match the name in `@SecurityScheme`.

## Step 7: Try it

```bash
# 1. Log in
curl -X POST localhost:8080/auth/login -H "Content-Type: application/json" \
     -d '{"email":"user@test.com","password":"Passw0rd!"}'
# 2. Call a protected endpoint
curl localhost:8080/api/me -H "Authorization: Bearer <accessToken>"        # 200
curl localhost:8080/api/me                                                 # 401
curl localhost:8080/api/admin/stats -H "Authorization: Bearer <userToken>" # 403
# 3. Refresh, then replay the OLD refresh token
curl -X POST localhost:8080/auth/refresh -H "Content-Type: application/json" \
     -d '{"refreshToken":"<old>"}'                                         # 401 and family revoked
```

## Rules and pitfalls

1. Send only the access token to the API; reject any token whose `token_use` is not `access`.
2. Keep access tokens short (5 to 15 minutes). Security comes from this plus revocable refresh tokens.
3. Store refresh tokens hashed, and return the raw value only once.
4. Never put sensitive data in a JWT, and never log tokens.
5. Load the signing secret from the environment. For HS256 use at least 256 bits.
6. Use generic error messages for failed logins and invalid tokens.
7. Always use HTTPS outside local development; tokens in plain HTTP can be stolen.
8. Use `@Transactional(noRollbackFor = ...)` where a security exception must not undo the revocation.

## Short notes (mention, do not over-implement)

- **HS256 vs RS256/ES256:** HS256 uses one shared secret for signing and verifying, so every service that verifies can also forge tokens. RS256/ES256 uses a private key to sign and a public key to verify, which fits multiple services (microservices) where only the auth service should issue tokens.
- **Where the client stores tokens:** `localStorage` is readable by injected JavaScript (XSS). An `HttpOnly` cookie is hidden from JavaScript but needs CSRF protection. Pick one deliberately.
- **Brute force:** add rate limiting or temporary lockout to `/auth/login` (for example with Bucket4j).
- **CORS:** allow only the origins of your real frontend.

## Checklist for the AI using this skill

- [ ] Passwords hashed with BCrypt
- [ ] Access and ID tokens signed, with `token_use` claims and expiry
- [ ] Refresh token opaque, hashed in DB, rotated on each use, reuse revokes the family
- [ ] `/auth/logout` revokes the family
- [ ] Filter accepts only access tokens; stateless session; `/auth/**` public
- [ ] OpenAPI declares the Bearer scheme and marks each protected controller or operation with `@SecurityRequirement` using the same scheme name
- [ ] User-owned resource endpoints derive the owner from the authenticated access-token subject, omit owner IDs from request DTOs, and enforce ownership for reads, updates, and deletes
- [ ] At least one USER and one ADMIN protected endpoint, verified with 200/401/403
- [ ] Secret read from environment, not committed
