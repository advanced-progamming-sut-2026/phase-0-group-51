package network.server;

import network.protocol.MessageType;
import network.protocol.NetworkMessage;
import network.server.service.AuthService;
import network.server.service.ProfileService;
import network.server.service.PlantOwnershipService;
import network.server.service.GameplayAccountService;

public class MessageRouter {
    private final AuthService authService =
            new AuthService();

    private final ProfileService profileService =
            new ProfileService();

    private final PlantOwnershipService plantOwnershipService =
            new PlantOwnershipService();

    private final GameplayAccountService gameplayAccountService =
            new GameplayAccountService();

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

        if (message.getType()
                == MessageType.PLANT_OWNERSHIP_GET_REQUEST) {
            return plantOwnershipService.handleGet(
                    connection,
                    message
            );
        }

        if (message.getType()
                == MessageType.GAMEPLAY_LOOT_COLLECT_REQUEST) {
            return gameplayAccountService.handleCollectLoot(
                    connection,
                    message
            );
        }

        if (message.getType()
                == MessageType.ADVENTURE_LOSS_REQUEST) {
            return gameplayAccountService.handleAdventureLoss(
                    connection,
                    message
            );
        }

        if (message.getType()
                == MessageType.ADVENTURE_PROGRESS_GET_REQUEST) {
            return gameplayAccountService.handleAdventureProgressGet(
                    connection,
                    message
            );
        }

        if (message.getType()
                == MessageType.ADVENTURE_WIN_REQUEST) {
            return gameplayAccountService.handleAdventureWin(
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