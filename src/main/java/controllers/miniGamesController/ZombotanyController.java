package controllers.miniGamesController;

import Data.loader.PlantData;
import controllers.GamingController;
import models.App;
import models.Plant.Plant;
import models.Result;
import models.Zombie.Zombie;
import models.enums.Menu;
import models.games.Game;
import models.games.GameState;
import models.minigames.MinigameType;
import models.minigames.zombotany.Zombotany;

public class ZombotanyController extends GamingController {

    private final MinigameProgressService progressService = new MinigameProgressService();

    public Result startStage(int stageNumber) {
        Result access = progressService.checkStageAccess(MinigameType.ZOMBOTANY, stageNumber);
        if (!access.success()) {
            return access;
        }
        Zombotany game;
        try {
            game = new Zombotany(stageNumber);
            game.loadLevel();
        } catch (RuntimeException e) {
            return new Result(false, e.getMessage() + "\n", null);
        }
        App.getInstance().setCurrentGame(game);
        App.getInstance().setCurrentMenu(Menu.ZOMBOTANY);
        return new Result(true, "Zombotany stage " + stageNumber + " started! You have "
            + game.getGameState().getSun()
            + " sun. Place your plants before the first wave arrives.\n", game);
    }

    public Result placePlant(String plantName, int x, int y) {
        Zombotany game = activeGame();
        if (game == null) {
            return new Result(false, "No active Zombotany game found.\n", null);
        }
        StringBuilder events = new StringBuilder();
        game.getGameState().setEventLogger(events::append);
        try {
            Plant plant = game.placePlant(plantName, x, y);
            return new Result(true, events + resultAfterPossibleFinish(game), plant);
        } catch (RuntimeException e) {
            return new Result(false, e.getMessage() + "\n", null);
        } finally {
            game.getGameState().setEventLogger(null);
        }
    }

    @Override
    public Result advanceTime(int tickCount) {
        Zombotany game = activeGame();
        if (game == null) {
            return new Result(false, "No active Zombotany game found.\n", null);
        }
        if (tickCount < 0) {
            return new Result(false, "Tick count cannot be negative.\n", null);
        }
        StringBuilder events = new StringBuilder();
        game.getGameState().setEventLogger(events::append);
        try {
            game.forward(tickCount);
        } finally {
            game.getGameState().setEventLogger(null);
        }
        String message = events.length() == 0
            ? tickCount + " ticks passed quietly.\n"
            : events.toString();
        return new Result(true, message + resultAfterPossibleFinish(game), null);
    }

    public Result showPlants() {
        Zombotany game = activeGame();
        if (game == null) {
            return new Result(false, "No active Zombotany game found.\n", null);
        }
        int sun = game.getGameState().getSun();
        StringBuilder output = new StringBuilder("===== AVAILABLE PLANTS =====\n");
        for (PlantData data : game.getAvailablePlants()) {
            output.append(data.name())
                .append(" (").append(data.category()).append(")")
                .append(" - ").append(data.cost()).append(" sun");
            if (data.cost() > sun) {
                output.append("  [not enough sun]");
            }
            output.append('\n');
        }
        output.append("\nCurrent sun: ").append(sun).append('\n');
        return new Result(true, output.toString(), null);
    }

    public Result showStatus() {
        Zombotany game = activeGame();
        if (game == null) {
            return new Result(false, "No active Zombotany game found.\n", null);
        }
        GameState state = game.getGameState();
        StringBuilder output = new StringBuilder("===== ZOMBOTANY STATUS =====\n");
        output.append("Stage: ").append(game.getStageNumber()).append('\n');
        output.append("Tick: ").append(state.getTickCounter()).append('\n');
        output.append("Sun: ").append(state.getSun()).append('\n');
        output.append("Waves sent: ").append(game.getWavesSent())
            .append(" / ").append(game.getTotalWaves()).append('\n');
        output.append("Living zombies: ").append(game.getLivingZombieCount()).append('\n');
        output.append("\nZombotany zombies in this stage:\n");
        for (Zombie template : game.getTemplates()) {
            output.append("- ").append(template.getAlias()).append('\n');
        }
        output.append('\n');
        for (Zombie zombie : state.getZombiesInTheGame()) {
            if (!zombie.isDead()) {
                output.append(zombie.getAlias())
                    .append(" | lane ").append(zombie.getLane() + 1)
                    .append(" | column ").append(zombie.getColumn() + 1)
                    .append(" | HP ").append(zombie.getHitpoints())
                    .append('\n');
            }
        }
        return new Result(true, output.toString(), null);
    }

    public Result showCurrentMenu() {
        return new Result(true, "You are now in the Zombotany menu.\n", null);
    }

    public Result exitMenu() {
        App.getInstance().setCurrentMenu(Menu.TRAVELLOG_MENU);
        return new Result(true, "Returned to the travel log menu.\n", null);
    }

    private String resultAfterPossibleFinish(Zombotany game) {
        GameState state = game.getGameState();
        if (!state.isFinished()) {
            return "";
        }
        App.getInstance().setCurrentMenu(Menu.TRAVELLOG_MENU);
        if (state.isWon()) {
            return progressService.recordWin(MinigameType.ZOMBOTANY, game.getStageNumber());
        }
        return "";
    }

    private Zombotany activeGame() {
        Game game = App.getInstance().getCurrentGame();
        if (game instanceof Zombotany zombotany) {
            return zombotany;
        }
        return null;
    }
}
