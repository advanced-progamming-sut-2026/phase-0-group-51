package models.Zombie.Behavior;

import lombok.Getter;
import models.Board.Board;
import models.Zombie.Zombie;
import models.games.GameState;
import models.sun.Sun;
import models.sun.SunType;

import java.util.ArrayList;
import java.util.Map;

@Getter
public class SunStealBehavior implements PersistableBehavior {
    private static final int MIN_SPAWN_AGE_TICKS = 3 * 10;
    private static final int PULL_DURATION_TICKS = 12;

    private final int maxAmount;
    private final int intervalTicks;
    private int totalStolen = 0;
    private int cooldownTicks = 0;

    private Sun targetSun;
    private int pullTicks;

    public SunStealBehavior(int maxAmount, int intervalTicks) {
        this.maxAmount = maxAmount;
        this.intervalTicks = intervalTicks;
    }

    private boolean isFull() {
        return totalStolen >= maxAmount;
    }

    public boolean isStealing() {
        return targetSun != null
            && targetSun.isActive()
            && targetSun.isBeingStolen();
    }

    @Override
    public void onTick(Zombie zombie, GameState gs) {
        if (targetSun != null) {
            updateSteal(zombie, gs);
            return;
        }

        if (isFull()) {
            return;
        }

        if (cooldownTicks > 0) {
            cooldownTicks--;
            return;
        }

        Sun target = findTargetSun(zombie, gs.getBoard());
        if (target == null) {
            return;
        }

        targetSun = target;
        pullTicks = 0;
        targetSun.beginSteal(zombie);
    }

    private Sun findTargetSun(Zombie zombie, Board board) {
        Sun target = null;
        float bestDistance = Float.MAX_VALUE;

        for (Sun sun : new ArrayList<>(board.getActiveSuns())) {
            if (!sun.isActive()
                || sun.getSunType() == SunType.RADIOACTIVE
                || sun.getLivedTicks() < MIN_SPAWN_AGE_TICKS
                || sun.isBeingStolen()) {
                continue;
            }

            float distance = Math.abs(sun.getX() - zombie.getX())
                + Math.abs(sun.getLane() - zombie.getLane()) * 2f;

            if (distance < bestDistance) {
                bestDistance = distance;
                target = sun;
            }
        }

        return target;
    }

    private void updateSteal(Zombie zombie, GameState gs) {
        if (!targetSun.isActive()
            || !targetSun.isBeingStolen()
            || targetSun.getStealingZombie() != zombie) {
            clearTarget(true);
            return;
        }

        pullTicks++;

        float progress = Math.min(
            1f,
            pullTicks / (float) PULL_DURATION_TICKS
        );

        targetSun.setStealProgress(progress);

        if (pullTicks < PULL_DURATION_TICKS) {
            return;
        }

        finishSteal(zombie, gs);
    }

    private void finishSteal(Zombie zombie, GameState gs) {
        Sun stolen = targetSun;
        int stolenAmount = stolen.getAmount();

        stolen.finishSteal();
        gs.getBoard().removeSun(stolen);

        totalStolen += stolenAmount;

        gs.logEvent(
            "Zombie " + zombie.getAlias() + " stole the sun at position ("
                + ((int) Math.floor(stolen.getX()) + 1) + ", "
                + (stolen.getLane() + 1)
                + ")! Kill it to get your sun back.\n"
        );

        targetSun = null;
        pullTicks = 0;
        cooldownTicks = intervalTicks;
    }

    private void clearTarget(boolean startCooldown) {
        if (targetSun != null && targetSun.isActive()) {
            targetSun.cancelSteal();
        }

        targetSun = null;
        pullTicks = 0;

        if (startCooldown) {
            cooldownTicks = intervalTicks;
        }
    }

    @Override
    public boolean suppressesMovement(Zombie zombie) {
        return isStealing();
    }

    @Override
    public boolean suppressesDefaultEating(Zombie zombie) {
        return isStealing();
    }

    @Override
    public void onDeath(Zombie zombie, GameState gs) {
        if (targetSun != null
            && targetSun.isActive()
            && targetSun.getStealingZombie() == zombie) {
            targetSun.cancelSteal();
        }

        targetSun = null;
        pullTicks = 0;

        if (totalStolen > 0) {
            gs.increaseSunBalance(totalStolen);
            gs.logEvent("Zombie " + zombie.getAlias() + " died and dropped "
                + totalStolen + " stolen sun.\n");
            totalStolen = 0;
        }
    }

    @Override
    public String behaviorType() {
        return "SUN_STEAL";
    }

    @Override
    public void applyToStatement(Map<String, Object> cols) {
        cols.put("max_amount", maxAmount);
        cols.put("steal_interval", intervalTicks);
    }

    @Override
    public ZombieBehavior copy() {
        return new SunStealBehavior(maxAmount, intervalTicks);
    }
}
