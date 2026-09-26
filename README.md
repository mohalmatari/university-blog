# University Blog

A blogging platform built around a secure, layered REST API, powered by **Spring Boot** and **PostgreSQL** (hosted on Supabase). It supports user registration/authentication, blog posts, and comments — with JWT-based auth, role-based authorization, input validation, and consistent, structured error responses across every endpoint.

A minimal single-page HTML/JS client is included as a working demo of the API (`src/main/resources/static/index.html`) — a full frontend rebuild is planned (see [Roadmap](#roadmap)).

## Tech Stack

- **Java 21** · **Spring Boot 4**
- **Spring Data JPA** (Hibernate) · **PostgreSQL** (via Supabase)
- **Spring Security** + **JWT** (JJWT) for stateless authentication
- **Bean Validation** (Jakarta Validation) for request validation
- **springdoc-openapi** for interactive API documentation
- **Maven**

## Features

- **Authentication** — register and log in; passwords hashed with BCrypt, sessions replaced by signed JWTs (stateless)
- **Authorization** — only a resource's owner (or an admin) can edit or delete it; enforced server-side, independent of the client
- **Posts & Comments** — full CRUD on posts, create/read/delete on comments, correctly modeled relationships (`User → Post → Comment`)
- **DTOs everywhere** — request/response shapes are explicit and separate from database entities; passwords never leave the server, and clients can never set server-controlled fields (role, timestamps, IDs) themselves
- **Validation** — every write endpoint rejects malformed input before it reaches business logic
- **Centralized error handling** — a single `@RestControllerAdvice` turns every failure mode (not found, duplicate entry, foreign-key conflict, bad credentials, expired/invalid token, forbidden action, validation failure) into a clean, consistent JSON response with the correct HTTP status code
- **Pagination** — list endpoints (`/posts`, `/users`, `/comments/post/{id}`) support `page`, `size`, and `sort` query parameters

## Architecture

The project follows a standard layered architecture:

```
Controller  →  Service  →  Repository  →  Database
     ↑             ↑
   DTOs      Business logic,
             authorization,
             exception handling
```

- **Controller** — HTTP layer only; maps requests to service calls
- **Service** — business logic, ownership/role checks, entity ↔ DTO conversion
- **Repository** — Spring Data JPA interfaces, no manual SQL
- **Entity** — JPA-mapped domain objects (`User`, `Post`, `Comment`)
- **DTO** — request/response contracts, decoupled from entities on purpose
- **Exception** — custom exceptions + one global handler for consistent error output
- **Config** — Spring Security setup, JWT generation/validation, the JWT auth filter

## Database Schema

```
users                          posts                           comments
------------------             ------------------              ------------------
id            PK               id            PK                id            PK
username      (unique)         title                           content
email         (unique)         content                         created_at
password      (hashed)         author_id     FK -> users.id     author_id     FK -> users.id
role          (USER/ADMIN)     created_at                      post_id       FK -> posts.id
created_at                     updated_at
```

- One `User` → many `Post`s
- One `User` → many `Comment`s
- One `Post` → many `Comment`s

## API Overview

All endpoints are prefixed with `/api`. Protected endpoints require a `Authorization: Bearer <token>` header, obtained from `/api/auth/login`.

| Method | Endpoint                    | Auth required | Description                        |
|--------|------------------------------|:---:|-------------------------------------|
| POST   | `/auth/register`            |  –  | Create a new account                |
| POST   | `/auth/login`               |  –  | Log in, receive a JWT               |
| GET    | `/users?page=&size=&sort=`  |  ✔  | List users (paginated)              |
| GET    | `/users/{id}`               |  ✔  | Get a user by id                    |
| PUT    | `/users/{id}`               |  ✔  | Update a user                       |
| DELETE | `/users/{id}`               |  ✔  | Delete a user                       |
| POST   | `/posts`                    |  ✔  | Create a post (author = logged-in user) |
| GET    | `/posts?page=&size=&sort=`  |  ✔  | List posts (paginated)              |
| GET    | `/posts/{id}`               |  ✔  | Get a post by id                    |
| PUT    | `/posts/{id}`               |  ✔  | Update a post (owner or admin only) |
| DELETE | `/posts/{id}`               |  ✔  | Delete a post (owner or admin only) |
| POST   | `/comments?postId=`         |  ✔  | Comment on a post (author = logged-in user) |
| GET    | `/comments/post/{postId}`   |  ✔  | List comments on a post (paginated) |
| GET    | `/comments/user/{userId}`   |  ✔  | List comments by a user             |
| GET    | `/comments/{id}`            |  ✔  | Get a comment by id                 |
| DELETE | `/comments/{id}`            |  ✔  | Delete a comment (owner or admin only) |

Full interactive documentation (request/response schemas, try-it-out) is available via Swagger UI once the app is running — see below.

## Getting Started

### Prerequisites
- Java 21+
- Maven
- A PostgreSQL database (this project was built against a free [Supabase](https://supabase.com) instance)

### Environment variables

The app expects these to be set (no secrets are committed to the repo):

| Variable      | Description                                   |
|---------------|------------------------------------------------|
| `DB_PASSWORD` | Password for the PostgreSQL database            |
| `JWT_SECRET`  | Secret key used to sign JWTs                    |

### Run locally

```bash
git clone https://github.com/<your-username>/university-blog.git
cd university-blog

# set environment variables, then:
./mvnw spring-boot:run
```

The API will start on `http://localhost:8080`.

- **Swagger UI:** `http://localhost:8080/swagger-ui/index.html`
- **Demo client:** `http://localhost:8080/` (served from `src/main/resources/static`)

## Security Notes

- Passwords are hashed with BCrypt before storage — never stored or returned in plain text
- JWTs are stateless (`SessionCreationPolicy.STATELESS`); no server-side session state
- CSRF protection is intentionally disabled — it protects against attacks that rely on browsers automatically attaching cookies, which doesn't apply here since auth is via an explicit `Authorization` header, not cookies
- Error messages are deliberately vague where specificity would leak information (e.g., login failures never reveal whether the username or the password was wrong)

## Roadmap

- [ ] React frontend (replacing the current demo HTML client)
- [ ] Refresh tokens
- [ ] Search/filtering on posts
- [ ] Likes / tags

## About

A personal project built to practice and demonstrate full-stack engineering — from database design and API architecture through to authentication, security, and (eventually) a proper frontend.