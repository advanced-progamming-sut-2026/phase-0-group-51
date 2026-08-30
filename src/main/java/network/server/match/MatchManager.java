package network.server.match;

import models.minigames.iZombie.multiplayer.MatchRole;
import models.minigames.iZombie.multiplayer.MultiplayerIZombieGame;
import network.protocol.match.GameActionDto;
import network.protocol.match.MatchEndedDto;

import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class MatchManager {

    private final Map<String, MatchSession> sessions = new ConcurrentHashMap<>();
    private final Map<String, PlayerBinding> players = new ConcurrentHashMap<>();
    private final Random seedSource = new Random();

    public String createMatch(PlayerChannel plantPlayer, PlayerChannel zombiePlayer, int stageNumber) {
        long seed;
        synchronized (seedSource) {
            seed = seedSource.nextLong();
        }
        return createMatch(plantPlayer, zombiePlayer, stageNumber, seed,
            MultiplayerIZombieGame.DEFAULT_MATCH_DURATION_TICKS);
    }

    public String createMatch(
            PlayerChannel plantPlayer,
            PlayerChannel zombiePlayer,
            int stageNumber,
            long seed,
            int matchDurationTicks
    ) {
        Objects.requireNonNull(plantPlayer, "plantPlayer");
        Objects.requireNonNull(zombiePlayer, "zombiePlayer");
        if (isInMatch(plantPlayer.playerId()) || isInMatch(zombiePlayer.playerId())) {
            throw new IllegalStateException("A player is already in a match.");
        }

        String matchId = UUID.randomUUID().toString();
        MatchBroadcaster broadcaster = new RoleBroadcaster(matchId, plantPlayer, zombiePlayer);
        MatchSession session =
            new MatchSession(matchId, stageNumber, seed, matchDurationTicks, broadcaster);

        sessions.put(matchId, session);
        players.put(plantPlayer.playerId(), new PlayerBinding(matchId, MatchRole.PLANT));
        players.put(zombiePlayer.playerId(), new PlayerBinding(matchId, MatchRole.ZOMBIE));

        session.start();
        return matchId;
    }

    public void submitAction(String playerId, GameActionDto action) {
        PlayerBinding binding = players.get(playerId);
        if (binding == null) {
            return;
        }
        MatchSession session = sessions.get(binding.matchId());
        if (session != null) {
            session.submitAction(binding.role(), action);
        }
    }

    public void handleDisconnect(String playerId) {
        PlayerBinding binding = players.get(playerId);
        if (binding == null) {
            return;
        }
        MatchSession session = sessions.get(binding.matchId());
        if (session != null) {
            session.forfeit(binding.role());
        }
    }

    public boolean isInMatch(String playerId) {
        return players.containsKey(playerId);
    }

    public MatchSession getMatch(String matchId) {
        return sessions.get(matchId);
    }

    public int activeMatchCount() {
        return sessions.size();
    }

    private void cleanup(String matchId) {
        sessions.remove(matchId);
        players.values().removeIf(binding -> binding.matchId().equals(matchId));
    }

    private final class RoleBroadcaster implements MatchBroadcaster {

        private final String matchId;
        private final PlayerChannel plant;
        private final PlayerChannel zombie;

        private RoleBroadcaster(String matchId, PlayerChannel plant, PlayerChannel zombie) {
            this.matchId = matchId;
            this.plant = plant;
            this.zombie = zombie;
        }

        @Override
        public void toRole(MatchRole role, Object message) {
            safeSend(role == MatchRole.PLANT ? plant : zombie, message);
        }

        @Override
        public void toBoth(Object message) {
            safeSend(plant, message);
            safeSend(zombie, message);
            if (message instanceof MatchEndedDto) {
                cleanup(matchId);
            }
        }

        private void safeSend(PlayerChannel channel, Object message) {
            try {
                channel.send(message);
            } catch (RuntimeException ignored) {
            }
        }
    }

    private record PlayerBinding(String matchId, MatchRole role) {
    }
}
