# Jira — a Kanban to-do app with Canvas sync

A small Spring Boot application for keeping track of university work. Tasks live on a three-column
Kanban board (To Do / In Progress / Done), lists can be shared with a group, and one button pulls
the current semester's assignments straight out of Canvas.

Built for USYD, so the Canvas defaults point at `canvas.sydney.edu.au`, but any Canvas instance
works — the API URL is a per-user setting.

## What it does

- **Accounts** — sign up, log in, JWT-authenticated API.
- **Personal boards** — any number of lists, each with tasks that carry a description and a due date.
- **Group boards** — invite other users to a group, share lists, and everyone in the group can add,
  edit, move and delete cards on them.
- **Canvas sync** — paste a Canvas access token once; each sync creates one list per course you are
  enrolled in *this semester* and one task per assignment, with the real due date. Re-syncing
  updates the assignments it already imported instead of duplicating them, and leaves any
  description you typed yourself alone.

### How "this semester" is decided

Canvas keeps an enrolment "active" long after a semester ends, so asking it for your active courses
returns every unit you have ever taken. The sync therefore matches on the term Canvas prints under
each course card on the dashboard — `Semester 2 2026`:

- `AcademicTerm.current(today)` works out the semester in progress; the year splits at 1 July.
- A course is kept only when its term name parses to that same year and semester.
- Anything else — a past semester, next semester, or a term named something unparseable like
  `Default Term` — is skipped, and the sync response names what it skipped so a zero-course sync
  explains itself instead of looking broken.

Lists that were imported from earlier semesters are left where they are; the sync just stops
touching them. Delete them yourself when you want them gone.

## Stack

| | |
|---|---|
| Backend | Java 21, Spring Boot 4.0.2, Spring Security, Spring Data JPA |
| Database | PostgreSQL |
| Auth | JJWT (HS256 bearer tokens) |
| Frontend | One static HTML file, no build step |
| Tests | JUnit 5, Mockito |

## Running it

**Prerequisites:** JDK 21 and a PostgreSQL server.

1. Create the database (the default configuration expects a database named `postgres` on
   `localhost:5432` with user `postgres`):

   ```bash
   createdb postgres
   ```

   Hibernate creates the tables itself on first start (`ddl-auto: update`).

2. Start the app:

   ```bash
   ./mvnw spring-boot:run        # macOS / Linux
   mvnw.cmd spring-boot:run      # Windows
   ```

3. Open <http://localhost:8081/jira-frontend.html>, sign up, and log in.

4. To pull in Canvas assignments: **Sync Canvas** → paste an access token
   (Canvas → Account → Settings → New Access Token) → **Save & Sync**.

### Configuration

Nothing needs configuring for local development. Every value is overridable by environment
variable, which is how it should be run anywhere else:

| Variable | Default | |
|---|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5432/postgres` | |
| `DB_USERNAME` | `postgres` | |
| `DB_PASSWORD` | `postgres` | |
| `SERVER_PORT` | `8081` | |
| `JWT_SECRET` | a development string | **Set this to something private outside local dev** — anyone with it can mint a token for any account. |
| `JWT_EXPIRATION` | `86400000` | Token lifetime in milliseconds (24h). |

Canvas tokens are not configuration: each user pastes their own in the UI and it is stored against
their account.

## Tests

```bash
./mvnw test
```

Everything under `src/test/java` runs without a database or a network, except
`JiraApplicationTests`, which boots the full Spring context and therefore needs PostgreSQL running.
To skip it:

```bash
./mvnw test -Dtest='!JiraApplicationTests'
```

| Test | Covers |
|---|---|
| `CanvasCourseTermTest` | which semester a course belongs to, and the terms that must be skipped |
| `CanvasCourseTest` | the list name built from a Canvas course, including the awkward shapes |
| `TodoServiceTest` | deleting a task actually removes it, list ownership, partial updates |
| `GroupServiceTest` | a group member who did not create a shared list can still work on it, and cannot reach lists outside their group |
| `AuthServiceTest` | signup validation, password hashing, login success and failure |
| `JwtUtilTest` | token round-trip, wrong user, forged signature, expiry |

## Layout

```
src/main/java/com/example/jira/
  Auth/           signup, login, JWT creation and the request filter
  Configration/   Spring Security configuration
  Group/          groups, invites, shared lists
  Todo/           lists and tasks
  User/           accounts, and the Canvas client and sync
src/main/resources/
  application.yml
  static/jira-frontend.html    the whole UI
src/test/java/                 unit tests
```

## Known limitations

- `ddl-auto: update` manages the schema. Fine for coursework; a real deployment wants migrations
  (Flyway or Liquibase).
- Only the creator of a shared list can rename or delete the list itself. Everyone in the group can
  work on the cards inside it.
- The frontend is a single hand-written HTML file with no build step or tests.
