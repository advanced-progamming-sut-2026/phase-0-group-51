package models.Plant;

import Data.loader.PlantData;
import models.Zombie.Zombie;
import models.games.GameState;
import models.projectile.ElementType;
import models.projectile.Projectile;
import models.projectile.move.ArcMove;
import models.projectile.move.HomingMove;
import models.projectile.move.StarMove;
import models.projectile.move.StraightMove;
import models.sun.Sun;
import models.sun.SunType;

import java.util.List;
import java.util.Locale;

/**
 * Data-driven fallback for plants that do not yet have a dedicated enum implementation.
 */
public final class DataDrivenPlantType implements PlantType {
    private final PlantData data;

    public DataDrivenPlantType(PlantData data) {
        this.data = data;
    }

    @Override
    public void onPlanted(Plant plant, GameState state) {
        String name = normalizedName();
        if (name.equals("gold bloom")) {
            produceSun(plant, state, 375);
            plant.setMarkedForRemoval(true);
        } else if (isMint()) {
            mintActing(plant, state);
            plant.setMarkedForRemoval(true);
        } else if (isImmediateExplosive(name)) {
            explode(plant, state, name.equals("jalapeno") ? 99 : 1.5);
            plant.setMarkedForRemoval(true);
        } else if (name.equals("potato mine")) {
            double armSeconds = plant.getLevel() >= 2 ? 12.0 : 15.0;
            plant.disableFor((float) (armSeconds * state.getTicksPerSecond()));
        } else if (data.tags().contains(PlantTag.CHARGE)) {
            double actionTicks = data.actionInterval() * state.getTicksPerSecond();
            plant.disableFor((float) Math.max(1, actionTicks));
        }
        if (plant.getPlantStat().lifespan() > 0) {
            plant.setLifespanRemaining((float) plant.getPlantStat().lifespan());
        }
    }

    @Override
    public void onTick(Plant plant, GameState state) {
        switch (data.category().toLowerCase(Locale.ROOT)) {
            case "sunproducer" -> produceSun(plant, state, sunAmount());
            case "shooter" -> shoot(plant, state, false);
            case "lobber" -> lob(plant, state);
            case "homing" -> home(plant, state);
            case "strikethrough" -> shoot(plant, state, true);
            case "explosive" -> trapOrExplode(plant, state);
            case "melee" -> melee(plant, state);
            default -> {
                return;
            }
        }
    }

    @Override
    public void onFeed(Plant plant, GameState state) {
        String category = data.category().toLowerCase(Locale.ROOT);
        switch (category) {
            case "sunproducer" -> produceSun(
                    plant,
                    state,
                    Math.max(150, sunAmount() * 3)
            );
            case "explosive" -> explode(plant, state, 2.0);
            case "wallnut", "wall-nut" -> plant.addArmor(plant.getPlantStat().maxHp());
            default -> repeatNormalAction(plant, state, 5);
        }
    }

    @Override
    public void onFoodTick(Plant plant, GameState state) {
        onTick(plant, state);
    }

    @Override
    public boolean isLobber() {
        return data.category().equalsIgnoreCase("lobber");
    }

    @Override
    public void onDeath(Plant plant, GameState state) {
        boolean explosive = data.tags().contains(PlantTag.EXPLOSIVE);
        if (explosive || normalizedName().equals("torchwood")) {
            explode(plant, state, 1.5);
        }
    }

    @Override
    public void mintActing(Plant plant, GameState state) {
        String familyCategory = data.category();
        for (Plant other : state.getBoard().getAllPlants()) {
            if (isSameDataDrivenFamily(plant, other, familyCategory)) {
                other.feed(state);
            }
        }
    }

    private void shoot(Plant plant, GameState state, boolean piercing) {
        if (!hasZombieAhead(plant, state)) {
            return;
        }
        int pierce = piercing ? Math.max(2, plant.getPlantStat().pierceCount()) : 1;
        if (normalizedName().equals("threepeater")) {
            shootThreeLanes(plant, state, pierce);
        } else if (isStarShooter()) {
            shootStars(plant, state);
        } else {
            shootForward(plant, state, pierce);
        }
    }

    private void shootThreeLanes(Plant plant, GameState state, int pierce) {
        int firstLane = Math.max(0, plant.getPosY() - 1);
        int lastLane = Math.min(
                state.getBoard().getLaneCount() - 1,
                plant.getPosY() + 1
        );
        for (int lane = firstLane; lane <= lastLane; lane++) {
            addStraightProjectile(plant, state, lane, pierce);
        }
    }

    private void shootStars(Plant plant, GameState state) {
        double[][] directions = {
                {1, 0},
                {1, 0.7},
                {1, -0.7},
                {-1, 0.7},
                {-1, -0.7}
        };
        for (double[] direction : directions) {
            state.getBoard().addProjectile(Projectile.directional(
                    plant.getDamage(),
                    element(),
                    plant.getPlantTags(),
                    speed(plant),
                    plant.getPosX(),
                    plant.getPosY(),
                    direction[0],
                    direction[1],
                    new StarMove()
            ));
        }
    }

    private void shootForward(Plant plant, GameState state, int pierce) {
        int count = shotCount(plant);
        for (int i = 0; i < count; i++) {
            addStraightProjectile(plant, state, plant.getPosY(), pierce);
        }
    }

    private void addStraightProjectile(
            Plant plant,
            GameState state,
            int lane,
            int pierce
    ) {
        state.getBoard().addProjectile(Projectile.straight(
                plant.getDamage(),
                element(),
                plant.getPlantTags(),
                speed(plant),
                plant.getPosX(),
                lane,
                new StraightMove(),
                pierce
        ));
    }

    private void lob(Plant plant, GameState state) {
        Zombie target = state.getBoard().getFirstZombieAheadInLane(
                plant.getPosY(),
                plant.getPosX()
        );
        if (target == null) {
            return;
        }
        double aoe = data.tags().contains(PlantTag.AOE) ? 1.5 : 0;
        state.getBoard().addProjectile(Projectile.targeted(
                plant.getDamage(),
                element(),
                plant.getPlantTags(),
                speed(plant),
                plant.getPosX(),
                plant.getPosY(),
                target.getX(),
                target.getLane(),
                new ArcMove(),
                aoe
        ));
    }

    private void home(Plant plant, GameState state) {
        Zombie target = state.getBoard().getClosestZombieAnywhere(
                plant.getPosY(),
                plant.getPosX()
        );
        if (target == null) {
            return;
        }
        state.getBoard().addProjectile(Projectile.homing(
                plant.getDamage(),
                element(),
                plant.getPlantTags(),
                speed(plant),
                plant.getPosX(),
                plant.getPosY(),
                target,
                new HomingMove()
        ));
    }

    private void trapOrExplode(Plant plant, GameState state) {
        if (data.tags().contains(PlantTag.TRAP)) {
            Zombie target = state.getBoard().getZombieNear(
                    plant.getPosY(),
                    plant.getPosX(),
                    0.65
            );
            if (target == null) {
                return;
            }
        }
        explode(plant, state, normalizedName().equals("jalapeno") ? 99 : 1.5);
        plant.setMarkedForRemoval(true);
    }

    private void melee(Plant plant, GameState state) {
        double radius = data.tags().contains(PlantTag.AOE) ? 1.5 : 1.0;
        List<Zombie> targets = state.getBoard().getZombiesInRadius(
                plant.getPosY(),
                plant.getPosX() + 0.5,
                radius
        );
        for (Zombie zombie : targets) {
            zombie.takeDamage(plant.getDamage(), state, plant);
        }
    }

    private void explode(Plant plant, GameState state, double radius) {
        List<Zombie> targets;
        if (radius > 10) {
            targets = state.getBoard().getZombiesInLane(plant.getPosY());
        } else {
            targets = state.getBoard().getZombiesInRadius(
                    plant.getPosY(),
                    plant.getPosX(),
                    radius
            );
        }
        for (Zombie zombie : targets) {
            zombie.takeDamage(plant.getDamage(), state, plant);
            element().onHit(zombie, state);
        }
    }

    private void produceSun(Plant plant, GameState state, int amount) {
        if (plant.isPendingSun()) {
            return;
        }
        int producedAmount = amount;
        if (plant.getPlantStat().doubleSunChance() && Math.random() < 0.5) {
            producedAmount *= 2;
        }
        plant.setPendingSun(true);
        state.getBoard().spawnSun(new Sun(
                plant.getPosX(),
                plant.getPosY(),
                plant.getPosY(),
                SunType.ORDINARY,
                producedAmount,
                Integer.MAX_VALUE,
                plant
        ));
        state.logEvent("plant " + plant.getName() + " produced a sun at ("
                + (plant.getPosX() + 1) + ", " + (plant.getPosY() + 1) + ")\n");
    }

    private void repeatNormalAction(Plant plant, GameState state, int count) {
        for (int i = 0; i < count; i++) {
            onTick(plant, state);
        }
    }

    private boolean isSameDataDrivenFamily(
            Plant source,
            Plant other,
            String familyCategory
    ) {
        if (other == source) {
            return false;
        }
        if (!(other.getPlantType() instanceof DataDrivenPlantType type)) {
            return false;
        }
        return type.data.category().equalsIgnoreCase(familyCategory);
    }

    private boolean isImmediateExplosive(String name) {
        return name.equals("cherry bomb")
                || name.equals("jalapeno")
                || name.equals("doom-shroom")
                || name.equals("ice-shroom");
    }

    private boolean isStarShooter() {
        return normalizedName().equals("starfruit")
                || normalizedName().equals("rotobaga");
    }

    private int sunAmount() {
        if (normalizedName().contains("twin")) {
            return 100;
        }
        if (normalizedName().contains("primal")) {
            return 75;
        }
        return 50;
    }

    private int shotCount(Plant plant) {
        if (normalizedName().equals("repeater")) {
            return 2;
        }
        if (normalizedName().equals("mega gatling pea")) {
            return 4;
        }
        if (normalizedName().equals("pea pod")) {
            return Math.max(1, plant.getStackCount());
        }
        return 1;
    }

    private boolean hasZombieAhead(Plant plant, GameState state) {
        return state.getBoard().getFirstZombieAheadInLane(
                plant.getPosY(),
                plant.getPosX()
        ) != null;
    }

    private double speed(Plant plant) {
        double configuredSpeed = plant.getPlantStat().projectileSpeed();
        return configuredSpeed > 0 ? configuredSpeed : 0.5;
    }

    private ElementType element() {
        if (data.tags().contains(PlantTag.FIRE)) {
            return ElementType.FIRE;
        }
        if (data.tags().contains(PlantTag.ICE)) {
            return ElementType.ICE;
        }
        if (data.tags().contains(PlantTag.POISON)) {
            return ElementType.POISON;
        }
        return ElementType.NORMAL;
    }

    private boolean isMint() {
        return normalizedName().endsWith("-mint");
    }

    private String normalizedName() {
        return data.name().toLowerCase(Locale.ROOT);
    }
}
