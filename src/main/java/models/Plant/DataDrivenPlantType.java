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
 * Safe data-driven baseline for every category in plants.csv. Exceptional plants can later
 * be registered as dedicated PlantType implementations without changing loading/upgrades.
 */
public final class DataDrivenPlantType implements PlantType {
    private final PlantData data;
    public DataDrivenPlantType(PlantData data) { this.data = data; }

    @Override public void onPlanted(Plant plant, GameState state) {
        String n = normalizedName();
        if (n.equals("gold bloom")) { produceSun(plant, state, 375); plant.setMarkedForRemoval(true); }
        else if (isMint()) { mintActing(plant, state); plant.setMarkedForRemoval(true); }
        else if (n.equals("cherry bomb") || n.equals("jalapeno") || n.equals("doom-shroom") || n.equals("ice-shroom")) {
            explode(plant, state, n.equals("jalapeno") ? 99 : 1.5);
            plant.setMarkedForRemoval(true);
        } else if (data.tags().contains(PlantTag.CHARGE)) {
            plant.disableFor((float) Math.max(1, data.actionInterval() * state.getTicksPerSecond()));
        }
        if (plant.getPlantStat().lifespan() > 0) plant.setLifespanRemaining((float) plant.getPlantStat().lifespan());
    }

    @Override public void onTick(Plant plant, GameState state) {
        switch (data.category().toLowerCase(Locale.ROOT)) {
            case "sunproducer" -> produceSun(plant, state, sunAmount());
            case "shooter" -> shoot(plant, state, false);
            case "lobber" -> lob(plant, state);
            case "homing" -> home(plant, state);
            case "strikethrough" -> shoot(plant, state, true);
            case "explosive" -> trapOrExplode(plant, state);
            case "melee" -> melee(plant, state);
            default -> { }
        }
    }

    @Override public void onFeed(Plant plant, GameState state) {
        String category = data.category().toLowerCase(Locale.ROOT);
        switch (category) {
            case "sunproducer" -> produceSun(plant, state, Math.max(150, sunAmount() * 3));
            case "explosive" -> explode(plant, state, 2.0);
            case "wallnut" -> plant.addArmor(Math.max(1000, plant.getPlantStat().maxHp() / 2));
            default -> { for (int i = 0; i < 5; i++) onTick(plant, state); }
        }
    }

    @Override public void onFoodTick(Plant plant, GameState state) { onTick(plant, state); }

    @Override public void onDeath(Plant plant, GameState state) {
        if (data.tags().contains(PlantTag.EXPLOSIVE) || normalizedName().equals("torchwood")) explode(plant, state, 1.5);
    }

    @Override public void mintActing(Plant plant, GameState state) {
        String familyCategory = data.category();
        for (Plant other : state.getBoard().getAllPlants()) {
            if (other != plant && other.getPlantType() instanceof DataDrivenPlantType type
                    && type.data.category().equalsIgnoreCase(familyCategory)) other.feed(state);
        }
    }

    private void shoot(Plant plant, GameState state, boolean piercing) {
        if (!hasZombieAhead(plant, state)) return;
        ElementType element = element();
        int count = shotCount(plant);
        int pierce = piercing ? Math.max(2, plant.getPlantStat().pierceCount()) : 1;
        if (normalizedName().equals("threepeater")) {
            for (int lane = Math.max(0, plant.getPosY()-1); lane <= Math.min(state.getBoard().getLaneCount()-1, plant.getPosY()+1); lane++)
                state.getBoard().addProjectile(Projectile.straight(plant.getDamage(), element, plant.getPlantTags(), speed(plant), plant.getPosX(), lane, new StraightMove(), pierce));
            return;
        }
        if (normalizedName().equals("starfruit") || normalizedName().equals("rotobaga")) {
            double[][] dirs = {{1,0},{1,.7},{1,-.7},{-1,.7},{-1,-.7}};
            for (double[] d : dirs) state.getBoard().addProjectile(Projectile.directional(plant.getDamage(), element, plant.getPlantTags(), speed(plant), plant.getPosX(), plant.getPosY(), d[0], d[1], new StarMove()));
            return;
        }
        for (int i=0;i<count;i++) state.getBoard().addProjectile(Projectile.straight(plant.getDamage(), element, plant.getPlantTags(), speed(plant), plant.getPosX(), plant.getPosY(), new StraightMove(), pierce));
    }

    private void lob(Plant plant, GameState state) {
        Zombie target = state.getBoard().getFirstZombieAheadInLane(plant.getPosY(), plant.getPosX());
        if (target == null) return;
        double aoe = data.tags().contains(PlantTag.AOE) ? 1.5 : 0;
        state.getBoard().addProjectile(Projectile.targeted(plant.getDamage(), element(), plant.getPlantTags(), speed(plant), plant.getPosX(), plant.getPosY(), target.getX(), target.getLane(), new ArcMove(), aoe));
    }

    private void home(Plant plant, GameState state) {
        Zombie target = state.getBoard().getClosestZombieAnywhere(plant.getPosY(), plant.getPosX());
        if (target == null) return;
        state.getBoard().addProjectile(Projectile.homing(plant.getDamage(), element(), plant.getPlantTags(), speed(plant), plant.getPosX(), plant.getPosY(), target, new HomingMove()));
    }

    private void trapOrExplode(Plant plant, GameState state) {
        if (data.tags().contains(PlantTag.TRAP)) {
            Zombie target = state.getBoard().getZombieNear(plant.getPosY(), plant.getPosX(), 0.65);
            if (target == null) return;
        }
        explode(plant, state, normalizedName().equals("jalapeno") ? 99 : 1.5);
        plant.setMarkedForRemoval(true);
    }

    private void melee(Plant plant, GameState state) {
        for (Zombie zombie : state.getBoard().getZombiesInRadius(plant.getPosY(), plant.getPosX()+0.5, data.tags().contains(PlantTag.AOE) ? 1.5 : 1.0))
            zombie.takeDamage(plant.getDamage(), state);
    }

    private void explode(Plant plant, GameState state, double radius) {
        List<Zombie> targets = radius > 10 ? state.getBoard().getZombiesInLane(plant.getPosY()) : state.getBoard().getZombiesInRadius(plant.getPosY(), plant.getPosX(), radius);
        for (Zombie zombie : targets) { zombie.takeDamage(plant.getDamage(), state); element().onHit(zombie, state); }
    }

    private void produceSun(Plant plant, GameState state, int amount) {
        state.getBoard().spawnSun(new Sun(plant.getPosX(), plant.getPosY(), plant.getPosY(), SunType.ORDINARY, amount, Integer.MAX_VALUE));
        state.logEvent("plant " + plant.getName() + " produced a sun at (" + plant.getPosX() + ", " + plant.getPosY() + ")\n");
    }

    private int sunAmount() { if (normalizedName().contains("twin")) return 100; if (normalizedName().contains("primal")) return 75; return 50; }
    private int shotCount(Plant plant) { if (normalizedName().equals("repeater")) return 2; if (normalizedName().equals("mega gatling pea")) return 4; if (normalizedName().equals("pea pod")) return Math.max(1, plant.getStackCount()); return 1; }
    private boolean hasZombieAhead(Plant plant, GameState state) { return state.getBoard().getFirstZombieAheadInLane(plant.getPosY(), plant.getPosX()) != null; }
    private double speed(Plant p) { return p.getPlantStat().projectileSpeed() > 0 ? p.getPlantStat().projectileSpeed() : 0.5; }
    private ElementType element() { if (data.tags().contains(PlantTag.FIRE)) return ElementType.FIRE; if (data.tags().contains(PlantTag.ICE)) return ElementType.ICE; if (data.tags().contains(PlantTag.POISON)) return ElementType.POISON; return ElementType.NORMAL; }
    private boolean isMint() { return normalizedName().endsWith("-mint"); }
    private String normalizedName() { return data.name().toLowerCase(Locale.ROOT); }
}
