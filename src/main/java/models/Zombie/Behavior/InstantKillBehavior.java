package models.Zombie.Behavior;

import lombok.Getter;
import models.Board.Board;
import models.Plant.Plant;
import models.Zombie.Zombie;
import models.games.GameState;
import java.util.Map;


@Getter
public class InstantKillBehavior implements PersistableBehavior {

    private final float speedScaleAfterKill;
    private final boolean alsoKillHypnotized;
    private final float runningSpeedScale;
    private final boolean repeating;

    private boolean hasKilled = false;
    private boolean runningApplied = false;

    public InstantKillBehavior(float speedScaleAfterKill, boolean alsoKillHypnotized) {
        this(speedScaleAfterKill, alsoKillHypnotized, 0f, false);
    }

    public InstantKillBehavior(float speedScaleAfterKill, boolean alsoKillHypnotized,
                               float runningSpeedScale) {
        this(speedScaleAfterKill, alsoKillHypnotized, runningSpeedScale, false);
    }

    public InstantKillBehavior(float speedScaleAfterKill, boolean alsoKillHypnotized,
                               float runningSpeedScale, boolean repeating) {
        this.speedScaleAfterKill = speedScaleAfterKill;
        this.alsoKillHypnotized = alsoKillHypnotized;
        this.runningSpeedScale = runningSpeedScale;
        this.repeating = repeating;
    }

    @Override
    public void onTick(Zombie zombie, GameState gs) {
        if (hasKilled && !repeating) {
            return;
        }
        if (!runningApplied && runningSpeedScale > 0) {
            applySpeedScale(zombie, runningSpeedScale);
            runningApplied = true;
        }

        Board board = gs.getBoard();
        Plant target = board.findNearestPlantInRange(zombie.getLane(), (int) zombie.getX(), 0);
        if (target != null) {
            target.takeDamage(target.getCurrentHP(), gs);
            afterKill(zombie);
            return;
        }

        if (alsoKillHypnotized) {
            Zombie hypnotized = gs.findNearestHypnotizedZombieInRange(
                zombie, zombie.getLane(), (int) zombie.getX(), 0);
            if (hypnotized != null) {
                hypnotized.killInstantly(gs);
                afterKill(zombie);
            }
        }
    }

    private void afterKill(Zombie zombie) {
        hasKilled = true;
        if (!repeating) {
            applySpeedScale(zombie, speedScaleAfterKill);
        }
    }

    private void applySpeedScale(Zombie zombie, float scale) {
        zombie.setSpeedMultiplier(zombie.getBaseSpeed() * scale);
    }


    @Override
    public boolean suppressesDefaultEating(Zombie zombie) {
        return repeating || !hasKilled;
    }

    @Override
    public String behaviorType() {
        return "CONTACT_KILL";
    }

    @Override
    public void applyToStatement(Map<String, Object> cols) {
        cols.put("speed_scale", getSpeedScaleAfterKill());
        cols.put("kill_hypnotized", isAlsoKillHypnotized() ? 1 : 0);
        cols.put("running_speed_scale", getRunningSpeedScale());
        cols.put("repeating", isRepeating() ? 1 : 0);
    }

    @Override
    public ZombieBehavior copy() {
        return new InstantKillBehavior(speedScaleAfterKill, alsoKillHypnotized,
            runningSpeedScale, repeating);
    }
}
