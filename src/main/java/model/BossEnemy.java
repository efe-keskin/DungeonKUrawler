package model;

/**
 * Tower boss enemy used on the boss floors. It behaves like a heavy caster:
 * high health, long range, and slower purple projectiles.
 */
public class BossEnemy extends Entity {

    private int hp;
    private final int maxHp;
    private int mana;
    private final int def;
    private final int projectileAttack;
    private AIState aiState = AIState.ROAMING;

    public BossEnemy(int x, int y, String name, int hp, int mana, int def, int projectileAttack) {
        super(x, y, name);
        this.hp = hp;
        this.maxHp = hp;
        this.mana = mana;
        this.def = def;
        this.projectileAttack = projectileAttack;
    }

    public int getHp() {
        return hp;
    }

    public void setHp(int hp) {
        this.hp = hp;
    }

    public int getMaxHp() {
        return maxHp;
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

    public int getProjectileAttack() {
        return projectileAttack;
    }

    public AIState getAiState() {
        return aiState;
    }

    public void setAiState(AIState aiState) {
        this.aiState = aiState;
    }
}
