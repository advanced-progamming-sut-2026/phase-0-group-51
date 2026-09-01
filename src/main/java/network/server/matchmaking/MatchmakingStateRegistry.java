package network.server.matchmaking;

import java.util.HashMap;
import java.util.Map;

public final class MatchmakingStateRegistry {

    private final Map<String, MatchmakingState> states =
            new HashMap<>();


    public synchronized MatchmakingState get(
            String username
    ) {

        String normalized =
                normalize(username);

        if (normalized == null) {
            return MatchmakingState.IDLE;
        }

        return states.getOrDefault(
                normalized,
                MatchmakingState.IDLE
        );
    }


    public synchronized boolean isIdle(
            String username
    ) {

        return get(username)
                == MatchmakingState.IDLE;
    }


    public synchronized boolean isQueued(
            String username
    ) {

        return get(username)
                == MatchmakingState.QUEUED;
    }


    public synchronized boolean isInviting(
            String username
    ) {

        return get(username)
                == MatchmakingState.INVITING;
    }


    public synchronized boolean isInvited(
            String username
    ) {

        return get(username)
                == MatchmakingState.INVITED;
    }


    public synchronized boolean isInMatch(
            String username
    ) {

        return get(username)
                == MatchmakingState.IN_MATCH;
    }


    public synchronized void set(
            String username,
            MatchmakingState state
    ) {

        String normalized =
                normalize(username);

        if (normalized == null) {
            return;
        }

        if (state == null
                || state == MatchmakingState.IDLE) {

            states.remove(normalized);

            return;
        }

        states.put(
                normalized,
                state
        );
    }


    public synchronized boolean transition(
            String username,
            MatchmakingState expected,
            MatchmakingState next
    ) {

        String normalized =
                normalize(username);

        if (normalized == null
                || expected == null
                || next == null) {

            return false;
        }

        MatchmakingState current =
                get(normalized);

        if (current != expected) {
            return false;
        }

        set(
                normalized,
                next
        );

        return true;
    }


    public synchronized void clear(
            String username
    ) {

        String normalized =
                normalize(username);

        if (normalized == null) {
            return;
        }

        states.remove(normalized);
    }


    public synchronized int size() {

        return states.size();
    }


    public synchronized Map<String, MatchmakingState> snapshot() {

        return Map.copyOf(states);
    }


    public synchronized void clearAll() {

        states.clear();
    }


    private String normalize(
            String username
    ) {

        if (username == null) {
            return null;
        }

        String normalized =
                username.trim();

        return normalized.isEmpty()
                ? null
                : normalized;
    }
}