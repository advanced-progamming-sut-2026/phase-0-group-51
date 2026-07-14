package models.zombie.Behavior;

import lombok.Getter;
import models.plant.Plant;
import models.zombie.Zombie;
import models.games.GameState;
import models.projectile.ElementType;
import java.util.Map;
@Getter
public class DynamiteBehavior implements PersistableBehavior {
    private final int explosionDelayTicks;
    private int timer = 0;
    private boolean exploded = false;
    private boolean extinguished = false;

    public DynamiteBehavior(int explosionDelayTicks) {
        this.explosionDelayTicks = explosionDelayTicks;
    }

    @Override
    public void onTick(Zombie zombie, GameState gs) {
        if (exploded || extinguished || gs == null) {
            return;
        }
        timer++;
        if (timer >= explosionDelayTicks) {
            explode(zombie);
        }
    }

    private void explode(Zombie zombie) {
        if (exploded) {
            return;
        }
        exploded = true;
        MovementBehavior movement = zombie.getBehavior(MovementBehavior.class);
        if (movement != null) {
            movement.jumpToBackRow(zombie);
        }
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
        }
        return rawDamage;
    }

    @Override
    public String behaviorType() {
        return "DYNAMITE";
    }

    @Override
    public void applyToStatement(Map<String, Object> cols) {
        cols.put("explosion_delay", getExplosionDelayTicks());
    }

    @Override
    public ZombieBehavior copy() {
        return new DynamiteBehavior(explosionDelayTicks);
    }
}
