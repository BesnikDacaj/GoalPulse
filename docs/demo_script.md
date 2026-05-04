# GoalPulse — Live Demo Script

**Target duration:** 5–6 minutes
**Presenter on keyboard:** Petar (frontend), Mateo backs up by switching modes if needed.
**Pre-flight:**

- Local PostgreSQL running with `database/postgresql/schema.sql` + `seed.sql` loaded.
- Two PowerShell windows already open:
  - **Window A** — in-memory mode (default): `.\scripts\run.ps1`
  - **Window B** — JDBC mode: env-vars set, ready to launch.
- Browser: Chrome with `http://localhost:8080` and `http://localhost:8081` already in tabs.
- Football-data.org token already exported in a third window (Window C) but **not** yet started.

> If demoing in a poor-network classroom, skip step 8 and stay in seeded mode — the project still earns full marks because the fallback is a graded feature.

---

## Step 0 — Pre-demo (do this 5 minutes before walking on stage)

1. In Window A: `.\scripts\run.ps1` → wait for `Server listening on http://localhost:8080`.
2. In Chrome → `http://localhost:8080` → confirm dashboard loads.
3. In Window C, set token but do **not** run yet:
   ```powershell
   $env:FOOTBALL_DATA_API_TOKEN="<token>"
   $env:PORT="8082"
   ```
4. Take a deep breath.

---

## Step 1 — Public dashboard (40 s)

- Open `http://localhost:8080` in Chrome.
- Point out: **brand bar**, **live indicator** on the live match, **score cards grouped by league**.
- Resize the browser window down to ~480 px → **mobile bottom navigation** appears.

*Talking point:* "All four members agreed the first impression had to be 'this is a sports app', not 'this is a school project'."

## Step 2 — Match detail view (30 s)

- Click **Details** on the live Manchester City vs Liverpool match.
- Highlight: **timeline of events**, **lineups**, **venue**, **referee**, **stats panel**.

## Step 3 — Standings + CSV export (30 s)

- Click **Standings** in the nav.
- Show the table sorted by Pts → GD → GF, with **top-four highlight** and **danger-zone highlight**.
- Click **Export CSV** → Chrome downloads `goalpulse-standings.csv` → open file briefly.

## Step 4 — Search (20 s)

- Press `Ctrl+K`, type `Liver` → autocomplete shows Liverpool team and match.
- Press Enter → navigate to Liverpool team profile.

## Step 5 — Register / Login as a regular user (40 s)

- Click **Login**.
- Enter `mia@goalpulse.test` / `User123!`.
- Show: session label changes to `mia (registered)`.
- Open **Teams** → click the **★ Favorite** on Liverpool.
- Open **Favorites** → confirm Liverpool is listed with next/recent matches and notifications.

## Step 6 — Login as Admin and update a score (60 s)

- Log out → log in as `admin@goalpulse.test` / `Admin123!`.
- Open **Admin** → **Update Score**.
- Pick the live match → change `home_score` from 2 to 3 → Save.
- Switch to **Live** tab → score on the dashboard updates to 3-1.
- Go back to Admin → **Audit Log** card → show the new row:
  `admin → update → match → record_id=1 → old:{home_score:2} → new:{home_score:3}`.

*Talking point:* "Audit writes happen on accepted **and** denied admin attempts — the denied path is graded too."

## Step 7 — Show RBAC denial (30 s)

- Open a second browser tab in **Incognito** mode → `http://localhost:8080`.
- Log in as `mia@goalpulse.test`.
- Open DevTools → Network tab.
- Use `curl` or DevTools Console:
  ```js
  fetch('/api/v1/admin/users', { headers: { Authorization: 'Bearer ' + localStorage.getItem('token') } })
    .then(r => r.status)
  ```
- Expect: `403`.
- Switch to admin window → audit log → the denied attempt was recorded.

## Step 8 — Switch to JDBC-backed mode against PostgreSQL (60 s)

- In Window A: stop the server (`Ctrl+C`).
- In Window B (already prepared):
  ```powershell
  $env:GOALPULSE_USE_DB="true"
  $env:GOALPULSE_DB_URL="jdbc:postgresql://localhost:5432/goalpulse"
  $env:GOALPULSE_DB_USER="goalpulse"
  $env:GOALPULSE_DB_PASSWORD="<password>"
  $env:PORT="8081"
  .\scripts\run.ps1
  ```
- Wait for `Server listening on http://localhost:8081 (JDBC mode)`.
- Browser tab → `http://localhost:8081` → confirm matches load (now from PostgreSQL via `JdbcGoalPulseDao`).
- In a `psql` window: `SELECT * FROM audit_log ORDER BY log_id DESC LIMIT 5;` to prove writes go to the database.

## Step 9 — Optional: live football-data.org integration (45 s, only if Wi-Fi works)

- In Window C: `.\scripts\run.ps1` → wait for `Loaded N matches from football-data.org` log line.
- Open `http://localhost:8082` → real fixtures from today's date appear with real crests.
- *Talking point:* "If the API rate-limits or fails mid-demo, fallback kicks in automatically — we'd never even notice on stage."

## Step 10 — Wrap (15 s)

- Switch back to slide deck for **Q&A**.

---

## What to do if something breaks

| Symptom | Fix |
| --- | --- |
| `Address already in use :8080` | `$env:PORT="8083"; .\scripts\run.ps1` |
| Browser shows empty page | Hard reload with `Ctrl+Shift+R`; check Window A logs |
| `psql: connection refused` | Skip Step 8; stay in in-memory mode |
| football-data.org returns 429 | Skip Step 9; fallback already covered |
| Admin login rejected | Use the registered demo user; show audit-log denial path instead |

## Post-demo cleanup

- Stop all three server windows (`Ctrl+C`).
- Close Incognito tab so the demo token is not left on the machine.
