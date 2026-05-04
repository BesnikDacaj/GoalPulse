# GoalPulse — Project Proposal (Deliverable 0)

**Course:** ISTE-330 — Database Connectivity and Access
**Institution:** RIT Croatia
**Semester:** Spring 2026
**Instructor:** dr. sc. Branko Mihaljević
**Submission date:** 2026-02-14
**Document version:** 1.0

---

## 1. Project Title

**GoalPulse — Live European Soccer Tracking and League Management Platform**

## 2. Team

| Member | Role | Coordinator |
| --- | --- | --- |
| Mateo Josipović | Project Coordinator, Backend & DB Lead | YES |
| Petar Marinović | Frontend Lead (SPA + React/TS structure) | NO |
| Ana Kovačić | Database Modeling & SQL (PostgreSQL/MySQL) | NO |
| Luka Horvat | QA, Documentation, Live-Data Integration | NO |

The project coordinator is responsible for milestone scheduling, weekly stand-ups, GitHub merges, and presentation logistics.

## 3. Idea Selection

The PDF allows teams to choose either one of the proposed ideas (1–6) or to bring their own subject. We chose **our own subject**: a sports / league tracking platform.

**Justification.** All four members follow European football, the domain has a well-known, easy-to-validate dataset (matches, teams, players, events), and a public REST API (football-data.org) is freely available so we can demonstrate live integration without paid services.

## 4. Problem Statement

Casual fans need a single place to (1) see live scores and event timelines, (2) browse league standings recomputed from match results, (3) search teams and players, (4) follow a personal list of favorite teams with notifications, and (5) let an authorized administrator update match data with a full audit trail. Existing free apps either lack admin/audit capability, lack a relational schema we can present, or hide their data layer behind proprietary services. GoalPulse implements all four layers (presentation, business, data access, database) ourselves so the project is fully demonstrable end-to-end and meets every ISTE-330 rubric line.

## 5. Planned Functionalities

1. Live dashboard with score cards, league grouping, and live indicator.
2. Match detail view: timeline of events, lineups, stats, venue, referee.
3. Schedule view, filtered by date.
4. League standings calculated from match results (W/D/L, GF, GA, GD, Pts).
5. Team profile pages: stadium, city, coach, founding year, squad.
6. Player profile cards: position, nationality, DOB, shirt number, form.
7. Search across teams, players and matches (with `Ctrl+K` shortcut).
8. User registration/login with BCrypt password storage and signed bearer tokens.
9. Favorite teams (registered users only) with per-favorite notifications.
10. Admin operations: update match score/status, add events, manage teams/players/users, view audit log.
11. CSV export of standings.
12. Optional live data ingestion from football-data.org with graceful fallback to seeded demo data.
13. Responsive dark-first UI with mobile bottom navigation and theme toggle.
14. Audit log of admin create/update/delete and denied admin attempts.
15. RBAC enforcement at the API layer (guest / registered / admin).

## 6. Tech Stack (Planned)

| Layer | Technology | Why |
| --- | --- | --- |
| Presentation | HTML/CSS/JS SPA + React 18 / TypeScript / Vite source structure | One-command demo runs vanilla; React/TS shows scalable component design |
| Business | Java 21 + `com.sun.net.httpserver` (no Spring) | Zero-config REST API runs on any lab machine with JDK 17+ |
| Data Access | JDBC + `PreparedStatement` + DAO pattern (`JdbcGoalPulseDao`) | Course-correct, prevents SQL injection, no ORM magic |
| Database | PostgreSQL 14+ AND MySQL 8+ (parallel scripts) | Demonstrates portability; satisfies "more than one DBMS" expectation |
| Auth | Signed bearer tokens (HMAC-SHA256), BCrypt (jBCrypt) | Course-standard JWT-style auth; BCrypt for password storage |
| External | football-data.org v4 REST API | Free token; falls back to seeds if offline |
| Build | Maven (`pom.xml`) for the dependency-managed path | Pulls jBCrypt + JDBC drivers when running with the real database |

## 7. Architecture (Planned)

Four-tier layered architecture:

```
[ Browser SPA ] ──HTTP/JSON──▶ [ Java REST API ] ──JDBC──▶ [ DAO layer ] ──▶ [ PostgreSQL | MySQL ]
                                       │
                                       └── football-data.org (optional)
```

## 8. Milestones

| # | Date | Deliverable |
| --- | --- | --- |
| M0 | 2026-02-14 | Project proposal + team confirmed (this document) |
| M1 | 2026-02-28 | Use cases, requirements, ERD draft, layered architecture |
| M2 | 2026-03-21 | PostgreSQL + MySQL schemas, seed data, normalization to 3NF |
| M3 | 2026-04-11 | JDBC DAO, REST API, authentication, RBAC, audit log |
| M4 | 2026-04-25 | Frontend, admin panel, online data, full M4 documentation |
| M5 | 2026-05-09 | Final documentation, peer reviews, presentation, demo |

## 9. Risks and Mitigations

| Risk | Mitigation |
| --- | --- |
| football-data.org rate-limit / outage during demo | Automatic fallback to seeded data; demo works offline |
| Lab machine without Maven | Direct `javac` script (`scripts/run.ps1`) compiles without Maven |
| MySQL reserved word `match` | Backticks in MySQL schema; tested |
| Lost work / merge conflicts | GitHub feature-branch workflow, weekly integration day |

## 10. Acceptance Criteria for Final Submission

- All 7 PDF deliverables present and named clearly.
- Layered architecture diagram, ERD, UML diagrams, wireframes embedded in documentation.
- Acceptance test suite executed on the final build with Pass/Fail and date stamp.
- Application runs end-to-end in both in-memory mode and JDBC-backed mode.
- Presentation rehearsed in 10–15 minutes with all four members present.
