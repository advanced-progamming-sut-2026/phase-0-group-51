package network.client;

import network.protocol.match.MatchEndedDto;
import network.protocol.match.MatchStartDto;
import network.protocol.matchmaking.MatchFoundDto;

public class ClientSession {

    public enum MatchState {

        IDLE,
        FOUND,
        RUNNING,
        ENDED

    }


    private String matchId;
    private String opponentUsername;
    private String role;

    private long seed;
    private int stageNumber;
    private int matchDurationTicks;

    private MatchEndedDto lastMatchResult;

    private MatchState matchState =
            MatchState.IDLE;


    public synchronized boolean applyMatchFound(
            MatchFoundDto dto
    ) {

        if (
                dto == null
                        || dto.matchId() == null
                        || dto.matchId().isBlank()
        ) {

            return false;
        }

        if (!isValidRole(dto.role())) {
            return false;
        }


        if (
                matchState == MatchState.RUNNING
                        && matchId != null
                        && !matchId.equals(dto.matchId())
        ) {

            return false;
        }

        this.matchId =
                dto.matchId();

        this.opponentUsername =
                normalizeNullable(
                        dto.opponentUsername()
                );

        this.role =
                dto.role();

        this.seed = 0L;

        this.stageNumber = 0;

        this.matchDurationTicks = 0;

        this.lastMatchResult = null;

        this.matchState =
                MatchState.FOUND;

        return true;
    }


    public synchronized boolean applyMatchStart(
            MatchStartDto dto
    ) {

        if (
                dto == null
                        || dto.getMatchId() == null
                        || dto.getMatchId().isBlank()
        ) {

            return false;
        }

        if (!isValidRole(dto.getRole())) {

            return false;
        }

        if (
                matchId != null
                        && !matchId.equals(
                        dto.getMatchId()
                )
        ) {

            return false;
        }

        this.matchId =
                dto.getMatchId();

        this.role =
                dto.getRole();

        this.seed =
                dto.getSeed();

        this.stageNumber =
                dto.getStageNumber();

        this.matchDurationTicks =
                dto.getMatchDurationTicks();

        this.lastMatchResult = null;

        this.matchState =
                MatchState.RUNNING;

        return true;
    }


    public synchronized boolean applyMatchEnded(
            MatchEndedDto dto
    ) {

        if (
                dto == null
                        || dto.getMatchId() == null
                        || dto.getMatchId().isBlank()
        ) {

            return false;
        }

        if (
                matchId == null
                        || !matchId.equals(
                        dto.getMatchId()
                )
        ) {

            return false;
        }


        if (
                matchState
                        == MatchState.ENDED
        ) {

            return true;
        }

        this.lastMatchResult =
                dto;

        this.matchState =
                MatchState.ENDED;

        return true;
    }


    public synchronized void clearMatch() {

        matchId = null;

        opponentUsername = null;

        role = null;

        seed = 0L;

        stageNumber = 0;

        matchDurationTicks = 0;

        lastMatchResult = null;

        matchState =
                MatchState.IDLE;
    }

    public synchronized void reset() {

        clearMatch();
    }


    public synchronized boolean isIdle() {

        return matchState
                == MatchState.IDLE;
    }


    public synchronized boolean isMatchFound() {

        return matchState
                == MatchState.FOUND;
    }


    public synchronized boolean isInMatch() {

        return matchState
                == MatchState.FOUND
                || matchState
                == MatchState.RUNNING;
    }


    public synchronized boolean isMatchRunning() {

        return matchState
                == MatchState.RUNNING;
    }


    public synchronized boolean hasMatchEnded() {

        return matchState
                == MatchState.ENDED;
    }


    public synchronized boolean isPlantPlayer() {

        return "PLANT".equals(
                role
        );
    }


    public synchronized boolean isZombiePlayer() {

        return "ZOMBIE".equals(
                role
        );
    }


    public synchronized boolean isRole(
            String expectedRole
    ) {

        if (expectedRole == null) {
            return false;
        }

        return expectedRole.equals(
                role
        );
    }


    public synchronized boolean belongsToMatch(
            String candidateMatchId
    ) {

        if (
                candidateMatchId == null
                        || matchId == null
        ) {

            return false;
        }

        return matchId.equals(
                candidateMatchId
        );
    }


    public synchronized String getMatchId() {

        return matchId;
    }


    public synchronized String getOpponentUsername() {

        return opponentUsername;
    }


    public synchronized String getRole() {

        return role;
    }


    public synchronized long getSeed() {

        return seed;
    }


    public synchronized int getStageNumber() {

        return stageNumber;
    }


    public synchronized int getMatchDurationTicks() {

        return matchDurationTicks;
    }


    public synchronized MatchEndedDto getLastMatchResult() {

        return lastMatchResult;
    }


    public synchronized MatchState getMatchState() {

        return matchState;
    }


    private boolean isValidRole(
            String role
    ) {

        return "PLANT".equals(role)
                || "ZOMBIE".equals(role);
    }


    private String normalizeNullable(
            String value
    ) {

        if (value == null) {
            return null;
        }

        String normalized =
                value.trim();

        return normalized.isEmpty()
                ? null
                : normalized;
    }
}