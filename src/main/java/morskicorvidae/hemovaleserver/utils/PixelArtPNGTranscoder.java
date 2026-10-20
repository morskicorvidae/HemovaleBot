package morskicorvidae.hemovaleserver.utils;

import org.apache.batik.gvt.renderer.ImageRenderer;
import org.apache.batik.transcoder.image.PNGTranscoder;
import java.awt.RenderingHints;

public class PixelArtPNGTranscoder extends PNGTranscoder {

    @Override
    protected ImageRenderer createRenderer() {
        ImageRenderer renderer = super.createRenderer();

        RenderingHints hints = renderer.getRenderingHints();
        hints.put(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        hints.put(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
        renderer.setRenderingHints(hints);

        return renderer;
    }
}