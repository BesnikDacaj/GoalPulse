# GoalPulse — User Stories

Standard format: **As a [role], I want [feature], so that [value].**

Each story carries an acceptance condition that maps to one or more functional requirements (FR-XX) and acceptance tests (AT-XX) — see `docs/acceptance_tests.md`.

---

## Guest

### US-G1
**As a Guest, I want to see today's live matches the moment I open the site, so that I can check scores without signing up.**
- Acceptance: dashboard renders within 2 s; `●LIVE` badge on every in-progress match. → FR-02, AT-01.

### US-G2
**As a Guest, I want to open a match detail page, so that I can read the event timeline (goals, cards, substitutions).**
- Acceptance: timeline shows minute, player, event type, optional detail. → FR-03, AT-02.

### US-G3
**As a Guest, I want to view league standings, so that I know which team is leading the table.**
- Acceptance: rows sorted by Pts → GD → GF; top-four highlighted; relegation zone highlighted. → FR-04, AT-03.

### US-G4
**As a Guest, I want a fast keyboard-driven search, so that I can jump to any team, player, or match.**
- Acceptance: `Ctrl+K` opens search; results appear < 300 ms; ↵ navigates. → FR-08, AT-04.

---

## Registered User

### US-R1
**As a Registered user, I want to favorite a team, so that I can find their next match without searching every time.**
- Acceptance: `★` button on team card persists; appears in Favorites tab. → FR-09, AT-06.

### US-R2
**As a Registered user, I want to receive notifications when my favorite team's match changes status, so that I don't miss kickoff or final score.**
- Acceptance: `notification` row created on score update for any match where one of the teams is favorited by the user. → FR-15.

### US-R3
**As a Registered user, I want to export the standings as CSV, so that I can paste them into a spreadsheet or my own analysis.**
- Acceptance: clicking Export CSV downloads `goalpulse-standings.csv`. → FR-13, AT-07.

### US-R4
**As a Registered user, I want my password to be stored safely, so that even if the database leaks, my password isn't usable.**
- Acceptance: `password_hash` column contains a BCrypt (`$2a$…`) hash, never plaintext. → NFR-Security, AT-18.

---

## Admin

### US-A1
**As an Admin, I want to update a match's score and status from the admin panel, so that I can correct mistakes or push live updates.**
- Acceptance: change persists; UI reflects new score across all open tabs after refresh. → FR-11, AT-09.

### US-A2
**As an Admin, I want to add match events (goals, cards, subs) with the player and minute, so that the timeline is complete.**
- Acceptance: new event appears in match detail timeline; `audit_log` row recorded. → FR-11, AT-10.

### US-A3
**As an Admin, I want every change I make to be logged, so that mistakes are traceable to a person and a time.**
- Acceptance: each create/update/delete writes to `audit_log` with actor, timestamp, old and new value. → FR-12, AT-09, AT-10.

### US-A4
**As an Admin, I want denied admin attempts to be logged too, so that I can detect probing or misuse.**
- Acceptance: a non-admin call to `/api/v1/admin/*` returns 403 **and** writes `action='denied'` to `audit_log`. → FR-12, AT-11.

### US-A5
**As an Admin, I want to manage teams, players, and users from one panel, so that I don't have to touch the database directly.**
- Acceptance: tables for each entity expose create/edit/delete, with confirmation on delete. → FR-12, AT-15, AT-16.

---

## Cross-cutting (system-level)

### US-S1
**As a user on a phone, I want the layout to remain readable below 560 px, so that I can use GoalPulse from a bus.**
- Acceptance: bottom navigation appears; cards stack into one column; no horizontal overflow. → NFR-Usability, AT-12.

### US-S2
**As an operator, I want the app to keep working when football-data.org is down, so that the demo never fails.**
- Acceptance: server logs the failure once and serves seeded data; UI shows no error. → NFR-Reliability, AT-13.

### US-S3
**As an operator, I want to switch the data backend to a real database without code changes, so that we can demonstrate the full data access layer.**
- Acceptance: setting `GOALPULSE_USE_DB=true` and JDBC URL env-vars makes the server use `JdbcGoalPulseDao`. → NFR-Portability.
