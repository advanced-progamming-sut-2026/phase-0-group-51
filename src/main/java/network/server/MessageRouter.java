package network.server;

import network.protocol.MessageType;
import network.protocol.NetworkMessage;
import network.server.matchmaking.InviteManager;
import network.server.matchmaking.MatchmakingService;
import network.server.matchmaking.MatchmakingStateRegistry;
import network.server.matchmaking.RandomQueue;
import network.server.presence.ConnectionRegistry;
import network.server.service.*;

import java.util.Objects;

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

    private final LeaderboardService leaderboardService =
            new LeaderboardService();

    private final NewsService newsService =
            new NewsService();

    private final ConnectionRegistry connectionRegistry;

    private final RandomQueue randomQueue =
            new RandomQueue();

    private final InviteManager inviteManager =
            new InviteManager();

    private final MatchmakingStateRegistry matchmakingStates =
            new MatchmakingStateRegistry();

    private final MatchmakingService matchmakingService;

    private final ReactionService reactionService;

    /**
     * Keeps compatibility with the pre-matchmaking code path that used
     * new MessageRouter(). New server code should prefer injecting the
     * shared ConnectionRegistry through the other constructor.
     */
    public MessageRouter() {
        this(new ConnectionRegistry());
    }

    public MessageRouter(
            ConnectionRegistry connectionRegistry
    ) {
        this.connectionRegistry =
                Objects.requireNonNull(
                        connectionRegistry,
                        "connectionRegistry cannot be null"
                );

        this.matchmakingService =
                new MatchmakingService(
                        connectionRegistry,
                        randomQueue,
                        inviteManager,
                        matchmakingStates
                );

        this.reactionService =
                new ReactionService(
                        connectionRegistry
                        // active match directory بعداً
                );
    }

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

            NetworkMessage response =
                    authService.handleLogin(
                            connection,
                            message
                    );

            registerAuthenticatedConnection(connection);
            return response;
        }

        if (message.getType()
                == MessageType.LOGOUT_REQUEST) {

            NetworkMessage response =
                    authService.handleLogout(
                            connection,
                            message
                    );

            if (!connection.getSession().isAuthenticated()) {
                String username =
                        connectionRegistry.unregister(connection);

                if (!connectionRegistry.isOnline(username)) {
                    handleDisconnect(username);
                }

                if (username != null) {
                    System.out.println(
                            "[PRESENCE] "
                                    + username
                                    + " logged out"
                    );
                }
            }

            return response;
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

            NetworkMessage response =
                    authService.handleResumeSession(
                            connection,
                            message
                    );

            registerAuthenticatedConnection(connection);
            return response;
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
                == MessageType.PROFILE_DIFFICULTY_UPDATE_REQUEST) {
            return profileService.handleDifficultyUpdate(
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
                == MessageType.PLANT_DEBUG_UNLOCK_REQUEST) {
            return shopService.handleDebugPlantUnlock(
                    connection,
                    message
            );
        }

        if (message.getType()
                == MessageType.NEWS_GET_REQUEST) {
            return newsService.handleGet(
                    connection,
                    message
            );
        }

        if (message.getType()
                == MessageType.NEWS_MARK_ALL_READ_REQUEST) {
            return newsService.handleMarkAllRead(
                    connection,
                    message
            );
        }

        if (message.getType()
                == MessageType.ZOMBIE_DISCOVER_REQUEST) {
            return newsService.handleZombieDiscover(
                    connection,
                    message
            );
        }

        // Quest routes from HEAD.
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

        // Minigame/account-stat routes from HEAD.
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

        if (message.getType()
                == MessageType.LEADERBOARD_GET_REQUEST) {
            return leaderboardService.handleGet(
                    connection,
                    message
            );
        }

        // Matchmaking routes from origin/main.
        if (message.getType()
                == MessageType.MATCHMAKING_QUEUE_REQUEST) {
            return matchmakingService.handleQueueRequest(
                    connection,
                    message
            );
        }

        if (message.getType()
                == MessageType.MATCHMAKING_QUEUE_LEAVE_REQUEST) {
            return matchmakingService.handleQueueLeave(
                    connection,
                    message
            );
        }

        if (message.getType()
                == MessageType.MATCHMAKING_INVITE_REQUEST) {
            return matchmakingService.handleInviteRequest(
                    connection,
                    message
            );
        }

        if (message.getType()
                == MessageType.MATCHMAKING_INVITE_DECISION) {
            return matchmakingService.handleInviteDecision(
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

    public void handleDisconnect(String username) {
        if (username == null || username.isBlank()) {
            return;
        }

        matchmakingService.handleDisconnect(username);
        reactionService.handleDisconnect(username);
    }

    private void registerAuthenticatedConnection(
            ClientConnection connection
    ) {
        if (connection == null
                || connection.getSession() == null
                || !connection.getSession().isAuthenticated()) {
            return;
        }

        String username =
                connection.getSession().getUsername();

        if (username == null || username.isBlank()) {
            return;
        }

        connectionRegistry.register(
                username,
                connection
        );

        System.out.println(
                "[PRESENCE] "
                        + username
                        + " is ONLINE"
                        + " | online users = "
                        + connectionRegistry
                        .getOnlineUserCount()
        );
    }
}
