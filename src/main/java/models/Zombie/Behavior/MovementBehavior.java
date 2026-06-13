package models.Zombie.Behavior;

import models.Zombie.Zombie;

public class MovementBehavior implements ZombieBehavior {
    private final MovementType movementType;
    private int speed;

    public MovementBehavior(MovementType type) {
        this.movementType = type;
    }
    public MovementType getMovementType() { return movementType; }
    @Override
    public void onTick(Zombie zombie) {}



    public enum MovementType {

    }
}
