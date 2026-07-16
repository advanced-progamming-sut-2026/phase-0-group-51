package controllers;

import Data.database.PlantRepository;
import Data.database.ProgressRepository;
import Data.database.UserRepository;
import models.App;
import models.Result;
import models.User;
import models.enums.Menu;
import models.games.ChapterTheme;
import models.games.Game;
import models.games.Level;

public class GameMenuController {
    private static final ChapterTheme[] ADVENTURE_CHAPTERS = {
            ChapterTheme.ANCIENT_EGYPT,
            ChapterTheme.FROSTBITE_CAVES,
            ChapterTheme.BIG_WAVE_BEACH,
            ChapterTheme.DARK_AGES
    };
    private int selectedChapterIndex = -1;
    private static final int[] CHAPTER_ONE_LEVEL_ONE_PLANTS = {
            1, 6, 7, 9, 25, 30, 44, 55
    };
    private int findChapterIndex(String chapterName) {
        if (chapterName == null || chapterName.isBlank()) {
            return -1;
        }
        String requestedName = chapterName.trim();
        for (int i = 0; i < ADVENTURE_CHAPTERS.length; i++) {
            ChapterTheme theme = ADVENTURE_CHAPTERS[i];
            String enumName = theme.name().replace('_', ' ');
            if (theme.getName().equalsIgnoreCase(requestedName)
                    || enumName.equalsIgnoreCase(requestedName)) {
                return i;
            }
        }
        return -1;
    }
    public Result handleEnterChapter(String chapter) {
        if (App.loggedInUser == null) {
            return new Result(false, "You must log in before entering a chapter.\n", null
            );
        }
        int requestedChapterIndex = findChapterIndex(chapter);
        if (requestedChapterIndex == -1) {
            return new Result(
                    false,
                    "Chapter not found.\n" + "Valid chapters:\n"
                            + "- Ancient Egypt\n" + "- Frostbite Caves\n" + "- Big Wave Beach\n" + "- Dark Ages\n",
                    null
            );
        }
        ProgressRepository progressRepository = new ProgressRepository();
        int[] currentProgress = progressRepository.getCurrentProgress(App.loggedInUser.getId());
        int unlockedChapterIndex = currentProgress[0] - 1;
        if (requestedChapterIndex > unlockedChapterIndex) {
            ChapterTheme highestUnlockedChapter = ADVENTURE_CHAPTERS[unlockedChapterIndex];
            return new Result(
                    false, "This chapter is locked for you.\n" + "Your highest unlocked chapter is "
                            + highestUnlockedChapter.getName() + ".\n",
                    null
            );
        }
        selectedChapterIndex = requestedChapterIndex;
        ChapterTheme selectedChapter = ADVENTURE_CHAPTERS[selectedChapterIndex];
        int highestUnlockedLevel;

        if (selectedChapterIndex < unlockedChapterIndex) {
            highestUnlockedLevel = selectedChapter.getLevels().size();
        } else {
            highestUnlockedLevel = Math.min(currentProgress[1], selectedChapter.getLevels().size());
        }
        return new Result(
                true,
                selectedChapter.getName()
                        + " selected.\n" + "Unlocked levels: 1 to "
                        + highestUnlockedLevel
                        + ".\n",
                null
        );
    }
    public Result enterLevel(int levelNumber) {
        if (App.loggedInUser == null) {
            return new Result(false, "You must log in before entering a level.\n", null);}
        if (selectedChapterIndex == -1) {
            return new Result(false, "You must select a chapter first.\n", null);
        }
        ChapterTheme selectedChapter = ADVENTURE_CHAPTERS[selectedChapterIndex];
        int numberOfLevels = selectedChapter.getLevels().size();
        if (levelNumber < 1 || levelNumber > numberOfLevels) {
            return new Result(
                    false,
                    "Invalid level number.\n" + selectedChapter.getName()
                            + " has levels 1 to " + numberOfLevels
                            + ".\n",
                    null
            );
        }
        ProgressRepository progressRepository = new ProgressRepository();
        int[] currentProgress = progressRepository.getCurrentProgress(App.loggedInUser.getId());
        int unlockedChapterIndex = currentProgress[0] - 1;
        int unlockedLevelIndex = currentProgress[1] - 1;
        if (selectedChapterIndex > unlockedChapterIndex) {
            return new Result(
                    false, "This chapter is locked for you.\n", null
            );
        }
        int requestedLevelIndex = levelNumber - 1;
        if (selectedChapterIndex == unlockedChapterIndex && requestedLevelIndex > unlockedLevelIndex) {
            return new Result(
                    false,
                    "Level " + levelNumber + " is locked.\n"
                            + "Your highest unlocked level in "
                            + selectedChapter.getName() + " is Level " + (unlockedLevelIndex + 1) + ".\n",
                    null
            );
        }
        Level selectedLevel = selectedChapter.getLevels().get(requestedLevelIndex);
        unlockChapterOneLevelOnePlants(selectedChapterIndex, requestedLevelIndex);
        Game newGame = new Game();
        newGame.setCurrentChapterIndex(selectedChapterIndex);
        newGame.setCurrentLevelIndex(requestedLevelIndex);
        App.getInstance().setCurrentGame(newGame);
        if (!selectedLevel.type().usesPlantSelection()) {
            return startLevelDirectly(newGame, selectedChapter, selectedLevel);}
        App.getInstance().setCurrentMenu(Menu.PlantSelection_Menu);
        return new Result(
                true, "Entered " + selectedChapter.getName() + " Level "
                        + levelNumber + ".\n" + "You are now in the Plant Selection Menu.\n", null);
    }
    private Result startLevelDirectly(Game game, ChapterTheme theme, Level level) {
        try {
            game.loadLevel();
            game.start();
            App.getInstance().setCurrentMenu(Menu.GAME_VIEW);
            String firstPlant = game.getConveyorBeltPlants().isEmpty() ? "none" :
                    game.getConveyorBeltPlants().getFirst().name();
            return new Result(
                    true,
                    "Entered " + theme.getName() + " Level " + level.levelNumber() + ".\n"
                            + "so plant selection was skipped.\n"
                            + "First conveyor plant: "
                            + firstPlant + ".\n"
                            + "A new random unlocked plant " + "arrives every 12 seconds.\n",
                    null
            );
        } catch (RuntimeException exception) {
            App.getInstance().setCurrentGame(null);
            App.getInstance().setCurrentMenu(Menu.GAME_MENU);
            String message = exception.getMessage();
            if (message == null || message.isBlank()) {
                message = "The level could not be initialized.";}
            return new Result(false, "Could not start the Conveyor Belt level: " + message + "\n", null);
        }
    }
    public void handleGreenhouse() {
        App.getInstance().setCurrentMenu(Menu.GREENHOUSE_MENU);
    }

    public void handleTravellog() {
        App.getInstance().setCurrentMenu(Menu.TRAVELLOG_MENU);
    }

    public void leaderboard() {
        App.getInstance().setCurrentMenu(Menu.LEADERBOARD_MENU);
    }

    public Result cheatAdd(int amount, String kind) {
        if(amount <= 0){
            return new Result(false, "Please enter a positive amount." , null);
        } else if (kind.equalsIgnoreCase("coin")) {
            User user = App.loggedInUser;
            user.setCoins(user.getCoins() + amount);
            UserRepository userRepository = new UserRepository();
            userRepository.updateStats(user);
            return new Result(true, "Successfully added coins." , null);
        } else if (kind.equalsIgnoreCase("diamond")) {
            User user = App.loggedInUser;
            user.setGems(user.getGems() + amount);
            UserRepository userRepository = new UserRepository();
            userRepository.updateStats(user);
            return new Result(true, "Successfully added gems." , null);
        } else{
            return new Result(false, "You can only add coin or diamond please specify." , null);
        }
    }

    public Result handleEnterMenu(String menuName) {
        if(menuName.equalsIgnoreCase("Collection")){
            App.getInstance().setCurrentMenu(Menu.COLLECTION_MENU);
            return new Result(true,"You are now in the Collection menu.",null);
        }else{
            return new Result(false, "You can only go to the Collection menu!",null);
        }
    }
    private void unlockChapterOneLevelOnePlants(int chapterIndex, int levelIndex) {
        User user = App.getInstance().getLoggedInUser();
        if (user == null || chapterIndex != 0 || levelIndex != 0) {
            return;
        }
        PlantRepository.unlockPlants(user.getId(), CHAPTER_ONE_LEVEL_ONE_PLANTS);
    }
}
