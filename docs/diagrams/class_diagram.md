# GoalPulse — UML Class Diagram (Domain Model)

```mermaid
classDiagram
    class League {
      +int leagueId
      +String name
      +String country
      +String currentSeason
      +LeagueTier tier
    }
    class LeagueTier {
      +int tierId
      +String tierName
    }
    class Season {
      +int seasonId
      +League league
      +String name
      +LocalDate startDate
      +LocalDate endDate
    }
    class Team {
      +int teamId
      +League league
      +String name
      +String shortName
      +String stadium
      +String city
      +String coachName
      +int foundedYear
      +boolean isActive
    }
    class PlayerPosition {
      +int positionId
      +String posName
      +String posCode
    }
    class Player {
      +int playerId
      +Team team
      +PlayerPosition position
      +String firstName
      +String lastName
      +String nationality
      +LocalDate dateOfBirth
      +int shirtNumber
      +boolean isActive
    }
    class Match {
      +int matchId
      +Season season
      +Team homeTeam
      +Team awayTeam
      +Instant matchDate
      +MatchStatus status
      +int homeScore
      +int awayScore
      +String venue
      +String referee
      +recomputeStanding() void
    }
    class MatchEvent {
      +int eventId
      +Match match
      +Player player
      +String eventType
      +int minute
      +String detail
    }
    class MatchLineup {
      +long lineupId
      +Match match
      +Player player
      +boolean isStarter
    }
    class Standing {
      +int standingId
      +Season season
      +Team team
      +int played
      +int won
      +int drawn
      +int lost
      +int goalsFor
      +int goalsAgainst
      +int points
      +int goalDifference()
    }
    class Role {
      +int roleId
      +String roleName
      +String description
    }
    class AppUser {
      +int userId
      +Role role
      +String username
      +String email
      +String passwordHash
      +String salt
      +Instant createdAt
      +boolean isActive
      +verify(plain : String) boolean
    }
    class UserFavorite {
      +int favoriteId
      +AppUser user
      +Team team
      +Instant createdAt
    }
    class Notification {
      +int notifId
      +AppUser user
      +Match match
      +String eventType
      +String message
      +boolean isRead
      +Instant createdAt
    }
    class AuditLog {
      +long logId
      +AppUser actor
      +String action
      +String tableName
      +String recordId
      +String oldValue
      +String newValue
      +Instant timestamp
      +String ipAddress
    }
    class GoalPulseDao {
      <<interface>>
      +listLiveMatches() List~Match~
      +findMatch(id) Match
      +saveMatchEvent(e) void
      +recomputeStandings(seasonId) void
      +createUser(u) AppUser
      +findUser(email) AppUser
      +addFavorite(userId, teamId) void
      +writeAudit(a) void
    }
    class JdbcGoalPulseDao {
      -DataSource ds
      +listLiveMatches() List~Match~
      +findMatch(id) Match
      +saveMatchEvent(e) void
      +recomputeStandings(seasonId) void
      +createUser(u) AppUser
      +findUser(email) AppUser
      +addFavorite(userId, teamId) void
      +writeAudit(a) void
    }
    class GoalPulseServer {
      +start() void
      -route(exchange) void
      -authenticate(req) AppUser
      -authorize(user, role) void
    }
    class PasswordHasher {
      +hash(plain) String
      +verify(plain, hash) boolean
    }

    LeagueTier "1" --o "0..*" League : groups
    League "1" --o "0..*" Season : has
    League "1" --o "0..*" Team : contains
    Season "1" --o "0..*" Match : schedules
    Season "1" --o "0..*" Standing : ranks
    Team "1" --o "0..*" Player : rosters
    Team "1" --o "0..*" Match : plays at home
    Team "1" --o "0..*" Match : plays away
    PlayerPosition "1" --o "0..*" Player : categorizes
    Match "1" --o "0..*" MatchEvent : timeline
    Match "1" --o "0..*" MatchLineup : lineup
    Player "1" --o "0..*" MatchEvent : performs
    Player "1" --o "0..*" MatchLineup : appears in
    Role "1" --o "0..*" AppUser : grants
    AppUser "1" --o "0..*" UserFavorite : owns
    Team "1" --o "0..*" UserFavorite : favored by
    AppUser "1" --o "0..*" Notification : receives
    Match "0..1" --o "0..*" Notification : triggers
    AppUser "0..1" --o "0..*" AuditLog : actor

    GoalPulseDao <|.. JdbcGoalPulseDao
    GoalPulseServer ..> GoalPulseDao : uses
    GoalPulseServer ..> PasswordHasher : uses
```

## Notes

- `GoalPulseDao` is the DAO interface; `JdbcGoalPulseDao` is the JDBC implementation. An in-memory implementation (`Store` inside `GoalPulseServer`) is used when `GOALPULSE_USE_DB` is unset.
- `MatchStatus` is an enum-like field in the schema; modeled as a `String` constrained by `CHECK status IN ('scheduled','live','finished')`.
- All identifier fields are surrogate keys (`SERIAL`/`AUTO_INCREMENT`) — no natural keys are exposed.
