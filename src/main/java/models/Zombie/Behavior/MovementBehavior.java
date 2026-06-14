package models.Zombie.Behavior;

import lombok.Getter;
import models.Zombie.Zombie;
import models.games.GameState;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
@Getter
public class MovementBehavior implements PersistableBehavior {
    private final MovementType type;
    private final float        extraParam;
    private final List<String> flyOverTargets;

    public MovementBehavior(MovementType type) { this(type, 0, Collections.emptyList()); }
    public MovementBehavior(MovementType type, float extraParam) { this(type, extraParam, Collections.emptyList()); }
    public MovementBehavior(MovementType type, List<String> targets) { this(type, 0, targets); }
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
        TACKLE_RUN ,      // AllStar charges
        PUSH_PLANT_BACK

    }

    @Override public String behaviorType() { return "MOVEMENT"; }

    @Override
    public void applyToStatement(PreparedStatement ps) throws SQLException {
        ps.setString(16, type.name()); // movement_type
        ps.setDouble(17, extraParam);  // movement_param
    }
}
