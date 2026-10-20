package morskicorvidae.hemovaleserver.database;

import morskicorvidae.hemovaleserver.species.Pet;
import morskicorvidae.hemovaleserver.species.Species;

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

    public LevelRoleHolder getHighestRoleForLevel(String guildId, int level) {
        String sql = """
        SELECT role_id, color_role_id, level_required
        FROM level_roles
        WHERE guild_id = ? AND level_required <= ?
        ORDER BY level_required DESC
        LIMIT 1
    """;
        try (PreparedStatement statement = database.getConnection().prepareStatement(sql)) {
            statement.setString(1, guildId);
            statement.setInt(2, level);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return new LevelRoleHolder(
                            rs.getString("role_id"),
                            rs.getString("color_role_id"),
                            rs.getInt("level_required")
                    );
                }
            }
        } catch (SQLException e) {
            System.err.println("Getting highest role for level FAILURE " + e.getMessage());
        }
        return null;
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

    public String getUserSireId(String guildId, String userId) {
        String sql = "SELECT sire_id FROM vampire_family WHERE guild_id = ? AND user_id = ?";
        try (PreparedStatement statement = database.getConnection().prepareStatement(sql)) {
            statement.setString(1, guildId);
            statement.setString(2, userId);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("sire_id");
                }
            }
        } catch (SQLException e) {
            System.err.println("Getting user sire FAILURE " + e.getMessage());
        }

        // If not found, create the user level and return a holder for it.
        return null;
    }

    public void setUserSireId(String guildId, String userId, String sireId) {
        String sql = """
            INSERT INTO vampire_family (guild_id, user_id, sire_id) 
            VALUES (?, ?, ?) 
            ON CONFLICT(guild_id, user_id) DO UPDATE SET sire_id = excluded.sire_id
        """;
        try (PreparedStatement statement = database.getConnection().prepareStatement(sql)) {
            statement.setString(1, guildId);
            statement.setString(2, userId);
            statement.setString(3, sireId);
            statement.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Setting user sire FAILURE " + e.getMessage());
        }
    }

    public void setReactionRole(String guildId, String messageId, String emoji, String roleId) {
        String sql = """
            INSERT INTO reaction_roles (guild_id, message_id, emoji, role_id) 
            VALUES (?, ?, ?, ?) 
            ON CONFLICT(guild_id, message_id, emoji) DO UPDATE SET role_id = excluded.role_id
        """;
        try (PreparedStatement statement = database.getConnection().prepareStatement(sql)) {
            statement.setString(1, guildId);
            statement.setString(2, messageId);
            statement.setString(3, emoji);
            statement.setString(4, roleId);
            statement.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Setting reaction role FAILURE " + e.getMessage());
        }
    }

    public String getReactionRole(String guildId, String messageId, String emoji) {
        String sql = "SELECT role_id FROM reaction_roles WHERE guild_id = ? AND message_id = ? AND emoji = ?";
        try (PreparedStatement statement = database.getConnection().prepareStatement(sql)) {
            statement.setString(1, guildId);
            statement.setString(2, messageId);
            statement.setString(3, emoji);

            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("role_id");
                }
            }
        } catch (SQLException e) {
            System.err.println("Getting reaction role FAILURE " + e.getMessage());
        }
        return null;
    }

    public void updatePodcastCategory(String guildId, String categoryId) {
        String sql = """
            INSERT INTO guild_config (guild_id, podcast_category_id) 
            VALUES (?, ?) 
            ON CONFLICT(guild_id) DO UPDATE SET podcast_category_id = excluded.podcast_category_id
        """;
        try (PreparedStatement statement = database.getConnection().prepareStatement(sql)) {
            statement.setString(1, guildId);
            statement.setString(2, categoryId);
            statement.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Updating podcast category FAILURE " + e.getMessage());
        }
    }

    public String getPodcastCategory(String guildId) {
        String sql = "SELECT podcast_category_id FROM guild_config WHERE guild_id = ?";
        try (PreparedStatement statement = database.getConnection().prepareStatement(sql)) {
            statement.setString(1, guildId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getString("podcast_category_id");
                }
            }
        } catch (SQLException e) {
            System.err.println("Getting podcast category FAILURE " + e.getMessage());
        }
        return null;
    }

    public Pet getUserPet(String guildId, String userId) {
        String sql = "SELECT species, level, attack, defense, accuracy FROM vampire_pets WHERE guild_id = ? AND user_id = ?";
        try (PreparedStatement statement = database.getConnection().prepareStatement(sql)) {
            statement.setString(1, guildId);
            statement.setString(2, userId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    String speciesStr = resultSet.getString("species");
                    int level = resultSet.getInt("level");
                    int attack = resultSet.getInt("attack");
                    int defense = resultSet.getInt("defense");
                    int accuracy = resultSet.getInt("accuracy");

                    Species species = Species.valueOf(speciesStr.toUpperCase());

                    return new Pet()
                            .setGuildId(guildId)
                            .setUserId(userId)
                            .setSpecies(species)
                            .setLevel(level)
                            .setAttack(attack)
                            .setDefense(defense)
                            .setAccuracy(accuracy);
                }
            }
        } catch (SQLException e) {
            System.err.println("Getting user pet FAILURE " + e.getMessage());
        }
        return null;
    }

    public boolean saveUserPet(Pet pet) {
        String sql = "INSERT INTO vampire_pets (guild_id, user_id, species, level, attack, defense, accuracy) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?) " +
                "ON CONFLICT(guild_id, user_id) DO UPDATE SET " +
                "species = excluded.species, " +
                "level = excluded.level, " +
                "attack = excluded.attack, " +
                "defense = excluded.defense, " +
                "accuracy = excluded.accuracy";

        try (PreparedStatement statement = database.getConnection().prepareStatement(sql)) {
            // 1. Insert values
            statement.setString(1, pet.getGuildId());
            statement.setString(2, pet.getUserId());
            statement.setString(3, pet.getSpecies().name().toLowerCase()); // Matches your getUserPet logic
            statement.setInt(4, pet.getLevel());
            statement.setInt(5, pet.getAttack());
            statement.setInt(6, pet.getDefense());
            statement.setInt(7, pet.getAccuracy());

            // 2. Execute
            statement.executeUpdate();
            return true;

        } catch (SQLException e) {
            System.err.println("Saving user pet FAILURE " + e.getMessage());
            return false;
        }
    }

    public boolean abandonPet(String guildId, String userId) {
        String sql = "DELETE FROM vampire_pets WHERE guild_id = ? AND user_id = ?";
        try (PreparedStatement statement = database.getConnection().prepareStatement(sql)) {
            statement.setString(1, guildId);
            statement.setString(2, userId);

            int rowsAffected = statement.executeUpdate();
            return rowsAffected > 0;
        } catch (SQLException e) {
            System.err.println("Abandoning user pet FAILURE " + e.getMessage());
            return false;
        }
    }

    public void deleteReactionRole(String guildId, String messageId, String emoji) {
        String sql = "DELETE FROM reaction_roles WHERE guild_id = ? AND message_id = ? AND emoji = ?";
        try (PreparedStatement statement = database.getConnection().prepareStatement(sql)) {
            statement.setString(1, guildId);
            statement.setString(2, messageId);
            statement.setString(3, emoji);
            statement.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Deleting reaction role FAILURE " + e.getMessage());
        }
    }

    public List<String> getSiredUsers(String guildId, String sireId) {
        List<String> siredUsers = new ArrayList<>();
        String sql = "SELECT user_id FROM vampire_family WHERE guild_id = ? AND sire_id = ?";
        try (PreparedStatement statement = database.getConnection().prepareStatement(sql)) {
            statement.setString(1, guildId);
            statement.setString(2, sireId);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    siredUsers.add(rs.getString("user_id"));
                }
            }
        } catch (SQLException e) {
            System.err.println("Getting sired users FAILURE " + e.getMessage());
        }
        return siredUsers;
    }

    // Removes the leaving user as a sire from all their children
    public void clearChildrenOfSire(String guildId, String sireId) {
        String sql = "UPDATE vampire_family SET sire_id = NULL WHERE guild_id = ? AND sire_id = ?";
        try (PreparedStatement statement = database.getConnection().prepareStatement(sql)) {
            statement.setString(1, guildId);
            statement.setString(2, sireId);

            int rowsUpdated = statement.executeUpdate();

            if (rowsUpdated > 0) {
                System.out.println("Cleared " + rowsUpdated + " members from sire " + sireId);
            }
        } catch (SQLException e) {
            System.err.println("Clearing children of sire FAILURE " + e.getMessage());
        }
    }

}
