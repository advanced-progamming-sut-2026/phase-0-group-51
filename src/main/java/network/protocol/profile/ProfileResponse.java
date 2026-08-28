package network.protocol.profile;

public class ProfileResponse {
    private boolean success;
    private String message;
    private ProfileDataDto profile;

    public ProfileResponse() {
    }

    public ProfileResponse(
            boolean success,
            String message,
            ProfileDataDto profile
    ) {
        this.success = success;
        this.message = message;
        this.profile = profile;
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

    public ProfileDataDto getProfile() {
        return profile;
    }

    public void setProfile(ProfileDataDto profile) {
        this.profile = profile;
    }
}
