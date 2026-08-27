package network.protocol;

import java.util.UUID;

public class NetworkMessage {
    private MessageType type;
    private String requestId;
    private String payload;

    public NetworkMessage() {
    }

    public NetworkMessage(MessageType type, String requestId, String payload) {
        this.type = type;
        this.requestId = requestId;
        this.payload = payload;
    }

    public static NetworkMessage ping(String payload) {
        return new NetworkMessage(MessageType.PING, UUID.randomUUID().toString(), payload);
    }

    public static NetworkMessage pong(String requestId, String payload) {
        return new NetworkMessage(MessageType.PONG, requestId, payload);
    }

    public static NetworkMessage error(String requestId, String payload) {
        return new NetworkMessage(MessageType.ERROR, requestId, payload);
    }

    public MessageType getType() {
        return type;
    }

    public void setType(MessageType type) {
        this.type = type;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }
}
