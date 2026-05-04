# GoalPulse — UML Use Case Diagram

Three actors: **Guest**, **Registered User**, **Admin**. Registered inherits everything Guest can do; Admin inherits everything Registered can do.

```mermaid
flowchart LR
    Guest((Guest))
    Registered((Registered User))
    Admin((Admin))

    subgraph System[GoalPulse]
        UC1[View Live Dashboard]
        UC2[View Match Detail]
        UC3[View Standings]
        UC4[View Schedule]
        UC5[Browse Teams]
        UC6[Browse Players]
        UC7[Search]
        UC8[Register]
        UC9[Login]
        UC10[Favorite Team]
        UC11[Receive Notifications]
        UC12[Export CSV]
        UC13[Update Match Score]
        UC14[Add Match Event]
        UC15[Manage Teams]
        UC16[Manage Players]
        UC17[Manage Users]
        UC18[View Audit Log]
        UC19[Logout]
    end

    Guest --- UC1
    Guest --- UC2
    Guest --- UC3
    Guest --- UC4
    Guest --- UC5
    Guest --- UC6
    Guest --- UC7
    Guest --- UC8
    Guest --- UC9

    Registered --- UC1
    Registered --- UC2
    Registered --- UC3
    Registered --- UC4
    Registered --- UC5
    Registered --- UC6
    Registered --- UC7
    Registered --- UC9
    Registered --- UC10
    Registered --- UC11
    Registered --- UC12
    Registered --- UC19

    Admin --- UC1
    Admin --- UC2
    Admin --- UC3
    Admin --- UC9
    Admin --- UC13
    Admin --- UC14
    Admin --- UC15
    Admin --- UC16
    Admin --- UC17
    Admin --- UC18
    Admin --- UC19
```

## Actor descriptions

- **Guest** — anonymous visitor. Read-only access. Can register or log in.
- **Registered User** — has an `app_user` row with `role='registered'`. Adds favorites, receives notifications, exports CSV.
- **Admin** — `role='admin'`. Performs CRUD on match data. Every write produces an `audit_log` row.

## Use-case extensions / includes

- `UC9 Login` is **«included by»** `UC10`, `UC12`, `UC13`, `UC14`, `UC15`, `UC16`, `UC17`, `UC18`.
- `UC11 Receive Notifications` **«extends»** `UC10` (only fires once a team has been favorited).
- `UC13 Update Match Score` **«includes»** `UC18 View Audit Log` write side-effect.
- `UC14 Add Match Event` **«includes»** `UC18` write side-effect.
- Denied admin attempts by Registered users are **also** logged via `UC18`.
