package network.server.match;

import models.minigames.iZombie.multiplayer.MatchRole;
import models.minigames.iZombie.multiplayer.MultiplayerIZombieGame;
import network.protocol.match.ActionResultDto;
import network.protocol.match.GameActionDto;
import network.protocol.match.MatchEndedDto;
import network.protocol.match.MatchSnapshot;
import network.protocol.match.MatchStartDto;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public final class MatchSession {

    private static final long TICK_PERIOD_MS = 100;

    private final String matchId;
    private final long seed;
    private final int stageNumber;

    private final MultiplayerIZombieGame engine;
    private final MatchActionQueue queue = new MatchActionQueue();
    private final MatchSnapshotMapper mapper = new MatchSnapshotMapper();
    private final EntityIdRegistry entityIds = new EntityIdRegistry();
    private final MatchBroadcaster broadcaster;
    private final ScheduledExecutorService ticker;

    private volatile MatchStatus status = MatchStatus.CREATED;

    public MatchSession(
            String matchId,
            int stageNumber,
            long seed,
            int matchDurationTicks,
            MatchBroadcaster broadcaster
    ) {
        this.matchId = Objects.requireNonNull(matchId, "matchId");
        this.stageNumber = stageNumber;
        this.seed = seed;
        this.broadcaster = Objects.requireNonNull(broadcaster, "broadcaster");
        this.engine = new MultiplayerIZombieGame(stageNumber, seed, matchDurationTicks);
        this.ticker = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "match-" + this.matchId);
            thread.setDaemon(true);
            return thread;
        });
    }

    public synchronized void start() {
        if (status != MatchStatus.CREATED) {
            throw new IllegalStateException("Match " + matchId + " has already started.");
        }
        engine.loadLevel();
        engine.start();
        status = MatchStatus.RUNNING;

        broadcaster.toRole(MatchRole.PLANT, startDto(MatchRole.PLANT));
        broadcaster.toRole(MatchRole.ZOMBIE, startDto(MatchRole.ZOMBIE));

        ticker.scheduleAtFixedRate(this::tick, 0, TICK_PERIOD_MS, TimeUnit.MILLISECONDS);
    }

    public void submitAction(MatchRole role, GameActionDto action) {
        if (status == MatchStatus.RUNNING) {
            queue.submit(role, action);
        }
    }

    public synchronized void forfeit(MatchRole loser) {
        if (status == MatchStatus.ENDED) {
            return;
        }
        MatchRole winner = loser == MatchRole.PLANT ? MatchRole.ZOMBIE : MatchRole.PLANT;
        finish(winner.name() + "_WON", winner.name(), "opponent left the match");
    }

    public MatchStatus getStatus() {
        return status;
    }

    public String getMatchId() {
        return matchId;
    }

    public MultiplayerIZombieGame getEngine() {
        return engine;
    }

    private void tick() {
        if (status != MatchStatus.RUNNING) {
            return;
        }
        try {
            List<ActionResultDto> results = queue.applyAll(engine);
            for (ActionResultDto result : results) {
                broadcaster.toBoth(result);
            }
            engine.onTick();
            MatchSnapshot snapshot = mapper.toSnapshot(engine, matchId, entityIds);
            broadcaster.toBoth(snapshot);
            if (engine.isFinished()) {
                finish(engine.getOutcome().name(), roleName(engine.getWinnerRole()), null);
            }
        } catch (RuntimeException e) {
            System.err.println("Match " + matchId + " tick failed: " + e);
            finish("ABORTED", null, "server error");
        }
    }

    private synchronized void finish(String outcome, String winnerRole, String reason) {
        if (status == MatchStatus.ENDED) {
            return;
        }
        status = MatchStatus.ENDED;
        ticker.shutdown();
        int finalTick = engine.getGameState() == null
            ? 0
            : engine.getGameState().getTickCounter();
        broadcaster.toBoth(new MatchEndedDto(matchId, outcome, winnerRole, finalTick, reason));
    }

    private MatchStartDto startDto(MatchRole role) {
        return new MatchStartDto(
            matchId,
            role.name(),
            seed,
            stageNumber,
            engine.getMatchDurationTicks());
    }

    private static String roleName(MatchRole role) {
        return role == null ? null : role.name();
    }
}
