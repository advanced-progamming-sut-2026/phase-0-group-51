package network.server.matchmaking;

import java.util.HashMap;
import java.util.Map;

public final class MatchmakingStateRegistry {
    private final Map<String, MatchmakingState>
            states = new HashMap<>();

    public synchronized MatchmakingState get(
            String username
    ) {
        return states.getOrDefault(
                username,
                MatchmakingState.IDLE
        );
    }

    public synchronized boolean isIdle(
            String username
    ) {
        return get(username)
                == MatchmakingState.IDLE;
    }

    public synchronized void set(
            String username,
            MatchmakingState state
    ) {
        if (state == MatchmakingState.IDLE) {
            states.remove(username);
        } else {
            states.put(username, state);
        }
    }

    public synchronized void clear(
            String username
    ) {
        states.remove(username);
    }
}
