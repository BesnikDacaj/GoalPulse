# GoalPulse — Layered Architecture Diagram

The GoalPulse system follows a strict four-tier layered architecture. Each layer has exactly one responsibility, and crossing layers always happens through a documented interface (HTTP for presentation→business, DAO methods for business→data access, JDBC `PreparedStatement` for data access→database).

```mermaid
flowchart TB
    subgraph Browser["Browser (Presentation Layer)"]
        SPA["index.html + app.js + styles.css\n(vanilla SPA)"]
        REACT["frontend/src/* (React 18 / TS)\nDesign-time source structure"]
    end

    subgraph Business["Business Layer (Java 21)"]
        API["GoalPulseServer.java\ncom.sun.net.httpserver"]
        AUTH["Token signing (HMAC-SHA256)\nRBAC checks\nPasswordHasher (BCrypt)"]
        SVC["Standings calculation\nAudit log writer\nNotification dispatcher\nfootball-data.org client"]
    end

    subgraph DataAccess["Data Access Layer"]
        DAO["JdbcGoalPulseDao.java\nDAO pattern, PreparedStatement only"]
        CFG["DatabaseConfig.java\nDriverManager + Connection pool"]
    end

    subgraph DB["Database Layer"]
        PG[("PostgreSQL 14+\nschema.sql + seed.sql")]
        MY[("MySQL 8+\nschema.sql + seed.sql")]
    end

    EXT["football-data.org v4 (External REST API)"]

    SPA -- "HTTPS / JSON\n/api/v1/*" --> API
    REACT -. "design-time\nsource" .-> SPA
    API --> AUTH
    API --> SVC
    SVC --> DAO
    DAO --> CFG
    CFG --> PG
    CFG --> MY
    SVC -. "optional, with token" .-> EXT
    EXT -. "fallback to seed\non failure" .-> SVC
```

## Layer responsibilities (one sentence each)

- **Presentation** renders the UI and never speaks to the database directly.
- **Business** validates requests, applies rules (RBAC, standings math, audit), and exposes JSON.
- **Data Access** translates DAO method calls into parameterized SQL and maps `ResultSet`s back to records.
- **Database** stores the relational data with FKs, CHECKs, and ON DELETE policies.

## Crossing rules (enforced by code review)

- Presentation may call **only** `/api/v1/*` endpoints.
- Business may **never** build SQL strings.
- Data Access may **never** decide on RBAC.
- Database may **never** be reached from the browser.
