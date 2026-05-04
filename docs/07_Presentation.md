# GoalPulse — Presentation (Deliverable 7)

**Course:** ISTE-330 — Database Connectivity and Access
**Target duration:** 12–14 minutes (presentation) + 3–5 minutes (Q&A)
**All four members must be present.** Speaking allocation is shown on every slide.

This file is the authoritative source for `docs/07_Presentation.pptx`. To build the deck:
1. Open PowerPoint, choose a dark template.
2. Create one slide per `## Slide N` heading in this file.
3. Copy the title, body bullets, and speaker notes verbatim.
4. Insert the diagram referenced in each slide (`docs/diagrams/*.md` rendered as PNG, or screenshots from `docs/screenshots/`).

---

## Slide 1 — Title

**GoalPulse**
Live European Soccer Tracking and League Management

ISTE-330 Final Project — Spring 2026
Mateo Josipović (coordinator), Petar Marinović, Ana Kovačić, Luka Horvat
Instructor: dr. sc. Branko Mihaljević

*Speaker:* Mateo (45 s)
*Notes:* Introduce the team, name the coordinator, state the project type ("our own subject — sports/league tracker").

---

## Slide 2 — Problem and Why It Matters

- Casual fans want **live scores + league standings + per-team feeds** in one place.
- Most free apps hide their data layer; impossible to demo end-to-end.
- We needed a project where **all four layers** are ours: presentation, business, data access, database.
- Domain has clean entities (match, team, player, event) — perfect for a 3NF schema.

*Speaker:* Mateo (60 s)

---

## Slide 3 — Demo Agenda

1. Public dashboard (live scores, standings)
2. Match detail view
3. Login as registered user → favorite a team → notifications
4. Login as admin → update a score → audit log
5. Switch to JDBC-backed mode (real PostgreSQL)
6. Show the football-data.org live data path

*Speaker:* Luka (30 s)

---

## Slide 4 — Layered Architecture

[Insert `docs/diagrams/architecture.png`]

```
[ Browser SPA ] ─HTTP/JSON─▶ [ Java REST API ] ─JDBC─▶ [ DAO ] ─▶ [ PostgreSQL | MySQL ]
                                    │
                                    └──▶ football-data.org (optional)
```

- Presentation: HTML/CSS/JS SPA + React/TS source structure
- Business: Java 21 + `com.sun.net.httpserver` (no Spring)
- Data Access: JDBC + `PreparedStatement` + DAO pattern
- Database: PostgreSQL **and** MySQL parallel scripts

*Speaker:* Mateo (90 s)

---

## Slide 5 — Why this stack (and not Spring / not an ORM)

| Layer | Choice | Why |
| --- | --- | --- |
| Business | `com.sun.net.httpserver` | Runs on any JDK 17+ lab machine, zero config |
| Data Access | JDBC + DAO | Course-correct, transparent SQL, prevents injection |
| Database | PG **and** MySQL | Demonstrates portability and reserved-word handling |
| Auth | HMAC bearer + BCrypt | Course-standard JWT-style flow |

*Speaker:* Petar (60 s)

---

## Slide 6 — ER Diagram

[Insert `docs/diagrams/erd.png`]

14 tables, 3NF, foreign keys with explicit ON DELETE policies (CASCADE / RESTRICT / SET NULL).

*Speaker:* Ana (75 s)
*Notes:* Walk through `match` → `match_event`, `season` → `standing`, `app_user` → `user_favorite`. Mention `match` is a MySQL reserved word, hence backticks.

---

## Slide 7 — Database Highlights

- All admin writes go through `PreparedStatement` (defense-in-depth vs SQL injection).
- `audit_log` writes happen on every admin create/update/delete **and** on denied attempts.
- `CHECK` constraints prevent impossible state (negative scores, same home/away team, minute outside 0–130).
- `ON DELETE CASCADE` on weak-entity FKs (events of a deleted match are removed).
- `ON DELETE RESTRICT` on team/league FKs prevents accidental data loss.

*Speaker:* Ana (45 s)

---

## Slide 8 — JDBC DAO (Deep Dive)

```java
try (PreparedStatement ps = con.prepareStatement(
        "SELECT m.match_id, ht.short_name, at.short_name, m.home_score, m.away_score " +
        "FROM match m " +
        "JOIN team ht ON ht.team_id = m.home_team_id " +
        "JOIN team at ON at.team_id = m.away_team_id " +
        "WHERE m.match_date::date = ?")) {
    ps.setDate(1, Date.valueOf(date));
    try (ResultSet rs = ps.executeQuery()) { ... }
}
```

- `JdbcGoalPulseDao` encapsulates every SQL statement.
- Business code never builds SQL strings.
- Toggle DB mode with `GOALPULSE_USE_DB=true`.

*Speaker:* Mateo (75 s)

---

## Slide 9 — Authentication and RBAC

- Registration: BCrypt hash via `PasswordHasher.java` (jBCrypt).
- Login: server signs an HMAC-SHA256 bearer token; refresh token returned in payload.
- Each admin endpoint checks `role == 'admin'` before executing.
- 403 path also writes to `audit_log` (denied attempt is auditable evidence).

*Speaker:* Mateo (45 s)

---

## Slide 10 — Live Data Integration

- Optional: `FOOTBALL_DATA_API_TOKEN` toggles real fixture ingest.
- Failure modes covered: missing token, expired token, rate-limit, network outage.
- Each failure mode falls back silently to seeded demo data — the demo never breaks.
- Crests proxied through `/api/v1/assets/crest` to avoid broken hotlinks.

*Speaker:* Luka (45 s)

---

## Slide 11 — Frontend (Presentation Layer)

[Insert `docs/screenshots/dashboard.png`]

- Dark-first sports dashboard (SofaScore / FotMob feel).
- Live indicators, sticky nav, command search (`Ctrl+K`), theme toggle.
- Mobile bottom navigation under 560 px.
- React/TS source structure documented in `frontend/src/` for code review.

*Speaker:* Petar (60 s)

---

## Slide 12 — Admin Panel and Audit

[Insert `docs/screenshots/admin-panel.png`]

- Score/status updater, event creator (player + minute + type + detail).
- Management tables for teams, players, users.
- Audit log card lists actor, action, table, record, old/new value, timestamp.
- All deletes confirm before executing.

*Speaker:* Petar (45 s)

---

## Slide 13 — Challenges and Lessons

| Challenge | What we did |
| --- | --- |
| MySQL reserved word `match` | Backticked everywhere; documented in DB dictionary |
| Lab machine without Maven | Direct `javac` script; PBKDF2 fallback inside server |
| Rate-limited external API | Retry with exponential backoff + offline fallback |
| Two frontends in parallel | Documented as design-time + runtime; chose vanilla for demo |

*Speaker:* Luka (75 s)

---

## Slide 14 — What We'd Do Differently

- Pick React/TS as the only frontend at M2 instead of carrying both.
- Add materialized views for standings instead of recomputing in business code.
- Add a hosted CI pipeline (GitHub Actions) earlier.
- Move auth secret loading from env-vars only to a small `config.properties` reader.

*Speaker:* Ana (45 s)

---

## Slide 15 — Q&A

Prepared answers (Q&A prep sheet — `docs/qa_prep.md`):

- *Why no Spring?* — JDK-only deploy, faster cold start, no framework magic to explain.
- *Why two SQL dialects?* — Portability requirement; reserved-word demonstration.
- *How is SQL injection prevented?* — `PreparedStatement` everywhere, no string concat.
- *How are passwords stored?* — BCrypt (jBCrypt) on the Maven path, PBKDF2 fallback.
- *What if football-data.org is down?* — Silent fallback to seeded data, demo unaffected.
- *How is the standings table calculated?* — Server iterates finished matches and increments W/D/L, GF, GA, GD, Pts (3 win, 1 draw).

*Speaker:* All four; Mateo opens, others answer in their domain.

---

## Speaking Allocation Summary

| Member | Slides | Approx. time |
| --- | --- | --- |
| Mateo | 1, 2, 4, 8, 9 | ~5 min |
| Petar | 5, 11, 12 | ~3 min |
| Ana | 6, 7, 14 | ~2.5 min |
| Luka | 3, 10, 13 | ~2.5 min |

## Backups

- Recorded screen capture of full demo (`docs/demo_backup.mp4`) in case the laptop fails.
- Printed copy of this outline for each presenter.
- Local Postgres + MySQL pre-loaded so we don't need an internet connection.
