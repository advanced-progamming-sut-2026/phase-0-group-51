package network.server;

import lombok.Getter;
import network.protocol.match.MatchEndedDto;
import network.protocol.match.MatchStartDto;
import network.protocol.matchmaking.MatchFoundDto;
@Getter
public final class ClientSession {
    private volatile Integer userId;
    private volatile String username;

    private volatile Integer recoveryUserId;
    private volatile String recoveryUsername;
    private volatile boolean recoveryVerified;

    private volatile String persistentTokenHash;


    public boolean isAuthenticated() {
        return userId != null;
    }


    public void authenticate(
            int authenticatedUserId,
            String authenticatedUsername
    ) {

        authenticate(
                authenticatedUserId,
                authenticatedUsername,
                null
        );
    }


    public void authenticate(
            int authenticatedUserId,
            String authenticatedUsername,
            String tokenHash
    ) {

        userId =
                authenticatedUserId;

        username =
                authenticatedUsername;

        persistentTokenHash =
                tokenHash;


        clearPasswordRecovery();
    }


    public void clear() {

        userId = null;

        username = null;

        persistentTokenHash = null;


        clearPasswordRecovery();
    }


    public void beginPasswordRecovery(
            int userId,
            String username
    ) {

        recoveryUserId =
                userId;

        recoveryUsername =
                username;

        recoveryVerified =
                false;
    }


    public boolean hasPasswordRecovery() {

        return recoveryUserId != null
                && recoveryUsername != null;
    }


    public void verifyPasswordRecovery() {

        if (hasPasswordRecovery()) {

            recoveryVerified =
                    true;
        }
    }


    public boolean canResetPassword() {

        return hasPasswordRecovery()
                && recoveryVerified;
    }



    public void clearPasswordRecovery() {

        recoveryUserId = null;

        recoveryUsername = null;

        recoveryVerified = false;
    }


}