# GoalPulse — UML Component Diagram

```mermaid
flowchart LR
    subgraph Client["Client tier"]
        BR[Browser SPA<br/>index.html + app.js + styles.css]
        REACT[React/TS module<br/>frontend/src]
    end

    subgraph Server["Server tier (JVM 21)"]
        HTTP[HTTP Server<br/>com.sun.net.httpserver]
        ROUTER[REST Router<br/>GoalPulseServer]
        AUTH[Auth Component<br/>HMAC token + BCrypt]
        AUDIT[Audit Writer]
        FDC[football-data.org Client]
        DAO[GoalPulseDao<br/>interface]
        JDBCDAO[JdbcGoalPulseDao]
        DBCFG[DatabaseConfig<br/>DriverManager]
    end

    subgraph DB["Database tier"]
        PG[(PostgreSQL 14+)]
        MY[(MySQL 8+)]
    end

    EXT[(football-data.org v4)]

    BR -- "HTTP/JSON" --> HTTP
    REACT -- "design-time<br/>source" -.-> BR
    HTTP --> ROUTER
    ROUTER --> AUTH
    ROUTER --> AUDIT
    ROUTER --> FDC
    ROUTER --> DAO
    DAO --> JDBCDAO
    JDBCDAO --> DBCFG
    DBCFG --> PG
    DBCFG --> MY
    FDC -. "REST + token" .-> EXT
```

## Deployment notes

- The browser, server, and database can all run on a single machine (typical demo).
- For a hosted deployment, place the server behind a TLS terminator (e.g., Caddy or nginx); the application server itself speaks plain HTTP.
- Only the server reaches the database; the browser never holds DB credentials.
- football-data.org credentials live only as a server-side environment variable.
