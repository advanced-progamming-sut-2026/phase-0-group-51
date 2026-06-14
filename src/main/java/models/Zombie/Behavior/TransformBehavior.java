package models.Zombie.Behavior;

import lombok.Getter;
import models.Zombie.Zombie;
import models.games.GameState;

import java.sql.PreparedStatement;
import java.sql.SQLException;

@Getter
public class TransformBehavior implements PersistableBehavior {

    private final TransformType type;
    private final int           intervalTicks; //هر چند تیک یبار شرط تبدیل رو چک کنیم
    private final int           range; //در چه فاصله ای از تارگت این تبدیل انجام شود
    private int cooldown;

    public TransformBehavior(TransformType type, int intervalTicks, int range) {
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
    @Override public String behaviorType() { return "TRANSFORM"; }

    @Override
    public void applyToStatement(PreparedStatement ps) throws SQLException {
        ps.setString(27, type.name());
        ps.setInt(28, intervalTicks);
        ps.setInt(29, range);
    }

}
