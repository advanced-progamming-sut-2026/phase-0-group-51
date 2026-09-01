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
    private long attackSerial;
    private boolean raged;
    private boolean spinning;
    private boolean hasKilled;
    private boolean impFired;
    private boolean rangedHasTarget;

    public ZombieNetState() {
    }

    public ZombieNetState(int entityId, String alias, int lane, float x,
                          int hp, int maxHp, boolean dead, boolean frozen, boolean glowing) {
        this(entityId, alias, lane, x, hp, maxHp, dead, frozen, glowing,
            false, null, -1, 0L);
    }

    public ZombieNetState(int entityId, String alias, int lane, float x,
                          int hp, int maxHp, boolean dead, boolean frozen,
                          boolean glowing, boolean eating,
                          String rangedAttackType, int rangedCooldown) {
        this(entityId, alias, lane, x, hp, maxHp, dead, frozen, glowing,
            eating, rangedAttackType, rangedCooldown, 0L);
    }

    public ZombieNetState(int entityId, String alias, int lane, float x,
                          int hp, int maxHp, boolean dead, boolean frozen,
                          boolean glowing, boolean eating,
                          String rangedAttackType, int rangedCooldown,
                          long attackSerial) {
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
        this.attackSerial = attackSerial;
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

    public long getAttackSerial() { return attackSerial; }
    public void setAttackSerial(long attackSerial) { this.attackSerial = attackSerial; }

    // The following flags mirror the internal state of a zombie's
    // ZombieBehavior instances (DamageReactionBehavior, InstantKillBehavior,
    // ImpThrowBehavior, RangedAttackBehavior). They are required so that
    // client-side render mirrors (e.g. RemoteMatchView) can reproduce the
    // same special zombie animations (rage, spin, smash/tackle, imp throw,
    // octopus net toss, ...) that the authoritative server-side simulation
    // plays, since the mirror zombies are never actually ticked.
    public boolean isRaged() { return raged; }
    public void setRaged(boolean raged) { this.raged = raged; }

    public boolean isSpinning() { return spinning; }
    public void setSpinning(boolean spinning) { this.spinning = spinning; }

    public boolean isHasKilled() { return hasKilled; }
    public void setHasKilled(boolean hasKilled) { this.hasKilled = hasKilled; }

    public boolean isImpFired() { return impFired; }
    public void setImpFired(boolean impFired) { this.impFired = impFired; }

    public boolean isRangedHasTarget() { return rangedHasTarget; }
    public void setRangedHasTarget(boolean rangedHasTarget) { this.rangedHasTarget = rangedHasTarget; }
}
