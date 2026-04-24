import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConfig {
    private final String url;
    private final String username;
    private final String password;

    public DatabaseConfig() {
        this.url = System.getenv().getOrDefault("GOALPULSE_DB_URL", "jdbc:postgresql://localhost:5432/goalpulse");
        this.username = System.getenv().getOrDefault("GOALPULSE_DB_USER", "goalpulse");
        this.password = System.getenv().getOrDefault("GOALPULSE_DB_PASSWORD", "goalpulse");
    }

    public Connection openConnection() throws SQLException {
        return DriverManager.getConnection(url, username, password);
    }
}
