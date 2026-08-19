package models.Zombie.Behavior;

import models.Zombie.Zombie;
import models.games.GameState;

public final class SandstormTransportBehavior implements ZombieBehavior {

    public static final int DEFAULT_DURATION_TICKS = 10;

    private final float startX;
    private final float targetX;
    private final int durationTicks;

    private int elapsedTicks;
    private boolean active = true;

    public SandstormTransportBehavior(
        float startX,
        float targetX
    ) {
        this(
            startX,
            targetX,
            DEFAULT_DURATION_TICKS
        );
    }

    public SandstormTransportBehavior(
        float startX,
        float targetX,
        int durationTicks
    ) {
        this.startX = startX;
        this.targetX = targetX;
        this.durationTicks = Math.max(1, durationTicks);
    }

    public float getStartX() {
        return startX;
    }

    public float getTargetX() {
        return targetX;
    }

    public int getDurationTicks() {
        return durationTicks;
    }

    public int getElapsedTicks() {
        return elapsedTicks;
    }

    public boolean isActive() {
        return active;
    }

    public float getProgress() {
        if (!active) {
            return 1f;
        }

        return Math.max(
            0f,
            Math.min(
                1f,
                elapsedTicks / (float) durationTicks
            )
        );
    }

    @Override
    public void onTick(
        Zombie zombie,
        GameState gs
    ) {
        if (!active || zombie == null || zombie.isDead()) {
            return;
        }

        elapsedTicks++;

        float t = Math.max(
            0f,
            Math.min(
                1f,
                elapsedTicks / (float) durationTicks
            )
        );

        float eased =
            t * t * (3f - 2f * t);

        zombie.setX(
            startX
                + (targetX - startX) * eased
        );

        if (elapsedTicks >= durationTicks) {
            zombie.setX(targetX);
            active = false;
        }
    }

    @Override
    public boolean suppressesDefaultEating(Zombie zombie) {
        return active;
    }

    @Override
    public boolean suppressesMovement(Zombie zombie) {
        return active;
    }

    @Override
    public void onDeath(
        Zombie zombie,
        GameState gs
    ) {
        active = false;
    }

    @Override
    public ZombieBehavior copy() {
        return new SandstormTransportBehavior(
            startX,
            targetX,
            durationTicks
        );
    }
}
