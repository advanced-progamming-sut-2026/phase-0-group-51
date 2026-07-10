package models.Zombie.Behavior;

import models.Board.Board;
import models.Plant.Plant;
import models.Zombie.Zombie;
import models.games.GameState;

import java.sql.PreparedStatement;
import java.sql.SQLException;

public class InstantKillBehavior implements PersistableBehavior {

    private final float speedScale;
    private final boolean alsoKillHypnotized;

    private boolean hasKilled = false;

    public InstantKillBehavior(float speedScale, boolean alsoKillHypnotized) {
        this.speedScale = speedScale;
        this.alsoKillHypnotized = alsoKillHypnotized;
    }

    @Override
    public void onTick(Zombie zombie, GameState gs) {
        if (hasKilled) return;

        Board board = gs.getBoard();
        Plant target = board.findNearestPlantInRange(zombie.getLane(), (int) zombie.getX(), 0);

        if (target != null) {
            target.takeDamage(target.getCurrentHP());
            applySpeedAfterKill(zombie);
            hasKilled = true;
            return;
        }

        if (alsoKillHypnotized) {
            Zombie hypnotized = gs.findNearestHypnotizedZombieInRange(
                zombie, zombie.getLane(), (int) zombie.getX(), 0);

            if (hypnotized != null) {
              //  hypnotized.takeDamage(hypnotized.getMaxHitpoints(), gs, null);
                applySpeedAfterKill(zombie);
                hasKilled = true;
            }
        }
    }

    private void applySpeedAfterKill(Zombie zombie) {
        zombie.setSpeedMultiplier(speedScale);
    }

    @Override
    public boolean suppressesDefaultEating(Zombie zombie) {
        return true;
    }

    @Override public String behaviorType() { return "CONTACT_KILL"; }

    @Override
    public void applyToStatement(PreparedStatement ps) throws SQLException {

    }

    @Override
    public ZombieBehavior copy() {
        return new InstantKillBehavior(speedScale, alsoKillHypnotized);
    }
}
