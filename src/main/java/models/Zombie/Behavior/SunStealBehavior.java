package models.Zombie.Behavior;

import lombok.Getter;
import models.Board.Board;
import models.Zombie.Zombie;
import models.games.GameState;
import models.sun.Sun;

import java.util.ArrayList;
import java.util.Map;

@Getter
public class SunStealBehavior implements PersistableBehavior {
    private final int maxAmount;
    private final int intervalTicks;
    private int totalStolen = 0;
    private int cooldownTicks = 0;

    public SunStealBehavior(int maxAmount, int intervalTicks) {
        this.maxAmount = maxAmount;
        this.intervalTicks = intervalTicks;
    }

    private boolean isFull() {
        return totalStolen >= maxAmount;
    }

    @Override
    public void onTick(Zombie zombie, GameState gs) {
        if (isFull()) {
            return;
        }
        if (cooldownTicks > 0) {
            cooldownTicks--;
            return;
        }

        Board board = gs.getBoard();

        Sun target = null;
        float bestDistance = Float.MAX_VALUE;
        for (Sun sun : new ArrayList<>(board.getActiveSuns())) {
            float distance = Math.abs(sun.getX() - zombie.getX())
                + Math.abs(sun.getLane() - zombie.getLane()) * 2f;
            if (distance < bestDistance) {
                bestDistance = distance;
                target = sun;
            }
        }
        if (target == null) {
            return;
        }

        totalStolen += target.getAmount();
        board.removeSun(target);
        gs.logEvent("Zombie " + zombie.getAlias() + " stole the sun at position ("
            + ((int) Math.floor(target.getX()) + 1) + ", " + (target.getLane() + 1)
            + ")! Kill it to get your sun back.\n");
        cooldownTicks = intervalTicks;
    }

    @Override
    public void onDeath(Zombie zombie, GameState gs) {
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
