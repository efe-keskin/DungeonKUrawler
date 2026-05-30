package model;

/**
 * Melee-focused enemy with a vision range used for AI / detection (future phases).
 */
public class Knight extends Entity {

    private int hp;
    private int str;
    private int def;
    private int visionRange;
    private Weapon weapon;
    private AIState aiState = AIState.ROAMING;

    public Knight(int x, int y, String name, int hp, int str, int def, int visionRange) {
        super(x, y, name);
        this.hp = hp;
        this.str = str;
        this.def = def;
        this.visionRange = visionRange;
    }


    public int getHp() {
        return hp;
    }

    public void setHp(int hp) {
        this.hp = hp;
    }

    public int getStr() {
        return str;
    }

    public void setStr(int str) {
        this.str = str;
    }

    public int getDef() {
        return def;
    }

    public void setDef(int def) {
        this.def = def;
    }

    public int getVisionRange() {
        return visionRange;
    }

    public void setVisionRange(int visionRange) {
        this.visionRange = visionRange;
    }

    public Weapon getWeapon() {
        return weapon;
    }

    public void setWeapon(Weapon weapon) {
        if (this.weapon == null) {
            this.weapon = weapon;
        }
    }

    public boolean hasWeapon() {
        return weapon != null;
    }

    public AIState getAiState() {
        return aiState;
    }

    public void setAiState(AIState aiState) {
        this.aiState = aiState;
    }
}
