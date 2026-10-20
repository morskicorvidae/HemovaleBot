package morskicorvidae.hemovaleserver.species;

import morskicorvidae.hemovaleserver.userprofile.ProfileImageGenerator;

import java.io.InputStream;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

public enum Species {
    POOCHYENA("Poochyena", "poochyena"),
    MURKROW("Murkrow", "murkrow"),
    UMBREON("Umbreon", "umbreon"),
    MISDREAVUS("Misdreavus", "misdreavus"),
    UNOWN("Unown", "unown");

    public static final List<Species> COLLECTION;

    static {
        COLLECTION = Arrays.stream(Species.values()).collect(Collectors.toList());
    }

    private final String name;
    private final String internalName;
    private final String backsprite64;
    private final String frontsprite64;

    Species(String name, String internalName) {
        this.name = name;
        this.internalName = internalName;

        this.frontsprite64 = loadSprite("_front.png");
        this.backsprite64 = loadSprite("_back.png");
    }

    private String loadSprite(String suffix) {
        try (InputStream is = ProfileImageGenerator.class.getResourceAsStream("/" + this.internalName + suffix)) {
            if (is == null) {
                System.err.println("Fetching sprite from resources (" + this.name + suffix + ") FAILURE");
                return "";
            }
            byte[] bytes = is.readAllBytes();
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(bytes);
        } catch (Exception e) {
            System.err.println("Reading sprite (" + this.name + suffix + ") FAILURE " + e.getMessage());
            return "";
        }
    }

    public String getName() {
        return name;
    }

    public String getInternalName() {
        return internalName;
    }

    public String getBacksprite64() {
        return backsprite64;
    }

    public String getFrontsprite64() {
        return frontsprite64;
    }
}