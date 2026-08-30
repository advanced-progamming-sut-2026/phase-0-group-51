package network.protocol.match;

public class ActionResultDto {

    private String clientActionId;
    private boolean accepted;
    private String reason;

    public ActionResultDto() {
    }

    public ActionResultDto(String clientActionId, boolean accepted, String reason) {
        this.clientActionId = clientActionId;
        this.accepted = accepted;
        this.reason = reason;
    }

    public static ActionResultDto accepted(String clientActionId) {
        return new ActionResultDto(clientActionId, true, null);
    }

    public static ActionResultDto rejected(String clientActionId, String reason) {
        return new ActionResultDto(clientActionId, false, reason);
    }

    public String getClientActionId() {
        return clientActionId;
    }

    public void setClientActionId(String clientActionId) {
        this.clientActionId = clientActionId;
    }

    public boolean isAccepted() {
        return accepted;
    }

    public void setAccepted(boolean accepted) {
        this.accepted = accepted;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
