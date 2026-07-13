package models.Plant;

import models.Zombie.Zombie;
import models.games.GameState;
import models.projectile.ElementType;
import models.projectile.Projectile;
import models.projectile.move.ArcMove;
import models.projectile.move.MovingStrategy;

import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

public enum Lobber implements PlantType{
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
            }
    );
    private final int id;
    private final List<PlantUpgrade> upgrades;

    Lobber(int id, PlantUpgrade upgrade2, PlantUpgrade upgrade3, PlantUpgrade upgrade4) {
        this.id = id;
        this.upgrades = Arrays.asList(upgrade2, upgrade3, upgrade4);
    }

    @Override
    public void onTick(Plant plant, GameState gameState) {
        boolean canShoot = false;
        List<Zombie> zombies = gameState.getBoard().getZombiesInLane(plant.getPosY());
        for(Zombie zombie : zombies){
            if(zombie.getX() >= plant.getPosX()){
                canShoot = true;
                break;
            }
        }
        if(canShoot){
            Projectile projectile = new Projectile(plant.getDamage(), ElementType.NORMAL, plant.getPlantTags(),
                    plant.getPlantStat().projectileSpeed(), plant.getPosX(), plant.getPosY(), new ArcMove());
            gameState.getBoard().addProjectile(projectile);
        }
    }

    @Override
    public void onPlantFood(Plant plant, GameState gameState) {
        // i don't get the "به چند زامبی تصادفی" :)
        // how many?
    }
}
