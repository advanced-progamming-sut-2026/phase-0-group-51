package views.graphical.screens.minigamesScreen.iZombie.online;

import lombok.Getter;
import network.protocol.match.MatchSnapshot;

import java.util.concurrent.atomic.AtomicReference;
@Getter
public final class RemoteMatchMirror {
    private final String matchId;
    private final AtomicReference<MatchSnapshot> latestSnapshot = new AtomicReference<>();

    public RemoteMatchMirror(String matchId) {

        if (matchId == null || matchId.isBlank()) {
            throw new IllegalArgumentException(
                    "matchId cannot be null or blank"
            );
        }

        this.matchId = matchId;
    }

    public boolean apply(MatchSnapshot snapshot) {

        if (snapshot == null || snapshot.getMatchId() == null || !matchId.equals(snapshot.getMatchId())) {
            return false;
        }


        while (true) {

            MatchSnapshot current = latestSnapshot.get();
            if (current != null && snapshot.getTick() <= current.getTick()) {
                return false;
            }


            if (latestSnapshot.compareAndSet(current, snapshot)) {
                return true;
            }
        }
    }

    public MatchSnapshot getLatestSnapshot() {
        return latestSnapshot.get();
    }


    public boolean hasSnapshot() {
        return latestSnapshot.get() != null;
    }


    public int getLatestTick() {
        MatchSnapshot snapshot = latestSnapshot.get();
        return snapshot == null ? -1 : snapshot.getTick();
    }

    public void clear() {
        latestSnapshot.set(
                null
        );
    }
}
