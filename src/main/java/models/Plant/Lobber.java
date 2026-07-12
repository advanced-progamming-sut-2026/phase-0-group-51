package models.Plant;

import Data.loader.PlantData;
import Data.loader.PlantRegistry;
import models.Zombie.Zombie;
import models.games.GameState;
import models.projectile.ElementType;
import models.projectile.Projectile;
import models.projectile.move.ArcMove;

import java.util.Arrays;
import java.util.List;

public enum Lobber implements PlantType {

    CABBAGE_PULT(25,
            new PlantUpgrade() {
                @Override
                public PlantStats apply(PlantStats current) {
                    return current.withDamage(current.damage() + 10);
                }
            },
            new PlantUpgrade() {
                @Override
                public PlantStats apply(PlantStats current) {
                    return current.withInterval(current.actionInterval() * 0.85);
                }
            },
            new PlantUpgrade() {
                @Override
                public PlantStats apply(PlantStats current) {
                    return current.withMaxHp(current.maxHp() + 150);
                }
            }) {
        @Override
        public void onTick(Plant plant, GameState gameState) {
            if (!Shooter.zombieInLane(plant, gameState)) return;
            gameState.getBoard().addProjectile(new Projectile(
                    plant.getDamage(), ElementType.NORMAL, plant.getPlantTags(),
                    plant.getPlantStat().projectileSpeed(),
                    plant.getPosX(), plant.getPosY(), new ArcMove()));
        }

        @Override
        public void onFoodTick(Plant plant, GameState gameState) {
            // "پرتاب کلم به چند زامبی تصادفی" — lob at several random zombies
            // on the board at once, not just the closest one in-lane. The
            // exact count isn't specified by the source game, 3-4 is typical;
            // pick a number and centralize it as a constant if several
            // Lobbers share this "food = N random targets" pattern.
            // NOTE: getRandomZombies(int) and a targeting Projectile factory
            // aren't in the files you've shown me — you'll need to add these
            // (or equivalent) to Board/Projectile. Left as the intended call
            // shape so the "several random zombies" pattern (which recurs for
            // Melon-pult, Winter Melon, Pepper-pult, Kernel-pult's food effect,
            // Caulipower, Electric Blueberry) only needs writing once.
            int targets = 3;
            for (Zombie zombie : gameState.getBoard().getRandomZombies(targets)) {
                gameState.getBoard().addProjectile(Projectile.targeting(
                        plant.getDamage(), ElementType.NORMAL, plant.getPlantTags(),
                        plant.getPlantStat().projectileSpeed(),
                        plant.getPosX(), plant.getPosY(), zombie));
            }
        }
    },
    ;

    private final int id;
    private final List<PlantUpgrade> upgrades;

    Lobber(int id, PlantUpgrade upgrade2, PlantUpgrade upgrade3, PlantUpgrade upgrade4) {
        this.id = id;
        this.upgrades = Arrays.asList(upgrade2, upgrade3, upgrade4);
    }

    public Plant create() {
        PlantData data = PlantRegistry.get(id);
        PlantStats baseStats = new PlantStats(
                data.baseHp(),
                data.damage(),
                data.cost(),
                data.actionInterval(),
                data.recharge(),
                0
        );
        return new Plant(
                data.id(), data.name(), this,
                baseStats,
                upgrades,
                data.tags()
        );
    }
}