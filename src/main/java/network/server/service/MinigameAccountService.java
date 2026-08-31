package network.server.service;

import Data.database.MinigameProgressRepository;
import Data.database.NewsRepository;
import Data.database.ScoringRepository;
import Data.database.UserRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import models.User;
import models.meowPoint.ScoringRules;
import models.minigames.MinigameType;
import network.protocol.MessageType;
import network.protocol.NetworkJsonCodec;
import network.protocol.NetworkMessage;
import network.protocol.minigame.MinigameCompleteRequest;
import network.protocol.minigame.MinigameProgressDto;
import network.protocol.minigame.MinigameProgressResponse;
import network.protocol.minigame.ScoringResultRequest;
import network.protocol.minigame.ScoringResultResponse;
import network.server.ClientConnection;

import java.util.ArrayList;
import java.util.List;

public class MinigameAccountService {
    private final MinigameProgressRepository progressRepository =
            new MinigameProgressRepository();
    private final NewsRepository newsRepository = new NewsRepository();
    private final UserRepository userRepository = new UserRepository();
    private final ScoringRepository scoringRepository = new ScoringRepository();
    private final NetworkJsonCodec codec = new NetworkJsonCodec();

    public NetworkMessage handleGet(
            ClientConnection connection,
            NetworkMessage message
    ) {
        Integer userId = authenticatedUserId(connection);
        MinigameProgressResponse response = userId == null
                ? progressFailure("You must log in first.")
                : snapshot(userId, true, "Minigame progress loaded.");
        return encodeProgress(
                message.getRequestId(),
                MessageType.MINIGAME_PROGRESS_GET_RESPONSE,
                response
        );
    }

    public NetworkMessage handleComplete(
            ClientConnection connection,
            NetworkMessage message
    ) {
        Integer userId = authenticatedUserId(connection);
        if (userId == null) {
            return encodeProgress(
                    message.getRequestId(),
                    MessageType.MINIGAME_COMPLETE_RESPONSE,
                    progressFailure("You must log in first.")
            );
        }
        if (message.getPayload() == null) {
            return NetworkMessage.error(
                    message.getRequestId(),
                    "Minigame completion payload is required."
            );
        }

        try {
            MinigameCompleteRequest request = codec.decodePayload(
                    message.getPayload(),
                    MinigameCompleteRequest.class
            );
            if (request == null || request.getType() == null) {
                return encodeProgress(
                        message.getRequestId(),
                        MessageType.MINIGAME_COMPLETE_RESPONSE,
                        snapshot(userId, false, "Minigame type is required.")
                );
            }
            if (request.getStageNumber() < 1
                    || request.getStageNumber() > 3) {
                return encodeProgress(
                        message.getRequestId(),
                        MessageType.MINIGAME_COMPLETE_RESPONSE,
                        snapshot(userId, false, "Minigame stage must be 1, 2, or 3.")
                );
            }
            if (!progressRepository.isStageUnlocked(
                    userId,
                    request.getType(),
                    request.getStageNumber()
            )) {
                return encodeProgress(
                        message.getRequestId(),
                        MessageType.MINIGAME_COMPLETE_RESPONSE,
                        snapshot(userId, false, "That minigame stage is locked.")
                );
            }

            MinigameProgressRepository.Completion completion =
                    progressRepository.completeStage(
                            userId,
                            request.getType(),
                            request.getStageNumber()
                    );
            if (!completion.saved()) {
                return encodeProgress(
                        message.getRequestId(),
                        MessageType.MINIGAME_COMPLETE_RESPONSE,
                        snapshot(userId, false, "Minigame progress could not be saved.")
                );
            }

            createProgressNews(userId, request.getType(), completion);

            return encodeProgress(
                    message.getRequestId(),
                    MessageType.MINIGAME_COMPLETE_RESPONSE,
                    snapshot(userId, true, "Minigame progress saved.")
            );
        } catch (JsonProcessingException exception) {
            return NetworkMessage.error(
                    message.getRequestId(),
                    "Invalid minigame completion payload."
            );
        }
    }

    public NetworkMessage handleScoringResult(
            ClientConnection connection,
            NetworkMessage message
    ) {
        Integer userId = authenticatedUserId(connection);
        if (userId == null) {
            return encodeScoring(
                    message.getRequestId(),
                    scoringFailure("You must log in first.")
            );
        }
        if (message.getPayload() == null) {
            return NetworkMessage.error(
                    message.getRequestId(),
                    "Scoring result payload is required."
            );
        }

        try {
            ScoringResultRequest request = codec.decodePayload(
                    message.getPayload(),
                    ScoringResultRequest.class
            );
            if (request == null || request.getScore() < 0) {
                return encodeScoring(
                        message.getRequestId(),
                        scoringFailure("MeowPoint score cannot be negative.")
                );
            }

            User user = userRepository.getUserById(userId);
            if (user == null) {
                return encodeScoring(
                        message.getRequestId(),
                        scoringFailure("The logged-in user no longer exists.")
                );
            }

            int dailyBest = scoringRepository.saveDailyBest(
                    user,
                    ScoringRules.currentDate(),
                    request.getScore(),
                    request.isWon()
            );

            return encodeScoring(
                    message.getRequestId(),
                    new ScoringResultResponse(
                            true,
                            "MeowPoint result saved.",
                            dailyBest,
                            user.getMostMeowPoint(),
                            user.getMaxPoint(),
                            user.getGamesPlayed()
                    )
            );
        } catch (JsonProcessingException exception) {
            return NetworkMessage.error(
                    message.getRequestId(),
                    "Invalid scoring result payload."
            );
        } catch (IllegalArgumentException | IllegalStateException exception) {
            return encodeScoring(
                    message.getRequestId(),
                    scoringFailure(exception.getMessage())
            );
        }
    }

    private void createProgressNews(
            int userId,
            MinigameType type,
            MinigameProgressRepository.Completion completion
    ) {
        if (completion.newlyUnlockedStage() > 0) {
            newsRepository.createNewsForUser(
                    userId,
                    "New " + type.getDisplayName()
                            + " stage unlocked: Stage "
                            + completion.newlyUnlockedStage() + "."
            );
        }
        if (completion.minigameNewlyCompleted()) {
            newsRepository.createNewsForUser(
                    userId,
                    "You completed all three stages of "
                            + type.getDisplayName() + "."
            );
        }
    }

    private MinigameProgressResponse snapshot(
            int userId,
            boolean success,
            String message
    ) {
        User user = userRepository.getUserById(userId);
        if (user == null) {
            return progressFailure("The logged-in user no longer exists.");
        }

        List<MinigameProgressDto> values = new ArrayList<>();
        for (MinigameType type : MinigameType.values()) {
            MinigameProgressRepository.Progress progress =
                    progressRepository.getProgress(userId, type);
            values.add(
                    new MinigameProgressDto(
                            type,
                            progress.highestUnlockedStage(),
                            progress.highestCompletedStage()
                    )
            );
        }

        return new MinigameProgressResponse(
                success,
                message,
                values,
                user.getMiniGamesPlayed()
        );
    }

    private MinigameProgressResponse progressFailure(String message) {
        return new MinigameProgressResponse(
                false,
                message,
                List.of(),
                0
        );
    }

    private ScoringResultResponse scoringFailure(String message) {
        return new ScoringResultResponse(
                false,
                message == null ? "Scoring result could not be saved." : message,
                0,
                0,
                0,
                0
        );
    }

    private NetworkMessage encodeProgress(
            String requestId,
            MessageType type,
            MinigameProgressResponse response
    ) {
        try {
            return new NetworkMessage(
                    type,
                    requestId,
                    codec.encodePayload(response)
            );
        } catch (JsonProcessingException exception) {
            return NetworkMessage.error(
                    requestId,
                    "Could not create minigame progress response."
            );
        }
    }

    private NetworkMessage encodeScoring(
            String requestId,
            ScoringResultResponse response
    ) {
        try {
            return new NetworkMessage(
                    MessageType.SCORING_RESULT_RESPONSE,
                    requestId,
                    codec.encodePayload(response)
            );
        } catch (JsonProcessingException exception) {
            return NetworkMessage.error(
                    requestId,
                    "Could not create scoring response."
            );
        }
    }

    private Integer authenticatedUserId(ClientConnection connection) {
        if (connection == null
                || !connection.getSession().isAuthenticated()) {
            return null;
        }
        return connection.getSession().getUserId();
    }
}
