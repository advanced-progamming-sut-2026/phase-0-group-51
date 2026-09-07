package models.Zombie.Behavior.Zombotany;

import models.Plant.Plant;
import models.Zombie.Behavior.ZombieBehavior;
import models.Zombie.Zombie;
import models.games.GameState;

public class SquashZombieBehavior implements ZombieBehavior {
    private static final float CONTACT_RANGE = 0.7f;

    private boolean squashed = false;

    @Override
    public void onTick(Zombie zombie, GameState gs) {
        if (squashed || gs == null || zombie.isDead()) {
            return;
        }
        Plant target = gs.getBoard()
            .findNearestPlantInRange(zombie.getLane(), zombie.getX(), CONTACT_RANGE);
        if (target == null || target.isDead()) {
            return;
        }
        squashed = true;
        zombie.killInstantly(gs);
        target.takeDamage(target.getCurrentHP(), gs);
        gs.logEvent(zombie.getAlias() + " squashed a plant in lane "
            + (zombie.getLane() + 1) + " and was destroyed with it!\n");
    }

    @Override
    public boolean suppressesDefaultEating(Zombie zombie) {
        return true;
    }

    @Override
    public ZombieBehavior copy() {
        return new SquashZombieBehavior();
    }
}
