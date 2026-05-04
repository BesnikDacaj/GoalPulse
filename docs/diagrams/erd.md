# GoalPulse — Entity-Relationship Diagram

14 tables, normalized to 3NF. Foreign keys carry explicit ON DELETE policies (CASCADE / RESTRICT / SET NULL) chosen per relationship.

```mermaid
erDiagram
    LEAGUE_TIER ||--o{ LEAGUE : "tiers"
    LEAGUE      ||--o{ SEASON : "has seasons"
    LEAGUE      ||--o{ TEAM   : "groups teams"
    SEASON      ||--o{ MATCH  : "schedules"
    SEASON      ||--o{ STANDING : "ranks"
    TEAM        ||--o{ STANDING : "row per team per season"
    TEAM        ||--o{ PLAYER : "rosters"
    TEAM        ||--o{ MATCH   : "home/away"
    PLAYER_POSITION ||--o{ PLAYER : "categorizes"
    MATCH       ||--o{ MATCH_EVENT : "timeline"
    MATCH       ||--o{ MATCH_LINEUP : "starting/bench"
    PLAYER      ||--o{ MATCH_EVENT : "scores/cards"
    PLAYER      ||--o{ MATCH_LINEUP : "appears in"
    ROLE        ||--o{ APP_USER : "grants"
    APP_USER    ||--o{ USER_FAVORITE : "favors"
    TEAM        ||--o{ USER_FAVORITE : "is favored by"
    APP_USER    ||--o{ NOTIFICATION : "receives"
    MATCH       ||--o{ NOTIFICATION : "triggers"
    APP_USER    ||--o{ AUDIT_LOG    : "performed by"

    LEAGUE_TIER {
      int    tier_id PK
      string tier_name UK
    }
    LEAGUE {
      int    league_id PK
      string name
      string country
      string current_season
      int    tier_id FK
    }
    SEASON {
      int    season_id PK
      int    league_id FK
      string name
      date   start_date
      date   end_date
    }
    TEAM {
      int    team_id PK
      int    league_id FK
      string name
      string short_name
      string stadium
      string city
      string coach_name
      int    founded_year "CHECK >= 1850"
      bool   is_active
    }
    PLAYER_POSITION {
      int    position_id PK
      string pos_name UK
      string pos_code UK
    }
    PLAYER {
      int    player_id PK
      int    team_id FK "ON DELETE SET NULL"
      int    position_id FK "ON DELETE SET NULL"
      string first_name
      string last_name
      string nationality
      date   date_of_birth
      int    shirt_number "CHECK 1..99"
      bool   is_active
    }
    MATCH {
      int       match_id PK
      int       season_id FK "ON DELETE CASCADE"
      int       home_team_id FK "RESTRICT"
      int       away_team_id FK "RESTRICT"
      timestamp match_date
      string    status "scheduled|live|finished"
      int       home_score "CHECK >= 0"
      int       away_score "CHECK >= 0"
      string    venue
      string    referee
    }
    MATCH_EVENT {
      int    event_id PK
      int    match_id FK "ON DELETE CASCADE"
      int    player_id FK "ON DELETE SET NULL"
      string event_type
      int    minute "CHECK 0..130"
      string detail
    }
    MATCH_LINEUP {
      bigint lineup_id PK
      int    match_id FK
      int    player_id FK
      bool   is_starter
    }
    STANDING {
      int  standing_id PK
      int  season_id FK
      int  team_id FK
      int  played
      int  won
      int  drawn
      int  lost
      int  goals_for
      int  goals_against
      int  points
    }
    ROLE {
      int    role_id PK
      string role_name UK
      string description
    }
    APP_USER {
      int       user_id PK
      int       role_id FK "ON DELETE RESTRICT"
      string    username UK
      string    email UK
      string    password_hash
      string    salt "legacy/PBKDF2 fallback"
      timestamp created_at
      bool      is_active
    }
    USER_FAVORITE {
      int       favorite_id PK
      int       user_id FK
      int       team_id FK
      timestamp created_at
    }
    NOTIFICATION {
      int       notif_id PK
      int       user_id FK
      int       match_id FK
      string    event_type
      string    message
      bool      is_read
      timestamp created_at
    }
    AUDIT_LOG {
      bigint    log_id PK
      int       user_id FK "ON DELETE SET NULL"
      string    action
      string    table_name
      string    record_id
      text      old_value
      text      new_value
      timestamp timestamp
      string    ip_address
    }
```

## Notes

- `match` is a reserved word in MySQL. The MySQL schema uses backticks (`` `match` ``) everywhere; PostgreSQL accepts it unquoted.
- `app_user.salt` is kept for the optional PBKDF2 fallback path and is `NULL` when BCrypt is in use.
- `standing` is denormalized for quick reads; recomputation logic lives in the business layer.
- Primary keys use `SERIAL`/`BIGSERIAL` (PG) and `AUTO_INCREMENT` (MySQL).

## How to render to PNG

1. Open this file in any Mermaid-aware viewer (GitHub, VS Code with the Markdown Preview Mermaid extension, or `mermaid-cli`).
2. Or paste the diagram block into <https://mermaid.live> and export PNG.
3. Save as `docs/diagrams/erd.png` and embed in the final documentation.
