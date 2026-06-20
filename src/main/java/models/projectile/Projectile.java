package models.projectile;

import models.Plant.PlantTag;
import models.projectile.move.MovingStrategy;
import models.Zombie.Zombie;
import models.games.GameState;

import java.util.Set;

public class Projectile {
    private final int damage;
    private final ElementType elementType;
    private final Set<PlantTag> tags;
    private final double speed;
    private double posX;
    private final int lane;
    private final MovingStrategy movingStrategy;

    public Projectile(int damage, ElementType elementType, Set<PlantTag> tags, double speed,
                      double posX, int lane, MovingStrategy movingStrategy) {
        this.damage      = damage;
        this.elementType = elementType;
        this.tags        = tags;
        this.speed       = speed;
        this.posX        = posX;
        this.lane        = lane;
        this.movingStrategy = movingStrategy;
    }

    private void hit(Zombie zombie, GameState state) {}
    private void move(){}
}
