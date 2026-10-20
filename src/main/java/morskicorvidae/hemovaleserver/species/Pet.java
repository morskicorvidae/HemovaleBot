package morskicorvidae.hemovaleserver.species;

public class Pet {

    private String guildId;
    private String userId;
    private Species species;
    private int level;
    private int attack;
    private int defense;
    private int accuracy;

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
