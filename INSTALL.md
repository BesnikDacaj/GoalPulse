# GoalPulse — Installation Instructions (Windows / macOS / Linux)

Three modes are supported, in order of "fastest to demo" → "most complete":

1. **In-memory demo** — no database, no Maven, no `npm`. Just JDK 17+.
2. **JDBC mode (PostgreSQL or MySQL)** — same code, real database.
3. **Maven build** — produces `backend/target/*.jar` with jBCrypt and JDBC drivers on the classpath.

The runnable demo defaults to mode (1). Modes (2) and (3) are toggled with environment variables.

---

## Prerequisites

| Tool | Required for | Verify with |
| --- | --- | --- |
| **JDK 17+** (tested with 21) | All modes | `java -version` |
| **PowerShell 7+** (Windows) or **bash** (macOS/Linux) | Running `scripts/run.ps1` or its bash equivalent | `pwsh --version` or `bash --version` |
| **PostgreSQL 14+** (or MySQL 8+) | Mode 2 only | `psql --version` or `mysql --version` |
| **Maven 3.8+** | Mode 3 only | `mvn -v` |
| **Node 20+ / npm 10+** | Building the React/TS source | `node --version` |
| **Git** | Cloning the repo | `git --version` |

---

## Mode 1 — In-memory demo (fastest)

### Windows (PowerShell)

```powershell
git clone <repo-url> GoalPulse
cd GoalPulse
.\scripts\run.ps1
```

If port 8080 is busy:

```powershell
$env:PORT="8081"
.\scripts\run.ps1
```

### macOS / Linux (bash)

```bash
git clone <repo-url> GoalPulse
cd GoalPulse
# Equivalent of scripts/run.ps1 on POSIX:
javac -d backend/out backend/src/*.java
PORT="${PORT:-8080}" java -cp backend/out GoalPulseServer
```

Open `http://localhost:8080` in any modern browser.

Demo accounts:
- Admin: `admin@goalpulse.test` / `Admin123!`
- Registered: `mia@goalpulse.test` / `User123!`

---

## Mode 2 — Real database (JDBC)

### A. Create the database (PostgreSQL)

```bash
createdb goalpulse
psql -d goalpulse -f database/postgresql/schema.sql
psql -d goalpulse -f database/postgresql/seed.sql
```

### A. Create the database (MySQL)

```bash
mysql -u root -p -e "CREATE DATABASE goalpulse CHARACTER SET utf8mb4;"
mysql -u root -p goalpulse < database/mysql/schema.sql
mysql -u root -p goalpulse < database/mysql/seed.sql
```

### B. Launch the server in JDBC mode

Windows:

```powershell
$env:GOALPULSE_USE_DB="true"
$env:GOALPULSE_DB_URL="jdbc:postgresql://localhost:5432/goalpulse"
$env:GOALPULSE_DB_USER="goalpulse"
$env:GOALPULSE_DB_PASSWORD="<password>"
.\scripts\run.ps1
```

macOS/Linux:

```bash
export GOALPULSE_USE_DB=true
export GOALPULSE_DB_URL="jdbc:postgresql://localhost:5432/goalpulse"
export GOALPULSE_DB_USER="goalpulse"
export GOALPULSE_DB_PASSWORD="<password>"
java -cp backend/out:backend/lib/* GoalPulseServer
```

The server will log `Server listening on http://localhost:8080 (JDBC mode)` when it has acquired a connection.

> **JDBC drivers.** The `backend/lib/` folder contains the JDBC drivers (PostgreSQL + MySQL) used in mode 2 without Maven. If you cleared `backend/lib/`, re-add them or use mode 3 (Maven) instead.

---

## Mode 3 — Maven build

```bash
cd backend
mvn -q -DskipTests package
java -jar target/goalpulse-*.jar
```

This packages jBCrypt and both JDBC drivers into the jar. Combine with the `GOALPULSE_*` environment variables from mode 2 to get full BCrypt + real database.

---

## Optional — Live football-data.org integration

```bash
export FOOTBALL_DATA_API_TOKEN="your_token_here"   # bash
$env:FOOTBALL_DATA_API_TOKEN="your_token_here"     # PowerShell
```

Then start the server in any mode. If the token is missing/expired/rate-limited, the app falls back silently to seeded data.

---

## Optional — Build the React/TypeScript frontend

```bash
cd frontend
npm install
npm run dev
```

This starts the Vite dev server at `http://localhost:5173`. Configure the proxy to point at `http://localhost:8080/api/v1` (see `frontend/vite.config.ts`). The runtime demo at `http://localhost:8080` continues to use the vanilla SPA.

---

## Troubleshooting

| Symptom | Fix |
| --- | --- |
| `Address already in use :8080` | Set `PORT` to another value before `.\scripts\run.ps1`. |
| `java.lang.NoClassDefFoundError: org/mindrot/jbcrypt/BCrypt` | Use mode 1 (PBKDF2 fallback) or run mode 3 (Maven). |
| `psql: connection refused` | Start the PostgreSQL service: `sudo service postgresql start` (Linux) or `services.msc` (Windows). |
| Browser shows blank page | Hard reload with `Ctrl+Shift+R`. Check the server console for errors. |
| `429 Too Many Requests` from football-data.org | Wait a minute; the app keeps serving seeded data in the meantime. |
| `mysql: command not found` | Use the MySQL Workbench GUI to run `database/mysql/schema.sql` and `seed.sql`. |

---

## Verifying installation

After mode 1 or mode 2 starts, verify:

```bash
curl -s http://localhost:8080/api/v1/health
# expects: {"success":true,"data":{"status":"ok"},"message":"OK"}
```

```bash
curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"admin@goalpulse.test","password":"Admin123!"}'
# expects: token + user role 'admin'
```
