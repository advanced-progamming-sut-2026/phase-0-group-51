package models.Plant;

import Data.loader.PlantData;
import Data.loader.PlantRegistry;
import models.Zombie.Zombie;
import models.games.GameState;
import models.projectile.ElementType;
import models.projectile.Projectile;
import models.projectile.move.ArcMove;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public enum Shooter implements PlantType{
    PEASHOOTER(6,
            new PlantUpgrade() {
                @Override
                public PlantStats apply(PlantStats current) {
                    return current.withDamage(current.damage() + 10);
                }
            },
            new PlantUpgrade() {
                @Override
                public PlantStats apply(PlantStats current) {
                    return current.withMaxHp(current.maxHp() + 150);
                }
            },
            new PlantUpgrade() {
                @Override
                public PlantStats apply(PlantStats current) {
                    return current.withCost(current.cost() - 25);
                }
            }),
    ;
    private final int id;
    private final List<PlantUpgrade> upgrades;

    Shooter(int id, PlantUpgrade upgrade2, PlantUpgrade upgrade3, PlantUpgrade upgrade4) {
        this.id = id;
        upgrades = Arrays.asList(upgrade2, upgrade3, upgrade4);
    }

    public Plant create() {
        PlantData data = PlantRegistry.get(id);
        PlantStats baseStats = new PlantStats(
                data.baseHp(),
                data.damage(),
                data.cost(),
                data.actionInterval(),
                0
        );
        return new Plant(
                data.id(), data.name(), this,
                baseStats,
                upgrades,
                data.tags()
        );
    }

    @Override
    public void onTick(Plant plant, GameState state) {
        boolean canShoot = false;
        List<Zombie> zombies = state.getBoard().getZombiesInLane(plant.getPosY());
        for(Zombie zombie : zombies){
            if(zombie.getX() >= plant.getPosX()){
                canShoot = true;
                break;
            }
        }
        if(canShoot){
            Projectile projectile = new Projectile(plant.getDamage(), ElementType.NORMAL, plant.getPlantTags(),
                    plant.getPlantStat().projectileSpeed(), plant.getPosX(), plant.getPosY(), new ArcMove());
            state.getBoard().addProjectile(projectile);
        }
    }

    @Override
    public void onPlantFood(Plant plant, GameState state) {
        //Action interval decrease
    }
}
