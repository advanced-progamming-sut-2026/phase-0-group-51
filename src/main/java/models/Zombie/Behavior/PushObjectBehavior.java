package models.Zombie.Behavior;

import Data.loader.ZombieRegistry;
import lombok.Getter;
import models.Board.Board;
import models.Plant.Plant;
import models.Zombie.Zombie;
import models.games.GameState;
import models.projectile.ElementType;
import java.util.Map;

@Getter
public class PushObjectBehavior implements PersistableBehavior {

    private final PushType type;
    private final int objectHitpoints;
    private final String spawnAliasOnBreak;
    private final int spawnCountOnBreak;
    private int objectsRemaining;
    private int currentObjectHP;

    private boolean pendingSpawn = false;
    private int breakLane;
    private float breakColumn;

    private final java.util.List<Zombie> pushedFrozenZombies = new java.util.ArrayList<>();

    public PushObjectBehavior(PushType type, int objectHitpoints, int objectCount) {
        this(type, objectHitpoints, objectCount, null, 0);
    }

    public PushObjectBehavior(PushType type, int objectHitpoints, int objectCount,
                              String spawnAliasOnBreak, int spawnCountOnBreak) {
        this.type = type;
        this.objectHitpoints = objectHitpoints;
        this.objectsRemaining = objectCount;
        this.spawnAliasOnBreak = spawnAliasOnBreak;
        this.spawnCountOnBreak = spawnCountOnBreak;
        this.currentObjectHP = objectCount > 0 ? objectHitpoints : 0;
    }

    public boolean hasObject() {
        return objectsRemaining > 0;
    }

    @Override
    public void onTick(Zombie zombie, GameState gs) {
        resolvePendingSpawn(gs);
        if (!hasObject()) {
            return;
        }
        if (type == PushType.ICE_BLOCK) {
            tickIceBlocks(zombie, gs);
            return;
        }
        Board board = gs.getBoard();
        int lane = zombie.getLane();
        int col = (int) zombie.getX();

        Plant crushed = board.findNearestPlantInRange(lane, col, 0);
        if (crushed != null) {
            crushed.takeDamage(crushed.getCurrentHP(),gs);
        }
        Zombie hypnotized = gs.findNearestHypnotizedZombieInRange(zombie, lane, col, 0);
        if (hypnotized != null) {
            hypnotized.killInstantly(gs);
        }
    }

    @Override
    public int onHit(Zombie zombie, int rawDamage, ElementType element, Plant plant) {
        if (type == PushType.ICE_BLOCK || !hasObject() || element == ElementType.POISON) {
            return rawDamage;
        }
        int remainingDamage = rawDamage;
        while (remainingDamage > 0 && hasObject()) {
            if (currentObjectHP > remainingDamage) {
                currentObjectHP -= remainingDamage;
                remainingDamage = 0;
            } else {
                remainingDamage -= currentObjectHP;
                objectsRemaining--;
                onObjectBroken(zombie);
                currentObjectHP = hasObject() ? objectHitpoints : 0;
            }
        }
        return remainingDamage;
    }

    private void onObjectBroken(Zombie zombie) {
        if (type != PushType.BARREL || spawnAliasOnBreak == null) {
            return;
        }
        pendingSpawn = true;
        breakLane = zombie.getLane();
        breakColumn = zombie.getX();
    }

    private void resolvePendingSpawn(GameState gs) {
        if (!pendingSpawn) {
            return;
        }
        pendingSpawn = false;
        for (int i = 0; i < spawnCountOnBreak; i++) {
            Zombie template = ZombieRegistry.getTemplate(spawnAliasOnBreak);
            if (template == null) {
                return;
            }
            Zombie imp = template.copy();
            imp.setLane(breakLane);
            imp.setX(breakColumn);
            gs.addZombie(imp);
        }
    }

    private void tickIceBlocks(Zombie zombie, GameState gs) {
        Board board = gs.getBoard();
        int lane = zombie.getLane();

        pushedFrozenZombies.removeIf(block ->
            block.isDead() || !block.hasIceShell() || block.getLane() != lane);

        adoptFrozenZombiesAhead(zombie, board, lane, gs);

        for (int i = 0; i < pushedFrozenZombies.size(); i++) {
            Zombie block = pushedFrozenZombies.get(i);
            float targetX = Math.max(0f, zombie.getX() - 1f - i);
            int previousColumn = (int) block.getX();
            block.setX(targetX);
            int newColumn = (int) targetX;
            if (newColumn != previousColumn) {
                crushEverythingOnTile(zombie, board, lane, newColumn, gs);
            }
        }
    }

    private void adoptFrozenZombiesAhead(Zombie zombie, Board board, int lane, GameState gs) {
        if (pushedFrozenZombies.size() >= objectsRemaining) {
            return;
        }
        for (Zombie other : new java.util.ArrayList<>(board.getZombies())) {
            if (other == zombie
                || other.isDead()
                || !other.hasIceShell()
                || other.getLane() != lane
                || pushedFrozenZombies.contains(other)) {
                continue;
            }
            double frontEdge = zombie.getX() - 1.0 - pushedFrozenZombies.size();
            if (other.getX() <= zombie.getX() && other.getX() >= frontEdge - 0.5) {
                pushedFrozenZombies.add(other);
                gs.logEvent(zombie.getAlias() + " started pushing the frozen "
                    + other.getAlias() + " forward!\n");
                if (pushedFrozenZombies.size() >= objectsRemaining) {
                    return;
                }
            }
        }
    }

    private void crushEverythingOnTile(Zombie zombie, Board board, int lane, int column, GameState gs) {
        if (column < 0) {
            return;
        }
        Plant crushed = board.findNearestPlantInRange(lane, column, 0);
        if (crushed != null) {
            gs.logEvent("The pushed ice block crushed " + crushed.getName()
                + " at (" + (column + 1) + ", " + (lane + 1) + ")!\n");
            crushed.takeDamage(crushed.getCurrentHP(), gs);
        }
        Zombie hypnotized = gs.findNearestHypnotizedZombieInRange(zombie, lane, column, 0);
        if (hypnotized != null) {
            hypnotized.killInstantly(gs);
        }
    }

    @Override
    public boolean suppressesDefaultEating(Zombie zombie) {
        if (type == PushType.ICE_BLOCK) {
            return !pushedFrozenZombies.isEmpty();
        }
        return hasObject();
    }

    public enum PushType {
        ARCADE_MACHINE, // Arcade zombie
        BARREL,         // Barrel roller zombie
        ICE_BLOCK       // TODO: Replace the carried HP model with movable board ice-block entities.
    }

    @Override
    public String behaviorType() {
        return "PUSH_OBJECT";
    }

    @Override
    public void applyToStatement(Map<String, Object> cols) {
        cols.put("push_type", getType().name());
        cols.put("object_hp", getObjectHitpoints());
        cols.put("object_count", getObjectsRemaining());
        cols.put("spawn_alias", getSpawnAliasOnBreak());
        cols.put("spawn_count", getSpawnCountOnBreak());
    }

    @Override
    public ZombieBehavior copy() {
        return new PushObjectBehavior(type, objectHitpoints, objectsRemaining,
            spawnAliasOnBreak, spawnCountOnBreak);
    }
}
