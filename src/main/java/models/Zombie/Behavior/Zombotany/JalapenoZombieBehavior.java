package models.Zombie.Behavior.Zombotany;

import models.Plant.Plant;
import models.Zombie.Behavior.ZombieBehavior;
import models.Zombie.Zombie;
import models.games.GameState;

public class JalapenoZombieBehavior implements ZombieBehavior {
    private final int fuseTicks;
    private int timer = 0;
    private boolean burned = false;

    public JalapenoZombieBehavior(int fuseTicks) {
        this.fuseTicks = fuseTicks;
    }

    @Override
    public void onTick(Zombie zombie, GameState gs) {
        if (burned || gs == null || zombie.isDead()) {
            return;
        }
        timer++;
        if (timer < fuseTicks) {
            return;
        }
        burned = true;
        int lane = zombie.getLane();
        for (Plant plant : gs.getBoard().getPlantsInLane(lane)) {
            if (!plant.isDead()) {
                plant.takeDamage(plant.getCurrentHP(), gs);
            }
        }
        gs.logEvent(zombie.getAlias() + " exploded and burned every plant in lane "
            + (lane + 1) + "!\n");
        zombie.killInstantly(gs);
    }

    @Override
    public ZombieBehavior copy() {
        return new JalapenoZombieBehavior(fuseTicks);
    }
}
