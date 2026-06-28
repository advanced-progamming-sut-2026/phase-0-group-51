package models.Zombie.Behavior;

import lombok.Getter;
import models.Plant.Plant;
import models.Plant.PlantType;
import models.Zombie.Zombie;
import models.games.GameState;
import models.projectile.ElementType;

import java.sql.PreparedStatement;
import java.sql.SQLException;

@Getter
public class TorchBehavior implements PersistableBehavior {

    private final InstantKillBehavior killLogic;
    private boolean lit = true;

    public TorchBehavior() {
        this.killLogic = new InstantKillBehavior(1,true);
    }

    @Override
    public void onTick(Zombie zombie, GameState gs) {
        if (!lit) return;
        killLogic.onTick(zombie, gs);
    }

    @Override
    public boolean suppressesDefaultEating(Zombie zombie) {
        return lit;
    }

    @Override
    public int onHit(Zombie zombie, int rawDamage, ElementType element, Plant plant) {
        if (element == ElementType.ICE) {
            lit = false;
        } else if (element == ElementType.FIRE) {
            lit = true;
        }
        return rawDamage;
    }

    @Override public String behaviorType() { return "TORCH"; }

    @Override
    public void applyToStatement(PreparedStatement ps) throws SQLException {

    }

    @Override
    public ZombieBehavior copy() {
        return new TorchBehavior();
    }
}
