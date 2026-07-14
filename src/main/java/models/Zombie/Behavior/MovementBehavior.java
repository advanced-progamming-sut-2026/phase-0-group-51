package models.Zombie.Behavior;

import lombok.Getter;
import models.Board.Board;
import models.Plant.Plant;
import models.Zombie.Zombie;
import models.games.GameState;

import java.util.Collections;
import java.util.List;
import java.util.Map;
@Getter
public class MovementBehavior implements PersistableBehavior {
    private final MovementType type;
    private final float extraParam;
    private final List<String> flyOverTargets;

    private boolean pianoSpeedApplied = false;

    public MovementBehavior(MovementType type) {
        this(type, 0, Collections.emptyList());
    }

    public MovementBehavior(MovementType type, float extraParam) {
        this(type, extraParam, Collections.emptyList());
    }

    public MovementBehavior(MovementType type, List<String> targets) {
        this(type, 0, targets);
    }

    MovementBehavior(MovementType type, float extraParam, List<String> targets) {
        this.type = type;
        this.extraParam = extraParam;
        this.flyOverTargets = targets;
    }

    @Override
    public void onTick(Zombie zombie, GameState gs) {
        Board board = gs.getBoard();
        int lane = zombie.getLane();
        int col = (int) zombie.getX();

        switch (type) {
            case FLY_OVER -> {
                // Dodo rider
            }
            case UNDERGROUND -> {
                // Snorkel
            }
            case PIANO_CRUSH -> {
                if (!pianoSpeedApplied && extraParam > 0) {
                    zombie.setSpeedMultiplier(extraParam);
                    pianoSpeedApplied = true;
                }
                Plant crushed = board.findNearestPlantInRange(lane, col, 0);
                if (crushed != null) {
                    crushed.takeDamage(crushed.getCurrentHP());
                }
            }
            default -> {
                // NORMAL_WALK / PROSPECTOR_JUMP
            }
        }
    }

    public void jumpToBackRow(Zombie zombie) {
        if (type != MovementType.PROSPECTOR_JUMP) {
            return;
        }
        zombie.setX(0);
        if (zombie.getDirection() > 0) {
            zombie.reverseDirection();
        }
    }

    @Override
    public boolean suppressesDefaultEating(Zombie zombie) {
        return type == MovementType.PIANO_CRUSH;
    }

    public enum MovementType {
        NORMAL_WALK, FLY_OVER, UNDERGROUND, PROSPECTOR_JUMP, PIANO_CRUSH
    }

    @Override
    public String behaviorType() {
        return "MOVEMENT";
    }

    @Override
    public void applyToStatement(Map<String, Object> cols) {
        cols.put("movement_type", getType().name());
        cols.put("movement_param", getExtraParam());
        if (!getFlyOverTargets().isEmpty()) {
            cols.put("movement_targets", String.join(",", getFlyOverTargets()));
        }
    }

    @Override
    public ZombieBehavior copy() {
        return new MovementBehavior(type, extraParam, flyOverTargets);
    }
}
