package models.Zombie.Behavior.Zombotany;

import models.Plant.Plant;
import models.Zombie.Behavior.ZombieBehavior;
import models.Zombie.Zombie;
import models.games.GameState;

public class PeashooterZombieBehavior implements ZombieBehavior {
    private final int intervalTicks;
    private final int range;
    private final int damage;
    private int cooldown = 0;
    private boolean justShot = false;

    public PeashooterZombieBehavior(int intervalTicks, int range, int damage) {
        this.intervalTicks = Math.max(1, intervalTicks);
        this.range = range;
        this.damage = damage;
    }

    @Override
    public void onTick(Zombie zombie, GameState gs) {
        if (gs == null || zombie.isDead()) {
            return;
        }
        if (cooldown > 0) {
            cooldown--;
            return;
        }
        Plant target = gs.getBoard()
            .findNearestPlantInRange(zombie.getLane(), (int) zombie.getX(), range);
        if (target == null || target.isDead()) {
            return;
        }
        target.takeDamage(damage, gs);
        cooldown = intervalTicks;
        justShot = true;
        gs.logEvent(zombie.getAlias() + " shot a pea that hit a plant in lane "
            + (zombie.getLane() + 1) + " for " + damage + " damage.\n");
    }

    public boolean consumeShot() {
        if (!justShot) {
            return false;
        }
        justShot = false;
        return true;
    }

    @Override
    public ZombieBehavior copy() {
        return new PeashooterZombieBehavior(intervalTicks, range, damage);
    }
}
