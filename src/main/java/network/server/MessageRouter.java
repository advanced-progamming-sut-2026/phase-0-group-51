package network.server;

import network.protocol.MessageType;
import network.protocol.NetworkMessage;
import network.server.service.AuthService;

public class MessageRouter {
    private final AuthService authService =
            new AuthService();

    public NetworkMessage route(
            ClientConnection connection,
            NetworkMessage message
    ) {
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
        if (message.getType()
                == MessageType.LOGIN_REQUEST) {
            return authService.handleLogin(
                    connection,
                    message
            );
        }
        if (message.getType()
                == MessageType.LOGOUT_REQUEST) {
            return authService.handleLogout(
                    connection,
                    message
            );
        }
        if (message.getType()
                == MessageType.FORGOT_PASSWORD_START_REQUEST) {
            return authService.handleForgotPasswordStart(
                    connection,
                    message
            );
        }

        if (message.getType()
                == MessageType.FORGOT_PASSWORD_ANSWER_REQUEST) {
            return authService.handleForgotPasswordAnswer(
                    connection,
                    message
            );
        }

        if (message.getType()
                == MessageType.PASSWORD_RESET_REQUEST) {
            return authService.handlePasswordReset(
                    connection,
                    message
            );
        }
        return NetworkMessage.error(
                message.getRequestId(),
                "Unsupported message type: "
                        + message.getType()
        );
    }
}