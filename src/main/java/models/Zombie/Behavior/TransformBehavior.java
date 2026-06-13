package models.Zombie.Behavior;

import models.Zombie.Zombie;
import models.games.GameState;

public class TransformBehavior implements ZombieBehavior {

    private final TransformType type;
    private final int           intervalTicks; //هر چند تیک یبار شرط تبدیل رو چک کنیم
    private final int           range; //در چه فاصله ای از تارگت این تبدیل انجام شود
    private int cooldown;

    TransformBehavior(TransformType type, int intervalTicks, int range) {
        this.type          = type;
        this.intervalTicks = intervalTicks;
        this.range         = range;
        this.cooldown      = intervalTicks;
    }

    @Override
    public void onTick(Zombie zombie, GameState gs) {}


    public enum TransformType {
        SHEEP_TRANSFORM
    }

}
