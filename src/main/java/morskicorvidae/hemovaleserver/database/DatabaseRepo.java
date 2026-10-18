package morskicorvidae.hemovaleserver.database;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

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
}
