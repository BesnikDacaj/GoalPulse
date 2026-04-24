import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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

    public void updateMatchScore(int matchId, int homeScore, int awayScore, String status) throws SQLException {
        String sql = "UPDATE match SET home_score = ?, away_score = ?, status = ? WHERE match_id = ?";
        try (Connection connection = config.openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, homeScore);
            statement.setInt(2, awayScore);
            statement.setString(3, status);
            statement.setInt(4, matchId);
            statement.executeUpdate();
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

    public record MatchSummary(int id, String homeTeam, String awayTeam, LocalDateTime date, String status, int homeScore, int awayScore) {
    }
}
