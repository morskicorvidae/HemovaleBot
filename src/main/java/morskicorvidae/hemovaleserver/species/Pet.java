package morskicorvidae.hemovaleserver.species;

import java.util.concurrent.ThreadLocalRandom;

public class Pet {

    private String guildId;
    private String userId;
    private Species species;
    private int level;
    private int health;
    private int attack;
    private int defense;
    private int accuracy;

    public int getHealth() {
        return health;
    }

    public Species getSpecies() {
        return species;
    }

    public String getGuildId() {
        return guildId;
    }

    public String getUserId() {
        return userId;
    }

    public int getLevel() {
        return level;
    }

    public int getAttack() {
        return attack;
    }

    public int getDefense() {
        return defense;
    }

    public int getAccuracy() {
        return accuracy;
    }

    private int hitChancePercent() {
        int acc = getAccuracy();
        int above = acc - 1;
        int denom = above + 10;

        if (denom <= 0) return 5;

        double hit = 50.0 + 50.0 * above / denom;
        return (int) Math.round(Math.max(5, Math.min(95, hit)));
    }

    public boolean willMissAttack() {
        return ThreadLocalRandom.current().nextInt(100) >= hitChancePercent();
    }

    public int calculateDamageTo(Pet other) {
        int attackRoll  = randomMinMax(1, getAttack());
        int defenseRoll = randomMinMax(1, other.getDefense());

        return Math.max(0, attackRoll - defenseRoll);
    }

    private static int randomMinMax(int min, int max) {
        if (max <= min) return min;
        return ThreadLocalRandom.current().nextInt(min, max + 1);
    }

    public Pet setHealth(int health) {
        this.health = health;
        return this;
    }

    public Pet setSpecies(Species species) {
        this.species = species;
        return this;
    }

    public Pet setAccuracy(int accuracy) {
        this.accuracy = accuracy;
        return this;
    }

    public Pet setAttack(int attack) {
        this.attack = attack;
        return this;
    }

    public Pet setDefense(int defense) {
        this.defense = defense;
        return this;
    }

    public Pet setGuildId(String guildId) {
        this.guildId = guildId;
        return this;
    }

    public Pet setLevel(int level) {
        this.level = level;
        return this;
    }

    public Pet setUserId(String userId) {
        this.userId = userId;
        return this;
    }
}
