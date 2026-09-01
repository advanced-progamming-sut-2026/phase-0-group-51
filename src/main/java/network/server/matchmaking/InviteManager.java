package network.server.matchmaking;

import java.util.HashMap;
import java.util.Map;

public final class InviteManager {

    private final Map<String, String> challengerByTarget =
            new HashMap<>();
    private final Map<String, String> targetByChallenger =
            new HashMap<>();


    public synchronized boolean createInvite(
            String challenger,
            String target
    ) {

        String normalizedChallenger =
                normalize(challenger);

        String normalizedTarget =
                normalize(target);

        if (normalizedChallenger == null
                || normalizedTarget == null) {

            return false;
        }

        if (normalizedChallenger.equalsIgnoreCase(
                normalizedTarget
        )) {

            return false;
        }

        if (hasPendingInvite(normalizedChallenger)
                || hasPendingInvite(normalizedTarget)) {

            return false;
        }

        challengerByTarget.put(
                normalizedTarget,
                normalizedChallenger
        );

        targetByChallenger.put(
                normalizedChallenger,
                normalizedTarget
        );

        return true;
    }


    public synchronized boolean hasPendingInvite(
            String username
    ) {

        String normalized =
                normalize(username);

        if (normalized == null) {
            return false;
        }

        return challengerByTarget.containsKey(normalized)
                || targetByChallenger.containsKey(normalized);
    }


    public synchronized String getChallenger(
            String target
    ) {

        String normalized =
                normalize(target);

        if (normalized == null) {
            return null;
        }

        return challengerByTarget.get(
                normalized
        );
    }


    public synchronized String getTarget(
            String challenger
    ) {

        String normalized =
                normalize(challenger);

        if (normalized == null) {
            return null;
        }

        return targetByChallenger.get(
                normalized
        );
    }


    public synchronized void removeUser(
            String username
    ) {

        String normalized =
                normalize(username);

        if (normalized == null) {
            return;
        }

        String challenger =
                challengerByTarget.remove(
                        normalized
                );

        if (challenger != null) {

            targetByChallenger.remove(
                    challenger
            );
        }

        String target =
                targetByChallenger.remove(
                        normalized
                );

        if (target != null) {

            challengerByTarget.remove(
                    target
            );
        }
    }


    public synchronized int pendingInviteCount() {

        return challengerByTarget.size();
    }


    public synchronized void clear() {

        challengerByTarget.clear();

        targetByChallenger.clear();
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