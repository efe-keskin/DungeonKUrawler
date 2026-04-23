package model;

/**
 * Magic-using enemy; {@code hasMagicRing} can gate special abilities later.
 */
public class Sorcerer extends Entity {

    private int hp;
    private int mana;
    private int def;
    private boolean hasMagicRing;
    private AIState aiState = AIState.ROAMING;

    public Sorcerer(int x, int y, String name, int hp, int mana, int def, boolean hasMagicRing) {
        super(x, y, name);
        this.hp = hp;
        this.mana = mana;
        this.def = def;
        this.hasMagicRing = hasMagicRing;
    }

    public int getHp() {
        return hp;
    }

    public void setHp(int hp) {
        this.hp = hp;
    }

    public int getMana() {
        return mana;
    }

    public void setMana(int mana) {
        this.mana = mana;
    }

    public int getDef() {
        return def;
    }

    public void setDef(int def) {
        this.def = def;
    }

    public boolean isHasMagicRing() {
        return hasMagicRing;
    }

    public void setHasMagicRing(boolean hasMagicRing) {
        this.hasMagicRing = hasMagicRing;
    }

    public AIState getAiState() {
        return aiState;
    }

    public void setAiState(AIState aiState) {
        this.aiState = aiState;
    }
}
