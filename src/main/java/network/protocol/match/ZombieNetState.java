package network.protocol.match;

public class ZombieNetState {

    private int entityId;
    private String alias;
    private int lane;
    private float x;
    private int hp;
    private int maxHp;
    private boolean dead;
    private boolean frozen;
    private boolean glowing;
    private boolean eating;
    private String rangedAttackType;
    private int rangedCooldown = -1;

    public ZombieNetState() {
    }

    public ZombieNetState(int entityId, String alias, int lane, float x,
                          int hp, int maxHp, boolean dead, boolean frozen, boolean glowing) {
        this(entityId, alias, lane, x, hp, maxHp, dead, frozen, glowing,
            false, null, -1);
    }

    public ZombieNetState(int entityId, String alias, int lane, float x,
                          int hp, int maxHp, boolean dead, boolean frozen,
                          boolean glowing, boolean eating,
                          String rangedAttackType, int rangedCooldown) {
        this.entityId = entityId;
        this.alias = alias;
        this.lane = lane;
        this.x = x;
        this.hp = hp;
        this.maxHp = maxHp;
        this.dead = dead;
        this.frozen = frozen;
        this.glowing = glowing;
        this.eating = eating;
        this.rangedAttackType = rangedAttackType;
        this.rangedCooldown = rangedCooldown;
    }

    public int getEntityId() { return entityId; }
    public void setEntityId(int entityId) { this.entityId = entityId; }

    public String getAlias() { return alias; }
    public void setAlias(String alias) { this.alias = alias; }

    public int getLane() { return lane; }
    public void setLane(int lane) { this.lane = lane; }

    public float getX() { return x; }
    public void setX(float x) { this.x = x; }

    public int getHp() { return hp; }
    public void setHp(int hp) { this.hp = hp; }

    public int getMaxHp() { return maxHp; }
    public void setMaxHp(int maxHp) { this.maxHp = maxHp; }

    public boolean isDead() { return dead; }
    public void setDead(boolean dead) { this.dead = dead; }

    public boolean isFrozen() { return frozen; }
    public void setFrozen(boolean frozen) { this.frozen = frozen; }

    public boolean isGlowing() { return glowing; }
    public void setGlowing(boolean glowing) { this.glowing = glowing; }

    public boolean isEating() { return eating; }
    public void setEating(boolean eating) { this.eating = eating; }

    public String getRangedAttackType() { return rangedAttackType; }
    public void setRangedAttackType(String rangedAttackType) { this.rangedAttackType = rangedAttackType; }

    public int getRangedCooldown() { return rangedCooldown; }
    public void setRangedCooldown(int rangedCooldown) { this.rangedCooldown = rangedCooldown; }
}
