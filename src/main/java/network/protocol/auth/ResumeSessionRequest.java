package network.protocol.auth;

public class ResumeSessionRequest {
    private String token;

    public ResumeSessionRequest() {
    }

    public ResumeSessionRequest(String token) {
        this.token = token;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }
}