package models.zombie.Behavior;

import data.loader.ZombieRegistry;
import lombok.Getter;
import models.zombie.Zombie;
import models.games.GameState;
import java.util.Map;


@Getter
public class ImpThrowBehavior implements PersistableBehavior {
    private static final int TARGET_COLUMN = 2;

    private final SummonType type;
    private final String summonAlias;
    private final int summonCount;
    private final int hpThreshold;
    private boolean fired = false;

    public ImpThrowBehavior(SummonType type, String summonAlias, int summonCount, int hpThreshold) {
        this.type = type;
        this.summonAlias = summonAlias;
        this.summonCount = summonCount;
        this.hpThreshold = hpThreshold;
    }

    @Override
    public void onTick(Zombie zombie, GameState gs) {
        if (fired || zombie.getHitpoints() > hpThreshold) {
            return;
        }
        fired = true;
        spawn(zombie.getLane(), TARGET_COLUMN, summonCount, gs);
    }

    private void spawn(int lane, int column, int count, GameState gs) {
        for (int i = 0; i < count; i++) {
            Zombie template = ZombieRegistry.getTemplate(summonAlias);
            if (template == null) {
                return;
            }
            Zombie summoned = template.copy();
            summoned.setLane(lane);
            summoned.setX(column);
            gs.addZombie(summoned);
        }
    }


    public enum SummonType {
        IMP_THROW
    }

    @Override
    public String behaviorType() {
        return "SUMMON";
    }

    @Override
    public void applyToStatement(Map<String, Object> cols) {
        cols.put("summon_type", getType().name());
        cols.put("summon_alias", getSummonAlias());
        cols.put("summon_count", getSummonCount());
        cols.put("hp_threshold", getHpThreshold());
    }

    @Override
    public ZombieBehavior copy() {
        return new ImpThrowBehavior(type, summonAlias, summonCount, hpThreshold);
    }
}
