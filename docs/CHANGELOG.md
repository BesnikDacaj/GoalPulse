# GoalPulse — CHANGELOG

Milestone-by-milestone progress for the ISTE-330 project.

## v1.0-final — 2026-05-09 (Final submission)

- Added Deliverable 0 (project proposal), Deliverable 6 (self-evaluation + peer reviews), Deliverable 7 (presentation outline), and the live-demo script.
- Added all UML diagrams (use case, class, component, two activity diagrams) and the layered-architecture diagram.
- Added pre-implementation UI wireframes for the five primary screens.
- Added full database dictionary covering all 14 tables, plus relationship and validation summary.
- Rewrote the final documentation to include sections 2.1–2.4 (architecture, DB layer, data access layer, business layer), detailed functional requirements, non-functional requirements, technology argumentation per layer, software-design-principles section, and a full references list.
- Replaced the M4 conclusion with a true final-tone conclusion (lessons learned, completed scope, future work as ideas).
- Re-captured screenshots of the running application.
- Recorded acceptance-test execution results: 18/18 pass on 2026-05-04.
- Added seed inserts for `app_user`, `user_favorite`, `notification` in both PostgreSQL and MySQL seed scripts.
- Fixed the hard-coded developer path in the M4 installation snippet.
- Added cross-platform `INSTALL.md`, `docs/team.md`, `docs/CHANGELOG.md`, `LICENSE` (MIT).

## M4 — 2026-04-25

- Added React/TypeScript source structure under `frontend/src/` (pages, routes, context, API modules, DTO types).
- Embedded screenshots in the M4 documentation.
- Added jBCrypt dependency to `backend/pom.xml` and the BCrypt path in `PasswordHasher.java`; PBKDF2 fallback retained inline.
- Implemented the football-data.org integration with offline fallback and the crest proxy endpoint.
- Implemented admin management tables (teams, players, users) and the audit log card.
- Implemented mobile bottom navigation, command search (`Ctrl+K`), CSV export, dark/light toggle.
- Documented M4 acceptance test plan (18 cases).

## M3 — 2026-04-11

- Implemented `JdbcGoalPulseDao` and `DatabaseConfig` (JDBC `Connection` + `PreparedStatement` + `ResultSet` mapping).
- Added authentication: registration, login, signed bearer tokens (HMAC-SHA256), refresh token field.
- Added RBAC checks for every admin endpoint; denied attempts are audited.
- Implemented the `audit_log` writer for create/update/delete and denied paths.
- Implemented standings recomputation in the business layer.
- Implemented favorites and notifications endpoints.

## M2 — 2026-03-21

- Finalized PostgreSQL and MySQL schemas (14 tables) with full FK / CHECK / UNIQUE constraints.
- Normalized all entity tables to 3NF; documented the deliberate denormalization of `standing` as a recomputable snapshot.
- Wrote seed scripts for both engines with realistic team / coach / player data.
- Documented engine-specific differences (the MySQL reserved word `match` requires backticks).

## M1 — 2026-02-28

- Drafted the use-case, class, and component diagrams.
- Drafted the ER diagram and the layered architecture diagram.
- Wrote functional requirements FR-01…FR-15 (one-line form).
- Wrote initial user stories.
- Set up the repository skeleton (`backend/`, `frontend/`, `database/`, `scripts/`, `docs/`).

## M0 — 2026-02-14

- Confirmed team and coordinator (Mateo Josipović).
- Chose project topic: a sports / league tracker (custom subject under "your own").
- Selected tech stack: Java 21 + JDBC + PostgreSQL + MySQL + browser SPA.
- Approved by instructor.
