package engine;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Random;

import model.Arch;
import model.Chest;
import model.Column;
import model.DungeonMap;
import model.Key;
import model.KeyColor;
import model.MissingBrick;
import model.ValuableItem;

import org.junit.jupiter.api.Test;

class ExitDoorKeyPlacerTest {

    @Test
    void generatedExitKeyUsesEmptyBreakableAndLeavesValuableFixtureUntouched() {
        DungeonMap map = new BuildMapFactory().createEmptyMap("Designed", 8, 8);
        ValuableItem valuable = new ValuableItem("Golden Idol");
        MissingBrick valuableFixture = new MissingBrick(valuable);
        Column breakable = new Column();
        map.getCell(3, 0).getItems().add(valuableFixture);
        map.getCell(4, 4).getItems().add(breakable);
        Arch arch = new Arch(null);

        ExitDoorKeyPlacer.Placement placement =
                new ExitDoorKeyPlacer(new Random(2)).ensureKeyForExit(map, arch);

        assertTrue(placement.placed());
        assertSame(valuable, valuableFixture.getHiddenItem());
        Key key = assertInstanceOf(Key.class, breakable.getHiddenItem());
        assertTrue(key.matches(arch.getRequiredKeyId()));
    }

    @Test
    void generatedExitKeyCanBeHiddenInsideChest() {
        DungeonMap map = new BuildMapFactory().createEmptyMap("Designed", 8, 8);
        Chest chest = new Chest("Open Chest", 4);
        map.getCell(4, 4).getItems().add(chest);
        Arch arch = new Arch(null);

        ExitDoorKeyPlacer.Placement placement =
                new ExitDoorKeyPlacer(new Random(2)).ensureKeyForExit(map, arch);

        assertTrue(placement.placed());
        Key key = assertInstanceOf(Key.class, chest.getContents().get(0));
        assertTrue(key.matches(arch.getRequiredKeyId()));
    }

    @Test
    void generatedExitKeyDiffersFromLockedChestKey() {
        DungeonMap map = new BuildMapFactory().createEmptyMap("Designed", 8, 8);
        Chest lockedChest = Chest.locked("Locked Chest", 4, "silver");
        map.getCell(4, 4).getItems().add(lockedChest);
        Arch arch = new Arch(null);

        ExitDoorKeyPlacer.Placement placement =
                new ExitDoorKeyPlacer(new Random(2)).ensureKeyForExit(map, arch);

        assertTrue(placement.placed());
        assertNotEquals(lockedChest.getRequiredKeyId(), arch.getRequiredKeyId());
        assertFalse(placement.key().matches(lockedChest.getRequiredKeyId()));
        assertNotEquals(KeyColor.SILVER, placement.key().getColor());
    }

    @Test
    void authoredKeyInsideChestCanBeAssignedToExit() {
        DungeonMap map = new BuildMapFactory().createEmptyMap("Designed", 8, 8);
        Chest chest = new Chest("Open Chest", 4);
        Key key = new Key("orange", KeyColor.ORANGE);
        assertTrue(chest.addItem(key));
        map.getCell(4, 4).getItems().add(chest);
        Arch arch = new Arch(null);

        ExitDoorKeyPlacer.Placement placement =
                new ExitDoorKeyPlacer(new Random(2)).ensureKeyForExit(map, arch);

        assertTrue(placement.reused());
        assertSame(key, placement.key());
        assertEquals("orange", arch.getRequiredKeyId());
    }
}
