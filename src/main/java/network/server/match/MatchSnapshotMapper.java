package network.server.match;

import models.Board.Board;
import models.Plant.Plant;
import models.Zombie.Zombie;
import models.Zombie.Behavior.DamageReactionBehavior;
import models.Zombie.Behavior.ImpThrowBehavior;
import models.Zombie.Behavior.InstantKillBehavior;
import models.Zombie.Behavior.RangedAttackBehavior;
import models.games.GameState;
import models.effects.VisualEffectEvent;
import models.minigames.iZombie.multiplayer.MultiplayerIZombieGame;
import models.minigames.vaseBreaker.Brain;
import models.projectile.Projectile;
import network.protocol.match.BrainNetState;
import network.protocol.match.MatchSnapshot;
import network.protocol.match.PlantNetState;
import network.protocol.match.ProjectileNetState;
import network.protocol.match.ZombieNetState;
import network.protocol.match.VisualEffectNetState;

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
            DamageReactionBehavior reaction =
                zombie.getBehavior(DamageReactionBehavior.class);
            InstantKillBehavior contact =
                zombie.getBehavior(InstantKillBehavior.class);
            ImpThrowBehavior impThrow =
                zombie.getBehavior(ImpThrowBehavior.class);

            ZombieNetState zombieState = new ZombieNetState(
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
                ranged == null ? -1 : ranged.getCooldown(),
                zombie.getAttackSerial());

            // Mirror the behavior-internal flags that drive special zombie
            // animations client-side (rage, spin, smash/tackle, imp throw,
            // octopus net toss, ...). Without these the remote render mirror
            // never sees anything but the generic walk/eat clips.
            zombieState.setRaged(reaction != null && reaction.isRaged());
            zombieState.setSpinning(reaction != null && reaction.isSpinning());
            zombieState.setHasKilled(contact != null && contact.isHasKilled());
            zombieState.setImpFired(impThrow != null && impThrow.isFired());
            zombieState.setRangedHasTarget(
                ranged != null && ranged.isOctopusHasTarget());

            zombies.add(zombieState);
        }

        List<ProjectileNetState> projectiles = new ArrayList<>();
        for (Projectile projectile : board.getProjectiles()) {
            if (projectile == null || projectile.isMarkedForRemoval()) {
                continue;
            }
            Plant source = projectile.getSourcePlant();
            projectiles.add(new ProjectileNetState(
                ids.idFor(projectile),
                projectile.getVisualProjectileKey(),
                projectile.getPosX(),
                projectile.getPosY(),
                source == null ? 0 : ids.idFor(source),
                source == null ? null : source.getName(),
                source == null ? -1 : source.getPosY(),
                source == null ? -1 : source.getPosX(),
                projectile.getVisualReleaseId(),
                projectile.isLaunched(),
                projectile.getVisualArcOffset(),
                projectile.getTargetX(),
                projectile.getTargetY()));
        }

        List<BrainNetState> brains = new ArrayList<>();
        for (Brain brain : game.getBrains()) {

            brains.add(new BrainNetState(brain.getRow() - 1, brain.isEaten()));
        }

        List<VisualEffectNetState> visualEffects = new ArrayList<>();
        for (VisualEffectEvent event : state.consumeVisualEffects()) {
            if (event == null) {
                continue;
            }
            visualEffects.add(new VisualEffectNetState(
                event.type().name(), event.posX(), event.posY()));
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
            brains,
            visualEffects);
    }
}
