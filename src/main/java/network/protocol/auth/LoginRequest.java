package network.protocol.auth;

public class LoginRequest {
    private String username;
    private String password;
    private boolean rememberMe;

    public LoginRequest() {
    }
    public LoginRequest(
            String username,
            String password,
            boolean rememberMe
    ) {
        this.username = username;
        this.password = password;
        this.rememberMe = rememberMe;
    }

    public LoginRequest(
            String username,
            String password
    ) {
        this(username, password, false);
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }
    public boolean isRememberMe() {
        return rememberMe;
    }

    public void setRememberMe(boolean rememberMe) {
        this.rememberMe = rememberMe;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}