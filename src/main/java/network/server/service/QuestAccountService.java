package network.server.service;

import Data.database.PlantRepository;
import Data.database.QuestsRepository;
import Data.database.UserRepository;
import Data.loader.QuestLoader;
import com.fasterxml.jackson.core.JsonProcessingException;
import models.User;
import models.quests.Quest;
import models.quests.QuestRewardType;
import models.quests.QuestService;
import models.quests.QuestType;
import models.quests.UserQuest;
import network.protocol.MessageType;
import network.protocol.NetworkJsonCodec;
import network.protocol.NetworkMessage;
import network.protocol.quests.QuestClaimRequest;
import network.protocol.quests.QuestEntryDto;
import network.protocol.quests.QuestResponse;
import network.protocol.quests.QuestRunSummary;
import network.protocol.quests.QuestSunProgressRequest;
import network.server.ClientConnection;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

public class QuestAccountService {
    private final QuestService questService = QuestService.getInstance();
    private final UserRepository userRepository = new UserRepository();
    private final NetworkJsonCodec codec = new NetworkJsonCodec();
    private boolean questDefinitionsLoaded;

    public NetworkMessage handleGet(
            ClientConnection connection,
            NetworkMessage message
    ) {
        Integer userId = authenticatedUserId(connection);
        QuestResponse response = userId == null
                ? failure("You must log in first.")
                : snapshot(userId, true, "Quests loaded.");
        return encode(
                message.getRequestId(),
                MessageType.QUEST_GET_RESPONSE,
                response
        );
    }

    public NetworkMessage handleClaim(
            ClientConnection connection,
            NetworkMessage message
    ) {
        Integer userId = authenticatedUserId(connection);
        if (userId == null) {
            return encode(
                    message.getRequestId(),
                    MessageType.QUEST_CLAIM_RESPONSE,
                    failure("You must log in first.")
            );
        }
        if (message.getPayload() == null) {
            return NetworkMessage.error(
                    message.getRequestId(),
                    "Quest claim payload is required."
            );
        }

        try {
            ensureQuestDefinitions();
            QuestClaimRequest request = codec.decodePayload(
                    message.getPayload(),
                    QuestClaimRequest.class
            );
            User user = userRepository.getUserById(userId);
            if (user == null) {
                return encode(
                        message.getRequestId(),
                        MessageType.QUEST_CLAIM_RESPONSE,
                        failure("The logged-in user no longer exists.")
                );
            }

            String reward = questService.claimReward(
                    user,
                    request.getQuestId()
            );
            return encode(
                    message.getRequestId(),
                    MessageType.QUEST_CLAIM_RESPONSE,
                    snapshot(
                            userId,
                            true,
                            "Quest " + request.getQuestId()
                                    + " claimed: " + reward + "."
                    )
            );
        } catch (JsonProcessingException exception) {
            return NetworkMessage.error(
                    message.getRequestId(),
                    "Invalid quest claim payload."
            );
        } catch (IllegalArgumentException | IllegalStateException exception) {
            return encode(
                    message.getRequestId(),
                    MessageType.QUEST_CLAIM_RESPONSE,
                    snapshot(userId, false, exception.getMessage())
            );
        }
    }

    public NetworkMessage handleSunProgress(
            ClientConnection connection,
            NetworkMessage message
    ) {
        Integer userId = authenticatedUserId(connection);
        if (userId == null) {
            return encode(
                    message.getRequestId(),
                    MessageType.QUEST_SUN_PROGRESS_RESPONSE,
                    failure("You must log in first.")
            );
        }
        if (message.getPayload() == null) {
            return NetworkMessage.error(
                    message.getRequestId(),
                    "Quest sun progress payload is required."
            );
        }

        try {
            ensureQuestDefinitions();
            QuestSunProgressRequest request = codec.decodePayload(
                    message.getPayload(),
                    QuestSunProgressRequest.class
            );
            if (request.getAmount() <= 0) {
                return encode(
                        message.getRequestId(),
                        MessageType.QUEST_SUN_PROGRESS_RESPONSE,
                        snapshot(userId, false, "Sun amount must be positive.")
                );
            }
            User user = userRepository.getUserById(userId);
            if (user == null) {
                return encode(
                        message.getRequestId(),
                        MessageType.QUEST_SUN_PROGRESS_RESPONSE,
                        failure("The logged-in user no longer exists.")
                );
            }

            questService.recordSunCollected(user, request.getAmount());
            return encode(
                    message.getRequestId(),
                    MessageType.QUEST_SUN_PROGRESS_RESPONSE,
                    snapshot(userId, true, "Quest progress updated.")
            );
        } catch (JsonProcessingException exception) {
            return NetworkMessage.error(
                    message.getRequestId(),
                    "Invalid quest sun progress payload."
            );
        } catch (IllegalStateException exception) {
            return encode(
                    message.getRequestId(),
                    MessageType.QUEST_SUN_PROGRESS_RESPONSE,
                    snapshot(userId, false, exception.getMessage())
            );
        }
    }

    public NetworkMessage handleRunRecord(
            ClientConnection connection,
            NetworkMessage message
    ) {
        Integer userId = authenticatedUserId(connection);
        if (userId == null) {
            return encode(
                    message.getRequestId(),
                    MessageType.QUEST_RUN_RECORD_RESPONSE,
                    failure("You must log in first.")
            );
        }
        if (message.getPayload() == null) {
            return NetworkMessage.error(
                    message.getRequestId(),
                    "Quest run payload is required."
            );
        }

        try {
            ensureQuestDefinitions();
            QuestRunSummary summary = codec.decodePayload(
                    message.getPayload(),
                    QuestRunSummary.class
            );
            if (summary == null || summary.getChapter() == null) {
                return encode(
                        message.getRequestId(),
                        MessageType.QUEST_RUN_RECORD_RESPONSE,
                        snapshot(userId, false, "Quest run summary is invalid.")
                );
            }
            User user = userRepository.getUserById(userId);
            if (user == null) {
                return encode(
                        message.getRequestId(),
                        MessageType.QUEST_RUN_RECORD_RESPONSE,
                        failure("The logged-in user no longer exists.")
                );
            }

            questService.evaluateAdventureRun(user, summary);
            return encode(
                    message.getRequestId(),
                    MessageType.QUEST_RUN_RECORD_RESPONSE,
                    snapshot(userId, true, "Quest run recorded.")
            );
        } catch (JsonProcessingException exception) {
            return NetworkMessage.error(
                    message.getRequestId(),
                    "Invalid quest run payload."
            );
        } catch (IllegalStateException exception) {
            return encode(
                    message.getRequestId(),
                    MessageType.QUEST_RUN_RECORD_RESPONSE,
                    snapshot(userId, false, exception.getMessage())
            );
        }
    }

    private QuestResponse snapshot(
            int userId,
            boolean success,
            String message
    ) {
        ensureQuestDefinitions();

        User user = userRepository.getUserById(userId);
        if (user == null) {
            return failure("The logged-in user no longer exists.");
        }

        questService.initializeForUser(userId);

        List<QuestEntryDto> entries = new ArrayList<>();
        for (QuestType type : QuestType.values()) {
            for (QuestsRepository.QuestEntry entry
                    : questService.getPage(user, type)) {
                if (entry.userQuest().isClaimed()
                        && entry.quest().getType() != QuestType.DAILY) {
                    continue;
                }
                entries.add(toDto(entry));
            }
        }
        entries.sort(
                Comparator.comparingInt(
                        (QuestEntryDto entry) -> entry.getPriority().ordinal()
                ).thenComparingInt(QuestEntryDto::getQuestId)
        );

        Set<Integer> unlocked = PlantRepository.loadUnlockedPlants(userId);
        return new QuestResponse(
                success,
                message == null ? "" : message,
                entries,
                user.getCoins(),
                user.getGems(),
                user.getQuestDailyNum(),
                user.getQuestNonDailyNum(),
                unlocked.stream().sorted().toList()
        );
    }

    private QuestEntryDto toDto(QuestsRepository.QuestEntry entry) {
        Quest quest = entry.quest();
        UserQuest assignment = entry.userQuest();
        return new QuestEntryDto(
                quest.getId(),
                quest.getName(),
                questService.resolvedCondition(quest, assignment),
                rewardText(quest, assignment),
                quest.getType(),
                quest.getPriority(),
                assignment.getProgress(),
                assignment.getTargetAmount(),
                assignment.isCompleted(),
                assignment.isClaimed()
        );
    }

    private String rewardText(Quest quest, UserQuest assignment) {
        if (quest.getRewardType() == QuestRewardType.CURRENCY_COINS) {
            return assignment.getRewardAmount() + " coins";
        }
        if (quest.getRewardType() == QuestRewardType.CURRENCY_GEMS) {
            return assignment.getRewardAmount() + " gems";
        }
        if (quest.getRewardType() == QuestRewardType.UNLOCKABLE) {
            String target = quest.getUnlockableId();
            return target == null || target.equalsIgnoreCase("any_plant")
                    ? "unlock one random locked plant"
                    : "unlock " + target;
        }
        return assignment.getRewardAmount()
                + " seed packets for a random unlocked plant";
    }

    private synchronized void ensureQuestDefinitions() {
        if (questDefinitionsLoaded) {
            return;
        }
        QuestLoader.loadQuestsToDatabase();
        questDefinitionsLoaded = true;
    }

    private Integer authenticatedUserId(ClientConnection connection) {
        if (connection == null
                || !connection.getSession().isAuthenticated()) {
            return null;
        }
        return connection.getSession().getUserId();
    }

    private QuestResponse failure(String message) {
        return new QuestResponse(
                false,
                message,
                List.of(),
                0,
                0,
                0,
                0,
                List.of()
        );
    }

    private NetworkMessage encode(
            String requestId,
            MessageType type,
            QuestResponse response
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
                    "Could not create quest response."
            );
        }
    }
}
