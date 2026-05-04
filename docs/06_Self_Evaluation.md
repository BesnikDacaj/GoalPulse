# GoalPulse — Self-Evaluation (Deliverable 6, Part A)

**Course:** ISTE-330
**Submission date:** 2026-05-09

This document contains every team member's self-assessment of what they personally contributed to GoalPulse, in **man-days**, plus a short reflection on what went well and what would be done differently.

> Conventions: 1 man-day = ~8 hours of focused work. Tasks shared between two people are split (e.g., 1.5 + 1.5 = 3.0 total). Numbers are rounded to the nearest 0.5.

---

## A. Mateo Josipović — Project Coordinator, Backend & DB Lead

| Task | Man-days |
| --- | --- |
| Repository scaffolding (`backend/`, `frontend/`, `database/`, `scripts/`, `docs/`) | 0.5 |
| PostgreSQL schema + seed (`database/postgresql/*.sql`), normalization to 3NF | 1.5 |
| MySQL schema + seed including reserved-word `match` handling | 1.0 |
| `GoalPulseServer.java` HTTP server, routing, REST envelope, JSON encoder | 3.5 |
| Token signing/verification (HMAC-SHA256), bearer-token middleware, RBAC checks | 1.5 |
| Audit log writes for all admin create/update/delete and denied attempts | 1.0 |
| `JdbcGoalPulseDao.java` + `DatabaseConfig.java` (Connection / PreparedStatement / ResultSet mapping) | 2.0 |
| `PasswordHasher.java` (BCrypt path) + PBKDF2 fallback inside the server | 0.5 |
| Standings calculation, schedule grouping, search endpoint | 1.0 |
| football-data.org integration + crest proxy + offline fallback | 1.5 |
| Final documentation: sections 1, 2.1–2.4, NFR, DB dictionary, references | 2.5 |
| Coordinator overhead: GitHub merges, milestone tracking, team meetings | 1.0 |
| **Total** | **17.5** |

**What went well.** The four-tier architecture stayed clean from M2 onward; we never had to refactor the data access layer because the DAO interface was agreed up front. Running the server with `javac` (no Spring, no Maven required) saved hours during lab demos.

**What I would do differently.** Start the React/TS structure as the only frontend on day one instead of shipping vanilla JS first; carrying both paths added documentation overhead in M4.

**Would I work with this team again?** Yes — see the peer review document for per-person reasoning.

---

## B. Petar Marinović — Frontend Lead

| Task | Man-days |
| --- | --- |
| Vanilla SPA: `frontend/index.html`, `app.js`, `styles.css` | 4.0 |
| React/TS source structure: pages, routes, context, API modules, DTO types | 2.5 |
| Dark-first dashboard styling, mobile bottom nav, theme toggle | 1.5 |
| Live indicators, skeleton loading, empty states, command search (`Ctrl+K`) | 1.5 |
| Match detail timeline, lineups, hero score block | 1.0 |
| Admin panel UI: score update form, event creation form, management tables | 1.5 |
| Favorites + notifications panel | 0.5 |
| CSV export from standings | 0.25 |
| Final documentation: section 2.5 (presentation layer), wireframes | 1.0 |
| Real screenshot capture pass for `docs/screenshots/` | 0.25 |
| **Total** | **14.0** |

**What went well.** Dark theme + responsive bottom navigation makes the app feel finished; visitors immediately understand it is a sports app.

**What I would do differently.** Pick one frontend (React) earlier; double-maintaining vanilla and React felt redundant.

**Would I work with this team again?** Yes.

---

## C. Ana Kovačić — Database Modeling & SQL

| Task | Man-days |
| --- | --- |
| Initial ERD draft and review with team | 1.0 |
| Final ERD (PNG export, `docs/diagrams/erd.md`) | 0.5 |
| Normalization to 3NF (review of every table, removed transitive dependencies) | 1.0 |
| Foreign-key strategy and ON DELETE policies (CASCADE / RESTRICT / SET NULL) | 0.5 |
| CHECK constraints (status, score ≥ 0, distinct teams, minute range, founded year) | 0.5 |
| Seed data realism (real teams, real coaches, real founding years) | 1.0 |
| Database dictionary (`docs/db_dictionary.md`, all 14 tables) | 2.0 |
| MySQL parity (reserved word `match`, MySQL CHECK syntax differences) | 0.5 |
| Final documentation: section 2.2 (database layer) | 1.0 |
| **Total** | **8.0** |

**What went well.** Building both PostgreSQL and MySQL scripts in lockstep caught two portability issues early (reserved word `match`, `BIGSERIAL` vs `BIGINT AUTO_INCREMENT`).

**What I would do differently.** Add a few materialized views or computed columns for standings; right now standings are computed by the business layer and the `standing` table is partially redundant.

**Would I work with this team again?** Yes.

---

## D. Luka Horvat — QA, Documentation, Live-Data Integration

| Task | Man-days |
| --- | --- |
| Acceptance test plan (18 cases, `docs/acceptance_tests.md`) | 1.0 |
| Acceptance test execution + Pass/Fail evidence | 1.5 |
| User stories file (`docs/user_stories.md`) | 0.5 |
| Demo script (`docs/demo_script.md`) | 0.5 |
| User manual section in final documentation | 1.0 |
| Football-data.org token testing + rate-limit/fallback verification | 1.0 |
| Cross-browser smoke test (Chrome, Edge, Firefox, mobile widths) | 0.5 |
| Presentation deck content + speaker allocation | 1.5 |
| Peer review coordination | 0.5 |
| **Total** | **8.0** |

**What went well.** Catching the offline path early meant we never had a "no internet, can't demo" moment.

**What I would do differently.** Capture screenshots from the running app earlier; the placeholder PNGs in M4 had to be replaced last-minute.

**Would I work with this team again?** Yes.

---

## Aggregate effort

| Member | Man-days |
| --- | --- |
| Mateo Josipović | 17.5 |
| Petar Marinović | 14.0 |
| Ana Kovačić | 8.0 |
| Luka Horvat | 8.0 |
| **Project total** | **47.5 man-days (≈ 380 hours)** |
