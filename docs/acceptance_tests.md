# GoalPulse Acceptance Tests - M4

| ID | Scenario | Steps | Expected Result |
| --- | --- | --- | --- |
| AT-01 | Public dashboard | Open `http://localhost:8080` | Match strip and match cards load without login. |
| AT-02 | Match detail | Click Details on a match | Timeline, venue, referee, and lineup display. |
| AT-03 | Standings | Open Standings tab | Teams are ranked by points, goal difference, and goals scored. |
| AT-04 | Search | Search for `Liverpool` | Matching team/match result appears. |
| AT-05 | Registered login | Log in as `mia@goalpulse.test` / `User123!` | Session label changes to registered user. |
| AT-06 | Favorite team | Click Favorite on a team | Favorite appears in Favorites tab. |
| AT-07 | CSV export | Logged-in user clicks Export CSV | Browser downloads `goalpulse-standings.csv`. |
| AT-08 | Admin login | Log in as `admin@goalpulse.test` / `Admin123!` | Session label shows admin role. |
| AT-09 | Admin score update | Admin updates score/status in Admin tab | Score changes on dashboard and audit log records update. |
| AT-10 | Admin event creation | Admin adds event in Admin tab | Event appears in match detail and audit log records insert. |
| AT-11 | RBAC denial | Registered user attempts admin action | API returns 403 and records denied attempt. |
| AT-12 | Responsive layout | Resize below 560px width | Navigation, panels, cards, and forms remain readable without overlap. |
| AT-13 | Online data load | Set `FOOTBALL_DATA_API_TOKEN` and start app | Server logs loaded matches from football-data.org and UI shows real teams/fixtures. |
| AT-14 | Crest proxy | Open page with online data | Team crest images load through `/api/v1/assets/crest` or fall back to short team code. |
| AT-15 | Admin team delete | Admin confirms delete in management table | Team is removed and audit log records delete action. |
| AT-16 | Admin users endpoint | Admin opens `/api/v1/admin/users` with bearer token | JSON response contains users; non-admin request is rejected. |
| AT-17 | React source structure | Inspect `frontend/src` | React/TypeScript pages, routes, API modules, context, and DTO types are present. |
| AT-18 | BCrypt dependency path | Inspect `backend/pom.xml` and `PasswordHasher.java` | jBCrypt dependency and BCrypt hasher are present for Maven/dependency-managed backend. |
