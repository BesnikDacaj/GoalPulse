# GoalPulse — Q&A Prep Sheet

Likely questions from the panel, with our prepared answers.

---

**Q1. Why no Spring?**
*Answer (Mateo).* Spring Boot would add ~30 MB on the classpath and 30–60 s to startup for capabilities we don't need at this scope (no autoconfig magic, no IoC container, no JPA). `com.sun.net.httpserver` lets the project run on any JDK 17+ lab machine with zero configuration, which is the priority for a graded demo.

**Q2. Why two SQL dialects?**
*Answer (Ana).* To prove portability and to demonstrate real-world dialect handling. We hit two genuine differences during development: `match` is a reserved word in MySQL (we backtick it), and CHECK constraints are written slightly differently. Maintaining both schemas in lockstep was about half a day of extra work but caught dialect bugs early.

**Q3. How is SQL injection prevented?**
*Answer (Mateo).* Every SQL statement that takes user input is a `PreparedStatement` with `?` parameter slots filled via `setInt`/`setString`/etc. There is no string concatenation of input into SQL anywhere in `JdbcGoalPulseDao.java`. The DAO interface forces callers to pass typed values, not strings.

**Q4. How are passwords stored?**
*Answer (Mateo).* BCrypt via jBCrypt (`PasswordHasher.java`), with a documented PBKDF2 fallback for environments without jBCrypt on the classpath. The `password_hash` column is a 60-char `$2a$…` BCrypt string. The `salt` column is kept as `NULL` when BCrypt is in use; it's used only by the PBKDF2 fallback.

**Q5. What happens if football-data.org is down?**
*Answer (Luka).* The server logs the failure once and serves seeded data instead. The UI shows no error. We tested four failure modes: missing token, expired token, 429 rate-limit, and full network outage. All four fall back silently.

**Q6. How does standings calculation work?**
*Answer (Ana).* The business layer iterates finished matches for the season and increments the affected teams' `played`, `won`/`drawn`/`lost`, `goals_for`, `goals_against`, and `points` (3 for a win, 1 for a draw). After each admin score update on a finished match, `recomputeStandings(seasonId)` runs inside the same transaction as the score update so the snapshot is consistent.

**Q7. Why is `standing` a separate table if it's derived?**
*Answer (Ana).* Read performance. The dashboard query for "show me the table" is one indexed `SELECT` instead of an aggregation across all finished matches. We accept a small denormalization for a large UX win, and we documented this explicitly in `docs/db_dictionary.md`.

**Q8. Why the dual frontend (vanilla + React)?**
*Answer (Petar).* The vanilla SPA is the runtime so the demo runs on any lab machine without `npm`. The React/TS source structure is provided as evidence of layered, scalable presentation design; it's the production evolution path. We documented the dual track explicitly in section 2.5 of the final documentation.

**Q9. How would you migrate from Mode 1 (in-memory) to Mode 2 (JDBC) in production?**
*Answer (Mateo).* The DAO interface is identical for both modes — `JdbcGoalPulseDao` implements the same contract as the in-memory `Store`. Switching is purely a matter of setting `GOALPULSE_USE_DB=true` and the DB URL/credentials. No business-layer code changes.

**Q10. Why log denied admin attempts?**
*Answer (Mateo).* Three reasons. (1) Detectability: a hostile registered user probing admin URLs leaves a paper trail. (2) Auditable evidence for the panel during the demo. (3) It's almost free — the same `audit_log` table already holds accepted writes.

**Q11. What's the threat model?**
*Answer (Mateo).* In-scope: account takeover (mitigated by BCrypt + signed token + token expiry), SQL injection (PreparedStatement), broken access control (server-side RBAC + audit), CSRF on token-bearing endpoints (mitigated by Authorization header instead of cookie). Out of scope for the course demo: TLS, rate limiting, log tamper-resistance — explicitly listed in the "Areas of Particular Concern" section.

**Q12. What did you under-deliver?**
*Answer (Petar/Luka).* We did not retire the vanilla SPA; both frontends ship. We chose to keep both because removing the vanilla path would have broken the one-command lab demo. We marked this as future work, not a gap.

**Q13. What did you over-deliver?**
*Answer (Mateo).* The crest proxy, the offline fallback for football-data.org, denied-attempt audit logging, and the React/TS source structure are all beyond the literal rubric. They demonstrate engineering taste.

**Q14. Could you walk me through one full-stack request, end to end?**
*Answer (Mateo).* For "admin updates score": browser sends `POST /api/v1/admin/match/1/score` with `Authorization: Bearer …`. Server verifies the token's HMAC, decodes role=admin, validates the body, opens a JDBC transaction, runs `UPDATE match SET … WHERE match_id=?` via `PreparedStatement`, inserts an `audit_log` row, runs `recomputeStandings(seasonId)`, dispatches notifications, commits, returns `{"success":true,…}`. The browser updates the UI on the response.

**Q15. What test coverage do you have?**
*Answer (Luka).* 18 acceptance tests exercised end-to-end on 2026-05-04, all passing. They cover public flows, registered flows, admin flows including RBAC denial, responsive layout, online data load, the BCrypt path, and the React/TS source structure. Evidence files are in `docs/evidence/`.
