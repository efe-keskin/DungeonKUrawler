package view.assets;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.awt.image.BufferedImage;

import model.Knight;
import model.Sorcerer;
import model.Team;
import model.Column;
import model.Crate;
import model.Vase;
import model.WaterPipe;

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

    @Test
    void teamASorcererWalkFrameUsesRedWizardSprite() {
        Sorcerer sorcerer = new Sorcerer(1, 1, "Team A Sorcerer", 10, 30, 3, false);
        sorcerer.setTeam(Team.TEAM_A);

        BufferedImage expected = AssetManager.get().image(AssetId.RED_WIZARD);

        assertNotNull(expected);
        assertSame(expected, SpriteRegistry.walkFrameFor(sorcerer, 0));
    }

    @Test
    void teamAKnightWalkFrameUsesRedKnightSprite() {
        Knight knight = new Knight(1, 1, "Team A Knight", 20, 8, 4, 5);
        knight.setTeam(Team.TEAM_A);

        BufferedImage expected = AssetManager.get().image(AssetId.RED_KNIGHT);

        assertNotNull(expected);
        assertSame(expected, SpriteRegistry.walkFrameFor(knight, 0));
    }

    @Test
    void movedBreakableAndSearchableAssetsResolveFromCanonicalPaths() {
        assertNotNull(AssetManager.get().image(Column.GRAY_SPRITE));
        assertNotNull(AssetManager.get().image(WaterPipe.LARGE_RING_SPRITE));
        assertNotNull(AssetManager.get().image(new Vase().spriteResource()));
        assertNotNull(AssetManager.get().image(Vase.BROKEN_SPRITE));
        assertNotNull(AssetManager.get().image(Crate.WOOD_TALL_SPRITE));
        assertNotNull(AssetManager.get().image(Crate.BREAKABLE_WOOD_TALL_SPRITE));
    }

    @Test
    void oldAssetPathsStillResolveAfterFolderMove() {
        assertSame(AssetManager.get().image(Column.GRAY_SPRITE),
                AssetManager.get().image("/background_floor/assets/searchable assets/column2.png"));
        assertSame(AssetManager.get().image(WaterPipe.LARGE_RING_SPRITE),
                AssetManager.get().image("/background_floor/assets/searchable assets/water_pipes.png"));
        assertSame(AssetManager.get().image(Crate.WOOD_TALL_SPRITE),
                AssetManager.get().image("/items/crates/17_crate_wood_tall_corrected.png"));
    }

}
