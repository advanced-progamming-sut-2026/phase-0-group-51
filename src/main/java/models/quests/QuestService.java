package models.quests;

import Data.database.QuestsRepository;
import lombok.Getter;
import models.User;
import models.games.ChapterTheme;
import models.games.GameState;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Random;
@Getter
public class QuestService {
    private static final QuestService INSTANCE = new QuestService();
    private final QuestManager manager = new QuestManager();

    private QuestService() {
    }

    public static QuestService getInstance() {
        return INSTANCE;
    }

    public void recordEvent(User user, QuestEventType eventType, int amount) {
        manager.recordEvent(user, eventType, amount);
    }

    public List<QuestsRepository.QuestEntry> getPage(User user, QuestType type) {
        return manager.getPage(user, type);
    }

    public String claimReward(User user, int questId) {
        return manager.claimQuest(user, questId);
    }

}
