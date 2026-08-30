package network.server.matchmaking;

import java.util.HashMap;
import java.util.Map;

public class InviteManager {

        private final Map<String, String> challengerByTarget = new HashMap<>();
        private final Map<String, String> targetByChallenger = new HashMap<>();
        public synchronized boolean createInvite(
                String challenger,
                String target
        ) {
            if (challenger == null
                    || target == null
                    || challenger.equals(target)) {
                return false;
            }

            if (hasPendingInvite(challenger)
                    || hasPendingInvite(target)) {
                return false;
            }

            challengerByTarget.put(
                    target,
                    challenger
            );

            targetByChallenger.put(
                    challenger,
                    target
            );

            return true;
        }

        public synchronized boolean hasPendingInvite(
                String username
        ) {
            return challengerByTarget
                    .containsKey(username)
                    || targetByChallenger
                    .containsKey(username);
        }

        public synchronized String getChallenger(
                String target
        ) {
            return challengerByTarget.get(target);
        }

        public synchronized String getTarget(
                String challenger
        ) {
            return targetByChallenger.get(
                    challenger
            );
        }

        public synchronized void removeUser(
                String username
        ) {
            String challenger =
                    challengerByTarget.remove(
                            username
                    );

            if (challenger != null) {
                targetByChallenger.remove(
                        challenger
                );
            }

            String target =
                    targetByChallenger.remove(
                            username
                    );

            if (target != null) {
                challengerByTarget.remove(
                        target
                );
            }
        }
    }

