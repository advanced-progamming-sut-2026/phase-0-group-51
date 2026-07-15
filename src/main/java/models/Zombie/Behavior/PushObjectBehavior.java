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
        if (!hasObject() || element == ElementType.POISON) {
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

    @Override
    public boolean suppressesDefaultEating(Zombie zombie) {
        return hasObject();
    }

    public enum PushType {
        ARCADE_MACHINE, // Arcade zombie
        BARREL,         // Barrel roller zombie
        ICE_BLOCK       // Troglobite
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
