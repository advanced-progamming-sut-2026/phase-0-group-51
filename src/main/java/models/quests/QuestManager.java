package models.quests;

import Data.database.PlantRepository;
import Data.database.QuestsRepository;
import Data.database.UserRepository;
import Data.loader.PlantData;
import Data.loader.PlantRegistry;
import models.User;

import java.util.List;

public class QuestManager {
    private final QuestsRepository repository;
    public QuestManager() {
        this(new QuestsRepository());
    }
    QuestManager(QuestsRepository repository) {
        this.repository = repository;
    }

    public void recordEvent(User user, QuestEventType eventType, int amount) {
        if (user == null || eventType == null || amount <= 0) {
            return;
        }
        repository.addProgress(user.getId(), eventType, amount);
    }

    public List<QuestsRepository.QuestEntry> getPage(User user, QuestType type) {
        if (user == null) {
            throw new IllegalStateException("A logged-in user is required.");
        }
        return repository.getQuestEntries(user.getId(), type);
    }

    public String claimQuest(User user, int questId) {
        if (user == null) {
            throw new IllegalStateException("A logged-in user is required.");
        }
        QuestsRepository.QuestEntry entry = repository
                .getQuestEntry(user.getId(), questId)
                .orElseThrow(() -> new IllegalArgumentException("Quest not found."));
        validateClaim(entry.userQuest());
        String reward = giveReward(user, entry.quest());
        if (!repository.markClaimed(user.getId(), questId)) {
            throw new IllegalStateException("Quest reward could not be claimed.");
        }
        repository.incrementQuestCounter(user.getId(), entry.quest().getType());
        updateInMemoryCounter(user, entry.quest().getType());
        return reward;
    }

    private void validateClaim(UserQuest userQuest) {
        if (!userQuest.isCompleted()) {
            throw new IllegalStateException("Quest is not complete yet.");
        }
        if (userQuest.isClaimed()) {
            throw new IllegalStateException("Quest reward was already claimed.");
        }
    }

    private String giveReward(User user, Quest quest) {
        return switch (quest.getRewardType()) {
            case CURRENCY_COINS -> giveCoins(user, quest.getRewardAmount());
            case CURRENCY_GEMS -> giveGems(user, quest.getRewardAmount());
            case INVENTORY -> giveSeedPackets(user, quest);
            case UNLOCKABLE -> unlockPlant(user, quest);
        };
    }

    private String giveCoins(User user, int amount) {
        int previous = user.getCoins();
        user.setCoins(previous + amount);
        if (!saveUser(user)) {
            user.setCoins(previous);
            throw new IllegalStateException("Could not save the coin reward.");
        }
        return amount + " coins";
    }

    private String giveGems(User user, int amount) {
        int previous = user.getGems();
        user.setGems(previous + amount);
        if (!saveUser(user)) {
            user.setGems(previous);
            throw new IllegalStateException("Could not save the gem reward.");
        }
        return amount + " gems";
    }

    private String giveSeedPackets(User user, Quest quest) {
        int plantId = rewardPlantId(quest);
        int before = PlantRepository.getSeedPackets(user.getId(), plantId);
        PlantRepository.addSeedPackets(user.getId(), plantId, quest.getRewardAmount());
        int after = PlantRepository.getSeedPackets(user.getId(), plantId);
        if (after < before + quest.getRewardAmount()) {
            throw new IllegalStateException("Could not save the seed-packet reward.");
        }
        return quest.getRewardAmount() + " seed packets for " + plantLabel(plantId);
    }

    private String unlockPlant(User user, Quest quest) {
        int plantId = rewardPlantId(quest);
        PlantRepository.unlockPlant(user.getId(), plantId);
        if (!PlantRepository.loadUnlockedPlants(user.getId()).contains(plantId)) {
            throw new IllegalStateException("Could not save the plant unlock reward.");
        }
        return plantLabel(plantId) + " unlocked";
    }

    private int rewardPlantId(Quest quest) {
        try {
            return Integer.parseInt(quest.getUnlockableId());
        } catch (NumberFormatException | NullPointerException exception) {
            throw new IllegalStateException("Quest has an invalid plant reward.");
        }
    }

    private String plantLabel(int plantId) {
        PlantData plant = PlantRegistry.getById(plantId);
        return plant == null ? "plant #" + plantId : plant.name();
    }

    private boolean saveUser(User user) {
        return new UserRepository().updateStats(user);
    }

    private void updateInMemoryCounter(User user, QuestType type) {
        if (type == QuestType.DAILY) {
            user.setQuestDailyNum(user.getQuestDailyNum() + 1);
        } else {
            user.setQuestNonDailyNum(user.getQuestNonDailyNum() + 1);
        }
    }
}
