package network.server;

import network.protocol.MessageType;
import network.protocol.NetworkMessage;

public class MessageRouter {

    public NetworkMessage route(NetworkMessage message) {
        if (message == null || message.getType() == null) {
            return NetworkMessage.error(null, "Message type is required.");
        }

        if (message.getType() == MessageType.PING) {
            return NetworkMessage.pong(message.getRequestId(), message.getPayload());
        }

        return NetworkMessage.error(
                message.getRequestId(),
                "Unsupported message type: " + message.getType()
        );
    }
}
