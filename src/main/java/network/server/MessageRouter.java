package network.server;

import network.protocol.MessageType;
import network.protocol.NetworkMessage;
import network.server.service.AuthService;

public class MessageRouter {
    private final AuthService authService =
            new AuthService();

    public NetworkMessage route(NetworkMessage message) {
        if (message == null || message.getType() == null) {
            return NetworkMessage.error(
                    null,
                    "Message type is required."
            );
        }

        if (message.getType() == MessageType.PING) {
            return NetworkMessage.pong(
                    message.getRequestId(),
                    message.getPayload()
            );
        }

        if (message.getType()
                == MessageType.REGISTER_REQUEST) {
            return authService.handleRegister(message);
        }

        return NetworkMessage.error(
                message.getRequestId(),
                "Unsupported message type: "
                        + message.getType()
        );
    }
}