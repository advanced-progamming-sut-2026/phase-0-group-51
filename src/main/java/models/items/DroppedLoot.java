package models.items;

import lombok.Getter;
import lombok.Setter;
import models.enums.LootType;
@Getter
@Setter
public class DroppedLoot {
    private static final int LIFETIME_SECONDS = 15;

    private final LootType type;
    private final float x;
    private final int lane;
    private int remainingTicks;
    private boolean collected;
    private boolean expired;

    public DroppedLoot(LootType type, float x, int lane, int ticksPerSecond) {
        if (type == null) {
            throw new IllegalArgumentException("Loot type is required");
        }
        if (ticksPerSecond <= 0) {
            throw new IllegalArgumentException("Ticks per second must be positive");
        }
        this.type = type;
        this.x = x;
        this.lane = lane;
        this.remainingTicks = LIFETIME_SECONDS * ticksPerSecond;
    }

    public void tick() {
        if (!isActive()) {
            return;
        }
        remainingTicks--;
        if (remainingTicks <= 0) {
            expired = true;
        }
    }

    public boolean collect() {
        if (!isActive()) {
            return false;
        }
        collected = true;
        return true;
    }

    public boolean isActive() {
        return !collected && !expired;
    }

    public int getColumn() {
        return (int) Math.floor(x);
    }

    public String getDisplayName() {
        return switch (type) {
            case COIN -> "coin";
            case GEM -> "gem";
            case POT -> "pot";
        };
    }
}
