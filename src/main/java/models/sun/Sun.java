package models.sun;

import lombok.Getter;
import lombok.Setter;
import models.Plant.Plant;
import models.Result;

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
        if (remainingTicks != Integer.MAX_VALUE) {
            remainingTicks--;
            if (remainingTicks <= 0) {
                expired = true;
                return new Result(false, "", null);
            }
        }

        if (!grounded && livedTicks >= FALL_DURATION) {
            grounded = true;
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

    public boolean isActive() {
        return !collected && !expired;
    }
}
