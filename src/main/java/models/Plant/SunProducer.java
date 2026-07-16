package models.Plant;

import models.games.GameState;
import models.sun.Sun;
import models.sun.SunType;

public enum SunProducer implements PlantType {
    SUNFLOWER(1, 50, 150, false),
    TWIN_SUNFLOWER(2, 100, 250, false),
    SUN_SHROOM(3, 25, 225, false),
    PRIMAL_SUNFLOWER(4, 75, 225, false),
    GOLD_BLOOM(5, 375, 0, true);

    private static final int DOUBLE_SUN_CHANCE_PERCENT = 25;
    private static final int SUN_SHROOM_STAGE_TWO_SECONDS = 24;
    private static final int SUN_SHROOM_STAGE_THREE_SECONDS = 72;

    private final int id;
    private final int baseSunAmount;
    private final int plantFoodSunAmount;
    private final boolean immediate;

    SunProducer(
            int id,
            int baseSunAmount,
            int plantFoodSunAmount,
            boolean immediate
    ) {
        this.id = id;
        this.baseSunAmount = baseSunAmount;
        this.plantFoodSunAmount = plantFoodSunAmount;
        this.immediate = immediate;
    }

    public Plant create() {
        return PlantEnumSupport.create(id, this);
    }

    @Override
    public void onPlanted(Plant plant, GameState state) {
        if (this == SUN_SHROOM) {
            plant.setGrowthStage(1);
        }
        if (!immediate) {
            return;
        }
        int amount = baseSunAmount + sunAmountUpgrade(plant);
        state.increaseSunBalance(amount);
        state.logEvent(plant.getName() + " immediately produced "
                + amount + " suns.\n");
        removeAfterActing(plant, state);
    }

    @Override
    public void onEveryTick(Plant plant, GameState state) {
        if (this != SUN_SHROOM || plant.getGrowthStage() >= 3) {
            return;
        }
        double reduction = Math.max(
                0,
                SUN_SHROOM_STAGE_TWO_SECONDS
                        - plant.getPlantStat().actionInterval()
        );
        int stageTwoTicks = (int) Math.round(
                (SUN_SHROOM_STAGE_TWO_SECONDS - reduction)
                        * state.getTicksPerSecond()
        );
        int stageThreeTicks = (int) Math.round(
                (SUN_SHROOM_STAGE_THREE_SECONDS - reduction)
                        * state.getTicksPerSecond()
        );
        if (plant.getAgeTicks() >= stageThreeTicks) {
            plant.setGrowthStage(3);
        } else if (plant.getAgeTicks() >= stageTwoTicks) {
            plant.setGrowthStage(2);
        }
    }

    @Override
    public void onTick(Plant plant, GameState state) {
        if (immediate) {
            return;
        }
        produceCollectableSun(plant, state, currentSunAmount(plant));
    }

    @Override
    public void onFeed(Plant plant, GameState state) {
        if (immediate) {
            return;
        }
        if (this == SUN_SHROOM) {
            plant.setGrowthStage(3);
        }
        state.increaseSunBalance(plantFoodSunAmount);
        state.logEvent(plant.getName() + " plant food produced "
                + plantFoodSunAmount + " suns.\n");
    }

    @Override
    public int plantFoodDurationTicks(Plant plant, GameState state) {
        return 0;
    }

    private int currentSunAmount(Plant plant) {
        if (this != SUN_SHROOM) {
            return baseSunAmount;
        }
        return switch (plant.getGrowthStage()) {
            case 3 -> 75;
            case 2 -> 50;
            default -> 25;
        };
    }

    private static void produceCollectableSun(
            Plant plant,
            GameState state,
            int baseAmount
    ) {
        if (plant.isPendingSun()) {
            return;
        }
        int amount = baseAmount;
        if (shouldProduceDoubleSun(plant, state)) {
            amount *= 2;
        }
        plant.setPendingSun(true);
        state.getBoard().spawnSun(new Sun(
                plant.getPosX(),
                plant.getPosY(),
                plant.getPosY(),
                SunType.ORDINARY,
                amount,
                Integer.MAX_VALUE,
                plant
        ));
        state.logEvent(plant.getName() + " produced a sun at ("
                + (plant.getPosX() + 1) + ", "
                + (plant.getPosY() + 1) + ")\n");
    }

    private static boolean shouldProduceDoubleSun(
            Plant plant,
            GameState state
    ) {
        return plant.getPlantStat().doubleSunChance()
                && state.getBoard().getRandom().nextInt(100)
                < DOUBLE_SUN_CHANCE_PERCENT;
    }

    private static int sunAmountUpgrade(Plant plant) {
        return Math.max(0, plant.getPlantStat().sunAmount() - 50);
    }

    private static void removeAfterActing(Plant plant, GameState state) {
        plant.setMarkedForRemoval(true);
        state.getBoard().removePlant(plant.getPosY(), plant.getPosX());
    }
}
