package Data.database;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.stream.Collectors;
public class DataBaseManager {
    private static final String URL = "jdbc:sqlite:pvz_database.db";

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL);
    }

    public static void initializeDatabase() {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            String schemaSql = new BufferedReader(
                new InputStreamReader(DataBaseManager.class.getResourceAsStream("/schema.sql")))
                .lines().collect(Collectors.joining("\n"));

            String[] queries = schemaSql.split(";");
            for (String query : queries) {
                if (!query.trim().isEmpty()) {
                    stmt.execute(query.trim());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

