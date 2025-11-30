package backend.config;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Handles creation of the SQLite connection and bootstrap of schema.
 */
public final class DatabaseConfig {
    private static final String DB_FILE = "restaurant.db";
    private static final String JDBC_URL = "jdbc:sqlite:" + DB_FILE;

    private DatabaseConfig() {
    }

    public static Connection getConnection() throws SQLException {
        ensureDatabaseFile();
        return DriverManager.getConnection(JDBC_URL);
    }

    private static void ensureDatabaseFile() {
        Path dbPath = Paths.get(DB_FILE);
        boolean dbExists = Files.exists(dbPath);
        try (Connection connection = DriverManager.getConnection(JDBC_URL);
             Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA foreign_keys = ON");
            statement.execute("CREATE TABLE IF NOT EXISTS dishes (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "name TEXT NOT NULL," +
                    "price REAL NOT NULL," +
                    "category TEXT NOT NULL" +
                    ")");
            statement.execute("CREATE TABLE IF NOT EXISTS orders (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "customer_name TEXT NOT NULL," +
                    "status TEXT NOT NULL," +
                    "created_at TEXT NOT NULL," +
                    "total REAL NOT NULL," +
                    "message TEXT," +
                    "payer INTEGER NOT NULL DEFAULT 0" +
                    ")");
            statement.execute("CREATE TABLE IF NOT EXISTS order_items (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "order_id INTEGER NOT NULL," +
                    "dish_id INTEGER NOT NULL," +
                    "quantity INTEGER NOT NULL," +
                    "price REAL NOT NULL," +
                    "FOREIGN KEY(order_id) REFERENCES orders(id) ON DELETE CASCADE," +
                    "FOREIGN KEY(dish_id) REFERENCES dishes(id)" +
                    ")");
            if (dbExists) {
                ensureOrderMessageColumn(connection);
                ensureOrderPayerColumn(connection);
            }
        } catch (SQLException exception) {
            throw new RuntimeException("Unable to initialize database", exception);
        }
    }

    private static void ensureOrderMessageColumn(Connection connection) throws SQLException {
        boolean hasMessageColumn = false;
        try (Statement statement = connection.createStatement();
             java.sql.ResultSet resultSet = statement.executeQuery("PRAGMA table_info(orders)")) {
            while (resultSet.next()) {
                if ("message".equalsIgnoreCase(resultSet.getString("name"))) {
                    hasMessageColumn = true;
                    break;
                }
            }
        }
        if (!hasMessageColumn) {
            try (Statement alterStatement = connection.createStatement()) {
                alterStatement.execute("ALTER TABLE orders ADD COLUMN message TEXT");
            }
        }
    }

    private static void ensureOrderPayerColumn(Connection connection) throws SQLException {
        boolean hasPayerColumn = false;
        try (Statement statement = connection.createStatement();
             java.sql.ResultSet resultSet = statement.executeQuery("PRAGMA table_info(orders)")) {
            while (resultSet.next()) {
                if ("payer".equalsIgnoreCase(resultSet.getString("name"))) {
                    hasPayerColumn = true;
                    break;
                }
            }
        }
        if (!hasPayerColumn) {
            try (Statement alterStatement = connection.createStatement()) {
                alterStatement.execute("ALTER TABLE orders ADD COLUMN payer INTEGER NOT NULL DEFAULT 0");
            }
        }
    }
}
