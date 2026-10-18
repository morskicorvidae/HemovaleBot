package morskicorvidae.hemovaleserver;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
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
    }

}