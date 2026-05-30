package engine;

import java.util.Objects;

import model.Hero;
import model.Pet;
import model.PetState;

/**
 * GRASP Controller for the pet equip/unequip use cases. It works purely on the
 * persistent model — the hero and its {@link model.FullGameInventory} — so it
 * can be driven from the tower-map inventory window without any active game
 * session. In-floor pet behaviour (spawning, roaming, abilities, damage) lives
 * in {@link GameEngine}; this class only decides ownership and the equipped slot.
 */
public final class PetController {

    public enum EquipResult {
        SUCCESS,
        NOT_OWNED,
        INVALID
    }

    private final Hero hero;

    public PetController(Hero hero) {
        this.hero = Objects.requireNonNull(hero, "hero");
    }

    public Pet equippedPet() {
        return hero.getEquippedPet();
    }

    public boolean isEquipped(Pet pet) {
        return pet != null && hero.getEquippedPet() == pet;
    }

    /**
     * UC: equip a pet from the persistent inventory. Dead pets may be equipped
     * (they are not deleted) but stay {@code FAINTED} until revived on a floor.
     */
    public EquipResult equip(Pet pet) {
        if (pet == null) {
            return EquipResult.INVALID;
        }
        if (!hero.getFullInventory().getItems().contains(pet)) {
            return EquipResult.NOT_OWNED;
        }
        Pet current = hero.getEquippedPet();
        if (current != null && current != pet) {
            current.setState(PetState.UNEQUIPPED);
        }
        hero.setEquippedPet(pet);
        pet.setState(pet.isAlive() ? PetState.ACTIVE : PetState.FAINTED);
        return EquipResult.SUCCESS;
    }

    /** Clears the equipped slot, if any. */
    public void unequip() {
        Pet current = hero.getEquippedPet();
        if (current != null) {
            current.setState(PetState.UNEQUIPPED);
        }
        hero.setEquippedPet(null);
    }
}
