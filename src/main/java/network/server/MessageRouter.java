package network.server;

import network.protocol.MessageType;
import network.protocol.NetworkMessage;
import network.server.service.AuthService;
import network.server.service.ProfileService;

public class MessageRouter {
    private final AuthService authService =
            new AuthService();

    private final ProfileService profileService =
            new ProfileService();

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
        if (message.getType()
                == MessageType.RESUME_SESSION_REQUEST) {
            return authService.handleResumeSession(
                    connection,
                    message
            );
        }

        if (message.getType()
                == MessageType.PROFILE_GET_REQUEST) {
            return profileService.handleGet(
                    connection,
                    message
            );
        }

        if (message.getType()
                == MessageType.PROFILE_UPDATE_REQUEST) {
            return profileService.handleUpdate(
                    connection,
                    message
            );
        }

        if (message.getType()
                == MessageType.PROFILE_PASSWORD_CHANGE_REQUEST) {
            return profileService.handlePasswordChange(
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