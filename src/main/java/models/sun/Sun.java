package models.sun;

import lombok.Getter;
import lombok.Setter;
import models.Board.Tile;
import models.Result;

@Getter
@Setter
public class Sun {
    private float x;
    private float y;
    private int lane;
    private SunType sunType;
    private int amount;
    private int remainingTicks;
    private boolean collected;
    private boolean expired;
    private boolean grounded;
    private int fallingTicks;
    private int livedTicks = 0;
    private static final int TICKS_PER_SECOND = 10;
    private static final int FALL_DURATION = 5 * TICKS_PER_SECOND;
    public Sun(float x, float y, int lane, SunType sunType, int amount, int lifeTicks) {
        this.x = x;
        this.y = y;
        this.lane = lane;
        this.sunType = sunType;
        this.amount = amount;
        this.remainingTicks = lifeTicks;
    }

    public Result tick() {
        if (collected || expired)
            return new Result(false,"",null);
        livedTicks++;
        if (remainingTicks != Integer.MAX_VALUE) {
            remainingTicks--;
            if (remainingTicks <= 0) expired = true;
        }
        if (!grounded) {
            float targetY = lane * Tile.TILEHEIGHT;
            y += targetY/FALL_DURATION;
            if (livedTicks >= FALL_DURATION) {
                grounded = true;
                y = targetY;
                if (sunType == SunType.RADIOACTIVE) {
                    sunType = SunType.ORDINARY;
                }
                return new Result(true,"Sun reached the ground at position ("+x+", "+y+")\n",null);
            }
        }
        return new Result(false,"",null);
    }

    public boolean isActive() {
        return !collected && !expired;
    }
}
