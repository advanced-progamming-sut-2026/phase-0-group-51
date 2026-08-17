package models.Zombie.Behavior;

import models.Board.Board;
import models.Board.Tile;
import models.Zombie.Zombie;
import models.games.GameState;

import java.util.Map;

public class WorldEffectBehavior implements PersistableBehavior {
    public static final int TOMB_CAST_TICKS = 10;
    public static final int TOMB_EFFECT_TICK = 6;

    private final WorldEffectType type;
    private final int intervalTicks;
    private final int count;

    private int cooldown;
    private boolean casting;
    private int castTick;
    private boolean effectAppliedThisCast;

    public WorldEffectBehavior(
        WorldEffectType type,
        int intervalTicks,
        int count
    ) {
        this.type = type;
        this.intervalTicks = Math.max(1, intervalTicks);
        this.count = Math.max(0, count);
        this.cooldown = this.intervalTicks;
    }


    public WorldEffectType getType() {
        return type;
    }

    public int getIntervalTicks() {
        return intervalTicks;
    }

    public int getCount() {
        return count;
    }

    public int getCooldown() {
        return cooldown;
    }

    public boolean isCasting() {
        return casting;
    }

    public int getCastTick() {
        return castTick;
    }

    @Override
    public void onTick(
        Zombie zombie,
        GameState gs
    ) {
        if (type == WorldEffectType.SPAWN_TOMB) {
            tickTombRaiser(gs);
            return;
        }

        if (--cooldown > 0) {
            return;
        }

        cooldown = intervalTicks;

        if (type == WorldEffectType.RANDOM_LANE_SWAP) {
            gs.swapRandomZombieLanes(count);
        }
    }

    private void tickTombRaiser(GameState gs) {
        if (!casting) {
            if (--cooldown > 0) {
                return;
            }

            casting = true;
            castTick = 0;
            effectAppliedThisCast = false;
            return;
        }

        castTick++;

        if (!effectAppliedThisCast
            && castTick >= TOMB_EFFECT_TICK) {
            spawnTombs(gs);
            effectAppliedThisCast = true;
        }

        if (castTick < TOMB_CAST_TICKS) {
            return;
        }

        casting = false;
        castTick = 0;
        effectAppliedThisCast = false;
        cooldown = Math.max(
            1,
            intervalTicks - TOMB_CAST_TICKS
        );
    }

    private void spawnTombs(GameState gs) {
        Board board = gs.getBoard();

        for (int i = 0; i < count; i++) {
            Tile placed = board.placeGraveOnRandomTile();
            if (placed == null) {
                break;
            }
        }
    }

    public float getCastProgress() {
        if (!casting) {
            return 0f;
        }

        return Math.min(
            1f,
            castTick / (float) TOMB_CAST_TICKS
        );
    }

    @Override
    public boolean suppressesDefaultEating(Zombie zombie) {
        return type == WorldEffectType.SPAWN_TOMB
            && casting;
    }

    @Override
    public boolean suppressesMovement(Zombie zombie) {
        return type == WorldEffectType.SPAWN_TOMB
            && casting;
    }

    public enum WorldEffectType {
        SPAWN_TOMB,
        RANDOM_LANE_SWAP
    }

    @Override
    public String behaviorType() {
        return "WORLD_EFFECT";
    }

    @Override
    public void applyToStatement(Map<String, Object> cols) {
        cols.put("world_effect_type", getType().name());
        cols.put("effect_interval", getIntervalTicks());
        cols.put("effect_count", getCount());
    }

    @Override
    public ZombieBehavior copy() {
        return new WorldEffectBehavior(
            type,
            intervalTicks,
            count
        );
    }
}
