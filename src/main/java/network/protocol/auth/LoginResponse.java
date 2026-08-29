package network.protocol.auth;

public class LoginResponse {
    private boolean success;
    private String message;
    private UserProfileDto user;
    private String sessionToken;

    public LoginResponse() {
    }

    public LoginResponse(
            boolean success,
            String message,
            UserProfileDto user
    ) {
        this(
                success,
                message,
                user,
                null
        );
    }
    public LoginResponse(
            boolean success,
            String message,
            UserProfileDto user,
            String sessionToken
    ) {
        this.success = success;
        this.message = message;
        this.user = user;
        this.sessionToken = sessionToken;
    }
    public String getSessionToken() {
        return sessionToken;
    }

    public void setSessionToken(String sessionToken) {
        this.sessionToken = sessionToken;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public UserProfileDto getUser() {
        return user;
    }

    public void setUser(UserProfileDto user) {
        this.user = user;
    }
}