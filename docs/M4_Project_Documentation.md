# GoalPulse Project Documentation - Milestone 4

## 1 Introduction

GoalPulse is a web application for tracking European soccer matches, live scores, match events, team/player profiles, league standings, favorites, notifications, and administrative match data updates. The Milestone 4 implementation includes database scripts, Java REST backend, optional football-data.org live-data integration, a runnable browser frontend, and a formal React/TypeScript frontend source structure.

## 2 Problem Description and Solution Architecture

### 2.5 Presentation Layer

The presentation layer is a responsive single-page application in `frontend/`. For the one-command course demo, `frontend/index.html`, `frontend/styles.css`, and `frontend/app.js` run directly from the Java server. To satisfy the documented React stack, `frontend/src/` also contains React/TypeScript source structure with API modules, DTO types, auth context, protected/admin routes, and page components.

Both frontend paths communicate with the Java backend only through `/api/v1` JSON endpoints and never read database data directly.

Primary views:

- Live dashboard: live/upcoming matches, league grouping, score cards, favorite buttons, and match details.
- Match details: hero score section, status, venue, referee, event timeline, lineups, stats, and H2H-ready data.
- Schedule: date-filtered fixture list.
- Standings: computed table with rank, played, wins, draws, losses, goals, goal difference, points, top-four and danger-zone highlighting.
- Teams and Players: profile cards with stadium, coach, founding year, squad/player information, and form chips.
- Favorites: registered-user favorite teams, next/recent matches, and notifications.
- Login/Register: validated forms with user-friendly messages.
- Admin: protected score updates, match event creation, management tables, and audit log viewer.

The UI is dark-first, mobile-first, and designed as a premium sports dashboard. It includes sticky navigation, live pulse indicators, responsive cards, styled tables, empty states, command search, theme toggle, CSV export, and mobile bottom navigation.

### 2.6 Areas of Particular Concern

Milestone 4 addresses the major project concerns:

- Authentication: login and registration issue signed bearer tokens with one-hour access token semantics and a refresh-token response field.
- Authorization: backend admin endpoints validate the authenticated role before allowing changes. Frontend admin screens are not the source of security.
- Password storage: `backend/pom.xml` includes jBCrypt, and `PasswordHasher.java` provides the BCrypt implementation expected for the dependency-managed version. The direct demo runner keeps a PBKDF2 fallback inside `GoalPulseServer` so the project still runs on lab machines without Maven.
- Auditing: admin create, update, and delete operations on match, match_event, team, player, and app_user data create audit log entries; denied admin attempts are also recorded.
- Data integrity: PostgreSQL and MySQL schemas define keys, foreign keys, checks, uniqueness, and delete behavior. JDBC examples use `PreparedStatement`.
- Live data: when `FOOTBALL_DATA_API_TOKEN` is set, GoalPulse loads real daily matches and team crests from football-data.org. If the API is unavailable, the app falls back to seeded demo data.
- Image reliability: football-data.org crest images are loaded through a backend proxy endpoint to prevent broken external hotlinks.

## 3 Requirements Updates

Implemented M4-level functional requirements:

- FR-01: registration and login with token-based authentication.
- FR-02: live match score display from seeded data or football-data.org.
- FR-03: match detail view with timeline, lineups, score, stats, venue, and referee.
- FR-04: computed league standings.
- FR-05: schedule/fixture display by date.
- FR-06: team profiles.
- FR-07: player profile cards when squad data is available.
- FR-08: search across teams, players, and matches.
- FR-09: favorite teams for registered users.
- FR-10: admin league/season visibility through protected admin endpoints.
- FR-11: admin match score/status updates and match event creation.
- FR-12: admin team, player, and user management endpoints and management tables.
- FR-13: CSV export for standings.
- FR-14: responsive design.
- FR-15: notifications for favorite-team activity.

## 4 User Documentation

### 4.1 Graphical User Interface Design

GoalPulse uses a premium dark sports-dashboard layout instead of a marketing-style landing page. The first screen immediately shows live match cards, current scores, live indicators, and matchday statistics. The global top navigation contains Live, Schedule, Standings, Teams, Players, Favorites, and Admin.

Main interface areas:

- Top bar: brand identity, navigation tabs, theme toggle, and session status.
- Match strip: quick score tiles for loaded matches.
- Workspace: account/search panel on the left and the active feature view on the right.
- Admin area: protected score/event forms, management tables, and audit log cards.

On tablets and phones, the layout collapses into one column and uses bottom navigation. Match cards, team cards, forms, and tables resize to avoid text overlap. Tables remain horizontally scrollable when needed.

Screenshots for M4 are stored in `docs/screenshots/`:

- `dashboard.png`
- `match-detail.png`
- `standings.png`
- `teams-players.png`
- `favorites.png`
- `admin-panel.png`
- `login-register.png`

### 4.2 User Manual

#### Guest User

1. Open `http://localhost:8080`, or the configured port.
2. Use Live to view today’s matches grouped by league.
3. Click Match Center to view match detail, timeline, lineups, and stats.
4. Open Schedule to filter fixtures by date.
5. Open Standings to view calculated rankings.
6. Open Teams or Players to browse profile information.
7. Use Command Search, or press `Ctrl+K`, to find teams, players, or matches.

#### Registered User

1. Log in with `mia@goalpulse.test` and `User123!`, or register a new account.
2. Open Teams and click the favorite button.
3. Open Favorites to view saved teams, next/recent matches, and notifications.
4. Use Export CSV in Standings to download standings data.

#### Admin User

1. Log in with `admin@goalpulse.test` and `Admin123!`.
2. Open Admin.
3. Update match score/status.
4. Add match events by choosing player, event type, minute, and detail.
5. Review management tables for teams and players.
6. Delete an entity only after confirming the destructive action.
7. Review audit logs after changes.

## 5 Installation, Configuration, and Acceptance Testing

### 5.1 Installation

Prerequisites:

- Java JDK 17 or newer. Tested locally with Java 21.
- PowerShell on Windows.
- Optional: PostgreSQL 14+ or MySQL 8+ if running database scripts.
- Optional: Maven if running the dependency-managed BCrypt/JDBC-driver version.
- Optional: Node/npm if building the React/TypeScript frontend in `frontend/src`.

Run the one-command demo:

```powershell
cd <path-to-repo>
.\scripts\run.ps1
```

If port `8080` is busy:

```powershell
$env:PORT="8081"
.\scripts\run.ps1
```

Database setup:

```text
database/postgresql/schema.sql
database/postgresql/seed.sql
database/mysql/schema.sql
database/mysql/seed.sql
```

Run the schema file first, then the seed file for the selected database engine.

### 5.2 Configuration

Environment variables:

- `PORT`: optional backend port. Default is `8080`.
- `GOALPULSE_TOKEN_SECRET`: secret used to sign bearer tokens.
- `FOOTBALL_DATA_API_TOKEN`: optional football-data.org API token for real online match data.
- `GOALPULSE_DB_URL`, `GOALPULSE_DB_USER`, `GOALPULSE_DB_PASSWORD`: JDBC connection variables used by database connectivity classes.

Run with live data:

```powershell
$env:FOOTBALL_DATA_API_TOKEN="your_token_here"
$env:PORT="8081"
.\scripts\run.ps1
```

The runnable demo uses an in-memory repository so no database credentials are required during presentation. The database connectivity layer is present through `DatabaseConfig.java` and `JdbcGoalPulseDao.java`, which demonstrate JDBC `Connection`, `PreparedStatement`, result-set mapping, updates, and audit inserts against the provided PostgreSQL/MySQL schemas.

### 5.3 Acceptance Testing

Acceptance testing for M4 focuses on end-to-end behavior:

- Public match, standings, team, player, schedule, and search pages load without login.
- Real API data loads when `FOOTBALL_DATA_API_TOKEN` is set.
- Registered users can log in, add favorites, view notifications, and export CSV.
- Admin users can update match score/status, add events, manage teams/players/users/matches, and view audit logs.
- Non-admin users are rejected by admin endpoints.
- Audit log records accepted admin changes and denied admin attempts.
- Interface remains usable on desktop and mobile widths.

Detailed test cases are listed in `docs/acceptance_tests.md`.

## 6 Final Remarks and Conclusion

Milestone 4 delivers a complete runnable version of GoalPulse with database deliverables, backend APIs, optional online sports data, React/TypeScript frontend source structure, and a responsive dark sports-dashboard frontend. The system demonstrates the layered architecture described in earlier milestones: presentation layer, business/API layer, database connectivity layer, and database design.

The strongest completed areas are match tracking, standings calculation, authentication, authorization, favorites, notifications, admin operations, audit logging, online data fallback, and M4 user documentation.

> **Note:** This file is the M4 milestone documentation, kept for historical reference. The authoritative final-submission document is `docs/Final_Project_Documentation.md` (and the generated `docs/GoalPulse_Final_Documentation.docx`).
