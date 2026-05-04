# GoalPulse — Final Project Documentation

| | |
| --- | --- |
| **Project** | GoalPulse — Live European Soccer Tracking & League Management |
| **Course** | ISTE-330 — Database Connectivity and Access |
| **Institution** | RIT Croatia |
| **Semester** | Spring 2026 |
| **Instructor** | dr. sc. Branko Mihaljević |
| **Team** | Mateo Josipović (coordinator), Petar Marinović, Ana Kovačić, Luka Horvat |
| **Submission date** | 2026-05-09 |
| **Document version** | 1.0 (Final) |

---

## Table of Contents

1. Introduction
2. Problem Description and Solution Architecture
   1. Layered Architecture Overview
   2. Database / Persistence Layer
   3. Database Connectivity / Data Access Layer
   4. Business Logic Layer
   5. Presentation Layer
   6. Areas of Particular Concern
3. Requirements
   1. User Stories (summary; full list in `docs/user_stories.md`)
   2. Functional Requirements (detailed)
   3. Non-Functional Requirements
4. User Documentation
   1. Graphical User Interface Design (with wireframes and screenshots)
   2. User Manual (Guest, Registered, Admin)
5. Installation, Configuration, and Acceptance Testing
   1. Installation
   2. Configuration
   3. Acceptance Testing (full results)
6. Database Dictionary (summary; full table in `docs/db_dictionary.md`)
7. Selection of Languages, Platforms, Technologies and Frameworks
8. Software Design Principles Applied
9. Final Remarks and Conclusion
10. References

---

## 1 Introduction

GoalPulse is a full-stack web application for tracking European soccer competitions. It targets three roles:

- **Guests** browse live matches, schedules, standings, teams, and players.
- **Registered users** add favorite teams, receive notifications, and export standings as CSV.
- **Administrators** update match scores and statuses, add match events, manage teams, players and users, and review an immutable audit log.

The application is a four-tier layered system: a browser SPA (presentation), a Java 21 REST server (business logic), a JDBC DAO (data access), and a relational database (PostgreSQL or MySQL). It is bundled with a one-command demo runner so an examiner can launch the app with a single PowerShell command, and it can be flipped to a real-database mode by setting environment variables. Optional ingestion from football-data.org demonstrates external integration; if that API is unreachable, the app silently falls back to seeded data.

This document is the authoritative reference for the GoalPulse final submission. Companion files in `docs/` provide the proposal (`00_Project_Proposal.md`), the team self-evaluation and peer reviews (`06_Self_Evaluation.md`, `06_Peer_Review.md`), the presentation outline (`07_Presentation.md`), the demo script (`demo_script.md`), the user stories (`user_stories.md`), the full database dictionary (`db_dictionary.md`), and the diagram set (`diagrams/`).

---

## 2 Problem Description and Solution Architecture

### 2.1 Layered Architecture Overview

GoalPulse uses a strict **four-tier layered architecture**:

```
[ Browser SPA ] ──HTTP/JSON──▶ [ Java REST API ] ──JDBC──▶ [ DAO ] ──▶ [ PostgreSQL | MySQL ]
                                       │
                                       └──▶ football-data.org (optional)
```

Layer responsibilities are summarized below; the full diagram is in `docs/diagrams/architecture.md`.

| Layer | Code | Responsibilities |
| --- | --- | --- |
| Presentation | `frontend/index.html`, `frontend/app.js`, `frontend/styles.css` (runtime); `frontend/src/*` (React/TS source structure) | Render UI, capture user input, call REST endpoints. **Never** speaks to the database directly. |
| Business | `backend/src/GoalPulseServer.java`, `PasswordHasher.java` | Routing, validation, RBAC, token signing, audit writes, standings recomputation, notification dispatch, football-data.org integration. |
| Data Access | `backend/src/JdbcGoalPulseDao.java`, `DatabaseConfig.java` | JDBC `Connection` / `PreparedStatement` / `ResultSet` mapping. Implements the DAO interface. **Never** decides RBAC. |
| Database | `database/postgresql/schema.sql`, `database/mysql/schema.sql` (+ `seed.sql`) | Persists relational data, enforces FKs / CHECKs / UNIQUE constraints. |

**Crossing rules (enforced by code review):**
- Presentation may call **only** `/api/v1/*`.
- Business may **never** build SQL strings.
- Data Access may **never** decide on RBAC.
- The browser may **never** reach the database directly.

### 2.2 Database / Persistence Layer

The relational schema contains **14 tables** that model leagues, seasons, teams, players, matches, events, lineups, standings, roles, users, favorites, notifications, and an audit log. Both engines are kept in lockstep:

- `database/postgresql/schema.sql` and `seed.sql` — PostgreSQL 14+
- `database/mysql/schema.sql` and `seed.sql` — MySQL 8+

**Normalization.** All entity tables are in **3NF**: every non-key attribute depends on the key, the whole key, and nothing but the key. We eliminated the obvious transitive dependencies (e.g., we do not denormalize `coach_name` into `match`). The `standing` table is intentionally a denormalized snapshot for fast reads; it is recomputable from `match` rows by the business layer and therefore does not violate normal form for the database as a whole.

**Integrity rules.**
- **Foreign keys** with explicit `ON DELETE` policies (CASCADE / RESTRICT / SET NULL) — chosen per relationship; full table in `docs/db_dictionary.md` § Relationships.
- **CHECK constraints**: `match.status IN ('scheduled','live','finished')`, `home_score / away_score >= 0`, `home_team_id <> away_team_id`, `match_event.minute BETWEEN 0 AND 130`, `player.shirt_number BETWEEN 1 AND 99`, `team.founded_year >= 1850`.
- **UNIQUE** keys: `league_tier.tier_name`, `player_position.pos_name`, `pos_code`, `role.role_name`, `app_user.username`, `email`, `(season_id, team_id)` on `standing`, `(match_id, player_id)` on `match_lineup`, `(user_id, team_id)` on `user_favorite`.

**Engine-specific notes.**
- `match` is a reserved word in MySQL; the MySQL schema backticks it (`` `match` ``) everywhere.
- Surrogate keys: PostgreSQL uses `SERIAL` / `BIGSERIAL`; MySQL uses `INT AUTO_INCREMENT` / `BIGINT AUTO_INCREMENT`.
- `BOOLEAN` is mapped to MySQL's `TINYINT(1)` automatically.

**Seed data.** The seed scripts populate three top-flight leagues (Premier League, La Liga, Bundesliga), six real clubs with real coaches and stadiums, eight notable players, three sample matches across the three leagues, and the role catalog (`guest`, `registered`, `admin`). User accounts (`admin@goalpulse.test`, `mia@goalpulse.test`) are also seeded for the JDBC mode of the demo, with BCrypt-hashed passwords.

The full per-table dictionary (column types, nullability, defaults, constraints, descriptions) is in `docs/db_dictionary.md`.

### 2.3 Database Connectivity / Data Access Layer

The data access layer is implemented in `backend/src/JdbcGoalPulseDao.java` (and configured by `DatabaseConfig.java`) and uses standard JDBC.

**Components used.**
- `DriverManager.getConnection(url, user, password)` — reads `GOALPULSE_DB_URL`, `GOALPULSE_DB_USER`, `GOALPULSE_DB_PASSWORD`.
- `Connection` — one per request, closed via try-with-resources.
- `PreparedStatement` — used for **every** statement that contains user input. Parameter slots (`?`) are filled with `setInt`, `setString`, `setDate`, etc. There is no string concatenation of input into SQL anywhere in the codebase.
- `ResultSet` — mapped row-by-row into Java records (e.g., `MatchSummary`).

**Pattern.** The class follows the **DAO pattern**: business code calls methods like `listLiveMatches()`, `findMatch(id)`, `saveMatchEvent(e)`, `recomputeStandings(seasonId)`, `createUser(u)`, `addFavorite(userId, teamId)`, `writeAudit(a)`. SQL never leaks above this layer.

**Example (sketch).**
```java
try (Connection con = DatabaseConfig.dataSource().getConnection();
     PreparedStatement ps = con.prepareStatement(
         "SELECT m.match_id, ht.short_name AS home, at.short_name AS away, " +
         "       m.home_score, m.away_score, m.status " +
         "FROM match m " +
         "JOIN team ht ON ht.team_id = m.home_team_id " +
         "JOIN team at ON at.team_id = m.away_team_id " +
         "WHERE m.status = ?")) {
    ps.setString(1, "live");
    try (ResultSet rs = ps.executeQuery()) {
        List<MatchSummary> out = new ArrayList<>();
        while (rs.next()) {
            out.add(new MatchSummary(
                rs.getInt("match_id"),
                rs.getString("home"),
                rs.getString("away"),
                rs.getInt("home_score"),
                rs.getInt("away_score"),
                rs.getString("status")));
        }
        return out;
    }
}
```

**Mode toggle.** Setting `GOALPULSE_USE_DB=true` causes `GoalPulseServer` to construct a `JdbcGoalPulseDao` instead of the in-memory `Store`. The REST surface is identical, which is why the same frontend works against both modes.

### 2.4 Business Logic Layer

The business layer is `GoalPulseServer.java`, a single-process Java 21 HTTP server using `com.sun.net.httpserver`. It exposes a versioned REST API under `/api/v1/`.

**Routing.** Each endpoint is registered with a context handler. The router parses the path and method, runs the authentication middleware, then dispatches to a handler method.

**Validation.** Request bodies are decoded as JSON, and field-level validation (non-empty strings, integer ranges, enum values) is performed before any DAO call. Validation failures return `400 Bad Request` with a structured envelope.

**Authentication.** Login and registration return a signed bearer token (HMAC-SHA256 over `header.payload` using `GOALPULSE_TOKEN_SECRET`) plus a refresh token. The token's `exp` claim (one hour) is enforced server-side on every authenticated request. Passwords are stored using **BCrypt** (`PasswordHasher.java` + jBCrypt). A documented PBKDF2 fallback exists for environments without jBCrypt on the classpath.

**Authorization (RBAC).** Three roles: `guest`, `registered`, `admin`. Admin endpoints check `role == 'admin'` before executing; **denied attempts are themselves audited**, so probing is detectable.

**Business rules enforced server-side.**
- **Standings recomputation** runs after any score update on a finished match.
- **Audit log** writes happen on every admin create/update/delete and on denied admin attempts.
- **Notification dispatch** fires for users who have favorited either the home or away team, after a score update.
- **football-data.org integration** ingests current-day matches and team crests when a token is set; failures (missing token, expired, rate-limit, network) fall back to seeded data without surfacing an error to the UI.
- **Crest proxy** (`/api/v1/assets/crest`) keeps external image URLs server-side to avoid broken hotlinks in the browser.

**REST response envelope.**
```json
{ "success": true, "data": { ... }, "message": "OK" }
```

### 2.5 Presentation Layer

The presentation layer is a responsive single-page application in `frontend/`.

**Two presentation tracks (deliberate).**
- **Runtime track** — `frontend/index.html` + `frontend/styles.css` + `frontend/app.js`. This is what is actually served by the Java backend at `http://localhost:8080`. It runs without `npm` and without a build step, which makes the one-command demo (`scripts/run.ps1`) work on any lab machine.
- **Design-time React/TS track** — `frontend/src/` contains React 18 / TypeScript page components, route protection, an auth context, API modules, and DTO types. This source structure documents how the presentation layer is **organized** and is the production-grade evolution path. It is included as evidence of layered presentation design and is not the runtime entry point.

This dual track is documented intentionally so the examiner is not surprised: the runnable demo uses the vanilla SPA; the React/TS source is provided for code review and for future builds (`npm run dev`).

**Communication contract.** Both tracks talk to the backend only through `/api/v1/*` JSON endpoints. The browser never reads database data directly.

**Primary views.**
- Live dashboard, Match detail, Schedule, Standings, Teams, Players, Favorites, Login/Register, Admin.

**UX features.** Dark-first theme with toggle, responsive layout with mobile bottom navigation under 560 px, command search (`Ctrl+K`), CSV export, skeleton loaders, empty states, sticky navigation, live pulse indicators.

Wireframes for the five primary screens are in `docs/diagrams/wireframes.md`. Real screenshots are in `docs/screenshots/`.

### 2.6 Areas of Particular Concern

Each item is presented as **risk → mitigation → residual risk**.

**Authentication.**
- *Risk:* leaking session credentials, token replay.
- *Mitigation:* HMAC-SHA256-signed tokens with one-hour expiry; refresh token returned at login; tokens stored in `localStorage` with `Authorization: Bearer …` header.
- *Residual:* the demo uses HTTP, not HTTPS. **Production deployment must terminate TLS.**

**Authorization.**
- *Risk:* a malicious frontend bypasses the admin UI and calls admin endpoints directly.
- *Mitigation:* role check happens **server-side** on every admin call. Denied calls are written to `audit_log`.
- *Residual:* none for the demo scope; for production, add per-endpoint rate limiting.

**Password storage.**
- *Risk:* if the database leaks, plaintext passwords would be stolen.
- *Mitigation:* BCrypt with `jBCrypt`. PBKDF2 fallback for lab machines without Maven.
- *Residual:* BCrypt's work factor (10) should be raised to 12+ for production.

**Auditing.**
- *Risk:* admin actions are unaccountable.
- *Mitigation:* every create/update/delete and every denied attempt writes to `audit_log` (actor, action, table, record, old/new value, timestamp, IP).
- *Residual:* logs are append-only by convention, not by DB privilege. A production deployment should grant `INSERT only` to the application role.

**Data integrity.**
- *Risk:* invalid scores, impossible team self-matches, score concurrency anomalies.
- *Mitigation:* CHECK constraints (`status`, `score >= 0`, `home_team_id <> away_team_id`, minute and shirt number ranges). Score updates run inside a transaction with audit and standings recomputation.
- *Residual:* no row-level locking; for production, add `SELECT ... FOR UPDATE` on the match row.

**Live data dependency.**
- *Risk:* football-data.org down or rate-limited at demo time.
- *Mitigation:* silent fallback to seeded data; logged once, not surfaced to the UI.
- *Residual:* none for grading; for production, cache responses with a short TTL.

**External image hotlinking.**
- *Risk:* football-data.org crest URLs become 404s, leaving broken image icons.
- *Mitigation:* `/api/v1/assets/crest` proxy serves crests through the backend, with short-code fallback.
- *Residual:* none.

---

## 3 Requirements

### 3.1 User Stories (summary)

Full list is in `docs/user_stories.md` (US-G1…G4 for Guest, US-R1…R4 for Registered, US-A1…A5 for Admin, US-S1…S3 for system-level). Two examples:

- *US-R1.* As a Registered user, I want to favorite a team, so that I can find their next match without searching every time.
- *US-A4.* As an Admin, I want denied admin attempts to be logged too, so that I can detect probing or misuse.

### 3.2 Functional Requirements (detailed)

> Each FR follows the format: **ID, Name, Description, Actor, Preconditions, Main Flow, Alternate Flow, Postconditions, Endpoint(s).**

**FR-01 — Authentication (registration and login).**
- *Description.* Users register and log in with email + password; receive a signed bearer token.
- *Actor.* Guest (registers/logs in), Registered, Admin.
- *Preconditions.* Server running; DB or seed user catalog available.
- *Main flow.* Submit credentials → password verified via BCrypt → token signed and returned.
- *Alternate flow.* Wrong credentials → 401 with generic "invalid credentials" message.
- *Postconditions.* Browser stores token; subsequent calls include `Authorization: Bearer <token>`.
- *Endpoints.* `POST /api/v1/auth/register`, `POST /api/v1/auth/login`.

**FR-02 — Live matches view.**
- *Description.* Public dashboard shows today's matches with score and live indicator.
- *Actor.* Guest, Registered, Admin.
- *Preconditions.* None.
- *Main flow.* Load `/api/v1/matches?status=live|today` → render cards.
- *Alternate flow.* No matches → empty state card.
- *Postconditions.* Cards visible.
- *Endpoint.* `GET /api/v1/matches`.

**FR-03 — Match detail view.**
- *Description.* Hero score, status, venue, referee, event timeline, lineups.
- *Actor.* Any.
- *Preconditions.* `match_id` exists.
- *Main flow.* `GET /api/v1/matches/{id}` → render.
- *Alternate flow.* Unknown id → 404.
- *Postconditions.* Detail page visible.

**FR-04 — Standings.**
- *Description.* Computed table sorted by Pts → GD → GF; top-four and danger-zone highlighted.
- *Actor.* Any.
- *Preconditions.* At least one finished match.
- *Main flow.* `GET /api/v1/standings?seasonId=X` → render table.
- *Alternate flow.* No matches → 0-row table.
- *Postconditions.* Table visible.

**FR-05 — Schedule by date.**
- *Description.* Fixture list filtered by date.
- *Actor.* Any.
- *Main flow.* `GET /api/v1/schedule?date=YYYY-MM-DD` → render.

**FR-06 — Team profiles.** `GET /api/v1/teams/{id}` shows stadium, city, coach, founding year, squad.

**FR-07 — Player profile cards.** `GET /api/v1/players/{id}` shows position, nationality, DOB, shirt number, recent form.

**FR-08 — Search.** `GET /api/v1/search?q=…` returns teams + players + matches; UI exposes `Ctrl+K`.

**FR-09 — Favorite teams.**
- *Actor.* Registered.
- *Preconditions.* Logged in.
- *Main flow.* `POST /api/v1/favorites` with `{teamId}` → row inserted into `user_favorite`.
- *Alternate.* Duplicate favorite → idempotent OK.
- *Postcondition.* Team appears in Favorites tab.

**FR-10 — Admin league/season visibility.**
- *Actor.* Admin.
- *Endpoints.* `GET /api/v1/admin/leagues`, `/admin/seasons`.

**FR-11 — Admin match score/status update and event creation.**
- *Actor.* Admin.
- *Main flow.* `POST /api/v1/admin/match/{id}/score` or `/admin/match/{id}/event` → validation → transaction (UPDATE/INSERT, audit insert, standings recompute, notification dispatch) → COMMIT.
- *Alternate.* Validation fails → 400; not admin → 403 + audit `denied`.

**FR-12 — Admin team/player/user management + audit.**
- *Endpoints.* `GET/POST/PUT/DELETE` under `/api/v1/admin/teams`, `/admin/players`, `/admin/users`, `/admin/audit`.

**FR-13 — CSV export of standings.**
- *Actor.* Registered.
- *Main flow.* `GET /api/v1/standings.csv` → browser saves file.

**FR-14 — Responsive design.** UI usable from ≥320 px wide. Mobile bottom navigation under 560 px.

**FR-15 — Notifications.** Score updates create rows in `notification` for users favoriting the relevant teams. UI displays unread badge.

### 3.3 Non-Functional Requirements

**NFR-Performance.**
- API median response time < 150 ms with seeded data on a developer laptop.
- Dashboard first contentful paint < 2 s on the same hardware.
- Capacity: tested up to 1000 concurrent read requests/min in JDBC mode.

**NFR-Security.**
- BCrypt password hashing (work factor ≥ 10).
- Signed bearer tokens (HMAC-SHA256), one-hour expiry.
- Server-side RBAC on every admin endpoint.
- Parameterized SQL (PreparedStatement) for **all** input-bearing queries.
- HTTPS recommended for production deployment (demo runs over HTTP on `localhost`).
- Audit log of every admin write **and** denied attempt.

**NFR-Reliability.**
- Graceful fallback when football-data.org is missing/expired/rate-limited/offline.
- DB-mode failure to acquire a connection logs and returns 503 instead of crashing.

**NFR-Usability.**
- Responsive layout from ≥320 px.
- Keyboard shortcuts: `Ctrl+K` (search), `Esc` (close modal).
- Empty states and skeleton loaders to indicate progress.
- Theme toggle (dark/light).

**NFR-Portability.**
- PostgreSQL **and** MySQL parallel scripts.
- JDK 17+ (tested on 21).
- Demo runs on Windows, macOS, and Linux without code changes.

**NFR-Maintainability.**
- Clear four-layer separation; DAO pattern; small per-file responsibility.
- Two SQL dialects kept in lockstep with reviewed parity.

**NFR-Auditability.**
- Append-only `audit_log` table; one row per admin action and per denied attempt.

---

## 4 User Documentation

### 4.1 Graphical User Interface Design

GoalPulse uses a **premium dark sports-dashboard layout** rather than a marketing landing page. The first screen shows live match cards, scores, live indicators, and matchday statistics. Top navigation: Live, Schedule, Standings, Teams, Players, Favorites, Admin.

**Main interface areas.**
- *Top bar.* Brand identity, navigation tabs, theme toggle, session status.
- *Match strip.* Quick score tiles for currently loaded matches.
- *Workspace.* Account/search panel on the left, active feature view on the right.
- *Admin area.* Score/event forms, management tables, audit log card.

**Mobile.** Below ~560 px, the layout collapses to one column and uses bottom navigation. Cards and tables resize to avoid overflow.

**Wireframes.** Pre-implementation wireframes for the five primary screens are in `docs/diagrams/wireframes.md`.

**Screenshots from the running app.**

| File | View |
| --- | --- |
| `docs/screenshots/dashboard.png` | Live dashboard |
| `docs/screenshots/match-detail.png` | Match detail page |
| `docs/screenshots/standings.png` | League standings |
| `docs/screenshots/teams-players.png` | Team / player profiles |
| `docs/screenshots/favorites.png` | Favorites + notifications |
| `docs/screenshots/admin-panel.png` | Admin panel |
| `docs/screenshots/login-register.png` | Auth modal |

> **Note on screenshots.** The screenshots were re-captured for the final submission directly from the running application (`http://localhost:8080`) so they reflect actual UI, not placeholders.

### 4.2 User Manual

#### Guest user

1. Open `http://localhost:8080` (or the configured port).
2. Use **Live** to view today's matches grouped by league.
3. Click **Details** on a card to open match detail (timeline, lineups, stats).
4. Open **Schedule** to filter fixtures by date.
5. Open **Standings** to view calculated rankings.
6. Open **Teams** or **Players** to browse profile information.
7. Press `Ctrl+K` to open command search; type a team, player, or match.

#### Registered user

1. Click **Login**.
2. Enter `mia@goalpulse.test` / `User123!` (or register a new account).
3. Open **Teams** and click the **★** favorite button.
4. Open **Favorites** to view saved teams, next/recent matches, and notifications.
5. Open **Standings** and click **Export CSV** to download `goalpulse-standings.csv`.

#### Admin user

1. Log in with `admin@goalpulse.test` / `Admin123!`.
2. Open **Admin**.
3. **Update score/status:** pick a match, edit `home_score`/`away_score`/`status`, save.
4. **Add match event:** choose match, player, event type, minute, optional detail.
5. Use **Management Tables** for teams, players, and users.
6. Confirm any destructive action (delete) before committing.
7. Review the **Audit Log** card after changes.

---

## 5 Installation, Configuration, and Acceptance Testing

### 5.1 Installation

**Prerequisites.**
- Java JDK 17+ (tested with Java 21).
- PowerShell on Windows (or bash on macOS/Linux — see `INSTALL.md`).
- Optional: PostgreSQL 14+ or MySQL 8+ to run the JDBC-backed mode.
- Optional: Maven 3.8+ to build the dependency-managed JAR (`backend/pom.xml`). The Maven build was performed at least once during development to produce `backend/target/*.jar` and exercise the BCrypt path; the one-command demo does not require it.
- Optional: Node 20+ / npm 10+ to build the React/TS source.

**One-command demo (Windows).**

```powershell
cd <path-to-repo>
.\scripts\run.ps1
```

If port `8080` is busy:

```powershell
$env:PORT="8081"
.\scripts\run.ps1
```

**Database setup (PostgreSQL).**

```bash
psql -U <user> -d goalpulse -f database/postgresql/schema.sql
psql -U <user> -d goalpulse -f database/postgresql/seed.sql
```

**Database setup (MySQL).**

```bash
mysql -u <user> -p goalpulse < database/mysql/schema.sql
mysql -u <user> -p goalpulse < database/mysql/seed.sql
```

Cross-platform install instructions (Windows + macOS + Linux) are in `INSTALL.md`.

### 5.2 Configuration

**Environment variables.**

| Variable | Purpose | Default |
| --- | --- | --- |
| `PORT` | HTTP port the server listens on | `8080` |
| `GOALPULSE_TOKEN_SECRET` | HMAC-SHA256 secret for bearer tokens | dev-only fallback |
| `FOOTBALL_DATA_API_TOKEN` | Optional football-data.org v4 token | (unset → use seeds) |
| `GOALPULSE_USE_DB` | `true` to use JDBC instead of in-memory `Store` | `false` |
| `GOALPULSE_DB_URL` | JDBC URL (PG: `jdbc:postgresql://…`, MySQL: `jdbc:mysql://…`) | — |
| `GOALPULSE_DB_USER` | DB username | — |
| `GOALPULSE_DB_PASSWORD` | DB password | — |

**Run with the real database (PostgreSQL).**

```powershell
$env:GOALPULSE_USE_DB="true"
$env:GOALPULSE_DB_URL="jdbc:postgresql://localhost:5432/goalpulse"
$env:GOALPULSE_DB_USER="goalpulse"
$env:GOALPULSE_DB_PASSWORD="<password>"
.\scripts\run.ps1
```

The server logs `Server listening on http://localhost:8080 (JDBC mode)` when JDBC mode is active.

**Run with live football-data.org.**

```powershell
$env:FOOTBALL_DATA_API_TOKEN="your_token_here"
.\scripts\run.ps1
```

If the token is missing/expired/rate-limited, the server falls back to seeded data without surfacing an error.

### 5.3 Acceptance Testing

The full acceptance test plan and execution log (all 18 cases with Pass/Fail and execution date) is in `docs/acceptance_tests.md`. Summary:

- **Total cases:** 18
- **Passed:** 18
- **Failed:** 0
- **Final execution date:** 2026-05-04

The plan covers public dashboard load, match detail, standings, search, registered login, favorites, CSV export, admin login, score update, event creation, RBAC denial (with audit), responsive layout, online data load, crest proxy, admin team delete, admin users endpoint, the React/TS source structure, and the BCrypt dependency path.

---

## 6 Database Dictionary

A summary of the 14 tables is below. Full per-column entries (type, nullability, default, constraint, description) and the relationship matrix are in `docs/db_dictionary.md`.

| Table | Purpose | PK |
| --- | --- | --- |
| `league_tier` | Catalog of league tiers (e.g., First Division). | `tier_id` |
| `league` | A national or competition league. | `league_id` |
| `season` | A specific season of a league. | `season_id` |
| `team` | A club competing in a league. | `team_id` |
| `player_position` | Position catalog (`GK`, `DEF`, `MID`, `FWD`). | `position_id` |
| `player` | A squad member. | `player_id` |
| `match` | A scheduled / live / finished match. | `match_id` |
| `match_event` | Goal, card, substitution, etc. (weak entity). | `event_id` |
| `match_lineup` | Starters and bench. | `lineup_id` |
| `standing` | Snapshot of a team's W/D/L/GF/GA/GD/Pts in a season. | `standing_id` |
| `role` | Role catalog (`guest`, `registered`, `admin`). | `role_id` |
| `app_user` | Application user account. | `user_id` |
| `user_favorite` | User → team favorites (M:N). | `favorite_id` |
| `notification` | Per-user notifications. | `notif_id` |
| `audit_log` | Append-only log of admin actions and denied attempts. | `log_id` |

---

## 7 Selection of Languages, Platforms, Technologies and Frameworks

| Layer | Choice | Why this and not the alternative |
| --- | --- | --- |
| Presentation (runtime) | HTML/CSS/JS SPA | One-command demo runs without `npm`, runs from the JVM. We prefer "no build step" over "best-of-breed framework" for the lab machine. |
| Presentation (design) | React 18 + TypeScript + Vite (`frontend/src`) | Source structure documents component boundaries and is the production evolution path. We didn't pick Angular (heavier framework, steeper learning curve) or Svelte (smaller community). |
| Business | Java 21 + `com.sun.net.httpserver` | Zero-config REST server; no Spring container to configure or explain. Spring Boot would have added 30-60 s startup time and ~30 MB classpath for capabilities we don't need at this scope. |
| Auth | Custom HMAC-SHA256 bearer tokens + jBCrypt | The exact JWT-style flow used in lecture; fits the course expectation. We didn't pick Keycloak (overkill) or Spring Security (drags Spring in). |
| Data Access | JDBC + DAO pattern | Course-correct, transparent SQL, easy to grade. We didn't pick Hibernate/JPA: ORMs hide SQL injection risk and the class needs explicit `PreparedStatement` evidence. |
| Database | **Both** PostgreSQL 14+ and MySQL 8+ | Demonstrates portability and teaches handling of dialect differences (reserved words, CHECK syntax). Both engines are commonly used in industry. |
| External | football-data.org v4 | Free token, well-documented JSON, no payment integration to mock. We didn't pick API-Sports (paywall) or web-scraping (fragile, ToS issues). |
| Build (optional) | Maven (`pom.xml`) | Pulls jBCrypt, PostgreSQL, MySQL JDBC drivers reproducibly. We didn't pick Gradle (more complex `build.gradle.kts`) or no build at all (the BCrypt path needs jBCrypt). |
| Operating system | Windows, tested on macOS / Linux | Lab provides Windows; bash + PowerShell scripts cover the rest. |

---

## 8 Software Design Principles Applied

The course material is reflected at several specific points in the codebase:

- **Layered architecture / separation of concerns.** Strict four tiers; documented crossing rules; SQL never appears above the DAO.
- **DAO pattern.** `JdbcGoalPulseDao` implements a single interface; business code is unaware of JDBC.
- **DTO / record types.** `MatchSummary` and similar Java records carry data across the layer boundary instead of leaking JDBC types.
- **Single Responsibility.** `PasswordHasher` is exclusively about hashing; `DatabaseConfig` exclusively about creating connections.
- **Encapsulation and immutability.** Records are immutable; mutable state is confined to the in-memory `Store` class used in the demo path.
- **Defense in depth.** `PreparedStatement` everywhere, RBAC check server-side regardless of frontend hiding, audit log records denied attempts.
- **REST + stateless authentication.** Tokens carry the role; the server is stateless across requests.
- **Audit logging.** Every admin write and denied attempt creates an immutable record.
- **3NF schema design.** All entity tables in 3NF; the only denormalized table (`standing`) is intentionally a recomputable snapshot.
- **Foreign-key integrity with explicit ON DELETE policies.** Each FK chooses CASCADE / RESTRICT / SET NULL deliberately (full rationale in `docs/db_dictionary.md`).
- **Input validation at the boundary.** Validation runs in the business layer before any DAO call; the DAO trusts its inputs.
- **Graceful degradation.** External-data failure falls back to seeds; missing Maven path falls back to PBKDF2.

---

## 9 Final Remarks and Conclusion

GoalPulse is delivered as a complete, self-contained ISTE-330 final project. Every layer required by the rubric is present and demonstrable: a 14-table relational schema in 3NF (with parallel PostgreSQL and MySQL scripts), a JDBC DAO using `PreparedStatement` exclusively, a Java REST API with token-based authentication, server-side RBAC, audit logging, and a responsive dark-themed SPA. The project also goes beyond the minimum: optional football-data.org integration with graceful fallback, a React/TypeScript design-time source structure, a crest proxy for image reliability, and an immutable audit trail that records denied admin attempts as well as accepted actions.

**Lessons learned.**
- Defining the REST envelope and the DAO interface up front meant the four layers could evolve independently. We never had to refactor the data access layer because we never had to change the contract above it.
- Maintaining PostgreSQL and MySQL schemas in lockstep caught two portability issues early (the reserved word `match`, CHECK syntax differences). It is cheaper to spot dialect drift in a 14-table project than in a 100-table one.
- Building the offline fallback for football-data.org **first** was the single best reliability decision we made. It eliminated the "Wi-Fi failed mid-demo" risk class entirely.
- Two parallel frontends (vanilla + React/TS) added documentation overhead. In a future project we would commit to the React/TS path from the first milestone.

**Completed scope.** Live dashboard, match detail, standings (with computed Pts/GD/GF), schedule by date, team and player profiles, search, favorites, notifications, CSV export, admin score/event/team/player/user management, audit log including denied-attempt path, BCrypt + PBKDF2 password storage, signed bearer tokens, RBAC, football-data.org integration with fallback, crest proxy, dark/light theme, mobile bottom navigation, command search, full M0–M5 documentation set, all 18 acceptance tests passing.

**Future work (ideas, not gaps).**
- Switch the runtime presentation to the React/TS build; retire the vanilla SPA.
- Add materialized views for standings to remove the recomputation step from the business layer.
- Hosted CI pipeline (GitHub Actions) to run schema lint, JDBC integration tests, and an automated smoke test on every push.
- Per-endpoint rate limiting and HTTPS termination as the first hosted-deployment hardening pass.
- WebSocket push for live scores instead of polling.

---

## 10 References

1. **Java Platform, SE 21 — API Specification.** Oracle. <https://docs.oracle.com/en/java/javase/21/docs/api/>
2. **Java JDBC API.** Oracle. <https://docs.oracle.com/en/java/javase/21/docs/api/java.sql/module-summary.html>
3. **PostgreSQL 16 Documentation.** PostgreSQL Global Development Group. <https://www.postgresql.org/docs/16/>
4. **MySQL 8.0 Reference Manual.** Oracle. <https://dev.mysql.com/doc/refman/8.0/en/>
5. **jBCrypt — A Java Implementation of OpenBSD's Blowfish Password Hashing Code.** <https://www.mindrot.org/projects/jBCrypt/>
6. **OWASP Top 10 (2021).** OWASP Foundation. <https://owasp.org/Top10/>
7. **OWASP Cheat Sheet Series — Password Storage.** OWASP Foundation. <https://cheatsheetseries.owasp.org/cheatsheets/Password_Storage_Cheat_Sheet.html>
8. **OWASP Cheat Sheet Series — SQL Injection Prevention.** <https://cheatsheetseries.owasp.org/cheatsheets/SQL_Injection_Prevention_Cheat_Sheet.html>
9. **football-data.org v4 API.** <https://www.football-data.org/documentation/api>
10. **React 18 Documentation.** Meta. <https://react.dev/>
11. **Vite — Next-Generation Frontend Tooling.** <https://vitejs.dev/>
12. **TypeScript Handbook.** Microsoft. <https://www.typescriptlang.org/docs/handbook/intro.html>
13. **Apache Maven 3.x Documentation.** Apache Software Foundation. <https://maven.apache.org/>
14. **RFC 7519 — JSON Web Token (JWT).** IETF. <https://www.rfc-editor.org/rfc/rfc7519>
15. **RFC 7617 — The 'Basic' HTTP Authentication Scheme.** IETF. <https://www.rfc-editor.org/rfc/rfc7617>
16. **ISTE-330 Lecture Slides.** dr. sc. Branko Mihaljević. RIT Croatia, Spring 2026.
17. **MDN Web Docs.** Mozilla. <https://developer.mozilla.org/>

---

## Appendix A — Companion documents

| File | Purpose |
| --- | --- |
| `docs/00_Project_Proposal.md` | Deliverable 0. Topic, team, idea, tech stack, milestones. |
| `docs/06_Self_Evaluation.md` | Per-member task list with man-days. |
| `docs/06_Peer_Review.md` | Pairwise reviews with the rubric "would you work with them again" question. |
| `docs/07_Presentation.md` | 15-slide presentation outline. |
| `docs/demo_script.md` | 5–6 minute live-demo click-by-click. |
| `docs/user_stories.md` | Standard-form user stories. |
| `docs/db_dictionary.md` | Full per-column dictionary for all 14 tables. |
| `docs/acceptance_tests.md` | 18 acceptance tests with Pass/Fail and execution date. |
| `docs/diagrams/architecture.md` | Layered architecture diagram. |
| `docs/diagrams/erd.md` | Entity-relationship diagram. |
| `docs/diagrams/use_case.md` | UML Use Case diagram. |
| `docs/diagrams/class_diagram.md` | UML Class diagram. |
| `docs/diagrams/component_diagram.md` | UML Component diagram. |
| `docs/diagrams/activity_admin_score_update.md` | UML Activity diagram (admin path). |
| `docs/diagrams/activity_login.md` | UML Activity diagram (login path). |
| `docs/diagrams/wireframes.md` | Pre-implementation UI wireframes. |
| `docs/qa_prep.md` | Q&A preparation sheet for the live presentation. |
| `docs/team.md` | Team roster with roles. |
| `docs/CHANGELOG.md` | Milestone-by-milestone progress (M1 → M5). |
| `INSTALL.md` | Cross-platform installation instructions. |
| `LICENSE` | MIT License. |
