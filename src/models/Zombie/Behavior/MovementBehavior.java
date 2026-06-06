package models.Zombie.Behavior;

import models.Zombie.Zombie;

public class MovementBehavior implements ZombieBehavior {
    private final MovementType movementType;

    public MovementBehavior(MovementType type) {
        this.movementType = type;
    }
    public MovementType getMovementType() { return movementType; }
    @Override
    public void onTick(Zombie zombie) {}





    public enum MovementType {
        NORMAL_WALK,     // default
        UNDERGROUND,     // Snorkel swims underwater until reaching plant
        FLY_OVER,        // Dodo flies over plants
        PUSH_ICE_BLOCK,  // Troglobite pushes ice block, crushing plants
        CAMEL_CHAIN,     // Camel → segments trail behind
        SURFER_BOARD,    // Surfer rides wave then walks when board gone
        FAST_SWIM        // FastSwimmer — faster underwater movement
    }
}
