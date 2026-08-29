package network.protocol.profile;

public class ProfilePasswordChangeRequest {
    private String oldPassword;
    private String newPassword;

    public ProfilePasswordChangeRequest() {
    }

    public ProfilePasswordChangeRequest(
            String oldPassword,
            String newPassword
    ) {
        this.oldPassword = oldPassword;
        this.newPassword = newPassword;
    }

    public String getOldPassword() {
        return oldPassword;
    }

    public void setOldPassword(String oldPassword) {
        this.oldPassword = oldPassword;
    }

    public String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }
}
