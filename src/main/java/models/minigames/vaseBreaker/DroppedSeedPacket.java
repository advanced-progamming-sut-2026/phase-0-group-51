package models.minigames.vaseBreaker;

import lombok.Getter;
import lombok.Setter;
import java.util.Objects;
@Getter
@Setter
public class DroppedSeedPacket {
    private final String plantName;
    private final int x;
    private final int y;
    private final int expiresAtTick;
    private boolean pickedUp;

    public DroppedSeedPacket(String plantName, int x, int y, int expiresAtTick) {
        if (plantName == null || plantName.isBlank()) {
            throw new IllegalArgumentException(
                    "Plant name cannot be empty."
            );
        }

        if (x < 1 || x > 9 || y < 1 || y > 5) {
            throw new IllegalArgumentException(
                    "Seed packet coordinates are outside the board."
            );
        }

        if (expiresAtTick < 0) {
            throw new IllegalArgumentException(
                    "Expiration tick cannot be negative."
            );}
        this.plantName = Objects.requireNonNull(plantName);
        this.x = x;
        this.y = y;
        this.expiresAtTick = expiresAtTick;
    }

    public boolean isExpired(int currentTick) {
        return !pickedUp && currentTick >= expiresAtTick;
    }

    public boolean isActive(int currentTick) {
        return !pickedUp && !isExpired(currentTick);
    }

    public void pickUp() {
        if (pickedUp) {
            throw new IllegalStateException("This seed packet was already picked up.");
        }
        pickedUp = true;
    }

    public int ticksRemaining(int currentTick) {
        return Math.max(0, expiresAtTick - currentTick);
    }

}
