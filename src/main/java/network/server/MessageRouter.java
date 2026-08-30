package network.server;

import network.protocol.MessageType;
import network.protocol.NetworkMessage;
import network.server.service.AuthService;
import network.server.service.ProfileService;
import network.server.service.PlantOwnershipService;
import network.server.service.GameplayAccountService;
import network.server.service.GreenHouseService;
import network.server.service.ShopService;
import network.server.service.QuestAccountService;
import network.server.service.MinigameAccountService;

public class MessageRouter {
    private final AuthService authService =
            new AuthService();

    private final ProfileService profileService =
            new ProfileService();

    private final PlantOwnershipService plantOwnershipService =
            new PlantOwnershipService();

    private final GameplayAccountService gameplayAccountService =
            new GameplayAccountService();

    private final GreenHouseService greenHouseService =
            new GreenHouseService();

    private final ShopService shopService =
            new ShopService();

    private final QuestAccountService questAccountService =
            new QuestAccountService();

    private final MinigameAccountService minigameAccountService =
            new MinigameAccountService();

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

        if (message.getType()
                == MessageType.GREENHOUSE_GET_REQUEST) {
            return greenHouseService.handleGet(
                    connection,
                    message
            );
        }

        if (message.getType()
                == MessageType.GREENHOUSE_PLANT_REQUEST) {
            return greenHouseService.handlePlant(
                    connection,
                    message
            );
        }

        if (message.getType()
                == MessageType.GREENHOUSE_GROW_REQUEST) {
            return greenHouseService.handleGrow(
                    connection,
                    message
            );
        }

        if (message.getType()
                == MessageType.GREENHOUSE_COLLECT_REQUEST) {
            return greenHouseService.handleCollect(
                    connection,
                    message
            );
        }


        if (message.getType()
                == MessageType.SHOP_GET_REQUEST) {
            return shopService.handleGet(
                    connection,
                    message
            );
        }

        if (message.getType()
                == MessageType.SHOP_PURCHASE_REQUEST) {
            return shopService.handlePurchase(
                    connection,
                    message
            );
        }

        if (message.getType()
                == MessageType.SHOP_DAILY_PURCHASE_REQUEST) {
            return shopService.handleDailyPurchase(
                    connection,
                    message
            );
        }

        if (message.getType()
                == MessageType.COLLECTION_PLANT_PURCHASE_REQUEST) {
            return shopService.handleCollectionPlantPurchase(
                    connection,
                    message
            );
        }

        if (message.getType()
                == MessageType.PLANT_UPGRADE_REQUEST) {
            return shopService.handlePlantUpgrade(
                    connection,
                    message
            );
        }

        if (message.getType()
                == MessageType.PLANT_BOOST_REQUEST) {
            return shopService.handlePlantBoost(
                    connection,
                    message
            );
        }

        if (message.getType()
                == MessageType.PLANT_BOOST_CONSUME_REQUEST) {
            return shopService.handlePlantBoostConsume(
                    connection,
                    message
            );
        }

        if (message.getType()
                == MessageType.PLANT_FOOD_CLAIM_REQUEST) {
            return shopService.handlePlantFoodClaim(
                    connection,
                    message
            );
        }

        if (message.getType()
                == MessageType.QUEST_GET_REQUEST) {
            return questAccountService.handleGet(
                    connection,
                    message
            );
        }

        if (message.getType()
                == MessageType.QUEST_CLAIM_REQUEST) {
            return questAccountService.handleClaim(
                    connection,
                    message
            );
        }

        if (message.getType()
                == MessageType.QUEST_SUN_PROGRESS_REQUEST) {
            return questAccountService.handleSunProgress(
                    connection,
                    message
            );
        }

        if (message.getType()
                == MessageType.QUEST_RUN_RECORD_REQUEST) {
            return questAccountService.handleRunRecord(
                    connection,
                    message
            );
        }

        if (message.getType()
                == MessageType.MINIGAME_PROGRESS_GET_REQUEST) {
            return minigameAccountService.handleGet(
                    connection,
                    message
            );
        }

        if (message.getType()
                == MessageType.MINIGAME_COMPLETE_REQUEST) {
            return minigameAccountService.handleComplete(
                    connection,
                    message
            );
        }

        if (message.getType()
                == MessageType.SCORING_RESULT_REQUEST) {
            return minigameAccountService.handleScoringResult(
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