package models.sun;

import lombok.Getter;
import lombok.Setter;
import models.Plant.Plant;
import models.Result;
import models.Zombie.Zombie;

@Getter
@Setter
public class Sun {
    private static final int TICKS_PER_SECOND = 10;
    private static final int FALL_DURATION = 5 * TICKS_PER_SECOND;

    private final float x;
    private final float y;
    private final int lane;
    private final Plant sourcePlant;
    private SunType sunType;
    private final int amount;
    private int remainingTicks;
    private boolean collected;
    private boolean expired;
    private boolean grounded;
    private int livedTicks;
    private int groundedTicks;

    private boolean beingStolen;
    private Zombie stealingZombie;
    private float previousStealProgress;
    private float stealProgress;

    public Sun(float x, float y, int lane, SunType sunType, int amount, int lifeTicks) {
        this(x, y, lane, sunType, amount, lifeTicks, null);
    }

    public Sun(float x, float y, int lane, SunType sunType, int amount, int lifeTicks, Plant sourcePlant) {
        this.x = x;
        this.y = y;
        this.lane = lane;
        this.sunType = sunType;
        this.amount = amount;
        this.remainingTicks = lifeTicks;
        this.sourcePlant = sourcePlant;
        this.grounded = sourcePlant != null;
    }

    public Result tick() {
        if (collected || expired) {
            return new Result(false, "", null);
        }

        livedTicks++;

        if (grounded) {
            groundedTicks++;
        }

        if (remainingTicks != Integer.MAX_VALUE) {
            remainingTicks--;
            if (remainingTicks <= 0) {
                expired = true;
                cancelSteal();
                return new Result(false, "", null);
            }
        }

        if (!grounded && livedTicks >= FALL_DURATION) {
            grounded = true;
            groundedTicks = 0;
            if (sunType == SunType.RADIOACTIVE) {
                sunType = SunType.ORDINARY;
            }
            return new Result(
                true,
                "Sun reached the ground at position (" + ((int) x + 1) + ", " + (lane + 1) + ")\n",
                null
            );
        }
        return new Result(false, "", null);
    }

    public float getFallProgress() {
        return getFallProgress(0f);
    }

    public float getFallProgress(float partialTick) {
        if (grounded) {
            return 1f;
        }

        float visualTicks =
            livedTicks
                + Math.max(
                0f,
                Math.min(1f, partialTick)
            );

        return Math.min(
            1f,
            visualTicks
                / (float) FALL_DURATION
        );
    }

    public void beginSteal(Zombie zombie) {
        if (!isActive()
            || zombie == null
            || sunType == SunType.RADIOACTIVE) {
            return;
        }

        beingStolen = true;
        stealingZombie = zombie;
        previousStealProgress = 0f;
        stealProgress = 0f;
    }

    public void setStealProgress(float progress) {
        if (!beingStolen) {
            return;
        }

        float clamped = Math.max(0f, Math.min(1f, progress));
        previousStealProgress = stealProgress;
        stealProgress = clamped;
    }

    public float getStealProgress(float partialTick) {
        float alpha = Math.max(0f, Math.min(1f, partialTick));
        return previousStealProgress
            + (stealProgress - previousStealProgress) * alpha;
    }

    public void cancelSteal() {
        beingStolen = false;
        stealingZombie = null;
        previousStealProgress = 0f;
        stealProgress = 0f;
    }

    public void finishSteal() {
        previousStealProgress = 1f;
        stealProgress = 1f;
        beingStolen = false;
        stealingZombie = null;
    }

    public boolean isActive() {
        return !collected && !expired;
    }
}
