package Data.database;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Objects;
import java.util.stream.Collectors;

public class DataBaseManager {
    public static final String DB_PATH_PROPERTY = "pvz.db.path";
    private static final String DEFAULT_DB_PATH = "pvz_database.db";

    private DataBaseManager() {
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(getJdbcUrl());
    }

    public static String getDatabasePath() {
        return System.getProperty(DB_PATH_PROPERTY, DEFAULT_DB_PATH);
    }

    private static String getJdbcUrl() {
        return "jdbc:sqlite:" + getDatabasePath();
    }

    public static void initializeDatabase() {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {

            String schemaSql = new BufferedReader(
                    new InputStreamReader(
                            Objects.requireNonNull(
                                    DataBaseManager.class
                                            .getResourceAsStream("/schema.sql")
                            )
                    )
            ).lines().collect(Collectors.joining("\n"));

            String[] queries = schemaSql.split(";");

            for (String query : queries) {
                if (!query.trim().isEmpty()) {
                    stmt.execute(query.trim());
                }
            }
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }
}