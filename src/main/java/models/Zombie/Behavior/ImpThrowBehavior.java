package models.Zombie.Behavior;

import Data.loader.ZombieRegistry;
import lombok.Getter;
import models.Zombie.Zombie;
import models.games.GameState;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.function.BiConsumer;


@Getter
public class ImpThrowBehavior implements PersistableBehavior {

    private final int hpThreshold;
    private final int impCount;
    private final int targetColumn;
    private boolean fired = false;


    public void spawnImps(int lane, int column, int count, GameState gs) {
        for (int i = 0; i < count; i++) {
            Zombie imp = ZombieRegistry.getTemplate("ZombieImp");
            imp.setLane(lane);
            imp.setX(column);
            gs.addZombie(imp);
        }

    }

    public ImpThrowBehavior(int hpThreshold, int impCount, int targetColumn) {
        this.hpThreshold = hpThreshold;
        this.impCount = impCount;
        this.targetColumn = targetColumn;
    }

    @Override
    public void onTick(Zombie zombie, GameState gs) {
        if (fired || zombie.getHitpoints() > hpThreshold) return;
        fired = true;
        spawnImps(zombie.getLane(), targetColumn, impCount, gs);
    }

    @Override public String behaviorType() { return "IMP_THROW"; }

    @Override
    public void applyToStatement(PreparedStatement ps) throws SQLException {

    }
    @Override
    public ZombieBehavior copy() {
        return new ImpThrowBehavior(hpThreshold, impCount, targetColumn);
    }
}
