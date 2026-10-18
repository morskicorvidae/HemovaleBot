package morskicorvidae.hemovaleserver;

import morskicorvidae.hemovaleserver.database.Database;
import morskicorvidae.hemovaleserver.database.DatabaseRepo;
import morskicorvidae.hemovaleserver.utils.EmbedManager;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.restaction.RoleAction;

import java.awt.*;
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

        roleCommand.addSubcommands(roleAddCommand, roleUpdateCommand, roleUseColorCommand);

        SlashCommandData joinroleCommand = Commands.slash("joinrole", "Pick a role which all members get when joining. | /joinrole [role]");
        joinroleCommand.addOption(OptionType.ROLE, "role", "The role which all members get when joining.", true);

        for(Guild guild : event.getJDA().getGuilds()) {
            guild.updateCommands().addCommands(roleCommand, joinroleCommand).queue();
        }
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        switch(event.getName()) {
            case "role":
                handleRoleCommand(event);
                break;
            case "joinrole": {
                Role joinRole = event.getInteraction().getOption("role").getAsRole();
                MessageEmbed reply = EmbedManager.error("Not implemented yet", "Command has not yet been implemented.");
                event.replyEmbeds(reply).queue();
                break;
            }
            default: // Shouldn't happen!
                MessageEmbed reply = EmbedManager.error("Not implemented yet", "Command has not yet been implemented.");
                event.replyEmbeds(reply).queue();
        }
    }

    private void handleRoleCommand(SlashCommandInteractionEvent event) {
        if(Objects.equals(event.getSubcommandName(), "add")) {
            handleRoleAddCommand(event);
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