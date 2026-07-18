package models.Plant;

import Data.loader.PlantData;
import Data.loader.PlantRegistry;
import Data.loader.ZombieRegistry;
import models.Board.Tile;
import models.Zombie.Behavior.ZombieBehavior;
import models.Zombie.Zombie;
import models.Zombie.ZombieType;
import models.games.GameState;


public enum Modifier implements PlantType {
    TORCHWOOD(52),
    HYPNO_SHROOM(54),
    IMITATER(56),
    LILY_PAD(58);

    private static final int BLUE_FLAME_SECONDS = 10;
    private static final int LILY_PAD_COPY_COUNT = 3;

    private final int id;

    Modifier(int id) {
        this.id = id;
    }

    public Plant create() {
        return PlantEnumSupport.create(id, this);
    }

    @Override
    public void onTick(Plant plant, GameState gameState) {
    }

    @Override
    public void onFeed(Plant plant, GameState state) {
        switch (this) {
            case TORCHWOOD -> plant.activateBlueFlame(
                    BLUE_FLAME_SECONDS * state.getTicksPerSecond()
            );
            case HYPNO_SHROOM -> plant.setHypnoGargantuarPrimed(true);
            case LILY_PAD -> createLilyPadCopies(plant, state);
            default -> {
                return;
            }
        }
    }

    @Override
    public int plantFoodDurationTicks(Plant plant, GameState state) {
        return 0;
    }

    @Override
    public void onEatenBy(
            Plant plant,
            Zombie zombie,
            int damage,
            GameState state
    ) {
        if (this != HYPNO_SHROOM || zombie == null || zombie.isDead()) {
            return;
        }
        if (plant.isHypnoGargantuarPrimed()) {
            replaceWithAlliedGargantuar(zombie, state);
        } else {
            hypnotizeAndBuff(zombie, plant);
        }
        plant.die(state);
    }

    @Override
    public void onDeath(Plant plant, GameState state) {
        if (this != TORCHWOOD || plant.getLevel() < 3) {
            return;
        }
        for (Zombie zombie : state.getBoard().getZombiesInSquare(plant.getPosY(), plant.getPosX(),
                1, 1)) {
            zombie.takeDamage(300, models.projectile.ElementType.FIRE, state, plant);
        }
    }

    public static Plant createImitaterCopy(
            PlantData copiedData,
            int copiedLevel,
            int imitaterLevel
    ) {
        Plant copied = PlantFactory.create(copiedData, copiedLevel);
        PlantStats stats = copied.getPlantStat();
        if (imitaterLevel >= 2) {
            stats = stats.withRecharge(Math.max(0, stats.recharge() - 2));
        }
        if (imitaterLevel >= 3) {
            stats = stats.withCost(Math.max(0, stats.cost() - 25));
        }
        Plant imitation = new Plant(
                56,
                "Imitater (" + copied.getName() + ")",
                copied.getPlantType(),
                stats,
                java.util.List.of(),
                copied.getPlantTags()
        );
        imitation.setLevel(imitaterLevel);
        imitation.setAutoPlantFoodOnEntry(imitaterLevel >= 4);
        return imitation;
    }

    private static void hypnotizeAndBuff(Zombie zombie, Plant plant) {
        zombie.setHypnotized(true);
        if (zombie.getDirection() > 0) {
            zombie.reverseDirection();
        }
        if (plant.getLevel() >= 3) {
            zombie.applyHealthScale(1.5f);
        }
        if (plant.getLevel() >= 4) {
            zombie.applyDamageScale(1.5f);
        }
    }

    private static void replaceWithAlliedGargantuar(
            Zombie consumed,
            GameState state
    ) {
        Zombie template = ZombieRegistry.getTemplate(
                ZombieType.GARGANTUAR.getAlias()
        );
        Zombie gargantuar;
        if (template == null) {
            gargantuar = new Zombie(
                    "Allied Gargantuar",
                    3600,
                    0.25f,
                    300,
                    0,
                    20
            );
        } else {
            gargantuar = new Zombie(
                    template.getAlias(),
                    template.getMaxHitpoints(),
                    template.getBaseSpeed(),
                    template.getBaseEatDps(),
                    template.getWavePointCost(),
                    template.getWeight()
            );
            for (ZombieBehavior behavior : template.getBehaviors()) {
                gargantuar.addBehavior(behavior.copy());
            }
        }
        gargantuar.setLane(consumed.getLane());
        gargantuar.setX(consumed.getX());
        gargantuar.setHypnotized(true);
        if (gargantuar.getDirection() > 0) {
            gargantuar.reverseDirection();
        }
        state.removeZombie(consumed);
        state.getZombiesInTheGame().add(gargantuar);
        state.logEvent("Hypno-shroom transformed its eater into an allied Gargantuar.\n");
    }

    private static void createLilyPadCopies(
            Plant source,
            GameState state
    ) {
        PlantData data = PlantRegistry.getById(58);
        if (data == null) {
            return;
        }
        for (Tile tile : state.getBoard().getRandomEmptyWaterTiles(
                LILY_PAD_COPY_COUNT
        )) {
            Plant copy = PlantFactory.create(data, source.getLevel());
            copy.setPosX(tile.getColumn());
            copy.setPosY(tile.getLane());
            tile.setLilyPadPlant(copy);
            copy.getPlantType().onPlanted(copy, state);
        }
    }
}
