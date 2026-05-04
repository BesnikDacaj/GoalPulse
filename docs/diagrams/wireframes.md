# GoalPulse — UI Wireframe Sketches

Pre-implementation low-fidelity wireframes (rough sketches) for the five primary screens. These are intentionally not pixel-perfect — they predate the dark-themed final UI and document the layout decisions.

> Render tip: paste any block into <https://asciiflow.com> to edit, or copy into Figma/Excalidraw and trace.

---

## 1. Live Dashboard

```
┌───────────────────────────────────────────────────────────────────────┐
│ [GoalPulse]  Live  Schedule  Standings  Teams  Players  Favs  Admin   │
│                                                          [☾]  [Login] │
├───────────────────────────────────────────────────────────────────────┤
│  MATCH STRIP   [MCI 2-1 LIV ●LIVE]  [RMA - BAR scheduled]  [BAY 3-2]  │
├──────────────────────────────────┬────────────────────────────────────┤
│  Search [_______________] (⌘K)   │  Premier League — Today            │
│  ───── Account ──────────        │  ┌──────────────┐ ┌──────────────┐ │
│  Not signed in                   │  │ MCI 2 - 1 LIV│ │ ARS 0 - 0 CHE│ │
│  [Login]  [Register]             │  │  ●LIVE 67'   │ │  scheduled   │ │
│                                  │  │  [Details ▸] │ │  [Details ▸] │ │
│                                  │  └──────────────┘ └──────────────┘ │
│                                  │                                    │
│                                  │  La Liga — Today                   │
│                                  │  ...                               │
└──────────────────────────────────┴────────────────────────────────────┘
[Mobile <560px] sticky bottom nav: [Live] [Sched] [Stand] [Teams] [Me]
```

## 2. Match Detail

```
┌───────────────────────────────────────────────────────────────────────┐
│  [ ◂ Back ]                       MATCH CENTER                        │
├───────────────────────────────────────────────────────────────────────┤
│                Manchester City   2  -  1   Liverpool                  │
│                       ●LIVE 67'    Etihad Stadium                     │
│                       Referee: Michael Oliver                         │
├───────────────────────────────────────────────────────────────────────┤
│  Timeline                       │  Lineups                            │
│  18'  ⚽ Haaland  (MCI)         │  MCI: Ederson, Walker, Dias, ...    │
│  37'  ⚽ Salah    (LIV) (pen)   │  LIV: Alisson, TAA, Van Dijk, ...   │
│  61'  ⚽ De Bruyne(MCI)         │                                     │
├───────────────────────────────────────────────────────────────────────┤
│  Stats:  Possession 56% / 44%   Shots 14 / 9   Corners 7 / 4          │
└───────────────────────────────────────────────────────────────────────┘
```

## 3. Standings

```
┌───────────────────────────────────────────────────────────────────────┐
│  Premier League — 2025/26                            [Export CSV ↓]   │
├───┬──────────────────────┬───┬───┬───┬───┬────┬────┬────┬─────────────┤
│ # │ Team                 │ P │ W │ D │ L │ GF │ GA │ GD │ Pts         │
├───┼──────────────────────┼───┼───┼───┼───┼────┼────┼────┼─────────────┤
│ 1 │ Manchester City    🟢│32 │24 │ 5 │ 3 │ 78 │ 28 │+50 │ 77          │
│ 2 │ Liverpool          🟢│32 │23 │ 6 │ 3 │ 71 │ 30 │+41 │ 75          │
│ 3 │ Arsenal            🟢│32 │22 │ 7 │ 3 │ 68 │ 31 │+37 │ 73          │
│ 4 │ Aston Villa        🟢│32 │19 │ 6 │ 7 │ 60 │ 40 │+20 │ 63          │
│...│                      │   │   │   │   │    │    │    │             │
│18 │ Luton             🔴 │32 │ 4 │ 6 │22 │ 28 │ 71 │-43 │ 18          │
└───┴──────────────────────┴───┴───┴───┴───┴────┴────┴────┴─────────────┘
   🟢 = top four (UCL)        🔴 = relegation zone
```

## 4. Admin Panel

```
┌───────────────────────────────────────────────────────────────────────┐
│  Admin                                       Signed in as admin       │
├───────────────────────────────────────────────────────────────────────┤
│  Update Match Score                                                   │
│  Match: [ MCI vs LIV — Live  ▼ ]                                      │
│  Home: [ 2 ]  Away: [ 1 ]  Status: [ live ▼ ]    [Save]               │
├───────────────────────────────────────────────────────────────────────┤
│  Add Match Event                                                      │
│  Match:  [ MCI vs LIV ▼ ]    Player: [ De Bruyne ▼ ]                  │
│  Type:   [ goal ▼ ]   Minute: [ 61 ]  Detail: [ Free kick ___ ]       │
│  [Add Event]                                                          │
├───────────────────────────────────────────────────────────────────────┤
│  Management Tables: [Teams] [Players] [Users] [Matches]               │
│  +-----+-----------+----------+--------+                              │
│  | id  | name      | stadium  | edit   |                              │
│  | 1   | Man City  | Etihad   | [✎][✕] |                              │
│  | 2   | Liverpool | Anfield  | [✎][✕] |                              │
│  +-----+-----------+----------+--------+                              │
├───────────────────────────────────────────────────────────────────────┤
│  Audit Log (latest 25)                                                │
│  17:42 admin update match #1 home_score 1→2                           │
│  17:43 admin insert match_event #87 player=De Bruyne min=61           │
│  17:51 mia    DENIED admin/users (403 recorded)                       │
└───────────────────────────────────────────────────────────────────────┘
```

## 5. Login / Register

```
┌─────────────────────────────────┐    ┌─────────────────────────────────┐
│         Login to GoalPulse      │    │       Create an account         │
├─────────────────────────────────┤    ├─────────────────────────────────┤
│  Email    [_________________]   │    │  Username [_________________]   │
│  Password [_________________]   │    │  Email    [_________________]   │
│                                 │    │  Password [_________________]   │
│  [   Sign In   ]                │    │  Confirm  [_________________]   │
│                                 │    │                                 │
│  Don't have an account?         │    │  [   Create Account   ]         │
│  → Register                     │    │  Already have one? → Login      │
└─────────────────────────────────┘    └─────────────────────────────────┘
                Validation messages appear inline beneath the offending field.
```

## Wireframe → final UI traceability

| Wireframe | Final screen |
| --- | --- |
| §1 Live Dashboard | `frontend/index.html` Live tab |
| §2 Match Detail | `frontend/index.html` Match Center |
| §3 Standings | `frontend/index.html` Standings tab |
| §4 Admin Panel | `frontend/index.html` Admin tab |
| §5 Login/Register | `frontend/index.html` Auth modal |
