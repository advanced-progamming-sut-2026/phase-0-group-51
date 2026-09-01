package network.server.match;

import Data.loader.PlantData;
import Data.loader.PlantRegistry;
import models.Board.Board;
import models.Plant.Plant;
import models.Zombie.Zombie;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
                    plant.getLevel()));
        }

        List<ZombieNetState> zombies = new ArrayList<>();
        for (Zombie zombie : state.getZombiesInTheGame()) {
            if (zombie == null) {
                continue;
            }
            zombies.add(new ZombieNetState(
                    ids.idFor(zombie),
                    zombie.getAlias(),
                    zombie.getLane(),
                    zombie.getX(),
                    zombie.getHitpoints(),
                    zombie.getMaxHitpoints(),
                    zombie.isDead(),
                    zombie.isFrozen(),
                    zombie.isGlowing()));
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

        Map<String, Integer> plantCooldownTicks =
                new LinkedHashMap<>();
        Map<String, Integer> plantCooldownTotalTicks =
                new LinkedHashMap<>();

        for (PlantData data : PlantRegistry.getAll()) {
            if (data == null
                    || data.name() == null
                    || data.name().isBlank()) {
                continue;
            }

            plantCooldownTicks.put(
                    data.name(),
                    game.getPlantCooldownTicks(
                            data.name()
                    )
            );

            plantCooldownTotalTicks.put(
                    data.name(),
                    game.getPlantCooldownTotalTicks(
                            data.name()
                    )
            );
        }

        Map<String, Integer> zombieCooldownTicks =
                new LinkedHashMap<>();
        Map<String, Integer> zombieCooldownTotalTicks =
                new LinkedHashMap<>();

        for (String alias : game.getRoster().keySet()) {
            zombieCooldownTicks.put(
                    alias,
                    game.getZombieCooldownTicks(
                            alias
                    )
            );

            zombieCooldownTotalTicks.put(
                    alias,
                    game.getZombieCooldownTotalTicks(
                            alias
                    )
            );
        }

        return new MatchSnapshot(
                matchId,
                state.getTickCounter(),
                Math.max(1, state.getTicksPerSecond()),
                game.getRemainingTicks(),
                game.getOutcome().name(),
                game.getPlantSun(),
                game.getZombieSun(),
                plantCooldownTicks,
                plantCooldownTotalTicks,
                zombieCooldownTicks,
                zombieCooldownTotalTicks,
                plants,
                zombies,
                projectiles,
                brains);
    }
}
