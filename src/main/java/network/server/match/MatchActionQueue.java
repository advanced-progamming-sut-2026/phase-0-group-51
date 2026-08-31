package network.server.match;

import models.minigames.iZombie.multiplayer.MatchRole;
import models.minigames.iZombie.multiplayer.MultiplayerIZombieGame;
import network.protocol.match.ActionResultDto;
import network.protocol.match.GameActionDto;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentLinkedQueue;

public final class MatchActionQueue {

    private final ConcurrentLinkedQueue<PendingAction> queue =
        new ConcurrentLinkedQueue<>();

    public void submit(MatchRole role, GameActionDto action) {
        Objects.requireNonNull(role, "role");
        Objects.requireNonNull(action, "action");
        queue.add(new PendingAction(role, action));
    }

    public List<ActionResultDto> applyAll(MultiplayerIZombieGame game) {
        Objects.requireNonNull(game, "game");
        List<ActionResultDto> results = new ArrayList<>();
        PendingAction pending;
        while ((pending = queue.poll()) != null) {
            results.add(apply(game, pending));
        }
        return results;
    }

    public boolean isEmpty() {
        return queue.isEmpty();
    }

    public int size() {
        return queue.size();
    }

    public void clear() {
        queue.clear();
    }

    private ActionResultDto apply(MultiplayerIZombieGame game, PendingAction pending) {
        GameActionDto action = pending.action();
        String actionId = action.getClientActionId();

        if (action.getType() == null) {
            return ActionResultDto.rejected(actionId, "Action has no type.");
        }

        try {

            int x = action.getColumn() + 1;
            int y = action.getRow() + 1;

            switch (action.getType()) {
                case PLACE_ZOMBIE ->
                    game.placeZombie(pending.role(), action.getEntityName(), x, y);
                case PLACE_PLANT ->
                    game.placePlant(pending.role(), action.getEntityName(), x, y);
                case PLUCK_PLANT ->
                    game.pluckPlant(pending.role(), x, y);
                case FEED_PLANT ->
                    game.feedPlant(pending.role(), x, y);
            }
            return ActionResultDto.accepted(actionId);
        } catch (RuntimeException e) {

            String reason = e.getMessage() == null ? "Action rejected." : e.getMessage();
            return ActionResultDto.rejected(actionId, reason);
        }
    }

    private record PendingAction(MatchRole role, GameActionDto action) {
    }
}
