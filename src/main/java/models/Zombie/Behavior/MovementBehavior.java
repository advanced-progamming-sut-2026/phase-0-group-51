package models.Zombie.Behavior;

import lombok.Getter;
import models.Board.Board;
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
    public void onTick(Zombie zombie, GameState gs) {
        Board board = gs.getBoard();
        int lane = zombie.getLane();
        int col  = (int) zombie.getX();

        switch (type) {
            case FLY_OVER -> {

            }


        }
    }

    public void jumpToBackRow(Zombie zombie, GameState gs) {
        if (type != MovementType.PROSPECTOR_JUMP) return;
        int lastColumn = gs.getBoard().getColumnCount() - 1;
        zombie.setX(lastColumn);
    }

    public enum MovementType {
        NORMAL_WALK, FLY_OVER, PUSH_ICE_BLOCK, UNDERGROUND,
        PROSPECTOR_JUMP, PIANO_CRUSH, PUSH_PLANT_BACK
    }

    @Override public String behaviorType() { return "MOVEMENT"; }

    @Override
    public void applyToStatement(PreparedStatement ps) throws SQLException {

    }

    @Override
    public ZombieBehavior copy() {
        return new MovementBehavior(type, extraParam, flyOverTargets);
    }
}
