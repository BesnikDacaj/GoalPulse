import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class JdbcGoalPulseDao {
    private final DatabaseConfig config;

    public JdbcGoalPulseDao(DatabaseConfig config) {
        this.config = config;
    }

    public List<MatchSummary> findMatchesByStatus(String status) throws SQLException {
        String sql = """
            SELECT m.match_id, ht.name AS home_team, at.name AS away_team,
                   m.match_date, m.status, m.home_score, m.away_score
            FROM match m
            JOIN team ht ON ht.team_id = m.home_team_id
            JOIN team at ON at.team_id = m.away_team_id
            WHERE m.status = ?
            ORDER BY m.match_date
            """;
        List<MatchSummary> matches = new ArrayList<>();
        try (Connection connection = config.openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, status);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    matches.add(new MatchSummary(
                        rs.getInt("match_id"),
                        rs.getString("home_team"),
                        rs.getString("away_team"),
                        rs.getTimestamp("match_date").toLocalDateTime(),
                        rs.getString("status"),
                        rs.getInt("home_score"),
                        rs.getInt("away_score")
                    ));
                }
            }
        }
        return matches;
    }

    public List<Map<String, Object>> matchesJson() throws SQLException {
        return matchRows(null);
    }

    public List<Map<String, Object>> matchesByStatusJson(String status) throws SQLException {
        return matchRows(status);
    }

    public Map<String, Object> matchJson(int matchId) throws SQLException {
        List<Map<String, Object>> rows = matchRowsById(matchId);
        if (rows.isEmpty()) throw new GoalPulseServer.HttpError(404, "Match not found");
        return rows.get(0);
    }

    public List<Map<String, Object>> teamsJson() throws SQLException {
        String sql = """
            SELECT t.team_id, l.name AS league, t.name, t.short_name, t.city, t.stadium,
                   t.coach_name, t.founded_year, COALESCE(t.logo_url, '') AS logo_url
            FROM team t
            JOIN league l ON l.league_id = t.league_id
            WHERE t.is_active = TRUE
            ORDER BY l.name, t.name
            """;
        List<Map<String, Object>> teams = new ArrayList<>();
        try (Connection connection = config.openConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            while (rs.next()) teams.add(teamMap(rs));
        }
        return teams;
    }

    public List<Map<String, Object>> playersJson(String teamId) throws SQLException {
        StringBuilder sql = new StringBuilder("""
            SELECT p.player_id, p.team_id, t.name AS team, p.first_name, p.last_name,
                   COALESCE(pp.pos_name, 'Unknown') AS position, p.nationality, p.shirt_number
            FROM player p
            LEFT JOIN team t ON t.team_id = p.team_id
            LEFT JOIN player_position pp ON pp.position_id = p.position_id
            WHERE p.is_active = TRUE
            """);
        boolean filterByTeam = teamId != null && !teamId.isBlank();
        if (filterByTeam) sql.append(" AND p.team_id = ?");
        sql.append(" ORDER BY t.name, p.shirt_number, p.last_name");

        List<Map<String, Object>> players = new ArrayList<>();
        try (Connection connection = config.openConnection();
             PreparedStatement statement = connection.prepareStatement(sql.toString())) {
            if (filterByTeam) statement.setInt(1, Integer.parseInt(teamId));
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> player = new LinkedHashMap<>();
                    player.put("id", rs.getInt("player_id"));
                    player.put("teamId", rs.getInt("team_id"));
                    player.put("team", rs.getString("team"));
                    player.put("name", rs.getString("first_name") + " " + rs.getString("last_name"));
                    player.put("position", rs.getString("position"));
                    player.put("nationality", rs.getString("nationality"));
                    player.put("shirtNumber", rs.getInt("shirt_number"));
                    players.add(player);
                }
            }
        }
        return players;
    }

    public List<Map<String, Object>> leaguesJson() throws SQLException {
        String sql = "SELECT league_id, name, country, current_season FROM league ORDER BY name";
        List<Map<String, Object>> leagues = new ArrayList<>();
        try (Connection connection = config.openConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            while (rs.next()) {
                Map<String, Object> league = new LinkedHashMap<>();
                league.put("id", rs.getInt("league_id"));
                league.put("name", rs.getString("name"));
                league.put("country", rs.getString("country"));
                league.put("currentSeason", rs.getString("current_season"));
                leagues.add(league);
            }
        }
        return leagues;
    }

    public List<Map<String, Object>> standingsJson() throws SQLException {
        String sql = """
            SELECT ROW_NUMBER() OVER (ORDER BY s.points DESC, (s.goals_for - s.goals_against) DESC, s.goals_for DESC) AS rank,
                   t.team_id, l.name AS league, t.name, t.short_name, t.city, t.stadium, t.coach_name,
                   t.founded_year, COALESCE(t.logo_url, '') AS logo_url,
                   s.played, s.won, s.drawn, s.lost, s.goals_for, s.goals_against, s.points
            FROM standing s
            JOIN team t ON t.team_id = s.team_id
            JOIN league l ON l.league_id = t.league_id
            ORDER BY s.points DESC, (s.goals_for - s.goals_against) DESC, s.goals_for DESC
            """;
        List<Map<String, Object>> standings = new ArrayList<>();
        try (Connection connection = config.openConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            while (rs.next()) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("rank", rs.getInt("rank"));
                row.put("team", teamMap(rs));
                row.put("played", rs.getInt("played"));
                row.put("won", rs.getInt("won"));
                row.put("drawn", rs.getInt("drawn"));
                row.put("lost", rs.getInt("lost"));
                row.put("goalsFor", rs.getInt("goals_for"));
                row.put("goalsAgainst", rs.getInt("goals_against"));
                row.put("goalDifference", rs.getInt("goals_for") - rs.getInt("goals_against"));
                row.put("points", rs.getInt("points"));
                standings.add(row);
            }
        }
        return standings;
    }

    public void updateMatchScore(int matchId, int homeScore, int awayScore, String status) throws SQLException {
        String sql = "UPDATE match SET home_score = ?, away_score = ?, status = ? WHERE match_id = ?";
        try (Connection connection = config.openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, homeScore);
            statement.setInt(2, awayScore);
            statement.setString(3, status);
            statement.setInt(4, matchId);
            if (statement.executeUpdate() == 0) throw new GoalPulseServer.HttpError(404, "Match not found");
        }
    }

    public void insertAuditLog(int userId, String action, String tableName, String recordId, String oldValue, String newValue, String ipAddress) throws SQLException {
        String sql = """
            INSERT INTO audit_log (user_id, action, table_name, record_id, old_value, new_value, ip_address)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            """;
        try (Connection connection = config.openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, userId);
            statement.setString(2, action);
            statement.setString(3, tableName);
            statement.setString(4, recordId);
            statement.setString(5, oldValue);
            statement.setString(6, newValue);
            statement.setString(7, ipAddress);
            statement.executeUpdate();
        }
    }

    private List<Map<String, Object>> matchRows(String status) throws SQLException {
        String sql = """
            SELECT m.match_id, m.match_date, m.status, m.home_score, m.away_score, m.venue, m.referee,
                   ht.team_id AS home_id, hl.name AS home_league, ht.name AS home_name, ht.short_name AS home_short_name,
                   ht.city AS home_city, ht.stadium AS home_stadium, ht.coach_name AS home_coach_name,
                   ht.founded_year AS home_founded_year, COALESCE(ht.logo_url, '') AS home_logo_url,
                   at.team_id AS away_id, al.name AS away_league, at.name AS away_name, at.short_name AS away_short_name,
                   at.city AS away_city, at.stadium AS away_stadium, at.coach_name AS away_coach_name,
                   at.founded_year AS away_founded_year, COALESCE(at.logo_url, '') AS away_logo_url
            FROM match m
            JOIN team ht ON ht.team_id = m.home_team_id
            JOIN team at ON at.team_id = m.away_team_id
            JOIN league hl ON hl.league_id = ht.league_id
            JOIN league al ON al.league_id = at.league_id
            """ + (status == null ? "" : " WHERE m.status = ?") + " ORDER BY m.match_date";
        try (Connection connection = config.openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            if (status != null) statement.setString(1, status);
            try (ResultSet rs = statement.executeQuery()) {
                return readMatchRows(rs);
            }
        }
    }

    private List<Map<String, Object>> matchRowsById(int matchId) throws SQLException {
        String sql = """
            SELECT m.match_id, m.match_date, m.status, m.home_score, m.away_score, m.venue, m.referee,
                   ht.team_id AS home_id, hl.name AS home_league, ht.name AS home_name, ht.short_name AS home_short_name,
                   ht.city AS home_city, ht.stadium AS home_stadium, ht.coach_name AS home_coach_name,
                   ht.founded_year AS home_founded_year, COALESCE(ht.logo_url, '') AS home_logo_url,
                   at.team_id AS away_id, al.name AS away_league, at.name AS away_name, at.short_name AS away_short_name,
                   at.city AS away_city, at.stadium AS away_stadium, at.coach_name AS away_coach_name,
                   at.founded_year AS away_founded_year, COALESCE(at.logo_url, '') AS away_logo_url
            FROM match m
            JOIN team ht ON ht.team_id = m.home_team_id
            JOIN team at ON at.team_id = m.away_team_id
            JOIN league hl ON hl.league_id = ht.league_id
            JOIN league al ON al.league_id = at.league_id
            WHERE m.match_id = ?
            """;
        try (Connection connection = config.openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, matchId);
            try (ResultSet rs = statement.executeQuery()) {
                return readMatchRows(rs);
            }
        }
    }

    private List<Map<String, Object>> readMatchRows(ResultSet rs) throws SQLException {
        List<Map<String, Object>> matches = new ArrayList<>();
        while (rs.next()) {
            Map<String, Object> match = new LinkedHashMap<>();
            match.put("id", rs.getInt("match_id"));
            match.put("homeTeam", teamMap(rs, "home_"));
            match.put("awayTeam", teamMap(rs, "away_"));
            match.put("date", rs.getTimestamp("match_date").toLocalDateTime().format(GoalPulseServer.ISO));
            match.put("status", rs.getString("status"));
            match.put("homeScore", rs.getInt("home_score"));
            match.put("awayScore", rs.getInt("away_score"));
            match.put("venue", rs.getString("venue"));
            match.put("referee", rs.getString("referee"));
            matches.add(match);
        }
        return matches;
    }

    private Map<String, Object> teamMap(ResultSet rs) throws SQLException {
        Map<String, Object> team = new LinkedHashMap<>();
        team.put("id", rs.getInt("team_id"));
        team.put("league", rs.getString("league"));
        team.put("name", rs.getString("name"));
        team.put("shortName", rs.getString("short_name"));
        team.put("city", rs.getString("city"));
        team.put("stadium", rs.getString("stadium"));
        team.put("coach", rs.getString("coach_name"));
        team.put("founded", rs.getInt("founded_year"));
        team.put("color", "#33d6ff");
        team.put("logoUrl", rs.getString("logo_url"));
        return team;
    }

    private Map<String, Object> teamMap(ResultSet rs, String prefix) throws SQLException {
        Map<String, Object> team = new LinkedHashMap<>();
        team.put("id", rs.getInt(prefix + "id"));
        team.put("league", rs.getString(prefix + "league"));
        team.put("name", rs.getString(prefix + "name"));
        team.put("shortName", rs.getString(prefix + "short_name"));
        team.put("city", rs.getString(prefix + "city"));
        team.put("stadium", rs.getString(prefix + "stadium"));
        team.put("coach", rs.getString(prefix + "coach_name"));
        team.put("founded", rs.getInt(prefix + "founded_year"));
        team.put("color", "#33d6ff");
        team.put("logoUrl", rs.getString(prefix + "logo_url"));
        return team;
    }

    public record MatchSummary(int id, String homeTeam, String awayTeam, LocalDateTime date, String status, int homeScore, int awayScore) {
    }
}
