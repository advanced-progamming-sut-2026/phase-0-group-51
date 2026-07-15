package models.Plant;

import models.Zombie.Zombie;
import models.games.GameState;
import models.projectile.ElementType;
import models.projectile.Projectile;
import models.projectile.move.StraightMove;

public enum StrikeThrough implements PlantType {
    CACTUS(17, AttackMode.PROJECTILE, 3, 9),
    FUME_SHROOM(24, AttackMode.CLOUD, Integer.MAX_VALUE, 4);

    private final int id;
    private final AttackMode mode;
    private final int basePierce;
    private final double range;

    StrikeThrough(int id, AttackMode mode, int basePierce, double range) {
        this.id = id;
        this.mode = mode;
        this.basePierce = basePierce;
        this.range = range;
    }

    public Plant create() {
        return PlantEnumSupport.create(id, this);
    }

    @Override
    public void onTick(Plant plant, GameState state) {
        if (mode == AttackMode.CLOUD) {
            damageWithCloud(plant, state);
        } else {
            shootPiercingProjectile(plant, state);
        }
    }

    @Override
    public void onFoodTick(Plant plant, GameState state) {
        onTick(plant, state);
    }

    private void shootPiercingProjectile(Plant plant, GameState state) {
        if (state.getBoard().getFirstZombieAheadInLane(
                plant.getPosY(),
                plant.getPosX()
        ) == null) {
            return;
        }
        int upgradePierce = Math.max(0, plant.getPlantStat().pierceCount() - 1);
        state.getBoard().addProjectile(Projectile.straight(
                plant.getDamage(),
                ElementType.NORMAL,
                plant.getPlantTags(),
                PlantEnumSupport.projectileSpeed(plant, 0.65),
                plant.getPosX(),
                plant.getPosY(),
                new StraightMove(),
                basePierce + upgradePierce
        ));
    }

    private void damageWithCloud(Plant plant, GameState state) {
        double effectiveRange = range + (plant.getLevel() >= 2 ? 1 : 0);
        for (Zombie zombie : state.getBoard().getZombiesInLane(plant.getPosY())) {
            double distance = zombie.getX() - plant.getPosX();
            if (distance >= 0 && distance <= effectiveRange) {
                zombie.takeDamage(plant.getDamage(), state, plant);
            }
        }
    }

    private enum AttackMode {
        PROJECTILE,
        CLOUD
    }
}
