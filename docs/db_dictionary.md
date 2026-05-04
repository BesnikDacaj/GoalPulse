# GoalPulse — Database Dictionary

This dictionary covers the 14 relations of the GoalPulse schema. It is engine-neutral: the PostgreSQL types are listed, with the MySQL equivalent in parentheses where they differ. Both engines preserve the same constraints and ON DELETE policies. Source files: `database/postgresql/schema.sql`, `database/mysql/schema.sql`.

> **Design note — reserved word:** `match` is a reserved word in MySQL. The MySQL schema backticks the table name everywhere (`` `match` ``); PostgreSQL accepts the unquoted identifier. This is the only deliberate engine-specific quoting in the project.
>
> **Design note — `app_user.salt`:** the column is kept for an optional PBKDF2 fallback path used when jBCrypt is not on the classpath. With BCrypt (the default), the salt is embedded in `password_hash` and `salt` is `NULL`.

---

## 1. `league_tier`

Tiers (e.g., First Division, Second Division) used to group leagues.

| Column     | Type                | Null | Default | Constraint     | Description                |
| ---------- | ------------------- | ---- | ------- | -------------- | -------------------------- |
| `tier_id`  | `SERIAL` (`INT AI`) | NO   | auto    | PK             | Surrogate key.             |
| `tier_name`| `VARCHAR(80)`       | NO   |         | UNIQUE         | Tier label, human-readable.|

## 2. `league`

A national or competition league.

| Column           | Type           | Null | Default | Constraint                                            | Description                       |
| ---------------- | -------------- | ---- | ------- | ----------------------------------------------------- | --------------------------------- |
| `league_id`      | `SERIAL`       | NO   | auto    | PK                                                    | Surrogate key.                    |
| `name`           | `VARCHAR(120)` | NO   |         |                                                       | League name.                      |
| `country`        | `VARCHAR(80)`  | NO   |         |                                                       | Country of the league.            |
| `logo_url`       | `VARCHAR(255)` | YES  | NULL    |                                                       | Optional crest URL.               |
| `current_season` | `VARCHAR(20)`  | YES  | NULL    |                                                       | Convenience, e.g., `2025/26`.     |
| `tier_id`        | `INT`          | YES  | NULL    | FK → `league_tier(tier_id)` ON DELETE **SET NULL**    | Tier the league belongs to.       |

## 3. `season`

A specific season of a league.

| Column      | Type          | Null | Default | Constraint                                       | Description                       |
| ----------- | ------------- | ---- | ------- | ------------------------------------------------ | --------------------------------- |
| `season_id` | `SERIAL`      | NO   | auto    | PK                                               | Surrogate key.                    |
| `league_id` | `INT`         | NO   |         | FK → `league(league_id)` ON DELETE **CASCADE**   | Owning league.                    |
| `name`      | `VARCHAR(30)` | NO   |         |                                                  | Season label, e.g., `2025/26`.    |
| `start_date`| `DATE`        | NO   |         |                                                  | Season opening date.              |
| `end_date`  | `DATE`        | NO   |         |                                                  | Season closing date.              |

## 4. `team`

A club competing in a league.

| Column         | Type           | Null | Default | Constraint                                          | Description                       |
| -------------- | -------------- | ---- | ------- | --------------------------------------------------- | --------------------------------- |
| `team_id`      | `SERIAL`       | NO   | auto    | PK                                                  | Surrogate key.                    |
| `league_id`    | `INT`          | NO   |         | FK → `league(league_id)` ON DELETE **RESTRICT**     | Prevents accidental data loss.    |
| `name`         | `VARCHAR(120)` | NO   |         |                                                     | Full team name.                   |
| `short_name`   | `VARCHAR(12)`  | NO   |         |                                                     | 2–4 letter code (e.g., `MCI`).    |
| `logo_url`     | `VARCHAR(255)` | YES  | NULL    |                                                     | Crest URL.                        |
| `stadium`      | `VARCHAR(120)` | YES  | NULL    |                                                     | Home stadium name.                |
| `city`         | `VARCHAR(80)`  | YES  | NULL    |                                                     |                                   |
| `coach_name`   | `VARCHAR(120)` | YES  | NULL    |                                                     |                                   |
| `founded_year` | `INT`          | YES  | NULL    | CHECK `founded_year >= 1850`                        | Domain rule: no pre-1850 founders.|
| `is_active`    | `BOOLEAN`      | NO   | TRUE    |                                                     | Soft-delete flag.                 |

## 5. `player_position`

Reference table of position codes.

| Column        | Type          | Null | Default | Constraint | Description                |
| ------------- | ------------- | ---- | ------- | ---------- | -------------------------- |
| `position_id` | `SERIAL`      | NO   | auto    | PK         | Surrogate key.             |
| `pos_name`    | `VARCHAR(40)` | NO   |         | UNIQUE     | e.g., `Goalkeeper`.        |
| `pos_code`    | `VARCHAR(3)`  | NO   |         | UNIQUE     | e.g., `GK`, `DEF`, `MID`.  |

## 6. `player`

Squad member of a team.

| Column           | Type          | Null | Default | Constraint                                                      | Description                          |
| ---------------- | ------------- | ---- | ------- | --------------------------------------------------------------- | ------------------------------------ |
| `player_id`      | `SERIAL`      | NO   | auto    | PK                                                              | Surrogate key.                       |
| `team_id`        | `INT`         | YES  | NULL    | FK → `team(team_id)` ON DELETE **SET NULL**                     | Allows free agents post-team-deletion.|
| `position_id`    | `INT`         | YES  | NULL    | FK → `player_position(position_id)` ON DELETE **SET NULL**      |                                      |
| `first_name`     | `VARCHAR(80)` | NO   |         |                                                                 |                                      |
| `last_name`      | `VARCHAR(80)` | NO   |         |                                                                 |                                      |
| `nationality`    | `VARCHAR(80)` | YES  | NULL    |                                                                 |                                      |
| `date_of_birth`  | `DATE`        | YES  | NULL    |                                                                 |                                      |
| `shirt_number`   | `INT`         | YES  | NULL    | CHECK `shirt_number BETWEEN 1 AND 99`                           | League rule.                         |
| `is_active`      | `BOOLEAN`     | NO   | TRUE    |                                                                 | Soft-delete flag.                    |

## 7. `match`

A scheduled, live, or finished match.

> MySQL: backtick the identifier — `` `match` ``.

| Column         | Type             | Null | Default | Constraint                                                       | Description                                |
| -------------- | ---------------- | ---- | ------- | ---------------------------------------------------------------- | ------------------------------------------ |
| `match_id`     | `SERIAL`         | NO   | auto    | PK                                                               | Surrogate key.                             |
| `season_id`    | `INT`            | NO   |         | FK → `season(season_id)` ON DELETE **CASCADE**                   |                                            |
| `home_team_id` | `INT`            | NO   |         | FK → `team(team_id)` ON DELETE **RESTRICT**                      |                                            |
| `away_team_id` | `INT`            | NO   |         | FK → `team(team_id)` ON DELETE **RESTRICT**                      |                                            |
| `match_date`   | `TIMESTAMP`/`DATETIME` | NO |     |                                                                  | Kickoff time.                              |
| `status`       | `VARCHAR(20)`    | NO   |         | CHECK `status IN ('scheduled','live','finished')`                | State machine.                             |
| `home_score`   | `INT`            | NO   | 0       | CHECK `home_score >= 0`                                          |                                            |
| `away_score`   | `INT`            | NO   | 0       | CHECK `away_score >= 0`                                          |                                            |
| `venue`        | `VARCHAR(120)`   | YES  | NULL    |                                                                  |                                            |
| `referee`      | `VARCHAR(120)`   | YES  | NULL    |                                                                  |                                            |
| (table-level)  |                  |      |         | CHECK `home_team_id <> away_team_id`                             | A team cannot play itself.                 |

## 8. `match_event`

One row per goal, card, substitution, etc.

| Column        | Type           | Null | Default | Constraint                                                  | Description                       |
| ------------- | -------------- | ---- | ------- | ----------------------------------------------------------- | --------------------------------- |
| `event_id`    | `SERIAL`       | NO   | auto    | PK                                                          | Surrogate key.                    |
| `match_id`    | `INT`          | NO   |         | FK → `match(match_id)` ON DELETE **CASCADE**                | Weak entity of `match`.           |
| `player_id`   | `INT`          | YES  | NULL    | FK → `player(player_id)` ON DELETE **SET NULL**             | NULL if player removed.           |
| `event_type`  | `VARCHAR(30)`  | NO   |         |                                                             | `goal`, `yellow`, `red`, etc.     |
| `minute`      | `INT`          | NO   |         | CHECK `minute BETWEEN 0 AND 130`                            | Includes extra time.              |
| `detail`      | `VARCHAR(255)` | YES  | NULL    |                                                             | Free-text description.            |

## 9. `match_lineup`

Starters and bench for a given match.

| Column       | Type      | Null | Default | Constraint                                                        | Description                  |
| ------------ | --------- | ---- | ------- | ----------------------------------------------------------------- | ---------------------------- |
| `lineup_id`  | `BIGSERIAL`/`BIGINT AI` | NO | auto | PK                                                          | Surrogate key.               |
| `match_id`   | `INT`     | NO   |         | FK → `match(match_id)` ON DELETE **CASCADE**                      |                              |
| `player_id`  | `INT`     | NO   |         | FK → `player(player_id)` ON DELETE **CASCADE**                    |                              |
| `is_starter` | `BOOLEAN` | NO   | TRUE    |                                                                   | Starter vs bench.            |
| (table)      |           |      |         | UNIQUE `(match_id, player_id)`                                    | One row per player per match.|

## 10. `standing`

Computed standing snapshot for a team in a season.

| Column          | Type     | Null | Default | Constraint                                              | Description                       |
| --------------- | -------- | ---- | ------- | ------------------------------------------------------- | --------------------------------- |
| `standing_id`   | `SERIAL` | NO   | auto    | PK                                                      | Surrogate key.                    |
| `season_id`     | `INT`    | NO   |         | FK → `season(season_id)` ON DELETE **CASCADE**          |                                   |
| `team_id`       | `INT`    | NO   |         | FK → `team(team_id)` ON DELETE **CASCADE**              |                                   |
| `played`        | `INT`    | NO   | 0       |                                                         |                                   |
| `won`           | `INT`    | NO   | 0       |                                                         |                                   |
| `drawn`         | `INT`    | NO   | 0       |                                                         |                                   |
| `lost`          | `INT`    | NO   | 0       |                                                         |                                   |
| `goals_for`     | `INT`    | NO   | 0       |                                                         |                                   |
| `goals_against` | `INT`    | NO   | 0       |                                                         |                                   |
| `points`        | `INT`    | NO   | 0       |                                                         | 3·won + drawn.                    |
| (table)         |          |      |         | UNIQUE `(season_id, team_id)`                           | One row per team per season.      |

## 11. `role`

Role catalog (`guest`, `registered`, `admin`).

| Column        | Type           | Null | Default | Constraint | Description                |
| ------------- | -------------- | ---- | ------- | ---------- | -------------------------- |
| `role_id`     | `SERIAL`       | NO   | auto    | PK         | Surrogate key.             |
| `role_name`   | `VARCHAR(30)`  | NO   |         | UNIQUE     | `guest`/`registered`/`admin`. |
| `description` | `VARCHAR(255)` | YES  | NULL    |            |                            |

## 12. `app_user`

Application user account.

| Column          | Type           | Null | Default            | Constraint                                          | Description                                            |
| --------------- | -------------- | ---- | ------------------ | --------------------------------------------------- | ------------------------------------------------------ |
| `user_id`       | `SERIAL`       | NO   | auto               | PK                                                  | Surrogate key.                                         |
| `role_id`       | `INT`          | NO   |                    | FK → `role(role_id)` ON DELETE **RESTRICT**         | Cannot delete a role still in use.                     |
| `username`      | `VARCHAR(60)`  | NO   |                    | UNIQUE                                              | Display name and login.                                |
| `email`         | `VARCHAR(120)` | NO   |                    | UNIQUE                                              | Login alternative.                                     |
| `password_hash` | `VARCHAR(255)` | NO   |                    |                                                     | BCrypt (`$2a$…`); PBKDF2 fallback uses `salt` column.  |
| `salt`          | `VARCHAR(80)`  | YES  | NULL               |                                                     | Used only by PBKDF2 fallback; NULL with BCrypt.        |
| `created_at`    | `TIMESTAMP`    | NO   | `CURRENT_TIMESTAMP`|                                                     |                                                        |
| `is_active`     | `BOOLEAN`      | NO   | TRUE               |                                                     | Soft-delete flag.                                      |

## 13. `user_favorite`

User → team favorites (M:N relation).

| Column        | Type        | Null | Default            | Constraint                                                | Description                       |
| ------------- | ----------- | ---- | ------------------ | --------------------------------------------------------- | --------------------------------- |
| `favorite_id` | `SERIAL`    | NO   | auto               | PK                                                        |                                   |
| `user_id`     | `INT`       | NO   |                    | FK → `app_user(user_id)` ON DELETE **CASCADE**            |                                   |
| `team_id`     | `INT`       | NO   |                    | FK → `team(team_id)` ON DELETE **CASCADE**                |                                   |
| `created_at`  | `TIMESTAMP` | NO   | `CURRENT_TIMESTAMP`|                                                           |                                   |
| (table)       |             |      |                    | UNIQUE `(user_id, team_id)`                               | A user can favorite a team once.  |

## 14. `notification`

Per-user notifications, often triggered by score updates.

| Column       | Type           | Null | Default            | Constraint                                                  | Description                       |
| ------------ | -------------- | ---- | ------------------ | ----------------------------------------------------------- | --------------------------------- |
| `notif_id`   | `SERIAL`       | NO   | auto               | PK                                                          |                                   |
| `user_id`    | `INT`          | NO   |                    | FK → `app_user(user_id)` ON DELETE **CASCADE**              |                                   |
| `match_id`   | `INT`          | YES  | NULL               | FK → `match(match_id)` ON DELETE **CASCADE**                | NULL if not match-related.        |
| `event_type` | `VARCHAR(30)`  | NO   |                    |                                                             | e.g., `score`, `start`, `final`.  |
| `message`    | `VARCHAR(255)` | NO   |                    |                                                             |                                   |
| `is_read`    | `BOOLEAN`      | NO   | FALSE              |                                                             |                                   |
| `created_at` | `TIMESTAMP`    | NO   | `CURRENT_TIMESTAMP`|                                                             |                                   |

## 15. `audit_log`

Append-only log of admin actions and denied attempts.

| Column        | Type           | Null | Default            | Constraint                                                | Description                                |
| ------------- | -------------- | ---- | ------------------ | --------------------------------------------------------- | ------------------------------------------ |
| `log_id`      | `BIGSERIAL`    | NO   | auto               | PK                                                        |                                            |
| `user_id`     | `INT`          | YES  | NULL               | FK → `app_user(user_id)` ON DELETE **SET NULL**           | NULL if actor account is later removed.    |
| `action`      | `VARCHAR(20)`  | NO   |                    |                                                           | `insert` / `update` / `delete` / `denied`. |
| `table_name`  | `VARCHAR(60)`  | NO   |                    |                                                           | Affected table.                            |
| `record_id`   | `VARCHAR(60)`  | YES  | NULL               |                                                           | PK of affected row, stringified.           |
| `old_value`   | `TEXT`         | YES  | NULL               |                                                           | JSON snippet of prior state.               |
| `new_value`   | `TEXT`         | YES  | NULL               |                                                           | JSON snippet of new state.                 |
| `timestamp`   | `TIMESTAMP`    | NO   | `CURRENT_TIMESTAMP`|                                                           |                                            |
| `ip_address`  | `VARCHAR(60)`  | YES  | NULL               |                                                           | Originating IP.                            |

---

## Relationships and validation rules summary

### Foreign-key delete policies

| Parent → Child                       | ON DELETE   | Why                                                                |
| ------------------------------------ | ----------- | ------------------------------------------------------------------ |
| `league_tier` → `league.tier_id`     | SET NULL    | Tier removal must not orphan/erase leagues.                        |
| `league` → `season.league_id`        | CASCADE     | A league's seasons are part of it.                                 |
| `league` → `team.league_id`          | RESTRICT    | Prevents accidental data loss; admins must move teams first.       |
| `season` → `match.season_id`         | CASCADE     | A season's matches are part of it.                                 |
| `season` → `standing.season_id`      | CASCADE     | Standings are derivative; recomputed if needed.                    |
| `team` → `match.home_team_id`        | RESTRICT    | A team with played matches cannot be silently deleted.             |
| `team` → `match.away_team_id`        | RESTRICT    | Same reason.                                                       |
| `team` → `player.team_id`            | SET NULL    | Player becomes a "free agent" if their team is removed.            |
| `team` → `standing.team_id`          | CASCADE     | Standings row is derivative.                                       |
| `team` → `user_favorite.team_id`     | CASCADE     | Favorite of a non-existent team is meaningless.                    |
| `match` → `match_event.match_id`     | CASCADE     | Events are weak entities of a match.                               |
| `match` → `match_lineup.match_id`    | CASCADE     | Same.                                                              |
| `match` → `notification.match_id`    | CASCADE     |                                                                    |
| `player` → `match_event.player_id`   | SET NULL    | Preserves the timeline if a player is removed.                     |
| `player` → `match_lineup.player_id`  | CASCADE     |                                                                    |
| `role` → `app_user.role_id`          | RESTRICT    | A role with users cannot disappear silently.                       |
| `app_user` → `user_favorite.user_id` | CASCADE     |                                                                    |
| `app_user` → `notification.user_id`  | CASCADE     |                                                                    |
| `app_user` → `audit_log.user_id`     | SET NULL    | Audit must outlive the deleted account.                            |

### Cross-row validation rules (CHECK)

- `match.status IN ('scheduled','live','finished')`
- `match.home_score >= 0`, `match.away_score >= 0`
- `match.home_team_id <> match.away_team_id`
- `match_event.minute BETWEEN 0 AND 130`
- `player.shirt_number BETWEEN 1 AND 99`
- `team.founded_year >= 1850`

### Uniqueness

- `league_tier.tier_name`, `player_position.pos_name`, `player_position.pos_code`,
  `role.role_name`, `app_user.username`, `app_user.email`,
  `(season_id, team_id)` on `standing`,
  `(match_id, player_id)` on `match_lineup`,
  `(user_id, team_id)` on `user_favorite`.

### Normalization

The schema is in 3NF: all non-key attributes depend on the key, the whole key, and nothing but the key. The denormalized `standing` row is **not** a 3NF violation because it is treated as a materialized snapshot — recomputable from `match` rows.
