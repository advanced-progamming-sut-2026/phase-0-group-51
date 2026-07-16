package models.Zombie;

import lombok.Getter;
import lombok.Setter;
import models.App;
import models.Plant.Plant;
import models.User;
import models.Zombie.Behavior.ArmorBehavior;
import models.Zombie.Behavior.MovementBehavior;
import models.Zombie.Behavior.ZombieBehavior;
import models.games.GameState;
import models.projectile.ElementType;
import models.quests.QuestKillSourceType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
@Getter
@Setter
public class Zombie {
    public static final String EFFECT_CHILLED = "chilled";
    public static final String EFFECT_FROZEN = "frozen";
    public static final String EFFECT_BUTTERED = "buttered";
    private static final float CHILL_SPEED_FACTOR = 0.5f;
    public static final int ICE_SHELL_MAX_HEALTH = 600;

    private final String alias;
    private final int maxHitpoints;
    private int hitpoints;
    private final float baseSpeed;
    private final float baseEatDps;
    private final float wavePointCost;
    private final int weight;

    private int lane;
    private float x;
    final float TILE_WIDTH = 80f;
    private int direction = 1; // 1 = walking normal, -1 = reversed

    private float speedMultiplier = 1.0f;
    private float damageMultiplier = 1.0f;

    private boolean eating = false;
    private boolean dead = false;
    private boolean hypnotized = false;
    private boolean glowing = false;
    private boolean questEligible = true;
    private int iceShellHealth;

    // effect name -> remaining ticks
    private final Map<String, Integer> effects = new LinkedHashMap<>();

    private float poisonDPS = 0f;
    private float poisonTicksRemaining = 0f;
    private float poisonDamageAccumulator = 0f;
    private Plant poisonSource;

    private final List<ZombieBehavior> behaviors = new ArrayList<>();
    private float eatDamageAccumulator = 0f;

    public Zombie(String alias, float hp, float speed, float eatDps, float wpc, int weight) {
        this.alias = alias;
        this.maxHitpoints = Math.round(hp);
        this.hitpoints = this.maxHitpoints;
        this.baseSpeed = speed;
        this.baseEatDps = eatDps;
        this.wavePointCost = wpc;
        this.weight = weight;
    }

    public void setSpeedMultiplier(float speedScale) {
        if (baseSpeed != 0) {
            this.speedMultiplier = speedScale / baseSpeed;
        }
    }

    public void applySpeedScale(float scale) {
        this.speedMultiplier *= scale;
    }

    public void applyDamageScale(float scale) {
        this.damageMultiplier *= scale;
    }

    public void applyChill(int ticks) {
        effects.merge(EFFECT_CHILLED, ticks, Math::max);
    }

    public void applyFreeze(int ticks) {
        effects.merge(EFFECT_FROZEN, ticks, Math::max);
    }
    public void applyButter(int ticks) {
        effects.merge(EFFECT_BUTTERED, ticks, Math::max);
    }

    public void clearColdEffects() {
        effects.remove(EFFECT_CHILLED);
        effects.remove(EFFECT_FROZEN);
    }

    public boolean isFrozen() {
        return effects.getOrDefault(EFFECT_FROZEN, 0) > 0;
    }

    public boolean isChilled() {
        return effects.getOrDefault(EFFECT_CHILLED, 0) > 0;
    }
    public boolean isButtered() {
        return effects.containsKey(EFFECT_BUTTERED);
    }

    public Map<String, Integer> getEffects() {
        return Collections.unmodifiableMap(effects);
    }

    public void addBehavior(ZombieBehavior behavior) {
        behaviors.add(behavior);
    }

    public List<ZombieBehavior> getBehaviors() {
        return Collections.unmodifiableList(behaviors);
    }

    @SuppressWarnings("unchecked")
    public <T extends ZombieBehavior> T getBehavior(Class<T> cls) {
        return (T) behaviors.stream()
            .filter(cls::isInstance)
            .findFirst().orElse(null);
    }

    public boolean hasMetallicArmor() {
        for (ZombieBehavior behavior : behaviors) {
            if (behavior instanceof ArmorBehavior armor
                    && armor.getDefinition().isMetallic()
                    && !armor.isGone()) {
                return true;
            }
        }
        return false;
    }

    public boolean pullMetallicArmor() {
        for (ZombieBehavior behavior : behaviors) {
            if (behavior instanceof ArmorBehavior armor && armor.tryMagnetPull()) {
                return true;
            }
        }
        return false;
    }

    public void takeDamage(int rawDamage, ElementType element, GameState gs, Plant plant) {
        if (dead) {
            return;
        }
        if (damageIceShell(rawDamage, element, gs)) {
            return;
        }
        int damage = rawDamage;
        for (ZombieBehavior behavior : behaviors) {
            damage = behavior.onHit(this, damage, element, plant);
        }
        hitpoints -= damage;
        if (hitpoints <= 0) {
            hitpoints = 0;
            die(gs, plant == null ? QuestKillSourceType.OTHER : QuestKillSourceType.PLANT, plant);
        }
    }

    public void takeDamage(int rawDamage, GameState gs, Plant plant) {
        takeDamage(rawDamage, ElementType.NORMAL, gs, plant);
    }

    public void killInstantly(GameState gs) {
        killInstantly(gs, QuestKillSourceType.OTHER);
    }

    public void killInstantly(GameState gs, QuestKillSourceType sourceType) {
        if (dead) {
            return;
        }
        hitpoints = 0;
        die(gs, sourceType, null);
    }

    public void applyPoison(GameState gs, float damagePerSecond, float durationSeconds) {
        applyPoison(gs, damagePerSecond, durationSeconds, null);
    }

    public void applyPoison(
            GameState gs, float damagePerSecond, float durationSeconds, Plant sourcePlant
    ) {
        this.poisonDPS = damagePerSecond;
        this.poisonTicksRemaining = durationSeconds * gs.getTicksPerSecond();
        this.poisonSource = sourcePlant;
    }


    public void onTick(GameState gs) {
        if (dead || hasIceShell()) {
            return;
        }
        tickEffects();
        tickPoison(gs);
        if (isFrozen() || isButtered()) {
            return;
        }
        if (hypnotized) {
            tickHypnotized(gs);
            return;
        }
        for (ZombieBehavior behavior : new ArrayList<>(behaviors)) {
            behavior.onTick(this, gs);
        }
        if (dead) {
            return;
        }
        boolean eatSuppressed = behaviors.stream().anyMatch(b -> b.suppressesDefaultEating(this));
        Plant target = eatSuppressed ? null
            : gs.getBoard().findNearestPlantInRange(lane, (int) x, 1);
        if (target != null) {
            eating = true;
            eatDamageAccumulator += (baseEatDps * damageMultiplier) / gs.getTicksPerSecond();
            int wholeDamage = (int) eatDamageAccumulator;
            if (wholeDamage > 0) {
                eatDamageAccumulator -= wholeDamage;
                target.getPlantType().onEatenBy(
                        target,
                        this,
                        wholeDamage,
                        gs
                );
                target.takeDamage(wholeDamage, gs);
            }
        } else {
            eating = false;
            boolean movementSuppressed = behaviors.stream().anyMatch(b -> b.suppressesMovement(this));
            if (!movementSuppressed) {
                float chillFactor = isChilled() ? CHILL_SPEED_FACTOR : 1.0f;
                float previousX = x;
                x -= direction * (baseSpeed * speedMultiplier * chillFactor) / gs.getTicksPerSecond();
                gs.getBoard().applyIceFloorIfCrossed(this, previousX, x, gs);
            }
        }
    }

    private void tickHypnotized(GameState gs) {
        Zombie enemy = gs.findNearestHostileZombieInRange(this, lane, x, 0.65f);
        if (enemy != null) {
            eating = true;
            eatDamageAccumulator += (baseEatDps * damageMultiplier)
                    / gs.getTicksPerSecond();
            int wholeDamage = (int) eatDamageAccumulator;
            if (wholeDamage > 0) {
                eatDamageAccumulator -= wholeDamage;
                enemy.takeDamage(wholeDamage, gs, null);
            }
            return;
        }
        eating = false;
        float chillFactor = isChilled() ? CHILL_SPEED_FACTOR : 1.0f;
        float previousX = x;
        x -= direction * (baseSpeed * speedMultiplier * chillFactor)
                / gs.getTicksPerSecond();
        gs.getBoard().applyIceFloorIfCrossed(this, previousX, x, gs);
    }

    private void tickEffects() {
        effects.replaceAll((name, ticks) -> ticks - 1);
        effects.values().removeIf(ticks -> ticks <= 0);
    }


    private void tickPoison(GameState gs) {
        if (poisonTicksRemaining <= 0) return;
        poisonTicksRemaining--;
        poisonDamageAccumulator += poisonDPS / gs.getTicksPerSecond();
        int whole = (int) poisonDamageAccumulator;
        if (whole > 0) {
            poisonDamageAccumulator -= whole;
            hitpoints -= whole;
            if (hitpoints <= 0) {
                hitpoints = 0;
                die(gs, poisonSource == null ? QuestKillSourceType.OTHER
                        : QuestKillSourceType.PLANT, poisonSource);
            }
        }
    }



    private static final double LOOT_DROP_CHANCE = 0.10;
    private void die(GameState gs, QuestKillSourceType sourceType, Plant sourcePlant) {
        if (dead) {
            return;
        }
        dead = true;
        models.items.Mower mower = lane >= 0 && lane < gs.getLawnMowers().length
                ? gs.getLawnMowers()[lane] : null;
        gs.getQuestTracker().recordZombieKill(
                this, sourceType, sourcePlant, gs.getTickCounter(), mower);
        gs.logEvent("Zombie of type " + alias + " is dead at ("
            + String.format(java.util.Locale.US, "%.2f", x + 1)
            + ", " + (lane + 1) + ")\n");
        if (glowing && gs.addPlantFood()) {
            gs.logEvent("The glowing zombie dropped a plant food; you have "
                + gs.getPlantFoodCount() + " plant foods now.\n");
        }
        if (Math.random() < LOOT_DROP_CHANCE) {
            dropLoot(gs);
        }
        for (ZombieBehavior behavior : behaviors) {
            behavior.onDeath(this, gs);
        }
        gs.removeZombie(this);
    }

    private void dropLoot(GameState gs) {
        User user = App.getInstance().getLoggedInUser();
        if (user == null) {
            return;
        }
        String[] options = {"coin", "diamond", "pot"};
        String item = options[(int) (Math.random() * options.length)];
        int count;
        switch (item) {
            case "coin" -> {
                user.setCoins(user.getCoins() + 1);
                count = user.getCoins();
            }
            case "diamond" -> {
                user.setGems(user.getGems() + 1);
                count = user.getGems();
            }
            default -> {
                if (!user.getGreenHouse().unlockNextPot()) {
                    return;
                }
                count = user.getGreenHouse().countUnlockedPots();
            }
        }
        gs.logEvent("A zombie dropped a " + item + "; you have "
            + count + " " + item + "s now.\n");
    }

    public Zombie copy() {
        int difficultyLevel = App.getInstance().getLoggedInUser().getDifficultyLevel();
        float increaseMultiplier = difficultyLevel / 3.0f;
        float decreaseMultiplier = 3.0f / difficultyLevel;
        Zombie z = new Zombie(alias,
            maxHitpoints * increaseMultiplier,
            baseSpeed,
            baseEatDps * increaseMultiplier,
            wavePointCost * decreaseMultiplier,
            weight);
        for (ZombieBehavior behavior : behaviors) {
            z.addBehavior(behavior.copy());
        }
        return z;
    }

    public void freezeInIce() {
        iceShellHealth = ICE_SHELL_MAX_HEALTH;
    }

    public boolean hasIceShell() {
        return iceShellHealth > 0;
    }

    public boolean ignoresIceFloors() {
        MovementBehavior movement = getBehavior(MovementBehavior.class);
        return movement != null && movement.getType() == MovementBehavior.MovementType.FLY_OVER;
    }

    public void meltIceShell(GameState state) {
        if (!hasIceShell()) {
            return;
        }
        iceShellHealth = 0;
        state.logEvent("The ice around " + alias + " was destroyed.\n");
    }

    private boolean damageIceShell(int damage, ElementType element, GameState state) {
        if (!hasIceShell()) {
            return false;
        }
        if (element == ElementType.FIRE) {
            meltIceShell(state);
        } else {
            iceShellHealth = Math.max(0, iceShellHealth - Math.max(0, damage));
            if (iceShellHealth == 0) {
                state.logEvent("The ice around " + alias + " was destroyed.\n");
            }
        }
        return true;
    }

    public void reverseDirection() {
        direction = direction * -1;
    }
    public int getColumn() {
        return (int) (x/TILE_WIDTH);
    }
}
