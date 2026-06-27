package models.Zombie.Behavior;

import lombok.Getter;
import lombok.Setter;
import models.Plant.Plant;
import models.Zombie.Zombie;
import models.games.GameState;
import models.projectile.ElementType;

import java.sql.PreparedStatement;
import java.sql.SQLException;
@Getter
@Setter
public class DynamiteBehavior implements PersistableBehavior{
    private final int explosionDelayTicks;
    private int timer = 0;
    private boolean exploded = false;
    private boolean extinguished = false;

    public DynamiteBehavior(int explosionDelayTicks) {
        this.explosionDelayTicks = explosionDelayTicks;
    }
    @Override
    public void onTick(Zombie zombie, GameState gs) {
        if (exploded || extinguished || gs == null) return;

        timer++;

        if (timer >= explosionDelayTicks) {
            explode(zombie, gs);
        }
    }

    private void explode(Zombie zombie, GameState gs) {
        if (exploded) return;
        exploded = true;
        moveZombieToFirstColumn(zombie, gs);
    }
    private void moveZombieToFirstColumn(Zombie zombie, GameState gs) {
        zombie.setX(0);
    }
    public void extinguish() {
        if (!exploded) {
            extinguished = true;
        }
    }
    @Override
    public int onHit(Zombie zombie, int rawDamage, ElementType element, Plant plant) {
        if (element == ElementType.ICE && !exploded) {
            extinguish();
            return 0;
        }
        return rawDamage;
    }
    @Override
    public String behaviorType() {
        return "DYNAMITE";
    }
    @Override
    public void applyToStatement(PreparedStatement ps) throws SQLException {

    }

    @Override
    public ZombieBehavior copy() {
        return new DynamiteBehavior(explosionDelayTicks);
    }

}
