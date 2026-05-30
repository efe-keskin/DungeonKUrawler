package engine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Path;

import model.DungeonMap;
import model.Vase;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class BuildMapPersistenceVaseTest {

    @TempDir
    Path tempDir;

    @Test
    void brokenVaseRoundTripRestoresBrokenState() throws IOException {
        DungeonMap map = new BuildMapFactory().createEmptyMap("Broken Vase", 4, 4);
        Vase vase = new Vase();
        vase.breakApart();
        map.getCell(2, 2).getItems().add(vase);
        Path path = tempDir.resolve("broken-vase.dkmap");

        persistence().save(map, path);
        DungeonMap loaded = persistence().load(path);

        Vase loadedVase = assertInstanceOf(Vase.class, loaded.getCell(2, 2).getItemsView().get(0));
        assertTrue(loadedVase.isBroken());
        assertEquals(Vase.BROKEN_SPRITE, loadedVase.spriteResource());
        assertFalse(loadedVase.isBlocking());
        assertTrue(loadedVase.getInventoryActions().isEmpty());
    }

    private BuildMapPersistence persistence() {
        return new BuildMapPersistence(new BuildToolCatalog(), new BuildMapFactory(),
                new StandardBuildPlacementStrategy());
    }
}
