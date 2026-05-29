package view.assets;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.awt.image.BufferedImage;

import model.Knight;
import model.Sorcerer;
import model.Team;

import org.junit.jupiter.api.Test;

class SpriteRegistryTest {

    @Test
    void teamASorcererUsesRedWizardSprite() {
        Sorcerer sorcerer = new Sorcerer(1, 1, "Team A Sorcerer", 10, 30, 3, false);
        sorcerer.setTeam(Team.TEAM_A);

        BufferedImage expected = AssetManager.get().image(AssetId.RED_WIZARD);

        assertNotNull(expected);
        assertSame(expected, SpriteRegistry.spriteFor(sorcerer));
    }

    @Test
    void teamAKnightUsesRedKnightSprite() {
        Knight knight = new Knight(1, 1, "Team A Knight", 20, 8, 4, 5);
        knight.setTeam(Team.TEAM_A);

        BufferedImage expected = AssetManager.get().image(AssetId.RED_KNIGHT);

        assertNotNull(expected);
        assertSame(expected, SpriteRegistry.spriteFor(knight));
    }

}
