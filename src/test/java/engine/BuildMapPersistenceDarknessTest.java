package engine;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import model.DungeonMap;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class BuildMapPersistenceDarknessTest {

    @TempDir
    Path tempDir;

    @Test
    void save_v2_withFogEnabledTrue_roundTripsToTrue() throws IOException {
        DungeonMap map = newMap(true);
        Path path = tempDir.resolve("fog-on.dkmap");

        persistence().save(map, path);
        DungeonMap loaded = persistence().load(path);

        assertTrue(loaded.isFogEnabled());
    }

    @Test
    void save_v2_withFogEnabledFalse_roundTripsToFalse() throws IOException {
        DungeonMap map = newMap(false);
        Path path = tempDir.resolve("fog-off.dkmap");

        persistence().save(map, path);
        DungeonMap loaded = persistence().load(path);

        assertFalse(loaded.isFogEnabled());
    }

    @Test
    void load_v1_jsonWithoutFogField_returnsFogDisabled() throws IOException {
        Path path = tempDir.resolve("v1.dkmap");
        Files.writeString(path, """
                {
                  "schema": "DungeonKUrawler.DungeonMap",
                  "version": 1,
                  "levelName": "Old Map",
                  "width": 2,
                  "height": 2,
                  "cells": [
                    { "x": 0, "y": 0, "passable": false, "items": [] },
                    { "x": 1, "y": 0, "passable": false, "items": [] },
                    { "x": 0, "y": 1, "passable": false, "items": [] },
                    { "x": 1, "y": 1, "passable": false, "items": [] }
                  ]
                }
                """);

        DungeonMap loaded = persistence().load(path);

        assertFalse(loaded.isFogEnabled());
    }

    private DungeonMap newMap(boolean fearOfTheDarkEnabled) {
        DungeonMap map = new BuildMapFactory().createEmptyMap("Fog Map", 4, 4);
        map.setFogEnabled(fearOfTheDarkEnabled);
        return map;
    }

    private BuildMapPersistence persistence() {
        return new BuildMapPersistence(new BuildToolCatalog(), new BuildMapFactory(),
                new StandardBuildPlacementStrategy());
    }
}
