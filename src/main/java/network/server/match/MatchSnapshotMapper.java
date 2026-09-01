package network.server.match;

import models.Board.Board;
import models.Plant.Plant;
import models.Zombie.Zombie;
import models.Zombie.Behavior.RangedAttackBehavior;
import models.games.GameState;
import models.minigames.iZombie.multiplayer.MultiplayerIZombieGame;
import models.minigames.vaseBreaker.Brain;
import models.projectile.Projectile;
import network.protocol.match.BrainNetState;
import network.protocol.match.MatchSnapshot;
import network.protocol.match.PlantNetState;
import network.protocol.match.ProjectileNetState;
import network.protocol.match.ZombieNetState;

import java.util.ArrayList;
import java.util.List;

public final class MatchSnapshotMapper {

    public MatchSnapshot toSnapshot(
            MultiplayerIZombieGame game,
            String matchId,
            EntityIdRegistry ids
    ) {
        GameState state = game.getGameState();
        Board board = state.getBoard();

        List<PlantNetState> plants = new ArrayList<>();
        for (Plant plant : board.getAllPlants()) {
            if (plant == null || plant.isMarkedForRemoval()) {
                continue;
            }
            plants.add(new PlantNetState(
                ids.idFor(plant),
                plant.getName(),
                plant.getPosY(),
                plant.getPosX(),
                plant.getCurrentHP(),
                plant.getPlantStat().maxHp(),
                plant.getLevel(),
                plant.getLastAction().name(),
                plant.getActionSerial()));
        }

        List<ZombieNetState> zombies = new ArrayList<>();
        for (Zombie zombie : state.getZombiesInTheGame()) {
            if (zombie == null) {
                continue;
            }
            RangedAttackBehavior ranged =
                zombie.getBehavior(RangedAttackBehavior.class);

            zombies.add(new ZombieNetState(
                ids.idFor(zombie),
                zombie.getAlias(),
                zombie.getLane(),
                zombie.getX(),
                zombie.getHitpoints(),
                zombie.getMaxHitpoints(),
                zombie.isDead(),
                zombie.isFrozen(),
                zombie.isGlowing(),
                zombie.isEating(),
                ranged == null ? null : ranged.getType().name(),
                ranged == null ? -1 : ranged.getCooldown()));
        }

        List<ProjectileNetState> projectiles = new ArrayList<>();
        for (Projectile projectile : board.getProjectiles()) {
            if (projectile == null || projectile.isMarkedForRemoval()) {
                continue;
            }
            projectiles.add(new ProjectileNetState(
                ids.idFor(projectile),
                projectile.getVisualProjectileKey(),
                projectile.getPosX(),
                projectile.getPosY()));
        }

        List<BrainNetState> brains = new ArrayList<>();
        for (Brain brain : game.getBrains()) {

            brains.add(new BrainNetState(brain.getRow() - 1, brain.isEaten()));
        }

        return new MatchSnapshot(
            matchId,
            state.getTickCounter(),
            game.getRemainingTicks(),
            game.getOutcome().name(),
            game.getPlantSun(),
            game.getZombieSun(),
            plants,
            zombies,
            projectiles,
            brains);
    }
}
