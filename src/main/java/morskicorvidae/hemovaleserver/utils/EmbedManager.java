package morskicorvidae.hemovaleserver.utils;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;

import java.awt.*;

public class EmbedManager {

    public static final Color NEUTRAL = Color.decode("#8a8d91");
    public static final Color ERROR = Color.decode("#8a0303");
    public static final Color SUCCESS = Color.decode("#72D06B");

    public static MessageEmbed out(String title, String description, Color color) {
        return new EmbedBuilder()
                .setTitle(title)
                .setDescription(description)
                .setColor(color)
                .build();
    }

    public static MessageEmbed success(String title, String description) {
        return out(title, description, SUCCESS);
    }

    public static MessageEmbed neutral(String title, String description) {
        return out(title, description, NEUTRAL);
    }

    public static MessageEmbed error(String title, String description) {
        return out(title, description, ERROR);
    }

}
