package models.Zombie.Behavior;

import lombok.Getter;
import models.Board.Board;
import models.Zombie.Zombie;
import models.games.GameState;
import models.sun.Sun;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
@Getter
public class SunStealBehavior implements PersistableBehavior {
    private final int maxAmount;
    private final int intervalTicks;
    private int totalStolen = 0;

    public SunStealBehavior(int maxAmount, int intervalTicks) {
        this.maxAmount = maxAmount;
        this.intervalTicks = intervalTicks;
    }

    private boolean isFull() {
        return totalStolen >= maxAmount;
    }

    @Override
    public void onTick(Zombie zombie, GameState gs) {
        if (isFull()) return;

        Board board = gs.getBoard();
        List<Sun> activeSuns = new ArrayList<>(board.getActiveSuns());

        for (Sun sun : activeSuns) {
            if (isFull()) break;

            totalStolen += sun.getAmount();
            board.removeSun(sun);
        }
    }

    @Override
    public void onDeath(Zombie zombie, GameState gs) {
        if (totalStolen > 0) {
            gs.increaseSunBalance(totalStolen);
            totalStolen = 0;
        }
    }


    @Override
    public String behaviorType() {
        return "SUN_STEAL";
    }

    @Override
    public void applyToStatement(Map<String, Object> cols) {
        cols.put("max_amount", getMaxAmount());
        cols.put("steal_interval", getIntervalTicks());
    }

    @Override
    public ZombieBehavior copy() {
        return new SunStealBehavior(maxAmount, intervalTicks);
    }
}
