package morskicorvidae.hemovaleserver.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class DatabaseRepo {

    private final Database database;

    public DatabaseRepo(Database database) {
        this.database = database;
    }

    public void addLevelRole(String guildId, String roleId, String colorRoleId, int levelRequirement) {
        String sql = "INSERT INTO level_roles (guild_id, role_id, color_role_id, level_required) VALUES (?, ?, ?, ?)";
        try (PreparedStatement statement = database.getConnection().prepareStatement(sql)) {
            statement.setString(1, guildId);
            statement.setString(2, roleId);
            statement.setString(3, colorRoleId);
            statement.setInt(4, levelRequirement);
            statement.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Adding level role (DatabaseRepo) FAILURE " + e.getMessage());
        }
    }

    // Get the color role id from the level role id
    public String getColorRoleId(String guildId, String roleId) {
        String sql = "SELECT color_role_id FROM level_roles WHERE guild_id = ? AND role_id = ?";
        try (PreparedStatement statement = database.getConnection().prepareStatement(sql)) {
            statement.setString(1, guildId);
            statement.setString(2, roleId);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("color_role_id");
                }
            }
        } catch (SQLException e) {
            System.err.println("Getting color role (DatabaseRepo) FAILURE " + e.getMessage());
        }
        return null;
    }

    public record LevelRoleHolder(String roleId, String colorRoleId, int levelRequirement) {}
    public record UserLevelHolder(int xp, int level) {}

    public List<LevelRoleHolder> getAllLevelRoles(String guildId) {
        List<LevelRoleHolder> roles = new ArrayList<>();
        // Order by level_required ascending so that they're printed in a very pretty and satisfying way (lovely jubbly)
        String sql = "SELECT role_id, color_role_id, level_required FROM level_roles WHERE guild_id = ? ORDER BY level_required ASC";
        try (PreparedStatement statement = database.getConnection().prepareStatement(sql)) {
            statement.setString(1, guildId);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    roles.add(new LevelRoleHolder(
                            rs.getString("role_id"),
                            rs.getString("color_role_id"),
                            rs.getInt("level_required")
                    ));
                }
            }
        } catch (SQLException e) {
            System.err.println("Getting all level roles FAILURE " + e.getMessage());
        }
        return roles;
    }

    public void deleteLevelRole(String guildId, String roleId) {
        String sql = "DELETE FROM level_roles WHERE guild_id = ? AND role_id = ?";
        try (PreparedStatement statement = database.getConnection().prepareStatement(sql)) {
            statement.setString(1, guildId);
            statement.setString(2, roleId);
            statement.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Deleting level role FAILURE " + e.getMessage());
        }
    }

    public void updateColorRoleId(String guildId, String roleId, String newColorRoleId) {
        String sql = "UPDATE level_roles SET color_role_id = ? WHERE guild_id = ? AND role_id = ?";
        try (PreparedStatement statement = database.getConnection().prepareStatement(sql)) {
            statement.setString(1, newColorRoleId);
            statement.setString(2, guildId);
            statement.setString(3, roleId);
            statement.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Updating color role id FAILURE " + e.getMessage());
        }
    }

    public void setUserPreference(String guildId, String userId, String preferredRoleId) {
        String sql = """
        INSERT INTO user_preferences (guild_id, user_id, preferred_role_id) 
        VALUES (?, ?, ?) 
        ON CONFLICT(guild_id, user_id) DO UPDATE SET preferred_role_id = excluded.preferred_role_id""";
        try (PreparedStatement statement = database.getConnection().prepareStatement(sql)) {
            statement.setString(1, guildId);
            statement.setString(2, userId);
            statement.setString(3, preferredRoleId);
            statement.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Setting user preference FAILURE " + e.getMessage());
        }
    }

    public String getUserPreference(String guildId, String userId) {
        String sql = "SELECT preferred_role_id FROM user_preferences WHERE guild_id = ? AND user_id = ?";
        try (PreparedStatement statement = database.getConnection().prepareStatement(sql)) {
            statement.setString(1, guildId);
            statement.setString(2, userId);

            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("preferred_role_id");
                }
            }
        } catch (SQLException e) {
            System.err.println("Getting user preference FAILURE " + e.getMessage());
        }
        return null;
    }

    public void deletePreferencesForRole(String guildId, String roleId) {
        String sql = "DELETE FROM user_preferences WHERE guild_id = ? AND preferred_role_id = ?";
        try (PreparedStatement statement = database.getConnection().prepareStatement(sql)) {
            statement.setString(1, guildId);
            statement.setString(2, roleId);
            statement.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Deleting preferences for role FAILURE " + e.getMessage());
        }
    }

    public UserLevelHolder getUserLevel(String guildId, String userId) {
        String sql = "SELECT xp, level FROM user_levels WHERE guild_id = ? AND user_id = ?";
        try (PreparedStatement statement = database.getConnection().prepareStatement(sql)) {
            statement.setString(1, guildId);
            statement.setString(2, userId);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return new UserLevelHolder(rs.getInt("xp"), rs.getInt("level"));
                }
            }
        } catch (SQLException e) {
            System.err.println("Getting user level FAILURE " + e.getMessage());
        }

        // If not found, create the user level and return a holder for it.
        updateUserLevel(guildId, userId, 0, 0);
        return new UserLevelHolder(0, 0);
    }

    public void updateUserLevel(String guildId, String userId, int newXp, int newLevel) {
        String sql = """
            INSERT INTO user_levels (guild_id, user_id, xp, level) 
            VALUES (?, ?, ?, ?) 
            ON CONFLICT(guild_id, user_id) DO UPDATE SET xp = excluded.xp, level = excluded.level
        """;
        try (PreparedStatement statement = database.getConnection().prepareStatement(sql)) {
            statement.setString(1, guildId);
            statement.setString(2, userId);
            statement.setInt(3, newXp);
            statement.setInt(4, newLevel);
            statement.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Updating user level FAILURE " + e.getMessage());
        }
    }

    public void updateJoinRole(String guildId, String joinRoleId) {
        String sql = """
            INSERT INTO guild_config (guild_id, join_role_id) 
            VALUES (?, ?) 
            ON CONFLICT(guild_id) DO UPDATE SET join_role_id = excluded.join_role_id
        """;
        try (PreparedStatement statement = database.getConnection().prepareStatement(sql)) {
            statement.setString(1, guildId);
            statement.setString(2, joinRoleId);
            statement.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Updating guild join role FAILURE " + e.getMessage());
        }
    }

    public String getJoinRole(String guildId) {
        String sql = """
        SELECT join_role_id FROM guild_config WHERE guild_id = ?
        """;
        try (PreparedStatement statement = database.getConnection().prepareStatement(sql)) {
            statement.setString(1, guildId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getString("join_role_id");
                }
            }
        } catch (SQLException e) {
            System.err.println("Getting guild join role FAILURE " + e.getMessage());
        }
        return null;
    }

}
