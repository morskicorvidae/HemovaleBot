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
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRemoveEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

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

        SlashCommandData profileCommand = Commands.slash("profile", "View your, or another members, profile card.");
        profileCommand.addOption(OptionType.USER, "user", "The user whose profile you wish to view");

        SlashCommandData vampireCommand = Commands.slash("vampire", "Vampire family related commands");

        SubcommandData sireSubcommand = new SubcommandData("sire", "Request to sire another user (passively gain experience from them) | /vampire sire [user]");
        sireSubcommand.addOption(OptionType.USER, "user", "The user you wish to sire", true);
        SubcommandData lineageCommand = new SubcommandData("lineage", "View your own lineage within this guild.");

        SubcommandData unsireSubcommand = new SubcommandData("unsire", "No longer sire someone you currently sire. | /vampire unsire [user]");
        unsireSubcommand.addOption(OptionType.USER, "user", "The user you wish to no longer sire", true);

        SubcommandData siringSubcommand = new SubcommandData("siring", "Lists all the users you are currently siring in this guild. | /vampire siring");

        vampireCommand.addSubcommands(sireSubcommand, lineageCommand, unsireSubcommand, siringSubcommand);

        SlashCommandData adminCommand = Commands.slash("admin", "Admin related commands for server management");

        SubcommandData nukeCommand = new SubcommandData("nuke", "Bulk delete at most 100 messages within this channel. | /admin nuke [amount]");
        nukeCommand.addOption(OptionType.INTEGER, "amount", "Number of messages to delete (1-100).", true);

        SubcommandData reactroleCommand = new SubcommandData("reactrole", "Create a reaction role. | /admin reactrole [message id] [emoji] [role]");
        reactroleCommand.addOption(OptionType.STRING, "messageid", "The id of the message.", true);
        reactroleCommand.addOption(OptionType.STRING, "emoji", "The emoji the role will be given from.", true);
        reactroleCommand.addOption(OptionType.ROLE, "role", "The role to be given.", true);

        adminCommand.addSubcommands(nukeCommand, reactroleCommand);

        for(Guild guild : event.getJDA().getGuilds()) {
            guild.updateCommands().addCommands(roleCommand, joinroleCommand, profileCommand, vampireCommand, adminCommand).queue();
        }
    }

    @Override
    public void onMessageReactionAdd(MessageReactionAddEvent event) {
        if (event.getUser().isBot() || !event.isFromGuild()) return;

        Guild guild = event.getGuild();
        String guildId = guild.getId();
        String messageId = event.getMessageId();

        Emoji emoji = event.getEmoji();

        String emojiString = emoji.getFormatted();

        String roleId = dbRepo.getReactionRole(guildId, messageId, emojiString);

        if(roleId == null) return;

        Member member = event.getMember();
        Role role = guild.getRoleById(roleId);

        if(member == null) return;
        if(role == null) return;

        guild.addRoleToMember(member, role).queue();
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

        String sireId = dbRepo.getUserSireId(guild.getId(), member.getId());

        if(sireId != null) {
            int xpToGive = newLevel * 15;

            DatabaseRepo.UserLevelHolder holder = dbRepo.getUserLevel(guild.getId(), sireId);
            int totalXp = holder.xp() + xpToGive;
            int nextLevelXp = LevelCalculations.getXPRequiredForLevel(holder.level() + 1);

            int level = holder.level();

            if(totalXp >= nextLevelXp) {
                int remainder = totalXp - nextLevelXp;
                level += 1;
                totalXp = remainder;

                handleLevelUp(event, guild, guild.getMemberById(sireId), level, totalXp);
            }

            dbRepo.updateUserLevel(guild.getId(), sireId, totalXp, level);
        }
    }

    @Override
    public void onGuildMemberRemove(GuildMemberRemoveEvent event) {
        dbRepo.clearChildrenOfSire(event.getGuild().getId(), event.getMember().getUser().getId());
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
            case "vampire": {
                handleVampireCommand(event);
                break;
            }
            case "profile": {
                handleProfileCommand(event);
                break;
            }
            case "admin": {
                handleAdminCommand(event);
                break;
            }
            default: // Shouldn't happen!
                MessageEmbed reply = EmbedManager.error("Not implemented yet", "Command has not yet been implemented.");
                event.replyEmbeds(reply).queue();
        }
    }

    private void handleAdminCommand(SlashCommandInteractionEvent event) {
        switch(event.getSubcommandName()) {
            case "nuke" -> handleAdminNukeCommand(event);
            case "reactrole" -> handleAdminReactroleCommand(event);
        }
    }

    private void handleAdminReactroleCommand(SlashCommandInteractionEvent event) {
        Guild guild = Objects.requireNonNull(event.getGuild());
        Member member = Objects.requireNonNull(event.getMember());

        // 1. ENSURE PERMISSIONS FOR ADMIN COMMAND
        if (!member.hasPermission(Permission.MANAGE_ROLES) || !member.hasPermission(Permission.MESSAGE_MANAGE)) {
            event.replyEmbeds(EmbedManager.error("You Lack Permissions", "You must have both MANAGE_ROLES and MESSAGE_MANAGE permissions to create reaction roles.")).setEphemeral(true).queue();
            return;
        }

        if (!guild.getSelfMember().hasPermission(Permission.MESSAGE_MANAGE, Permission.MESSAGE_ADD_REACTION)) {
            event.replyEmbeds(EmbedManager.error("Bot Missing Permissions", guild.getSelfMember().getAsMention() + " needs the MESSAGE_MANAGE and MESSAGE_ADD_REACTION permissions to create reaction roles.")).setEphemeral(true).queue();
            return;
        }

        // 2. GET ONLY NUMBERS FROM MESSAGE ID GIVEN + ROLE AND EMOJI
        String rawMessageId = event.getOption("messageid").getAsString();
        String messageId = rawMessageId.replaceAll("[^0-9]", "");

        String emojiString = event.getOption("emoji").getAsString();
        Role role = event.getOption("role").getAsRole();

        // 3. ENSURE ROLE CAN BE GIVEN (ROLE POSITIONS)

        if (role.getPosition() >= guild.getSelfMember().getRoles().get(0).getPosition()) {
            event.replyEmbeds(EmbedManager.error("Cannot Assign Role", guild.getSelfMember().getAsMention() + "'s highest role must be above " + role.getAsMention() + " in order to use this feature.")).setEphemeral(true).queue();
            return;
        }

        // 4. ACTUALLY GET THE EMOJI AS AN EMOJI

        Emoji emoji;

        try {
            emoji = Emoji.fromFormatted(emojiString);
        } catch (Exception e) {
            event.replyEmbeds(EmbedManager.error("Emoji Error", "The emoji given `" + emojiString + "` seems to be invalid. Please try a different emoji.")).setEphemeral(true).queue();
            return;
        }

        event.getChannel().retrieveMessageById(messageId).queue(message -> {
            // 5. SAVE REACTION ROLE TO THE DATABASE
            // use emoji.getFormatted just in case something fucks up somewhere along the line
            dbRepo.setReactionRole(guild.getId(), messageId, emoji.getFormatted(), role.getId());

            message.addReaction(emoji).queue(
                    success -> { // if works
                        event.getHook().editOriginalEmbeds(EmbedManager.success("Reaction Role Created", "The reaction role `" + emojiString + " -> " + role.getAsMention() + "` has been created successfully.")).queue();
                    },
                    error -> { // if not works
                        event.getHook().editOriginalEmbeds(EmbedManager.error("Reaction Failed", "The database was updated, but I couldn't add the reaction to the message. Try adding it yourself and it should work.")).queue();
                    }
            );
        }, error -> {
            event.getHook().editOriginalEmbeds(EmbedManager.error("Message Not Found", "The message was not found in the currrent channel. Please run the command in the same channel the reaction role message is located in.")).queue();
        });

    }

    private void handleAdminNukeCommand(SlashCommandInteractionEvent event) {
        Guild guild = Objects.requireNonNull(event.getGuild());
        Member member = Objects.requireNonNull(event.getMember());

        // 1. MAKE SURE USER HAS PERMISSION (BECAUSE THATS KINDA IMPORTANT LMFAO)
        if (!member.hasPermission(Permission.MESSAGE_MANAGE)) {
            event.replyEmbeds(EmbedManager.error("You Lack Permissions", "You must have the MESSAGE_MANAGE permission to use this command.")).setEphemeral(true).queue();
            return;
        }

        if (!guild.getSelfMember().hasPermission(event.getGuildChannel(), Permission.MESSAGE_MANAGE)) {
            event.replyEmbeds(EmbedManager.error("Bot Missing Permissions", guild.getSelfMember().getAsMention() + " needs the MESSAGE_MANAGE permission to delete messages.")).setEphemeral(true).queue();
            return;
        }

        int amount = event.getOption("amount").getAsInt();

        // 2. ENSURE AMOUNT BETWEEN 1 AND 100
        if (amount < 1 || amount > 100) {
            event.replyEmbeds(EmbedManager.error("Amount Is Invalid", "You must delete between 1 and 100 messages. You entered " + amount + " messages.")).setEphemeral(true).queue();
            return;
        }

        // 3. GET MESSAGES FROM MAXIMUM 14 DAYS AGO (ERM THANKS DISCORD API I GUESS? THAT JUST SEEMS SO IMPORTANT DOESN'T IT)
        event.deferReply(true).queue();

        TextChannel channel = event.getChannel().asTextChannel();

        channel.getHistory().retrievePast(amount).queue(messages -> {
            if (messages.isEmpty()) {
                event.getHook().editOriginalEmbeds(EmbedManager.error("Cannot Delete Messages", "There are no messages in this channel.")).queue();
                return;
            }

            long fourteenDaysAgo = System.currentTimeMillis() - (14L * 24 * 60 * 60 * 1000);

            List<Message> validMessages = messages.stream()
                    .filter(m -> m.getTimeCreated().toInstant().toEpochMilli() > fourteenDaysAgo)
                    .toList();

            if (validMessages.isEmpty()) {
                event.getHook().editOriginalEmbeds(EmbedManager.error("Messages Too Old", "The `wonderful` Discord API only allows the deletion of messages below 14 days of age. *clap clap clap*")).queue();
                return;
            }

            if (validMessages.size() == 1) {
                validMessages.get(0).delete().queue(
                        success -> event.getHook().editOriginalEmbeds(EmbedManager.success("Success", "Successfully nuked 1 message.")).queue(),
                        error -> event.getHook().editOriginalEmbeds(EmbedManager.error("Error", "Failed to delete the message.")).queue()
                );
            } else {
                channel.deleteMessages(validMessages).queue(
                        success -> event.getHook().editOriginalEmbeds(EmbedManager.success("Success", "Successfully nuked " + validMessages.size() + " messages.")).queue(),
                        error -> event.getHook().editOriginalEmbeds(EmbedManager.error("Error", "Failed to delete messages. Error message is `" + error.getMessage() + "`")).queue()
                );
            }
        });
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

    private void handleVampireCommand(SlashCommandInteractionEvent event) {
        switch (event.getSubcommandName()) {
            case "lineage" -> handleVampireLineageCommand(event);
            case "sire" -> handleVampireSireCommand(event);
            case "unsire" -> handleVampireUnsireCommand(event);
            case "siring" -> handleVampireSiringCommand(event);
        }
    }

    private void handleVampireSiringCommand(SlashCommandInteractionEvent event) {
        Guild guild = Objects.requireNonNull(event.getGuild());
        Member sire = Objects.requireNonNull(event.getMember());

        List<String> siring = dbRepo.getSiredUsers(guild.getId(), sire.getId());

        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("Users You Sire")
                .setColor(EmbedManager.SUCCESS)
                .setDescription("*Sire a user using /vampire sire [user], or unsire them using /vampire unsire [user]*");

        if(!siring.isEmpty()) {
            for (String sired : siring) {
                Member siredMember = guild.getMemberById(sired);

                if (siredMember == null) continue;

                DatabaseRepo.UserLevelHolder levelHolder = dbRepo.getUserLevel(guild.getId(), sired);
                DatabaseRepo.LevelRoleHolder roleHolder = dbRepo.getHighestRoleForLevel(guild.getId(), levelHolder.level());
                Role role = guild.getRoleById(roleHolder.roleId());
                String roleName;

                if(role != null) {
                    roleName = role.getName();
                } else {
                    roleName = "No Title";
                }

                embed.addField(siredMember.getAsMention(), "**Level " + levelHolder.level() + "** (" + roleName + ")", false);
            }
        } else {
            embed.setDescription("*You are not siring anybody. Request to sire a user using /vampire sire [user]*");
            embed.setColor(EmbedManager.ERROR);
            embed.addField("*But I am siring someone in another server?*", "Your dynasties are attached to the guild they were created in, rather than transferring across multiple servers. Feel free to re-create your lineage in multiple servers if you wish.", false);
        }

        event.replyEmbeds(embed.build()).queue();
    }

    private void handleVampireUnsireCommand(SlashCommandInteractionEvent event) {
        Guild guild = Objects.requireNonNull(event.getGuild());
        Member sire = Objects.requireNonNull(event.getMember());
        Member target = event.getOption("user").getAsMember();

        String existingSireId = dbRepo.getUserSireId(guild.getId(), target.getId());
        if (!Objects.equals(existingSireId, sire.getId())) {
            event.replyEmbeds(EmbedManager.error("User Not Sired By You", target.getAsMention() + " is not sired to you.")).queue();
            return;
        }

        dbRepo.setUserSireId(guild.getId(), target.getId(), null);

        event.replyEmbeds(EmbedManager.success("User Un-Sired", target.getAsMention() + " is now no longer your sire.")).queue();
    }

    private void handleVampireSireCommand(SlashCommandInteractionEvent event) {
        Guild guild = Objects.requireNonNull(event.getGuild());
        Member sire = Objects.requireNonNull(event.getMember());
        Member target = event.getOption("user").getAsMember();

        if (sire.getId().equals(target.getId())) {
            event.replyEmbeds(EmbedManager.error("Not Allowed", "You cannot sire yourself.")).queue();
            return;
        }

        if (target.getUser().isBot()) {
            event.replyEmbeds(EmbedManager.error("Not Allowed", "You cannot sire a bot.")).queue();
            return;
        }

        String existingSire = dbRepo.getUserSireId(guild.getId(), target.getId());
        if (existingSire != null) {
            event.replyEmbeds(EmbedManager.error("User Already Sired", target.getAsMention() + " already belongs to another dynasty.")).queue();
            return;
        }

        DatabaseRepo.UserLevelHolder sireLevel = dbRepo.getUserLevel(guild.getId(), sire.getId());

        String buttonIdAccept = "sire_accept:" + sire.getId() + ":" + guild.getId();
        String buttonIdReject = "sire_reject:" + sire.getId() + ":" + guild.getId();

        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("Dynastic Request")
                .setDescription("**" + sire.getEffectiveName() + "** (Level " + sireLevel.level() + ") wishes to sire you in **" + guild.getName() + "**!")
                .addField("What does this actually do?", "If you accept, you will become part of their lineage and they will passively gain a small amount of XP whenever you level up.", false)
                .setColor(EmbedManager.NEUTRAL)
                .setThumbnail(sire.getEffectiveAvatarUrl())
                .setFooter("This request will expire if ignored.");

        target.getUser().openPrivateChannel().queue(privateChannel -> {
            privateChannel.sendMessageEmbeds(embed.build())
                    .setComponents(
                            ActionRow.of(
                                    Button.success(buttonIdAccept, "Accept"),
                                    Button.danger(buttonIdReject, "Reject")
                            )
                    )
                    .queue(
                            success -> event.replyEmbeds(EmbedManager.success("Request Sent", "A siring request has been sent to " + target.getAsMention() + "'s DMs.")).queue(),
                            error -> event.replyEmbeds(EmbedManager.error("DM Failed", "I could not DM " + target.getAsMention() + ". They may have DMs disabled.")).queue()
                    );
        }, throwable -> {
            event.replyEmbeds(EmbedManager.error("DM Failed", "I could not DM " + target.getAsMention() + ". They may have DMs disabled.")).queue();
        });
    }

    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        String[] idParts = event.getComponentId().split(":"); // action:sireId:guildId
        String action = idParts[0];

        if (action.equals("sire_accept") || action.equals("sire_reject")) {
            String sireId = idParts[1];
            String guildId = idParts[2];
            String targetId = event.getUser().getId(); // the target is the person who presses the accept/reject button

            Guild guild = event.getJDA().getGuildById(guildId);

            if (guild == null) {
                event.editMessage("The bot seems to have left the server.").setComponents().queue();
                return;
            }

            Member target = guild.getMemberById(targetId);
            Member sire = guild.getMemberById(sireId);

            if (target == null || sire == null) {
                event.editMessage("Either you, the sire, or both have left the server and therefore the request is cancelled.").setComponents().queue();
                return;
            }

            if (action.equals("sire_reject")) {
                event.editMessageEmbeds(
                        new EmbedBuilder()
                                .setTitle("Sire Request Rejected")
                                .setDescription("You have rejected " + sire.getEffectiveName() + "'s request to sire you.")
                                .setColor(EmbedManager.ERROR)
                                .build()
                ).setComponents().queue();
                return;
            }

            String existingSire = dbRepo.getUserSireId(guildId, targetId);
            if (existingSire != null) {
                event.editMessage("You are already sired by someone else.").setComponents().queue();
                return;
            }

            dbRepo.setUserSireId(guildId, targetId, sireId);

            event.editMessageEmbeds(
                    new EmbedBuilder()
                            .setTitle("Sire Request Accepted")
                            .setDescription("You have accepted " + sire.getEffectiveName() + "'s request. You are now part of their lineage!")
                            .setColor(EmbedManager.SUCCESS)
                            .build()
            ).setComponents().queue();

        }
    }

    private void handleVampireLineageCommand(SlashCommandInteractionEvent event) {
        Guild guild = event.getGuild();
        Member currentMember = event.getMember();
        DatabaseRepo.UserLevelHolder level = dbRepo.getUserLevel(guild.getId(), currentMember.getId());
        DatabaseRepo.LevelRoleHolder roleHolder = dbRepo.getHighestRoleForLevel(guild.getId(), level.level());

        String roleName;
        StringBuilder lineage;

        if(roleHolder == null) {
            lineage = new StringBuilder(currentMember.getAsMention() + " is sired to ");
        } else {
            roleName = guild.getRoleById(roleHolder.roleId()).getName();
            lineage = new StringBuilder(currentMember.getAsMention() + " is a " + roleName + " of ");
        }

        while(dbRepo.getUserSireId(guild.getId(), currentMember.getId()) != null) {
            String id = dbRepo.getUserSireId(guild.getId(), currentMember.getId());
            currentMember = guild.getMemberById(id);
            level = dbRepo.getUserLevel(guild.getId(), currentMember.getId());
            roleHolder = dbRepo.getHighestRoleForLevel(guild.getId(), level.level());

            if(roleHolder != null) {
                roleName = guild.getRoleById(roleHolder.roleId()).getName();
                lineage.append(currentMember.getAsMention()).append(", who is a ").append(roleName).append(" of ");
            } else {
                lineage.append(currentMember.getAsMention()).append(", who is sired to ");
            }

        }

        lineage.append("nobody.");

        event.replyEmbeds(EmbedManager.success("Your Lineage", lineage.toString())).queue();
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