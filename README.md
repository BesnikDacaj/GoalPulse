# GoalPulse

GoalPulse is a full-stack ISTE-330 course project for tracking European soccer matches, standings, teams, player rosters, favorites, notifications, and admin match updates.

## Stack

- Backend: Java 21 using the built-in HTTP server, layered service/repository structure, token auth, RBAC checks, and audit logging.
- Frontend: responsive browser SPA for the one-command demo, plus formal React/TypeScript source structure in `frontend/src`.
- Database deliverables: PostgreSQL and MySQL schema plus seed data in `database/`.
- Dependency-managed backend path: `backend/pom.xml` includes jBCrypt, PostgreSQL JDBC, and MySQL JDBC dependencies.

The backend ships with an in-memory repository so it runs immediately for grading/demo purposes. The SQL files provide the full relational database implementation expected by the documentation.

## Direction Applied

- Dark-first premium dashboard inspired by SofaScore, FotMob, and Flashscore.
- Live dashboard, schedule, standings, teams, players, favorites, search, login/register, and admin panel.
- Live pulse indicators, timeline-centered match detail, styled tables, responsive mobile bottom navigation, skeleton loading, empty states, command search with `Ctrl+K`, dark/light toggle, CSV export, favorites, notifications, and admin stats.
- REST responses use the course-style envelope: `success`, `data`, and `message`.
- Backend keeps JWT-style signed tokens, role checks, prepared-statement JDBC examples, PostgreSQL/MySQL scripts, and audit logging.
- Admin APIs cover matches, match events, teams, players, users, leagues, seasons, and audit logs.
- M4 documentation and embedded screenshots are in `docs/GoalPulse_Project_Documentation_M4.docx`.

## Run

```powershell
.\scripts\run.ps1
```

Then open:

```text
http://localhost:8080
```

Demo users:

- Admin: `admin@goalpulse.test` / `Admin123!`
- Registered user: `mia@goalpulse.test` / `User123!`

## Optional Real Match Data

GoalPulse can load real daily match data from football-data.org v4. Create a free API token at football-data.org, then run:

```powershell
$env:FOOTBALL_DATA_API_TOKEN="your_token_here"
.\scripts\run.ps1
```

If the token is missing, expired, rate-limited, or offline, the app automatically falls back to the seeded course/demo data.

Team crests from football-data.org are loaded through the backend at `/api/v1/assets/crest`, which keeps images reliable in the browser and avoids broken external hotlinks.

## Important Paths

- `backend/src/GoalPulseServer.java` - Java REST API and business layer.
- `frontend/` - SPA presentation layer.
- `database/postgresql/schema.sql` - PostgreSQL database.
- `database/mysql/schema.sql` - MySQL database.
- `docs/M4_Project_Documentation.md` - Milestone 4 documentation content.
- `docs/acceptance_tests.md` - M4 acceptance test plan.
