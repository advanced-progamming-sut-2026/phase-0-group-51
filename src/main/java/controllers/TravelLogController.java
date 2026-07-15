package controllers;

import Data.database.QuestsRepository;
import Data.loader.PlantData;
import Data.loader.PlantRegistry;
import models.App;
import models.Result;
import models.User;
import models.enums.Menu;
import models.quests.*;

import java.util.List;
import java.util.Locale;

public class TravelLogController {
    private final QuestService questService = QuestService.getInstance();
    private String currentPage = "main";
    public Result changePage(String pageName) {
        String normalized = normalize(pageName);
        if (!isValidPage(normalized)) {
            return failure("Travel Log pages are main, daily, epic, and minigame.\n");
        }
        currentPage = normalized;
        Result page = showCurrentPage();
        if (!page.success()) {
            return page;
        }
        return success("Travel Log page changed to " + normalized + ".\n"
                + page.message());
    }

    public Result showCurrentPage() {
        if (currentPage.equals("minigame")) {
            return success(minigamePage());
        }
        User user = App.getInstance().getLoggedInUser();
        if (user == null) {
            return failure("You must log in before viewing quests.\n");
        }
        QuestType type = QuestType.fromPageName(currentPage);
        List<QuestsRepository.QuestEntry> entries = questService.getPage(user, type);
        return success(formatQuestPage(type, entries));
    }

    public Result claimQuest(int questId) {
        User user = App.getInstance().getLoggedInUser();
        try {
            String reward = questService.claimReward(user, questId);
            return success("Quest " + questId + " claimed: " + reward + ".\n");
        } catch (IllegalArgumentException | IllegalStateException exception) {
            return failure(exception.getMessage() + "\n");
        }
    }

    public Result showCurrentMenu() {
        return success("You are in the Travel Log on the " + currentPage + " page.\n");
    }

    public Result exitMenu() {
        App.getInstance().setCurrentMenu(Menu.GAME_MENU);
        return success("You returned to the Game Menu.\n");
    }

    private String formatQuestPage(
            QuestType type, List<QuestsRepository.QuestEntry> entries
    ) {
        StringBuilder output = new StringBuilder()
                .append(type.name()).append(" QUESTS\n");
        if (entries.isEmpty()) {
            return output.append("No quests are available.\n").toString();
        }
        for (QuestsRepository.QuestEntry entry : entries) {
            appendQuest(output, entry.quest(), entry.userQuest());
        }
        output.append("Use: claim quest -i <id>\n");
        return output.toString();
    }

    private void appendQuest(StringBuilder output, Quest quest, UserQuest userQuest) {
        output.append('[').append(quest.getId()).append("] ")
                .append(quest.getName()).append(" [")
                .append(quest.getPriority()).append("]\n")
                .append("  ").append(quest.getCondition()).append('\n')
                .append("  Progress: ").append(userQuest.getProgress())
                .append('/').append(quest.getTargetAmount()).append('\n')
                .append("  Reward: ").append(rewardText(quest)).append('\n')
                .append("  Status: ").append(statusText(userQuest)).append("\n\n");
    }

    private String rewardText(Quest quest) {
        if (quest.getRewardType() == QuestRewardType.CURRENCY_COINS) {
            return quest.getRewardAmount() + " coins";
        }
        if (quest.getRewardType() == QuestRewardType.CURRENCY_GEMS) {
            return quest.getRewardAmount() + " gems";
        }
        int plantId = Integer.parseInt(quest.getUnlockableId());
        PlantData plant = PlantRegistry.getById(plantId);
        String plantName = plant == null ? "plant #" + plantId : plant.name();
        return quest.getRewardType() == QuestRewardType.UNLOCKABLE
                ? "unlock " + plantName
                : quest.getRewardAmount() + " seed packets for " + plantName;
    }

    private String statusText(UserQuest userQuest) {
        if (userQuest.isClaimed()) {
            return "CLAIMED";
        }
        return userQuest.isCompleted() ? "READY TO CLAIM" : "IN PROGRESS";
    }

    private String minigamePage() {
        return """
                MINIGAMES
                Vasebreaker: stages 1-3
                start vasebreaker -s <stage>
                Wall-nut Bowling: stages 1-3
               start wallnut bowling -s <stage>
                I, Zombie: stages 1-3
                start IZombie -s <stage>
                Beghouled: stages 1-3
                start Beghouled -s <stage>
                Zombotany: stages 1-3
                start Zombotany -s <stage>
                """;
    }

    private boolean isValidPage(String page) {
        return page.equals("main") || page.equals("daily")
                || page.equals("epic") || page.equals("minigame");
    }

    private String normalize(String pageName) {
        return pageName == null ? "" : pageName.trim().toLowerCase(Locale.ROOT);
    }

    private Result success(String message) {
        return new Result(true, message, null);
    }

    private Result failure(String message) {
        return new Result(false, message, null);
    }
}
