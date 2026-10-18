package morskicorvidae.hemovaleserver;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class Main extends ListenerAdapter {

    public static void main(String[] args) {
        String token = System.getenv("DISCORD_TOKEN");

        if (token == null) {
            System.err.println("DISCORD_TOKEN environment variable is missing!");
            return;
        }

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

        SlashCommandData joinroleCommand = Commands.slash("joinrole", "Pick a role which all members get when joining.");
        joinroleCommand.addOption(OptionType.ROLE, "role", "The role which all members get when joining.");

        event.getJDA().updateCommands().addCommands(
                roleCommand.addSubcommands(roleAddCommand),
                joinroleCommand
        ).queue();
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        switch(event.getName()) {
            case "role":
                handleRoleCommand(event);
                break;
            case "joinrole":
                Role joinRole = event.getInteraction().getOption("role").getAsRole();
                break;
        }
    }

    private void handleRoleCommand(SlashCommandInteractionEvent event) {
    }
}