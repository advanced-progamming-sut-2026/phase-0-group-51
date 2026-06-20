package models.Zombie.Behavior;

import models.Board.Board;
import models.Plant.Plant;
import models.Zombie.Zombie;
import models.games.GameState;
import models.projectile.ElementType;

import java.sql.PreparedStatement;
import java.sql.SQLException;

public class TorchBehavior implements PersistableBehavior{
    private final int range;
    private boolean lit = true;
    public TorchBehavior(int range) {
        this.range = range;
    }
    @Override
    public void onTick(Zombie zombie, GameState gs) {
        if (!lit) return;
        Board board = gs.getBoard();
        Plant target = board.findNearestPlantInRange(zombie.getLane(), (int) zombie.getX(), range);
        if (target != null) {
            target.takeDamage(target.getCurrentHP());
        }
    }
    @Override
    public int onHit(Zombie zombie, int rawDamage, ElementType element) {
        if (element == ElementType.ICE) {
            lit = false;
        } else if (element == ElementType.FIRE) {
            lit = true;
        }
        return rawDamage;
    }

    @Override public String behaviorType() { return "TORCH"; }

    @Override
    public void applyToStatement(PreparedStatement ps) throws SQLException {
        ps.setInt(33, range);
    }

    @Override
    public ZombieBehavior copy() {
        return new TorchBehavior(range);
    }
}
