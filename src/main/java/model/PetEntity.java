package model;

/**
 * Transient on-grid presence of the hero's equipped {@link Pet} during a floor.
 * It is created when a floor starts and discarded with the floor (only the
 * underlying {@link Pet} is persistent). Combat/roaming read and mutate the
 * referenced pet's HP and state.
 */
public final class PetEntity extends Entity {

    private final Pet pet;

    public PetEntity(Pet pet, int x, int y) {
        super(x, y, pet.getName());
        this.pet = pet;
    }

    public Pet getPet() {
        return pet;
    }

    @Override
    public String spriteResource() {
        return pet.spriteResource();
    }
}
