package network.server;

public class ClientSession {
    private volatile Integer userId;
    private volatile String username;
    private volatile Integer recoveryUserId;
    private volatile String recoveryUsername;
    private volatile boolean recoveryVerified;

    public boolean isAuthenticated() {
        return userId != null;
    }

    public void authenticate(
            int authenticatedUserId,
            String authenticatedUsername
    ) {
        userId = authenticatedUserId;
        username = authenticatedUsername;
    }

    public void clear() {
        userId = null;
        username = null;
        clearPasswordRecovery();
    }

    public void beginPasswordRecovery(
            int userId,
            String username
    ) {
        recoveryUserId = userId;
        recoveryUsername = username;
        recoveryVerified = false;
    }

    public boolean hasPasswordRecovery() {
        return recoveryUserId != null
                && recoveryUsername != null;
    }

    public void verifyPasswordRecovery() {
        if (hasPasswordRecovery()) {
            recoveryVerified = true;
        }
    }

    public boolean canResetPassword() {
        return hasPasswordRecovery()
                && recoveryVerified;
    }

    public Integer getRecoveryUserId() {
        return recoveryUserId;
    }

    public String getRecoveryUsername() {
        return recoveryUsername;
    }

    public void clearPasswordRecovery() {
        recoveryUserId = null;
        recoveryUsername = null;
        recoveryVerified = false;
    }

    public Integer getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }
}