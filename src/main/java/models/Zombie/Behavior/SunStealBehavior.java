package models.Zombie.Behavior;

import lombok.Getter;
import models.Board.Board;
import models.Zombie.Zombie;
import models.games.GameState;
import models.sun.Sun;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

@Getter
public class SunStealBehavior implements PersistableBehavior {
    private final int maxAmount;// MaxClaimedSunCurrency
    private int totalStolen = 0;
    private final int intervalTicks;

    public SunStealBehavior(int maxAmount, int intervalTicks) {
        this.maxAmount = maxAmount;
        this.intervalTicks = intervalTicks;
    }
    private boolean isFull() {
        return  totalStolen >= maxAmount;
    }
    @Override
    public void onTick(Zombie zombie, GameState gs) {
        if (isFull()) return;

        Board board = gs.getBoard();
        List<Sun> activeSuns = board.getActiveSuns();

        for (Sun sun : activeSuns) {
            if (isFull()) break;

            totalStolen += sun.getAmount();
            board.removeSun(sun);
        }
    }
    public void onDeath(Zombie zombie, GameState gs) {
        if (totalStolen > 0) {
            gs.addSun(totalStolen);
            totalStolen = 0;
        }
    }
    @Override public String behaviorType() { return "SUN_STEAL"; }

    @Override
    public void applyToStatement(PreparedStatement ps) throws SQLException {
        ps.setInt(30, maxAmount);
    }
    @Override
    public ZombieBehavior copy() {
        return new SunStealBehavior(maxAmount, intervalTicks);
    }
}
