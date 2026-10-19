package morskicorvidae.hemovaleserver;

import morskicorvidae.hemovaleserver.database.Database;
import morskicorvidae.hemovaleserver.database.DatabaseRepo;
import morskicorvidae.hemovaleserver.userprofile.LevelCalculations;
import morskicorvidae.hemovaleserver.userprofile.ProfileImageGenerator;
import morskicorvidae.hemovaleserver.utils.EmbedManager;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

public class Main extends ListenerAdapter {

    public static Database db;
    public static DatabaseRepo dbRepo;

    public static void main(String[] args) {
        String token = System.getenv("DISCORD_TOKEN");

        if (token == null) {
            System.err.println("DISCORD_TOKEN environment variable is missing!");
            return;
        }

        db = new Database();
        dbRepo = new DatabaseRepo(db);

        JDA jda = JDABuilder.createDefault(token)
                .enableIntents(GatewayIntent.MESSAGE_CONTENT)
                .addEventListeners(new Main())
                .build();
    }

    @Override
    public void onReady(ReadyEvent event) {
        System.out.println("Logged in as: " + event.getJDA().getSelfUser().getName());

        SlashCommandData roleCommand = Commands.slash("role", "Commands involving the levelling systems and roles");
        SubcommandData roleAddCommand = new SubcommandData("add", "Allows you to add a role to the levelling system. | /role add [role] [level]");
        roleAddCommand.addOption(OptionType.ROLE, "role", "The role to be added to the levelling system.", true);
        roleAddCommand.addOption(OptionType.INTEGER, "level", "The level at which the user gains the role.", true);

        SubcommandData roleUpdateCommand = new SubcommandData("update", "Updates the role system. | /role update");
        SubcommandData roleUseColorCommand = new SubcommandData("usecolor", "Allows you to use the color of a role you have unlocked. | /role usecolor [role]");
        roleUseColorCommand.addOption(OptionType.ROLE, "role", "The unlocked role you wish to use the color of.", true);

        SubcommandData roleListCommand = new SubcommandData("list", "Lists the roles currently added to the roles system. | /role list");

        roleCommand.addSubcommands(roleAddCommand, roleUpdateCommand, roleUseColorCommand, roleListCommand);

        SlashCommandData joinroleCommand = Commands.slash("joinrole", "Pick a role which all members get when joining. | /joinrole [role]");
        joinroleCommand.addOption(OptionType.ROLE, "role", "The role which all members get when joining.", true);

        SlashCommandData profileCommand = Commands.slash("profile", "View your profile card.");
        profileCommand.addOption(OptionType.USER, "user", "The user whose profile you wish to view");

        for(Guild guild : event.getJDA().getGuilds()) {
            guild.updateCommands().addCommands(roleCommand, joinroleCommand, profileCommand).queue();
        }
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        Guild guild = event.getGuild();

        if(event.getAuthor().isBot()) return;

        User user = event.getAuthor();
        String userId = user.getId();

        int xpToGive = LevelCalculations.getExpFromMessage(event.getMessage().getContentRaw());

        DatabaseRepo.UserLevelHolder holder = dbRepo.getUserLevel(guild.getId(), userId);
        int totalXp = holder.xp() + xpToGive;
        int nextLevelXp = LevelCalculations.getXPRequiredForLevel(holder.level() + 1);

        int level = holder.level();

        System.out.println("User (Level " + level + ") has " + totalXp + " XP; next level XP is " + nextLevelXp + "!");

        if(totalXp >= nextLevelXp) {
            int remainder = totalXp - nextLevelXp;
            level += 1;
            totalXp = remainder;

            System.out.println("Calculated " + level + " with " + totalXp + " xp!");

            handleLevelUp(event, guild, event.getMember(), level, totalXp);

            System.out.println("handleLevelUp should've been run");
        }

        dbRepo.updateUserLevel(guild.getId(), userId, totalXp, level);

        System.out.println("Updated user level in database.");
    }

    public void handleLevelUp(MessageReceivedEvent event, Guild guild, Member member, int newLevel, int newXp) {
        System.out.println("handle level up IS being run actually");
        event.getMessage().reply(member.getAsMention() + " has ascended to **Level " + newLevel + "**!").queue();

        // 2. FIND NEW ROLES
        List<DatabaseRepo.LevelRoleHolder> allRoles = dbRepo.getAllLevelRoles(guild.getId());

        Role roleToGive = null;
        for (DatabaseRepo.LevelRoleHolder holder : allRoles) {
            if (newLevel >= holder.levelRequirement()) {
                Role role = guild.getRoleById(holder.roleId());

                if (role != null && !member.getRoles().contains(role)) {
                    roleToGive = role;
                }
            }
        }

        if (roleToGive != null) {
            guild.addRoleToMember(member, roleToGive).queue();
        }
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        switch(event.getName()) {
            case "role":
                handleRoleCommand(event);
                break;
            case "joinrole": {
                Guild guild = event.getGuild();
                Role joinRole = event.getInteraction().getOption("role").getAsRole();

                if (!event.getMember().hasPermission(Permission.MANAGE_ROLES)) {
                    event.replyEmbeds(EmbedManager.error("You lack permissions!", "Required permission: MANAGE_ROLES")).queue();
                    return;
                }

                dbRepo.updateJoinRole(guild.getId(), joinRole.getId());

                MessageEmbed reply = EmbedManager.success("Set Join Role Successfully", "Users will now receive " + joinRole.getAsMention() + " upon joining the server!");
                event.replyEmbeds(reply).queue();
                break;
            }
            case "profile": {
                handleProfileCommand(event);
            }
            default: // Shouldn't happen!
                MessageEmbed reply = EmbedManager.error("Not implemented yet", "Command has not yet been implemented.");
                event.replyEmbeds(reply).queue();
        }
    }

    private void handleProfileCommand(SlashCommandInteractionEvent event) {
        Guild guild = Objects.requireNonNull(event.getGuild());

        Member eventMember = event.getMember();

        OptionMapping option = event.getOption("user");
        if(option != null) {
            eventMember = event.getOption("user").getAsMember();
        }

        Member member = eventMember; // get around the lambda stuff because why not

        // 1. GET INFORMATION ABOUT THE USER
        DatabaseRepo.UserLevelHolder userData = dbRepo.getUserLevel(guild.getId(), member.getId());
        int level = userData.level();
        int xp = userData.xp();

        // 2. GET XP NEEDED FOR NEXT LEVEL
        int needed = LevelCalculations.getXPRequiredForLevel(level + 1);

        event.deferReply().queue();

        // 3. CREATE THREAD SO THAT BOT DOES NOT FREEZE CREATING IMAGE

        new Thread(() -> {
            try {
                byte[] imageBytes = ProfileImageGenerator.generateProfileCard(
                        member.getEffectiveName(),
                        member.getEffectiveAvatarUrl(),
                        level,
                        xp,
                        needed
                );

                // 4. SEND THE ACTUAL IMAGE
                event.getHook().sendFiles(
                        net.dv8tion.jda.api.utils.FileUpload.fromData(imageBytes, "profile.png")
                ).queue();

            } catch (Exception e) {
                e.printStackTrace();
                event.getHook().sendMessageEmbeds(EmbedManager.error("Error", "Failed to generate profile card.")).queue();
            }
        }).start();
    }

    private void handleRoleCommand(SlashCommandInteractionEvent event) {
        switch (event.getSubcommandName()) {
            case "add" -> handleRoleAddCommand(event);
            case "usecolor" -> handleRoleUseColorCommand(event);
            case "list" -> handleRoleListCommand(event);
            case "update" -> handleRoleUpdateCommand(event);
        }
    }

    private void handleRoleUpdateCommand(SlashCommandInteractionEvent event) {
        Guild guild = Objects.requireNonNull(event.getGuild());
        List<DatabaseRepo.LevelRoleHolder> dbRoles = dbRepo.getAllLevelRoles(guild.getId());

        // STEP 1. PERMISSIONS + ENSURE THERE ACTUALLY ARE ROLES
        if (!event.getMember().hasPermission(Permission.MANAGE_ROLES)) {
            event.replyEmbeds(EmbedManager.error("You lack permissions!", "Required permission: MANAGE_ROLES")).queue();
            return;
        }

        if (dbRoles.isEmpty()) {
            event.replyEmbeds(EmbedManager.neutral("No Roles", "There aren't any roles, so updating is not necessary.")).queue();
            return;
        }

        Role botHighestRole = guild.getSelfMember().getRoles().get(0);

        // STEP 2. BASICALLY JUST DO THE UPDATE
        event.deferReply().queue();

        List<String> changedCosmeticColors = new ArrayList<>();
        List<String> cosmeticRolesRecreated = new ArrayList<>();
        List<String> rolesDeletedDb = new ArrayList<>();

        for (DatabaseRepo.LevelRoleHolder roleHolder : dbRoles) {
            Role role = guild.getRoleById(roleHolder.roleId());
            Role colorRole = guild.getRoleById(roleHolder.colorRoleId());

            if (role == null) {
                // Original role has been deleted
                boolean colorRoleNull = colorRole == null;
                if(!colorRoleNull) colorRole.delete().queue();

                dbRepo.deleteLevelRole(guild.getId(), roleHolder.roleId());
                dbRepo.deletePreferencesForRole(guild.getId(), roleHolder.roleId());
                rolesDeletedDb.add("Deleted internal data for missing role ID: `" + roleHolder.roleId() + "`!");

                if (!colorRoleNull) {
                    rolesDeletedDb.add("+ Deleted vestigial color role for missing ID: `" + roleHolder.roleId() + "`");
                }
                continue;
            }

            if (colorRole == null) {
                // Color role has been deleted but level role remains; recreate it
                guild.createRole()
                        .setName(toHex(role.getColor()))
                        .setColor(role.getColor())
                        .setPermissions(0L)
                        .setMentionable(false)
                        .setHoisted(false)
                        .queue(cosmeticRole -> {
                            guild.modifyRolePositions().selectPosition(cosmeticRole).moveBelow(botHighestRole).queue();
                            dbRepo.updateColorRoleId(guild.getId(), role.getId(), cosmeticRole.getId());
                        });
                cosmeticRolesRecreated.add("Re-created cosmetic role for " + role.getAsMention());
                continue; // Skip color check since we literally just made it with the right color when creating it lol
            }

            // Check for cosmetic roles with the wrong color
            if (!Objects.equals(colorRole.getColorRaw(), role.getColorRaw())) {
                colorRole.getManager().setColor(role.getColor()).queue();
                colorRole.getManager().setName(toHex(role.getColor())).queue();
                changedCosmeticColors.add(colorRole.getAsMention() + " -> " + role.getAsMention());
            }
        }

        // STEP 3. BUILD EMBED
        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("Update Log")
                .setColor(EmbedManager.SUCCESS);

        if (changedCosmeticColors.isEmpty()) embed.addField("Cosmetic Roles", "No color changes were necessary.", false);
        else embed.addField("Cosmetic Roles | Changed Color", String.join("\n", changedCosmeticColors), false);

        if (rolesDeletedDb.isEmpty()) embed.addField("Deleted Roles/Data", "No deletions were necessary.", false);
        else embed.addField("Deleted Roles/Data", String.join("\n", rolesDeletedDb), false);

        if (cosmeticRolesRecreated.isEmpty()) embed.addField("Re-created Cosmetic Roles", "No re-creations were necessary.", false);
        else embed.addField("Re-created Cosmetic Roles", String.join("\n", cosmeticRolesRecreated), false);

        event.getHook().editOriginalEmbeds(embed.build()).queue();
    }

    private void handleRoleListCommand(SlashCommandInteractionEvent event) {
        // Not sure why Intellij is saying to Objects.requireNonNull here. Perhaps user might use in DMs. Might need to fix later time.
        List<DatabaseRepo.LevelRoleHolder> dbRoles = dbRepo.getAllLevelRoles(Objects.requireNonNull(event.getGuild()).getId());

        // STEP 1. ENSURE THERE ACTUALLY ARE ROLES

        if (dbRoles.isEmpty()) {
            event.replyEmbeds(EmbedManager.neutral("No Roles", "There aren't any roles yet.")).queue();
            return;
        }

        // STEP 2. PRINT A NICE AND PRETTY LIST OF THEM

        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("Leveling Roles")
                .setColor(EmbedManager.NEUTRAL);

        for (DatabaseRepo.LevelRoleHolder data : dbRoles) {
            Role role = event.getGuild().getRoleById(data.roleId());
            String roleMention = (role != null) ? role.getAsMention() : "*(Invalid?)*";
            embed.addField("Level " + data.levelRequirement(), roleMention, true);
        }

        event.replyEmbeds(embed.build()).queue();
    }

    private void handleRoleUseColorCommand(SlashCommandInteractionEvent event) {
        Guild guild = Objects.requireNonNull(event.getGuild());
        User user = event.getUser();
        Member member = event.getMember();
        Role roleWanted = event.getOption("role").getAsRole();

        System.out.println("Executing /role usecolor [" + roleWanted.getId() + "/ '" + roleWanted.getName() + "']");

        // STEP 1. GET THE USER'S LEVEL
        DatabaseRepo.UserLevelHolder level = dbRepo.getUserLevel(guild.getId(), user.getId());

        // SET 2. GET THE REQUIRED LEVEL FOR THE ROLE

        // replace all this with an sql command (in DatabaseRepo.java) later, genuinely

        List<DatabaseRepo.LevelRoleHolder> roleHolders = dbRepo.getAllLevelRoles(guild.getId());
        DatabaseRepo.LevelRoleHolder roleHolder = null;
        List<String> cosmeticRoleIds = new ArrayList<>();

        for(DatabaseRepo.LevelRoleHolder holder : roleHolders) {
            if(Objects.equals(holder.roleId(), roleWanted.getId())) {
                roleHolder = holder;
            }
            cosmeticRoleIds.add(holder.colorRoleId());
        }

        if(roleHolder == null) {
            event.replyEmbeds(EmbedManager.error("Role Not Applicable", "The role you selected is not a level role.")).queue();
            return;
        }

        int requiredLevel = roleHolder.levelRequirement();

        // STEP 3. COMPARE

        if(requiredLevel > level.level()) {
            event.replyEmbeds(EmbedManager.error("You Don't Have This Role", "The role you selected is of a higher level (" + requiredLevel + ") than you are (your current level is " + level.level() + "!)")).queue();
            return;
        }

        // Otherwise, let the user have that preference.
        // But first delete all color roles they have.

        assert member != null;
        for(Role role : member.getRoles()) {
            if(cosmeticRoleIds.contains(role.getId())) {
                guild.removeRoleFromMember(member, role).queue();
            }
        }

        Role colorRole = Objects.requireNonNull(guild.getRoleById(roleHolder.colorRoleId()));
        guild.addRoleToMember(member, colorRole).queue();
        dbRepo.setUserPreference(guild.getId(), user.getId(), roleHolder.roleId());

        event.replyEmbeds(EmbedManager.success("Assigned Color", "You have been assigned the cosmetic color role " + colorRole.getAsMention())).queue();
    }

    @Override
    public void onGuildMemberJoin(GuildMemberJoinEvent event) {
        Guild guild = event.getGuild();
        Member member = event.getMember();

        String roleId = dbRepo.getJoinRole(guild.getId());

        if(roleId != null) {
            Role role = guild.getRoleById(roleId);
            guild.addRoleToMember(member, role).queue();
        }
    }

    private void handleRoleAddCommand(SlashCommandInteractionEvent event) {
        //MessageEmbed reply = EmbedManager.error("Not implemented yet", "Command has not yet been implemented.");
        //event.replyEmbeds(reply).queue();

        // STEP 1. CHECK FOR PERMISSIONS
        Guild guild = event.getGuild();
        Member botMember = guild.getSelfMember();

        Role levelRole = event.getOption("role").getAsRole();
        int levelRequirement = event.getOption("level").getAsInt();

        if(!event.getMember().hasPermission(Permission.MANAGE_ROLES)) {
            event.replyEmbeds(EmbedManager.error("You lack permissions!", "You do not have permission to use this command. Required permission: MANAGE_ROLES")).queue();
            return;
        }

        if (!botMember.hasPermission(Permission.MANAGE_ROLES)) {
            MessageEmbed reply = EmbedManager.error("Bot does not have correct permissions setup", "Please ensure the bot has the MANAGE_ROLES permission.");
            event.replyEmbeds(reply).setEphemeral(true).queue();
            return;
        }

        Role botHighestRole = botMember.getRoles().get(0);

        if(levelRole.getPosition() > botHighestRole.getPosition()) {
            MessageEmbed reply = EmbedManager.error("Bot does not have correct role setup", "Please ensure the bot's highest role is above the role " + levelRole.getAsMention() + ".");
            event.replyEmbeds(reply).setEphemeral(true).queue();
            return;
        }

        // STEP 2. ENSURE ROLE HAS COLOR

        if(levelRole.getColor() == null) {
            MessageEmbed reply = EmbedManager.error("Role does not have a color", "Please ensure the role " + levelRole.getAsMention() + "has a color and try again.");
            event.replyEmbeds(reply).setEphemeral(true).queue();
            return;
        }

        // STEP 3. DUPLICATE ROLE INTO COLOR ROLE

        event.deferReply().queue(); // Defer the reply before creating a new role

        botMember.getGuild().createRole()
                .setName(toHex(levelRole.getColor()))
                .setColor(levelRole.getColor())
                .setPermissions(0L)
                .setMentionable(false)
                .setHoisted(false)
                .queue(cosmeticRole -> { // MOVE ROLE BELOW BOT ROLE
                    guild.modifyRolePositions()
                            .selectPosition(cosmeticRole)
                            .moveBelow(botHighestRole)
                            .queue();

                    dbRepo.addLevelRole(guild.getId(), levelRole.getId(), cosmeticRole.getId(), levelRequirement);

                    MessageEmbed success = new EmbedBuilder()
                            .setTitle("Role Added")
                            .setDescription("Successfully added " + levelRole.getAsMention() + "!")
                            .addField("Cosmetic role has also been added!", cosmeticRole.getAsMention(), false)
                            .setColor(EmbedManager.SUCCESS)
                            .build();

                    event.getHook().editOriginalEmbeds(success).queue();
                });
    }

    public static String toHex(Color color) {
        return String.format("#%06X", color.getRGB() & 0xFFFFFF);
    }
}