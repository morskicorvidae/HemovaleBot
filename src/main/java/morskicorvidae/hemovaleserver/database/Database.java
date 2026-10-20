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

        // 4. Guild Config Table
        // Stores data related to the guild and the bot's use in the guild
        statement.execute("""
            CREATE TABLE IF NOT EXISTS guild_config (
                guild_id TEXT,
                join_role_id TEXT,
                podcast_category_id TEXT,
                PRIMARY KEY (guild_id)
            )
        """);

        // 5. Vampire Family Table
        // Stores who is someone is sired to (a sire gains xp when the sired levels up, passively)
        statement.execute("""
            CREATE TABLE IF NOT EXISTS vampire_family (
                guild_id TEXT,
                user_id TEXT,
                sire_id TEXT,
                PRIMARY KEY (guild_id, user_id)
            )
        """);

        // 6. Reaction Roles Table
        // Stores.. well... what do you think? Lmfao. Reaction roles obviously.
        // Use emoji:getFormatted
        statement.execute("""
            CREATE TABLE IF NOT EXISTS reaction_roles (
                guild_id TEXT,
                message_id TEXT,
                emoji TEXT,
                role_id TEXT,
                PRIMARY KEY (guild_id, message_id, emoji)
            )
        """);

        // 7. Vampire Pets Table
        // this is gonna be fucking sick if it can be done
        statement.execute("""
            CREATE TABLE IF NOT EXISTS vampire_pets (
                guild_id TEXT,
                user_id TEXT,
                species TEXT,
                attack INTEGER DEFAULT 10,
                defense INTEGER DEFAULT 10,
                accuracy INTEGER DEFAULT 10,
                PRIMARY KEY (guild_id, user_id)
            )
        """);
    }

    public Connection getConnection() {
        return connection;
    }
}
