package models.Plant;

import models.Zombie.Zombie;
import models.games.GameState;

public enum WallNut implements PlantType {
    WALL_NUT(44),
    EXPLODE_O_NUT(49) {
        @Override
        public void onDeath(Plant plant, GameState state) {
            int damage = plant.getDamage();
            if (plant.getLevel() >= 3) {
                damage += 200;
            }
            for (Zombie zombie : state.getBoard().getZombiesInRadius(
                    plant.getPosY(),
                    plant.getPosX(),
                    1.5
            )) {
                zombie.takeDamage(damage, state, plant);
            }
        }
    };

    private final int id;

    WallNut(int id) {
        this.id = id;
    }

    public Plant create() {
        return PlantEnumSupport.create(id, this);
    }

    @Override
    public void onTick(Plant plant, GameState state) {
        return;
    }

    @Override
    public void onFeed(Plant plant, GameState state) {
        plant.addArmor(plant.getPlantStat().maxHp());
    }
}
