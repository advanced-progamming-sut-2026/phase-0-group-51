package network.server.service;

import Data.database.NewsRepository;
import Data.database.PlantRepository;
import Data.database.ProgressRepository;
import Data.database.UserRepository;
import Data.loader.PlantData;
import Data.loader.PlantRegistry;
import com.fasterxml.jackson.core.JsonProcessingException;
import models.enums.LootType;
import models.games.ChapterTheme;
import network.protocol.MessageType;
import network.protocol.NetworkJsonCodec;
import network.protocol.NetworkMessage;
import network.protocol.gameplay.AdventureLossResponse;
import network.protocol.gameplay.AdventureProgressResponse;
import network.protocol.gameplay.AdventureWinRequest;
import network.protocol.gameplay.AdventureWinResponse;
import network.protocol.gameplay.LootCollectRequest;
import network.protocol.gameplay.LootCollectResponse;
import network.server.ClientConnection;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class GameplayAccountService {
    private static final List<ChapterTheme> ADVENTURE_CHAPTERS =
            List.of(
                    ChapterTheme.ANCIENT_EGYPT,
                    ChapterTheme.FROSTBITE_CAVES,
                    ChapterTheme.BIG_WAVE_BEACH,
                    ChapterTheme.DARK_AGES
            );

    private final UserRepository userRepository =
            new UserRepository();

    private final ProgressRepository progressRepository =
            new ProgressRepository();

    private final NewsRepository newsRepository =
            new NewsRepository();

    private final NetworkJsonCodec codec =
            new NetworkJsonCodec();

    public NetworkMessage handleCollectLoot(
            ClientConnection connection,
            NetworkMessage message
    ) {
        if (message.getPayload() == null) {
            return NetworkMessage.error(
                    message.getRequestId(),
                    "Loot collection payload is required."
            );
        }

        try {
            LootCollectRequest request =
                    codec.decodePayload(
                            message.getPayload(),
                            LootCollectRequest.class
                    );

            LootCollectResponse response =
                    collectLoot(
                            connection,
                            request
                    );

            return encodeLootResponse(
                    message.getRequestId(),
                    response
            );

        } catch (JsonProcessingException exception) {
            return NetworkMessage.error(
                    message.getRequestId(),
                    "Invalid loot collection payload."
            );
        }
    }

    public NetworkMessage handleAdventureLoss(
            ClientConnection connection,
            NetworkMessage message
    ) {
        AdventureLossResponse response =
                recordAdventureLoss(
                        connection
                );

        try {
            return new NetworkMessage(
                    MessageType.ADVENTURE_LOSS_RESPONSE,
                    message.getRequestId(),
                    codec.encodePayload(response)
            );
        } catch (JsonProcessingException exception) {
            return NetworkMessage.error(
                    message.getRequestId(),
                    "Could not create Adventure loss response."
            );
        }
    }

    public NetworkMessage handleAdventureProgressGet(
            ClientConnection connection,
            NetworkMessage message
    ) {
        AdventureProgressResponse response =
                getAdventureProgress(connection);

        try {
            return new NetworkMessage(
                    MessageType.ADVENTURE_PROGRESS_GET_RESPONSE,
                    message.getRequestId(),
                    codec.encodePayload(response)
            );
        } catch (JsonProcessingException exception) {
            return NetworkMessage.error(
                    message.getRequestId(),
                    "Could not create Adventure progress response."
            );
        }
    }

    public NetworkMessage handleAdventureWin(
            ClientConnection connection,
            NetworkMessage message
    ) {
        if (message.getPayload() == null) {
            return NetworkMessage.error(
                    message.getRequestId(),
                    "Adventure win payload is required."
            );
        }

        try {
            AdventureWinRequest request =
                    codec.decodePayload(
                            message.getPayload(),
                            AdventureWinRequest.class
                    );

            AdventureWinResponse response =
                    recordAdventureWin(
                            connection,
                            request
                    );

            return new NetworkMessage(
                    MessageType.ADVENTURE_WIN_RESPONSE,
                    message.getRequestId(),
                    codec.encodePayload(response)
            );
        } catch (JsonProcessingException exception) {
            return NetworkMessage.error(
                    message.getRequestId(),
                    "Invalid Adventure win payload."
            );
        }
    }

    private LootCollectResponse collectLoot(
            ClientConnection connection,
            LootCollectRequest request
    ) {
        Integer userId =
                authenticatedUserId(
                        connection
                );

        if (userId == null) {
            return lootFailure(
                    "You must log in first.",
                    request == null
                            ? null
                            : request.getType()
            );
        }

        if (request == null
                || request.getType() == null) {
            return lootFailure(
                    "Loot type is required.",
                    null
            );
        }

        LootType type = request.getType();

        UserRepository.LootResult result =
                userRepository.applyZombieLoot(
                        userId,
                        type
                );

        if (!result.saved()) {
            return lootFailure(
                    "Loot could not be saved.",
                    type
            );
        }

        return new LootCollectResponse(
                true,
                "Loot collected.",
                type,
                result.total(),
                result.unlockedRow(),
                result.unlockedColumn()
        );
    }

    private AdventureLossResponse recordAdventureLoss(
            ClientConnection connection
    ) {
        Integer userId =
                authenticatedUserId(
                        connection
                );

        if (userId == null) {
            return new AdventureLossResponse(
                    false,
                    "You must log in first.",
                    0
            );
        }

        int gamesPlayed =
                userRepository.recordAdventureLoss(
                        userId
                );

        if (gamesPlayed < 0) {
            return new AdventureLossResponse(
                    false,
                    "Adventure loss could not be saved.",
                    0
            );
        }

        return new AdventureLossResponse(
                true,
                "Adventure loss recorded.",
                gamesPlayed
        );
    }

    private AdventureProgressResponse getAdventureProgress(
            ClientConnection connection
    ) {
        Integer userId = authenticatedUserId(connection);

        if (userId == null) {
            return new AdventureProgressResponse(
                    false,
                    "You must log in first.",
                    1,
                    1
            );
        }

        int[] progress =
                progressRepository.getCurrentProgress(userId);

        return new AdventureProgressResponse(
                true,
                "Adventure progress loaded.",
                progress[0],
                progress[1]
        );
    }

    private AdventureWinResponse recordAdventureWin(
            ClientConnection connection,
            AdventureWinRequest request
    ) {
        Integer userId = authenticatedUserId(connection);

        if (userId == null) {
            return adventureWinFailure(
                    "You must log in first."
            );
        }

        ValidationResult validation =
                validateAdventureWin(
                        userId,
                        request
                );

        if (!validation.valid()) {
            return adventureWinFailure(
                    validation.message()
            );
        }

        int completedChapter =
                request.getCompletedChapter();
        int completedLevel =
                request.getCompletedLevel();

        ChapterTheme completedTheme =
                ADVENTURE_CHAPTERS.get(
                        completedChapter - 1
                );

        Integer candidateChapter = null;
        Integer candidateLevel = null;

        if (completedLevel
                < completedTheme.getLevels().size()) {
            candidateChapter = completedChapter;
            candidateLevel = completedLevel + 1;
        } else if (completedChapter
                < ADVENTURE_CHAPTERS.size()) {
            candidateChapter = completedChapter + 1;
            candidateLevel = 1;
        }

        ProgressRepository.AdventureWinResult result =
                progressRepository.recordAdventureWin(
                        userId,
                        completedChapter,
                        completedLevel,
                        candidateChapter,
                        candidateLevel
                );

        if (!result.saved()) {
            return adventureWinFailure(
                    "Adventure win could not be saved."
            );
        }

        LinkedHashSet<Integer> newlyUnlocked =
                new LinkedHashSet<>();

        newlyUnlocked.addAll(
                PlantRepository.unlockPlantsAndReturnNew(
                        userId,
                        PlantRegistry.getLevelRewardPlantIds(
                                completedTheme,
                                completedLevel
                        )
                )
        );

        if (result.progressAdvanced()) {
            ChapterTheme unlockedTheme =
                    ADVENTURE_CHAPTERS.get(
                            result.newChapter() - 1
                    );

            if (result.newChapter()
                    > result.oldChapter()) {
                newlyUnlocked.addAll(
                        PlantRepository.unlockPlantsAndReturnNew(
                                userId,
                                PlantRegistry.getChapterPlantIds(
                                        unlockedTheme
                                )
                        )
                );

                newsRepository.createNewsForUser(
                        userId,
                        "New chapter unlocked: "
                                + unlockedTheme.getName()
                                + ". Level 1 is now available."
                );
            } else {
                newsRepository.createNewsForUser(
                        userId,
                        "New level unlocked: "
                                + unlockedTheme.getName()
                                + " Level "
                                + result.newLevel()
                                + "."
                );
            }
        }

        createPlantUnlockNews(
                userId,
                newlyUnlocked
        );

        Set<Integer> allUnlocked =
                PlantRepository.loadUnlockedPlants(
                        userId
                );

        String lastWonGame =
                "Chapter "
                        + completedChapter
                        + " Level "
                        + completedLevel;

        return new AdventureWinResponse(
                true,
                "Adventure win recorded.",
                result.gamesPlayed(),
                lastWonGame,
                result.newChapter(),
                result.newLevel(),
                result.progressAdvanced(),
                new ArrayList<>(allUnlocked),
                new ArrayList<>(newlyUnlocked)
        );
    }

    private ValidationResult validateAdventureWin(
            int userId,
            AdventureWinRequest request
    ) {
        if (request == null) {
            return new ValidationResult(
                    false,
                    "Adventure win request is required."
            );
        }

        int chapter = request.getCompletedChapter();
        int level = request.getCompletedLevel();

        if (chapter < 1
                || chapter > ADVENTURE_CHAPTERS.size()) {
            return new ValidationResult(
                    false,
                    "Adventure chapter is invalid."
            );
        }

        ChapterTheme theme =
                ADVENTURE_CHAPTERS.get(chapter - 1);

        if (level < 1
                || level > theme.getLevels().size()) {
            return new ValidationResult(
                    false,
                    "Adventure level is invalid."
            );
        }

        int[] currentProgress =
                progressRepository.getCurrentProgress(
                        userId
                );

        if (isLater(
                chapter,
                level,
                currentProgress[0],
                currentProgress[1]
        )) {
            return new ValidationResult(
                    false,
                    "That Adventure level is not unlocked yet."
            );
        }

        return new ValidationResult(true, "");
    }

    private void createPlantUnlockNews(
            int userId,
            Set<Integer> newlyUnlocked
    ) {
        for (int plantId : newlyUnlocked) {
            PlantData plant =
                    PlantRegistry.getById(plantId);

            String plantName =
                    plant == null
                            ? "Plant #" + plantId
                            : plant.name();

            newsRepository.createNewsForUser(
                    userId,
                    "New plant unlocked: "
                            + plantName
                            + "."
            );
        }
    }

    private boolean isLater(
            int candidateChapter,
            int candidateLevel,
            int currentChapter,
            int currentLevel
    ) {
        return candidateChapter > currentChapter
                || (candidateChapter == currentChapter
                && candidateLevel > currentLevel);
    }

    private AdventureWinResponse adventureWinFailure(
            String message
    ) {
        return new AdventureWinResponse(
                false,
                message,
                0,
                null,
                1,
                1,
                false,
                List.of(),
                List.of()
        );
    }

    private record ValidationResult(
            boolean valid,
            String message
    ) {
    }

    private Integer authenticatedUserId(
            ClientConnection connection
    ) {
        if (connection == null
                || !connection.getSession()
                .isAuthenticated()) {
            return null;
        }

        return connection.getSession()
                .getUserId();
    }

    private NetworkMessage encodeLootResponse(
            String requestId,
            LootCollectResponse response
    ) {
        try {
            return new NetworkMessage(
                    MessageType.GAMEPLAY_LOOT_COLLECT_RESPONSE,
                    requestId,
                    codec.encodePayload(response)
            );
        } catch (JsonProcessingException exception) {
            return NetworkMessage.error(
                    requestId,
                    "Could not create loot collection response."
            );
        }
    }

    private LootCollectResponse lootFailure(
            String message,
            LootType type
    ) {
        return new LootCollectResponse(
                false,
                message,
                type,
                0,
                0,
                0
        );
    }
}
