package models.sun;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Sun {
    private float x, y;
    private int lane;
    private final SunType sunType;
    private final int amount;

    private boolean collected = false;
    private boolean expired = false;
    private boolean grounded = false;

    public Sun(float x, float y, int lane, SunType sunType, int amount) {
        this.x = x;
        this.y = y;
        this.lane = lane;
        this.sunType = sunType;
        this.amount = amount;
    }

    public boolean isActive() {
        return !collected && !expired;
    }
}
