package models.Zombie.Behavior;

import lombok.Getter;
import models.Zombie.Zombie;
import models.games.GameState;
@Getter
public class WorldEffectBehavior implements ZombieBehavior {
    private final WorldEffectType type;
    private final int             intervalTicks;
    private final int             count;
    private int cooldown;

    public WorldEffectBehavior(WorldEffectType type, int intervalTicks, int count) {
        this.type          = type;
        this.intervalTicks = intervalTicks;
        this.count         = count;
        this.cooldown      = intervalTicks;
    }

    @Override
    public void onTick(Zombie zombie, GameState gs) {}




    public enum WorldEffectType {
        SPAWN_TOMB,     // TombRaiser
        FREEZE_COLUMN   // Troglobite ice block

    }
}
