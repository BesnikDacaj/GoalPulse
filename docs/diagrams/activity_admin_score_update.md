# GoalPulse — UML Activity Diagram: "Admin updates match score"

Covers happy path **and** error/denied paths.

```mermaid
flowchart TD
    A([Start: admin opens Admin tab]) --> B[Submit POST /api/v1/admin/match/{id}/score]
    B --> C{Bearer token<br/>present and valid?}
    C -- No --> X1[401 Unauthorized] --> Z([End])
    C -- Yes --> D{Decoded role == 'admin'?}
    D -- No --> X2[Write audit_log: action='denied'] --> X3[403 Forbidden] --> Z
    D -- Yes --> E[Validate body:<br/>home_score >= 0, away_score >= 0,<br/>status in scheduled/live/finished]
    E --> F{Valid?}
    F -- No --> X4[400 Bad Request] --> Z
    F -- Yes --> G[Begin transaction]
    G --> H[SELECT old row from match]
    H --> I[UPDATE match SET ... WHERE match_id = ?<br/>via PreparedStatement]
    I --> J{Update OK?}
    J -- No --> X5[ROLLBACK] --> X6[500 Internal Server Error] --> Z
    J -- Yes --> K[INSERT audit_log<br/>action='update', old_value, new_value]
    K --> L[Recompute season standings]
    L --> M[Dispatch notifications<br/>to favoriting users]
    M --> N[COMMIT]
    N --> O[200 OK<br/>JSON envelope: success=true]
    O --> Z
```

## Notes

- The "denied" branch (`D == No`) is itself an audit event — a hostile user cannot quietly probe admin endpoints.
- Notification dispatch happens **after** COMMIT to avoid sending pings for a transaction that fails.
- All SQL is parameterized via `PreparedStatement`. No string concatenation of user input ever reaches the database driver.
