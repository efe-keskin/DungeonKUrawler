package model;

/**
 * Light source. When present in the hero's inventory, expands the
 * fog-of-war vision radius via the Strategy pattern (see
 * {@link engine.FogOfWarEngine}). Always-on while in inventory;
 * no equip step. Inventory actions are just DISCARD inherited
 * from {@link Item}.
 */
public class Torch extends Item {
    public Torch() {
        super("Torch");
    }
}
