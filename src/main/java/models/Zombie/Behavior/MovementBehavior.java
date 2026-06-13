package models.Zombie.Behavior;

import models.Zombie.Zombie;
import models.games.GameState;

import java.util.Collections;
import java.util.List;

public class MovementBehavior implements ZombieBehavior {
    private final MovementType type;
    private final float        extraParam;
    private final List<String> flyOverTargets;

    MovementBehavior(MovementType type) { this(type, 0, Collections.emptyList()); }
    MovementBehavior(MovementType type, float extraParam) { this(type, extraParam, Collections.emptyList()); }
    MovementBehavior(MovementType type, List<String> targets) { this(type, 0, targets); }
    MovementBehavior(MovementType type, float extraParam, List<String> targets) {
        this.type           = type;
        this.extraParam     = extraParam;
        this.flyOverTargets = targets;
    }
    @Override
    public void onTick(Zombie zombie, GameState gs) {}



    public enum MovementType {
        NORMAL_WALK,
        FLY_OVER,           // Dodo
        PUSH_ICE_BLOCK,     // Troglobite
        UNDERGROUND,        // Snorkel
        PROSPECTOR_JUMP,    // Prospector jumps to back row
        PIANO_CRUSH,        // Piano rolls and crushes plants
        TACKLE_RUN          // AllStar charges

    }
}
