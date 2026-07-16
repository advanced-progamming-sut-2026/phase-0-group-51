package controllers.miniGamesController;

import Data.database.MinigameProgressRepository;
import Data.database.NewsRepository;
import Data.database.UserRepository;
import models.App;
import models.Result;
import models.User;
import models.minigames.MinigameType;

public class MinigameProgressService {

        private final MinigameProgressRepository progressRepository = new MinigameProgressRepository();
        private final NewsRepository newsRepository = new NewsRepository();
        private final UserRepository userRepository = new UserRepository();

        public Result checkStageAccess(MinigameType type, int stageNumber) {
            if (stageNumber < 1 || stageNumber > 3) {
                return new Result(false, type.getDisplayName() + " stage must be 1, 2, or 3.\n", null
                );
            }

            User user = App.getInstance().getLoggedInUser();
            if (user == null) {
                return new Result(
                        false, "You must log in before starting a minigame.\n", null
                );
            }

            if (!progressRepository.isStageUnlocked(user.getId(), type, stageNumber)) {
                return new Result(
                        false,
                        type.getDisplayName() + " stage " + stageNumber + " is locked.\n"
                                + "Complete stage " + (stageNumber - 1) + " first.\n",
                        null
                );
            }

            return new Result(true, "", null);
        }

        public String recordWin(MinigameType type, int stageNumber) {
            User user = App.getInstance().getLoggedInUser();
            if (user == null) {
                return "Minigame progress was not saved because no user is logged in.\n";
            }

            MinigameProgressRepository.Completion completion =
                    progressRepository.completeStage(user.getId(), type, stageNumber);

            if (!completion.saved()) {
                return "Minigame progress could not be saved.\n";
            }

            StringBuilder message = new StringBuilder();

            if (completion.newlyUnlockedStage() > 0) {
                int unlockedStage = completion.newlyUnlockedStage();
                String news = "New " + type.getDisplayName()
                        + " stage unlocked: Stage " + unlockedStage + ".";
                newsRepository.createNewsForUser(user.getId(), news);
                message.append("Stage ")
                        .append(unlockedStage)
                        .append(" is now unlocked.\n");
            }

            if (completion.minigameNewlyCompleted()) {
                user.setMiniGamesPlayed(user.getMiniGamesPlayed() + 1);
                if (!userRepository.updateStats(user)) {
                    message.append("The completed-minigame counter could not be saved.\n");
                }

                String news = "You completed all three stages of "
                        + type.getDisplayName() + ".";
                newsRepository.createNewsForUser(user.getId(), news);
                message.append(type.getDisplayName())
                        .append(" is now fully completed.\n");
            }

            return message.toString();
        }

        public String formatStages(int userId, MinigameType type) {
            MinigameProgressRepository.Progress progress =
                    progressRepository.getProgress(userId, type);
            StringBuilder output = new StringBuilder();

            for (int stage = 1; stage <= 3; stage++) {
                String status;
                if (stage <= progress.highestCompletedStage()) {
                    status = "COMPLETED";
                } else if (stage <= progress.highestUnlockedStage()) {
                    status = "UNLOCKED";
                } else {
                    status = "LOCKED";
                }

                output.append("  Stage ")
                        .append(stage)
                        .append(" [")
                        .append(status)
                        .append("]\n");
            }

            return output.toString();
        }
    }


