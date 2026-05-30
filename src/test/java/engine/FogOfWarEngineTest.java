package engine;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import model.DungeonMap;
import model.Hero;
import model.Torch;

import org.junit.jupiter.api.Test;

class FogOfWarEngineTest {

    @Test
    void circularVisibility_atDistanceZero_isVisible() {
        CircularVisibility visibility = new CircularVisibility(3.0);

        assertTrue(visibility.isVisible(map(true), 5, 5, 5, 5));
    }

    @Test
    void circularVisibility_atRadius3_isVisible() {
        CircularVisibility visibility = new CircularVisibility(3.0);

        assertTrue(visibility.isVisible(map(true), 5, 5, 8, 5));
    }

    @Test
    void circularVisibility_at3x3DiagonalIsHidden() {
        CircularVisibility visibility = new CircularVisibility(3.0);

        assertFalse(visibility.isVisible(map(true), 5, 5, 8, 8));
    }

    @Test
    void revealAround_onFogDisabledMap_marksNothing() {
        DungeonMap map = map(false);
        Hero hero = heroAt(5, 5);

        new FogOfWarEngine().revealAround(map, hero);

        assertFalse(map.getCell(5, 5).isDiscovered());
        assertFalse(map.getCell(8, 5).isDiscovered());
    }

    @Test
    void revealAround_withoutTorch_usesRadius3() {
        DungeonMap map = map(true);
        Hero hero = heroAt(5, 5);

        new FogOfWarEngine().revealAround(map, hero);

        assertTrue(map.getCell(8, 5).isDiscovered());
        assertFalse(map.getCell(9, 5).isDiscovered());
    }

    @Test
    void revealAround_withTorchInInventory_usesRadius5() {
        DungeonMap map = map(true);
        Hero hero = heroAt(5, 5);

        new FogOfWarEngine().revealAround(map, hero);
        assertFalse(map.getCell(9, 5).isDiscovered());

        hero.getInventory().tryAdd(new Torch());
        new FogOfWarEngine().revealAround(map, hero);

        assertTrue(map.getCell(9, 5).isDiscovered());
    }

    @Test
    void heroAwareIsVisible_respectsTorchPresence() {
        DungeonMap map = map(true);
        Hero hero = heroAt(5, 5);
        FogOfWarEngine fog = new FogOfWarEngine();

        assertFalse(fog.isVisible(map, hero, 9, 5));

        hero.getInventory().tryAdd(new Torch());

        assertTrue(fog.isVisible(map, hero, 9, 5));
    }

    private DungeonMap map(boolean fogEnabled) {
        DungeonMap map = new DungeonMap("Fog Test", 12, 12);
        map.setFogEnabled(fogEnabled);
        return map;
    }

    private Hero heroAt(int x, int y) {
        return new Hero(x, y, "Hero", 17, 10, 80, 2, 100);
    }
}
