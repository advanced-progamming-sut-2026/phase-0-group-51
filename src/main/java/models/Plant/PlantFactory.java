package models.Plant;

import Data.loader.PlantData;
import Data.loader.PlantRegistry;

import java.util.Comparator;
import java.util.List;

public final class PlantFactory {
    private PlantFactory() {
    }

    public static Plant create(String name) {
        PlantData data = PlantRegistry.getByName(name);
        if (data == null) {
            throw new IllegalArgumentException("Unknown plant: " + name);
        }
        return create(data);
    }

    public static Plant create(int id) {
        PlantData data = PlantRegistry.getById(id);
        if (data == null) {
            throw new IllegalArgumentException("Unknown plant id: " + id);
        }
        return create(data);
    }

    public static Plant create(PlantData data) {
        return create(data, 1);
    }

    public static Plant create(PlantData data, int requestedLevel) {
        Plant plant = createBasePlant(data);
        int maximumLevel = plant.getUpgrades().size() + 1;
        int safeLevel = Math.max(1, Math.min(requestedLevel, maximumLevel));
        while (plant.getLevel() < safeLevel) {
            plant.levelUp();
        }
        return plant;
    }

    private static Plant createBasePlant(PlantData data) {
        return switch (data.id()) {
            case 1 -> SunProducer.SUNFLOWER.create();
            case 2 -> SunProducer.TWIN_SUNFLOWER.create();
            case 3 -> SunProducer.SUN_SHROOM.create();
            case 4 -> SunProducer.PRIMAL_SUNFLOWER.create();
            case 5 -> SunProducer.GOLD_BLOOM.create();
            case 6 -> Shooter.PEASHOOTER.create();
            case 7 -> Shooter.REPEATER.create();
            case 8 -> Shooter.THREEPEATER.create();
            case 9 -> Shooter.SNOW_PEA.create();
            case 10 -> Shooter.ROTOBAGA.create();
            case 11 -> Shooter.PEA_POD.create();
            case 12 -> Shooter.SPLIT_PEA.create();
            case 13 -> Shooter.CITRON.create();
            case 14 -> Homing.CAULIPOWER.create();
            case 15 -> Homing.ELECTRIC_BLUEBERRY.create();
            case 16 -> Shooter.BOWLING_BULB.create();
            case 17 -> StrikeThrough.CACTUS.create();
            case 18 -> Shooter.FIRE_PEASHOOTER.create();
            case 19 -> Shooter.STARFRUIT.create();
            case 20 -> Shooter.GOO_PEASHOOTER.create();
            case 21 -> Shooter.MEGA_GATLING_PEA.create();
            case 22 -> Shooter.SEA_SHROOM.create();
            case 23 -> Shooter.PUFF_SHROOM.create();
            case 24 -> StrikeThrough.FUME_SHROOM.create();
            case 25 -> Lobber.CABBAGE_PULT.create();
            case 26 -> Lobber.KERNEL_PULT.create();
            case 27 -> Lobber.MELON_PULT.create();
            case 28 -> Lobber.WINTER_MELON.create();
            case 29 -> Lobber.PEPPER_PULT.create();
            case 30 -> Explosive.POTATO_MINE.create();
            case 31 -> Explosive.PRIMAL_POTATO_MINE.create();
            case 32 -> Explosive.CHERRY_BOMB.create();
            case 33 -> Explosive.SQUASH.create();
            case 34 -> Explosive.GRAPESHOT.create();
            case 35 -> Explosive.JALAPENO.create();
            case 36 -> Explosive.DOOM_SHROOM.create();
            case 37 -> Explosive.TANGLE_KELP.create();
            case 38 -> Explosive.ICEBERG_LETTUCE.create();
            case 39 -> Melee.BONK_CHOY.create();
            case 40 -> Melee.PHAT_BEET.create();
            case 41 -> Melee.CHOMPER.create();
            case 42 -> Melee.WASABI_WHIP.create();
            case 43 -> Melee.KIWIBEAST.create();
            case 44 -> WallNut.WALL_NUT.create();
            case 45 -> WallNut.TALL_NUT.create();
            case 46 -> WallNut.ENDURIAN.create();
            case 47 -> WallNut.GARLIC.create();
            case 48 -> WallNut.SWEET_POTATO.create();
            case 49 -> WallNut.EXPLODE_O_NUT.create();
            case 50 -> WallNut.PUMPKIN.create();
            case 51 -> WallNut.SUN_BEAN.create();
            case 52 -> Modifier.TORCHWOOD.create();
            case 53 -> Homing.MAGNET_SHROOM.create();
            case 54 -> Modifier.HYPNO_SHROOM.create();
            case 55 -> Homing.CAT_TAIL.create();
            case 56 -> Modifier.IMITATER.create();
            case 57 -> Explosive.ICE_SHROOM.create();
            case 58 -> Modifier.LILY_PAD.create();
            case 59 -> Explosive.HOT_POTATO.create();
            case 60 -> Explosive.GRAVE_BUSTER.create();
            case 61 -> Mint.ENLIGHTEN_MINT.create();
            case 62 -> Mint.APPEASE_MINT.create();
            case 63 -> Mint.ARMA_MINT.create();
            case 64 -> Mint.BOMBARD_MINT.create();
            case 65 -> Mint.ENFORCE_MINT.create();
            case 66 -> Mint.REINFORCE_MINT.create();
            case 67 -> Mint.ENCHANT_MINT.create();
            case 68 -> Mint.PIERCE_MINT.create();
            case 69 -> Mint.CAT_TAIL_MINT.create();
            default -> createDataDrivenPlant(data);
        };
    }

    private static Plant createDataDrivenPlant(PlantData data) {
        PlantStats stats = new PlantStats(
                data.baseHp(),
                data.damage(),
                data.cost(),
                data.actionInterval(),
                data.recharge(),
                data.projectileSpeed()
        );
        List<PlantUpgrade> upgrades = data.upgrades().stream()
                .sorted(Comparator.comparingInt(upgrade -> upgrade.level()))
                .map(DataDrivenPlantUpgrade::new)
                .map(upgrade -> (PlantUpgrade) upgrade)
                .toList();
        return new Plant(
                data.id(),
                data.name(),
                new DataDrivenPlantType(data),
                stats,
                upgrades,
                data.tags()
        );
    }
}
