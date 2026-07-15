package models.Zombie.Behavior;

import models.Plant.Plant;
import models.Zombie.Zombie;
import models.games.GameState;
import models.projectile.ElementType;
import java.util.Map;


public class TorchBehavior implements PersistableBehavior {

    private final int reachTiles;
    private boolean lit = true;

    public TorchBehavior() {
        this(0);
    }

    public TorchBehavior(int reachTiles) {
        this.reachTiles = reachTiles;
    }

    @Override
    public void onTick(Zombie zombie, GameState gs) {
        if (!lit) {
            return;
        }
        Plant target = gs.getBoard()
            .findNearestPlantInRange(zombie.getLane(), (int) zombie.getX(), reachTiles);
        if (target != null) {
            target.takeDamage(target.getCurrentHP(),gs);
        }
    }

    @Override
    public boolean suppressesDefaultEating(Zombie zombie) {
        return lit;
    }

    @Override
    public int onHit(Zombie zombie, int rawDamage, ElementType element, Plant plant) {
        if (element == ElementType.ICE) {
            lit = false;
        } else if (element == ElementType.FIRE) {
            lit = true;
        }
        return rawDamage;
    }

    public boolean isLit() {
        return lit;
    }

    public int getReachTiles() {
        return reachTiles;
    }

    @Override
    public String behaviorType() {
        return "TORCH";
    }

    @Override
    public void applyToStatement(Map<String, Object> cols) {
        cols.put("torch_reach", getReachTiles());
    }

    @Override
    public ZombieBehavior copy() {
        return new TorchBehavior(reachTiles);
    }
}
