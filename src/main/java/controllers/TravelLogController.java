package controllers;

import Data.database.QuestsRepository;
import controllers.miniGamesController.MinigameProgressService;
import models.App;
import models.Result;
import models.User;
import models.enums.Menu;
import models.minigames.MinigameType;
import models.quests.*;

import java.util.List;
import java.util.Locale;

public class TravelLogController {
    private final QuestService questService = QuestService.getInstance();
    private final MinigameProgressService minigameProgressService = new MinigameProgressService();
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
        User user = App.getInstance().getLoggedInUser();
        if (user == null) {
            return failure("You must log in before viewing the Travel Log.\n");
        }
        if (currentPage.equals("minigame")) {
            return success(minigamePage(user));
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
                .append("  ").append(questService.resolvedCondition(quest, userQuest)).append('\n')
                .append("  Progress: ").append(userQuest.getProgress())
                .append('/').append(userQuest.getTargetAmount()).append('\n')
                .append("  Reward: ").append(rewardText(quest, userQuest)).append('\n')
                .append("  Status: ").append(statusText(userQuest)).append("\n\n");
    }

    private String rewardText(Quest quest, UserQuest userQuest) {
        if (quest.getRewardType() == QuestRewardType.CURRENCY_COINS) {
            return userQuest.getRewardAmount() + " coins";
        }
        if (quest.getRewardType() == QuestRewardType.CURRENCY_GEMS) {
            return userQuest.getRewardAmount() + " gems";
        }
        String target = quest.getUnlockableId();
        if (quest.getRewardType() == QuestRewardType.UNLOCKABLE) {
            return target == null || target.equalsIgnoreCase("any_plant")
                    ? "unlock one random locked plant" : "unlock " + target;
        }
        return userQuest.getRewardAmount() + " seed packets for a random unlocked plant";
    }

    private String statusText(UserQuest userQuest) {
        if (userQuest.isClaimed()) {
            return "CLAIMED";
        }
        return userQuest.isCompleted() ? "READY TO CLAIM" : "IN PROGRESS";
    }

    private String minigamePage(User user) {
        StringBuilder output = new StringBuilder("MINIGAMES\n");
        appendMinigame(
                output, user, MinigameType.VASEBREAKER,
                "start vasebreaker -s <stage>"
        );
        appendMinigame(
                output, user, MinigameType.WALLNUT_BOWLING,
                "start wallnut bowling -s <stage>"
        );
        appendMinigame(
                output, user, MinigameType.IZOMBIE,
                "start IZombie -s <stage>"
        );
        appendMinigame(
                output, user, MinigameType.BEGHOULDED,
                "start Beghouled -s <stage>"
        );
        appendMinigame(
                output, user, MinigameType.ZOMBOTANY,
                "start Zombotany -s <stage>"
        );
        return output.toString();
    }

    private void appendMinigame(
            StringBuilder output,
            User user,
            MinigameType type,
            String command
    ) {
        output.append(type.getDisplayName()).append(":\n")
                .append(minigameProgressService.formatStages(user.getId(), type))
                .append("  Use: ").append(command).append("\n\n");
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
