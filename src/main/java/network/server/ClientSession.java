package network.server;

public class ClientSession {
    private volatile Integer userId;
    private volatile String username;

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
    }

    public Integer getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }
}