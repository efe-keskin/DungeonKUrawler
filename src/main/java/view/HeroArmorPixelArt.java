package view;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import javax.imageio.ImageIO;

/** Loads the hero armor overlay once from {@code /weapons/armor.png}. */
final class HeroArmorPixelArt {

    static BufferedImage armorImage;

    static {
        try (InputStream in = HeroArmorPixelArt.class.getResourceAsStream("/weapons/armor.png")) {
            if (in != null) {
                armorImage = ImageIO.read(in);
            }
        } catch (IOException ignored) {
            armorImage = null;
        }
    }

    private HeroArmorPixelArt() {
    }
}
