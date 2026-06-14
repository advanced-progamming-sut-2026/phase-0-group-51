package models.Zombie.Behavior;

import lombok.Getter;
import models.Zombie.Zombie;
import models.games.GameState;
@Getter
public class AuraBehavior implements ZombieBehavior {
    private final AuraType auraType;
    private final float    radius;
    private final int      intervalTicks;
    private int timer;

    public AuraBehavior(AuraType type, float radius, int intervalTicks) {
        this.auraType      = type;
        this.radius        = radius;
        this.intervalTicks = intervalTicks;
    }

    @Override
    public void onTick(Zombie zombie, GameState gs) {}





    public enum AuraType {
        BUFF_SPEED_NEARBY,   // DarkKing
        BUFF_DAMAGE_NEARBY,  // DarkKing
        STEAL_SUN_PASSIVE    // Ra

    }
}
