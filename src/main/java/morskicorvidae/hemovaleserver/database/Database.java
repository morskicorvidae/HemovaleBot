package morskicorvidae.hemovaleserver.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;


public class Database {

    private Connection connection;

    public Database() {
        try {
            connection = DriverManager.getConnection("jdbc:sqlite:database.db");

            try (Statement statement = connection.createStatement()) {
                statement.execute("PRAGMA journal_mode=WAL;");
                createTables(statement);
            }

            System.out.println("Connecting to database SUCCESS");
        } catch (SQLException e) {
            System.err.println("Connecting to database FAILURE " + e.getMessage());
        }
    }

    private void createTables(Statement statement) throws SQLException {

        // 1. Level Roles Table
        // Stores roles and their levels, as well as the (duplicated) colour role for that role
        statement.execute("""
            CREATE TABLE IF NOT EXISTS level_roles (
                guild_id TEXT,
                role_id TEXT,
                color_role_id TEXT,
                level_required INTEGER,
                PRIMARY KEY (guild_id, role_id)
            )
        """);

        // 2. User Levels Table
        // Stores the levels of users
        statement.execute("""
            CREATE TABLE IF NOT EXISTS user_levels (
                guild_id TEXT,
                user_id TEXT,
                xp INTEGER DEFAULT 0,
                level INTEGER DEFAULT 0,
                PRIMARY KEY (guild_id, user_id)
            )
        """);

        // 3. User Preferences Table
        // Stores the preferences of users
        statement.execute("""
            CREATE TABLE IF NOT EXISTS user_preferences (
                guild_id TEXT,
                user_id TEXT,
                preferred_role_id TEXT,
                PRIMARY KEY (guild_id, user_id)
            )
        """);
    }

    public Connection getConnection() {
        return connection;
    }
}
