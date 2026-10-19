package morskicorvidae.hemovaleserver.userprofile;

import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.batik.transcoder.image.PNGTranscoder;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class ProfileImageGenerator {

    public static byte[] generateProfileCard(String username, String avatarUrl, int level, int currentXp, int requiredXp) throws Exception {
        // 1. GET SVG TEMPLATE FROM RESOURCES
        String svgTemplate;

        try (InputStream is = ProfileImageGenerator.class.getResourceAsStream("/profile.svg")) {
            if (is == null) throw new RuntimeException("Fetching profile.svg from resources FAILURE");
            svgTemplate = new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }

        // 2. GET USER AVATAR AND CONVERT TO BASE64
        String base64Avatar = "";
        try (InputStream avatarStream = new URL(avatarUrl).openStream()) {
            byte[] avatarBytes = avatarStream.readAllBytes();
            base64Avatar = "data:image/png;base64," + Base64.getEncoder().encodeToString(avatarBytes);
        } catch (Exception e) {
            System.err.println("Downloading avatar (ProfileImageGenerator) FAILURE " + e.getMessage());
        }

        // 3. CALCULATE THE WIDTH OF THE XP BAR GIVEN THAT 460PX IS THE MAX WIDTH

        double percentage = (double) currentXp / requiredXp;
        int xpBarWidth = (int) (460 * Math.max(0, Math.min(1, percentage)));

        // 4. JUST ASSIGN THE VALUES TO THE PLACEHOLDERS

        String filledSvg = svgTemplate
                .replace("{{USERNAME}}", username)
                .replace("{{AVATAR}}", base64Avatar)
                .replace("{{LEVEL}}", String.valueOf(level))
                .replace("{{XP_TEXT}}", currentXp + " / " + requiredXp + " XP")
                .replace("{{XP_WIDTH}}", String.valueOf(xpBarWidth));

        // 5. AND CONVERT THAT TO A PNG USING APACHE BATIK

        // 5. Transcode SVG to PNG
        PNGTranscoder transcoder = new PNGTranscoder();

        transcoder.addTranscodingHint(PNGTranscoder.KEY_WIDTH, 480f);
        transcoder.addTranscodingHint(PNGTranscoder.KEY_HEIGHT, 180f);

        TranscoderInput input = new TranscoderInput(new ByteArrayInputStream(filledSvg.getBytes(StandardCharsets.UTF_8)));
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        TranscoderOutput output = new TranscoderOutput(outputStream);

        transcoder.transcode(input, output);

        return outputStream.toByteArray();
    }
}
