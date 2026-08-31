package controllers;

import controllers.miniGamesController.MinigameProgressService;
import models.App;
import models.Result;
import models.User;
import models.enums.Menu;
import models.minigames.MinigameType;
import models.quests.QuestType;
import network.client.ClientQuestState;
import network.protocol.quests.QuestEntryDto;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class TravelLogController {
    private final MinigameProgressService minigameProgressService =
            new MinigameProgressService();
    private String currentPage = "main";

    public Result changePage(String pageName) {
        String normalized = normalize(pageName);
        if (!isValidPage(normalized)) {
            return failure(
                    "Travel Log pages are main, daily, epic, and minigame.\n"
            );
        }
        currentPage = normalized;
        Result page = showCurrentPage();
        if (!page.success()) {
            return page;
        }
        return success(
                "Travel Log page changed to " + normalized + ".\n"
                        + page.message()
        );
    }

    public Result showCurrentPage() {
        User user = App.getInstance().getLoggedInUser();
        if (user == null) {
            return failure(
                    "You must log in before viewing the Travel Log.\n"
            );
        }
        if (currentPage.equals("minigame")) {
            return success(minigamePage(user));
        }
        if (!ClientQuestState.isLoaded()) {
            return failure(
                    "Quest data has not been loaded from the server yet. "
                            + "Open the graphical Travel Log first.\n"
            );
        }

        QuestType type = QuestType.fromPageName(currentPage);
        return success(formatQuestPage(
                type,
                ClientQuestState.getEntries(type)
        ));
    }

    public Result claimQuest(int questId) {
        return failure(
                "Quest rewards are server-backed. "
                        + "Claim them from the graphical Travel Log.\n"
        );
    }

    public Result showCurrentMenu() {
        return success(
                "You are in the Travel Log on the "
                        + currentPage + " page.\n"
        );
    }

    public Result exitMenu() {
        App.getInstance().setCurrentMenu(Menu.GAME_MENU);
        return success("You returned to the Game Menu.\n");
    }

    private String formatQuestPage(
            QuestType type,
            List<QuestEntryDto> entries
    ) {
        StringBuilder output = new StringBuilder()
                .append(type.name())
                .append(" QUESTS\n");
        if (entries.isEmpty()) {
            return output.append("No quests are available.\n").toString();
        }
        for (QuestEntryDto entry : entries) {
            output.append('[')
                    .append(entry.getQuestId())
                    .append("] ")
                    .append(entry.getName())
                    .append(" [")
                    .append(entry.getPriority())
                    .append("]\n")
                    .append("  ")
                    .append(entry.getDescription())
                    .append('\n')
                    .append("  Progress: ")
                    .append(entry.getProgress())
                    .append('/')
                    .append(entry.getTargetAmount())
                    .append('\n')
                    .append("  Reward: ")
                    .append(entry.getRewardText())
                    .append('\n')
                    .append("  Status: ")
                    .append(statusText(entry))
                    .append("\n\n");
        }
        return output.toString();
    }

    private String statusText(QuestEntryDto entry) {
        if (entry.isClaimed()) {
            return "CLAIMED";
        }
        return entry.isCompleted()
                ? "READY TO CLAIM"
                : "IN PROGRESS";
    }

    private String minigamePage(User user) {
        StringBuilder output = new StringBuilder("MINIGAMES\n");
        appendMinigame(
                output,
                user,
                MinigameType.VASEBREAKER,
                "start vasebreaker -s <stage>"
        );
        appendMinigame(
                output,
                user,
                MinigameType.WALLNUT_BOWLING,
                "start wallnut bowling -s <stage>"
        );
        appendMinigame(
                output,
                user,
                MinigameType.IZOMBIE,
                "start IZombie -s <stage>"
        );
        appendMinigame(
                output,
                user,
                MinigameType.BEGHOULDED,
                "start Beghouled -s <stage>"
        );
        appendMinigame(
                output,
                user,
                MinigameType.ZOMBOTANY,
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
        output.append(type.getDisplayName())
                .append(":\n")
                .append(minigameProgressService.formatStages(
                        user.getId(),
                        type
                ))
                .append("  Use: ")
                .append(command)
                .append("\n\n");
    }

    private boolean isValidPage(String page) {
        return page.equals("main")
                || page.equals("daily")
                || page.equals("epic")
                || page.equals("minigame");
    }

    private String normalize(String pageName) {
        return pageName == null
                ? ""
                : pageName.trim().toLowerCase(Locale.ROOT);
    }

    public List<QuestEntryDto> getQuestEntries(QuestType type) {
        if (!ClientQuestState.isLoaded()) {
            return List.of();
        }
        return ClientQuestState.getEntries(type);
    }

    public List<QuestEntryDto> getAllQuestEntries() {
        if (!ClientQuestState.isLoaded()) {
            return List.of();
        }
        List<QuestEntryDto> entries = new ArrayList<>(
                ClientQuestState.getEntries()
        );
        entries.sort(
                Comparator.comparingInt(
                        (QuestEntryDto entry) ->
                                entry.getPriority().ordinal()
                ).thenComparingInt(QuestEntryDto::getQuestId)
        );
        return entries;
    }

    private Result success(String message) {
        return new Result(true, message, null);
    }

    private Result failure(String message) {
        return new Result(false, message, null);
    }
}
