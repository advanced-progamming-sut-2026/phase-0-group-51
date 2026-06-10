package models.Zombie.Behavior;

import models.Zombie.Zombie;

public class TransformBehavior implements ZombieBehavior {

    private final TransformType transformType;
    private final int           intervalTicks; //هر چند تیک یبار شرط تبدیل رو چک کنیم
    private final int           range; //در چه فاصله ای از تارگت این تبدیل انجام شود
    private int cooldown;

    public TransformBehavior(TransformType type, int intervalTicks, int range) {
        this.transformType = type;
        this.intervalTicks = intervalTicks;
        this.range         = range;
        this.cooldown      = intervalTicks;
    }

    @Override
    public void onTick(Zombie zombie) {}


    public enum TransformType {

    }

}
