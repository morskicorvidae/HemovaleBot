package morskicorvidae.hemovaleserver.userprofile;

public class LevelCalculations {

    public static int getXPRequiredForLevel(int level) {
        if (level < 1) return 0;
        return (4 * level * level + 170 * level - 94) / 10;
    }

    public static int getLevelFromXP(int xp) {
        if (xp < getXPRequiredForLevel(1)) return 0;
        return (int) Math.floor((-170 + Math.sqrt(170 * 170 + 16 * (94 + 10.0 * xp))) / 8);
    }

    public static int getExpFromMessage(String messageContent) {
        if (messageContent == null) return 0;
        String trimmed = messageContent.strip();
        if (trimmed.isEmpty()) return 0;

        int wordCount = trimmed.split("\\s+").length;

        int lengthAddition = (int) Math.max(0, Math.round(2 * log2(wordCount + 1) - 2));
        lengthAddition = Math.min(8, lengthAddition);

        return 1 + lengthAddition;
    }

    private static double log2(double value) {
        return Math.log(value) / Math.log(2);
    }
}
