# TaskFlow — a Kanban to-do app with Canvas sync

A small Spring Boot application for keeping track of university work. Tasks live on a three-column
Kanban board (To Do / In Progress / Done), lists can be shared with a group, and one button pulls
the current term's assignments straight out of Canvas.

**Live at:** <https://todolist-7yim.onrender.com/jira-frontend.html>

Works with any Canvas instance, any institution, any hemisphere — the API URL is a per-user setting,
and which courses count as "current" is decided from each course's own term dates rather than a
hardcoded semester calendar.

## What it does

- **Accounts** — email + password, or email + one-time code (OTP). Signup requires verifying the
  email with a code before the account is created. JWT-authenticated API.
- **Personal boards** — any number of lists, each with tasks that carry a description and a due
  date. Tasks can be sorted by due date.
- **Group boards** — create a group, invite other users by email, and everyone in the group can
  add, edit, move and delete cards on shared lists. Members can leave a group; the owner can remove
  members (and see who else is in it) from a Members panel. Shared boards poll for changes every
  few seconds so a teammate's edit shows up without a manual refresh.
- **Canvas sync** — paste a Canvas access token once; each sync creates one list per course whose
  term has not ended yet (this term, plus any future term you are already enrolled in) and one task
  per assignment, with the real due date. Re-syncing updates the assignments it already imported
  instead of duplicating them, and leaves any description you typed yourself alone.
- **Theme + responsive layout** — light/dark toggle (defaults to your OS preference), a collapsible
  sidebar, and a mobile layout that collapses secondary actions behind a menu below ~768px.

### How "current term" is decided

Canvas keeps an enrolment "active" long after a term ends, so asking it for your active courses
returns every unit you have ever taken. The sync matches on each course's own term dates instead of
guessing a semester calendar or parsing the term's display name (which has no fixed vocabulary
across institutions — "Semester 2", "Term 1", "Autumn" all mean different things):

- A course is kept while its term's `end_at` date has not passed yet.
- A term with no `end_at` at all (Canvas's catch-all "Default Term", used for courses nobody
  assigned a real semester) is treated as already ended, so it does not sync forever.
- This naturally includes a future term you are already enrolled in, so next term's assignments
  show up as soon as Canvas lists them rather than only once that term starts.

Lists that were imported from a term that has since ended are left where they are; the sync just
stops touching them. Delete them yourself when you want them gone.

## Stack

| | |
|---|---|
| Backend | Java 21, Spring Boot 4.0.2, Spring Security, Spring Data JPA |
| Database | PostgreSQL ([Neon](https://neon.tech) in production) |
| Auth | JJWT (HS256 bearer tokens), bcrypt password hashing, email OTP |
| Email | [Brevo](https://www.brevo.com) transactional email HTTPS API |
| Frontend | One static HTML file, no build step |
| Deployment | Docker on [Render](https://render.com) |
| Tests | JUnit 5, Mockito |

## Running it

**Prerequisites:** JDK 21. Maven is not needed — the wrapper downloads it on first run. PostgreSQL
is the default database, but you can skip installing it — see
[without PostgreSQL](#running-without-postgresql) below.

### With PostgreSQL

1. Check PostgreSQL is running. No database needs creating: the defaults point at the `postgres`
   database that every PostgreSQL installation already has, on `localhost:5432` with user
   `postgres`. Hibernate creates the tables itself on first start (`ddl-auto: update`).

   If the `postgres` user's password is not `postgres`, set it before starting:

   ```powershell
   $env:DB_PASSWORD = "your-password"     # PowerShell
   ```
   ```bash
   export DB_PASSWORD=your-password       # macOS / Linux
   ```

2. Start the app **from the project folder**:

   ```powershell
   .\mvnw.cmd spring-boot:run             # Windows PowerShell — the .\ is required
   ```
   ```bash
   ./mvnw spring-boot:run                 # macOS / Linux
   ```

   First run takes a few minutes: the wrapper fetches Maven and then the dependencies.

3. Open <http://localhost:8081/jira-frontend.html>, sign up (an emailed code is required to
   verify the address), and log in.

4. To pull in Canvas assignments: **Canvas** → paste an access token
   (Canvas → Account → Settings → New Access Token) → **Save & Sync**.

   Sending the signup/login codes needs `MAIL_FROM` and `BREVO_API_KEY` set (see
   [Configuration](#configuration)) — without them, OTP requests fail at the mail-send step.

### Running without PostgreSQL

There is an `h2` profile that runs the app on an embedded database instead. Nothing to install and
nothing to configure — the database is a file under `data/`, so accounts and Canvas tokens still
survive a restart:

```powershell
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=h2"      # Windows
```
```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=h2            # macOS / Linux
```

To avoid repeating the flag, set the profile once for the session:

```powershell
$env:SPRING_PROFILES_ACTIVE = "h2"
```
```bash
export SPRING_PROFILES_ACTIVE=h2
```

Everything works the same. Delete the `data/` folder to start from an empty database. This is meant
for trying the app out and for development — PostgreSQL is what it is otherwise built against.

### If it will not start

| What you see | What it means |
|---|---|
| `无法将"mvnw.cmd"项识别为...` / `mvnw.cmd is not recognized` | PowerShell does not run programs from the current folder unless you prefix them: use `.\mvnw.cmd`. Check you are in the folder that contains `pom.xml`. |
| `Connection to localhost:5432 refused` | PostgreSQL is not running. Start the service (Windows: Services → `postgresql-x64-…`), or run on the `h2` profile instead. |
| `password authentication failed for user "postgres"` | Set `DB_PASSWORD` as in step 1. |
| `Web server failed to start. Port 8081 was already in use` | Something else has the port. Set `SERVER_PORT` to something free. |
| `JAVA_HOME not found` | Install JDK 21 and point `JAVA_HOME` at it. |
| Signup/login-by-code requests fail or hang | `MAIL_FROM` / `BREVO_API_KEY` are not set, or the sender is not verified with Brevo — see [Configuration](#configuration). |

### Configuration

Nothing needs configuring for local development against PostgreSQL or H2. Every value is
overridable by environment variable, which is how it should be run anywhere else:

| Variable | Default | |
|---|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5432/postgres` | |
| `DB_USERNAME` | `postgres` | |
| `DB_PASSWORD` | `postgres` | |
| `SERVER_PORT` | `8081` | Render sets this to `10000`. |
| `JWT_SECRET` | a development string | **Set this to something private outside local dev** — anyone with it can mint a token for any account. |
| `JWT_EXPIRATION` | `86400000` | Token lifetime in milliseconds (24h). |
| `MAIL_FROM` | `noreply@example.com` | The sender address for OTP emails. Must be a sender verified with Brevo (Settings → Senders, Domains & Dedicated IPs), or Gmail/Yahoo/Microsoft silently drop the mail under their DKIM/DMARC requirements. |
| `BREVO_API_KEY` | none | Settings → SMTP & API → API Keys in the Brevo dashboard. Required for signup verification and login-by-code emails to send. |

OTP mail goes through Brevo's HTTPS transactional API rather than SMTP — several hosts (Render's
free tier included) throttle or block outbound SMTP as an anti-abuse measure, which left signup
hanging rather than failing outright; the HTTPS API isn't subject to that restriction.

Canvas tokens are not configuration: each user pastes their own in the UI and it is stored against
their account.

## Deployment

The app is deployed as a Docker container on Render, connected to a [Neon](https://neon.tech)
Postgres database and Brevo for email. `Dockerfile` and `render.yaml` are in the repository root.

To redeploy: push to `main` — Render rebuilds and redeploys automatically on every push, using the
same environment variables configured in its dashboard.

## Tests

```powershell
.\mvnw.cmd test      # Windows
```
```bash
./mvnw test          # macOS / Linux
```

No database or network needed. The unit tests use mocks, and `JiraApplicationTests` — which boots
the whole Spring context — runs against an in-memory H2 database configured in
`src/test/resources/application.yml`.

| Test | Covers |
|---|---|
| `CanvasCourseTermTest` | which courses sync based on their term's end date, and the Default Term case |
| `CanvasCourseTest` | the list name built from a Canvas course, including the awkward shapes |
| `TodoServiceTest` | deleting a task actually removes it, list ownership, partial updates |
| `GroupServiceTest` | shared-list access for non-creator members, leaving a group, and the owner-only kick |
| `AuthServiceTest` | login success and failure |
| `OtpServiceTest` | signup and login OTP requests, code verification, purpose scoping |
| `JwtUtilTest` | token round-trip, wrong user, forged signature, expiry |

## Layout

```
src/main/java/com/example/jira/
  Auth/           signup, login, JWT creation, email OTP, and the request filter
  Configration/   Spring Security configuration
  Group/          groups, invites, membership, shared lists
  Todo/           lists and tasks
  User/           accounts, and the Canvas client and sync
src/main/resources/
  application.yml
  static/jira-frontend.html    the whole UI
src/test/java/                 unit tests
Dockerfile, render.yaml         deployment config for Render
```

## Known limitations

- `ddl-auto: update` manages the schema. Fine for coursework; a real deployment wants migrations
  (Flyway or Liquibase).
- Only the creator of a shared list can rename or delete the list itself. Everyone in the group can
  work on the cards inside it.
- Shared-board updates are polled every few seconds rather than pushed live (no WebSockets).
- The frontend is a single hand-written HTML file with no build step or tests.
