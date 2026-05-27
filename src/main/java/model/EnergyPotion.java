package model;

import java.awt.Color;

/**
 * Blue potion. Adds {@link #OVERFLOW_AMOUNT} energy and allows the
 * hero's energy to exceed its normal cap.
 *
 * <p><b>Design decision (spec 2.4.1 leaves energy mechanics open):</b>
 * we permit overflow rather than clamping at {@code maxEnergy} because:
 * <ol>
 *   <li>It distinguishes Energy Potions from passive idle regeneration
 *       (which respects the cap). A potion has to feel meaningfully
 *       different from "stand still for a few seconds".</li>
 *   <li>It gives the player a pre-fight buff option: drink before
 *       engaging a knight so the energy reserve absorbs the cost of
 *       attacking and moving simultaneously.</li>
 *   <li>The overflow is self-limiting — it drains naturally during play
 *       and cannot stack indefinitely because passive regen does not
 *       refill above {@code maxEnergy}.</li>
 * </ol>
 *
 * <p>Contrast with {@link HealPotion}, which fills to max with no
 * overflow. That asymmetry is deliberate: HP is binary (alive/dead),
 * while energy is a tactical buffer.
 */
public class EnergyPotion extends Potion {

    private static final Color COLOR = new Color(50, 130, 255);
    private static final int OVERFLOW_AMOUNT = 100;

    public EnergyPotion() {
        super("Energy Potion");
    }

    @Override
    public Color getColor() {
        return COLOR;
    }

    @Override
    public void drink(Hero hero) {
        hero.setEnergy(hero.getEnergy() + OVERFLOW_AMOUNT);
    }
}
