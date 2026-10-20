package morskicorvidae.hemovaleserver.species;

import morskicorvidae.hemovaleserver.userprofile.ProfileImageGenerator;
import morskicorvidae.hemovaleserver.utils.PixelArtPNGTranscoder;
import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.batik.transcoder.image.PNGTranscoder;
import java.awt.RenderingHints;
import org.apache.batik.transcoder.image.ImageTranscoder;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class BattleImageGenerator {

    public static byte[] generateBattleImage(Pet backPet, int backCurrentHp, Pet frontPet, int frontCurrentHp) throws Exception {
        // 1. GET SVG TEMPLATE FROM RESOURCES
        String svgTemplate;

        try (InputStream is = ProfileImageGenerator.class.getResourceAsStream("/battleback.svg")) {
            if (is == null) throw new RuntimeException("Fetching battleback.svg from resources FAILURE");
            svgTemplate = new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }

        String back64 = backPet.getSpecies().getBacksprite64();
        String front64 = frontPet.getSpecies().getFrontsprite64();

        String filledSvg = svgTemplate
                .replace("{{BATTLER_BACK}}", back64)
                .replace("{{BATTLER_FRONT}}", front64)
                .replace("{{BACK_NAME}}", backPet.getSpecies().getName())
                .replace("{{FRONT_NAME}}", frontPet.getSpecies().getName())
                .replace("{{BACK_HP_WIDTH}}", "30")
                .replace("{{FRONT_HP_WIDTH}}", "30")
                .replace("{{FRONT_LEVEL}}", String.valueOf(frontPet.getLevel()))
                .replace("{{BACK_LEVEL}}", String.valueOf(backPet.getLevel()))
                .replace("{{BACK_HP_TEXT}}", backCurrentHp + " / " + backPet.getHealth())
                .replace("{{FRONT_HP_TEXT}}", frontCurrentHp + " / " + frontPet.getHealth());

        PixelArtPNGTranscoder transcoder = new PixelArtPNGTranscoder();

        TranscoderInput input = new TranscoderInput(new ByteArrayInputStream(filledSvg.getBytes(StandardCharsets.UTF_8)));
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        TranscoderOutput output = new TranscoderOutput(outputStream);

        transcoder.addTranscodingHint(PNGTranscoder.KEY_WIDTH, 800f);
        transcoder.addTranscodingHint(PNGTranscoder.KEY_HEIGHT, 480f);

        transcoder.transcode(input, output);

        return outputStream.toByteArray();
    }
}
