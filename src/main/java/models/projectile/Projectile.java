package models.projectile;

import models.Plant.PlantTag;
import models.projectile.move.MovingStrategy;
import models.Zombie.Zombie;
import models.games.GameState;

import java.util.List;
import java.util.Set;

public class Projectile {
    private final int damage;
    private final ElementType elementType;
    private final List<PlantTag> tags;
    private final double speed;
    private double posX;
    private final int lane;
    private final MovingStrategy movingStrategy;

    public Projectile(int damage, ElementType elementType, List<PlantTag> tags, double speed,
                      double posX, int lane, MovingStrategy movingStrategy) {
        this.damage      = damage;
        this.elementType = elementType;
        this.tags        = tags;
        this.speed       = speed;
        this.posX        = posX;
        this.lane        = lane;
        this.movingStrategy = movingStrategy;
    }

    private void tick(GameState state) {
        movingStrategy.move(this, speed);
        Zombie target = state.getBoard().getZombieInPosition(lane, (int) posX); // for now it is cast into int
        if (target != null) {
            target.takeDamage(damage, state);
            elementType.onHit(); // each type has its own behavior
            state.getBoard().removeProjectile(this);
        }
    }
}
