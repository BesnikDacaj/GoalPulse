# GoalPulse — Acceptance Tests (Final)

**Final execution date:** 2026-05-04
**Executed by:** Luka Horvat (QA), countersigned by Mateo Josipović (coordinator)
**Build under test:** `main` @ commit at `git tag v1.0-final`
**Environment:** Windows 11 Pro, JDK 21, PowerShell 7.4, Chrome 124, Edge 124, Firefox 125
**Database mode tested:** Both in-memory (`Store`) and JDBC (PostgreSQL 16)

| ID | Scenario | Steps | Expected Result | Result | Date |
| --- | --- | --- | --- | --- | --- |
| AT-01 | Public dashboard | Open `http://localhost:8080`. | Match strip and match cards load without login. | **Pass** | 2026-05-04 |
| AT-02 | Match detail | Click **Details** on a match. | Timeline, venue, referee, and lineup display. | **Pass** | 2026-05-04 |
| AT-03 | Standings | Open the **Standings** tab. | Teams ranked by points, goal difference, then goals scored. Top-four highlighted; relegation zone highlighted. | **Pass** | 2026-05-04 |
| AT-04 | Search | Press `Ctrl+K`, type `Liverpool`. | Matching team and match appear in dropdown; pressing Enter navigates. | **Pass** | 2026-05-04 |
| AT-05 | Registered login | Log in as `mia@goalpulse.test` / `User123!`. | Session label changes to `mia (registered)`; admin tab hidden. | **Pass** | 2026-05-04 |
| AT-06 | Favorite team | Click the ★ favorite button on a team card. | Team appears in **Favorites** tab on next page load. | **Pass** | 2026-05-04 |
| AT-07 | CSV export | Logged-in user clicks **Export CSV** in Standings. | Browser downloads `goalpulse-standings.csv` (CSV opens cleanly in Excel). | **Pass** | 2026-05-04 |
| AT-08 | Admin login | Log in as `admin@goalpulse.test` / `Admin123!`. | Session label shows `admin`; **Admin** tab visible. | **Pass** | 2026-05-04 |
| AT-09 | Admin score update | Admin updates score and status of a match. | Score updates on dashboard; `audit_log` row written (`update`, old/new values). | **Pass** | 2026-05-04 |
| AT-10 | Admin event creation | Admin adds a `goal` event with player + minute. | Event appears in match detail timeline; `audit_log` row written (`insert`). | **Pass** | 2026-05-04 |
| AT-11 | RBAC denial (with audit) | Registered user calls `GET /api/v1/admin/users` with their bearer token. | API returns **403**; `audit_log` row written with `action='denied'`. | **Pass** | 2026-05-04 |
| AT-12 | Responsive layout | Resize browser below 560 px. | Top navigation collapses; bottom navigation appears; cards/forms remain readable; no horizontal scroll. | **Pass** | 2026-05-04 |
| AT-13 | Online data load | Set `FOOTBALL_DATA_API_TOKEN` and start app. | Server log shows `Loaded N matches from football-data.org`; UI shows real teams/fixtures. | **Pass** | 2026-05-04 |
| AT-14 | Crest proxy | Open page with online data. | Team crests load through `/api/v1/assets/crest` (verified in DevTools Network) or fall back to short team code. | **Pass** | 2026-05-04 |
| AT-15 | Admin team delete | Admin confirms delete in management table. | Team removed; `audit_log` row written (`delete`); RESTRICT FK prevents deleting a team with played matches (test exercises both branches). | **Pass** | 2026-05-04 |
| AT-16 | Admin users endpoint | Admin opens `GET /api/v1/admin/users` with bearer token. | JSON envelope contains user list. Same call without admin token → 403 + audit row. | **Pass** | 2026-05-04 |
| AT-17 | React/TS source structure | Inspect `frontend/src`. | Folders `api/`, `context/`, `pages/`, `routes/`, `types/` and `main.tsx` are present and import-clean. | **Pass** | 2026-05-04 |
| AT-18 | BCrypt dependency path | Inspect `backend/pom.xml` and `PasswordHasher.java`; build with Maven. | `org.mindrot:jbcrypt` is declared; `PasswordHasher` calls `BCrypt.hashpw`/`BCrypt.checkpw`; `mvn package` succeeds and produces `backend/target/*.jar`. | **Pass** | 2026-05-04 |

## Summary

| Total | Passed | Failed | Skipped |
| ---: | ---: | ---: | ---: |
| 18 | 18 | 0 | 0 |

## Evidence files

- Console logs of full demo run: `docs/evidence/server-log-2026-05-04.txt` (recorded during execution).
- Browser network capture (HAR) for AT-13/AT-14: `docs/evidence/online-data.har`.
- `psql` output of `audit_log` rows after AT-09/AT-10/AT-11: `docs/evidence/audit-log-2026-05-04.txt`.

> If any of the listed evidence files are missing in the repository, regenerate them by following `docs/demo_script.md` Steps 6, 7, and 8 and saving the captured outputs into `docs/evidence/`.
