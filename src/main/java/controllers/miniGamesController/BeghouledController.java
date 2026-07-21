package controllers.miniGamesController;

import controllers.GamingController;
import models.App;
import models.Board.Board;
import models.Board.Tile;
import models.Plant.Plant;
import models.Result;
import models.enums.Menu;
import models.games.Game;
import models.games.GameState;
import models.minigames.MinigameType;
import models.minigames.beghouled.Beghouled;

import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

public class BeghouledController extends GamingController {
    private final MinigameProgressService progressService = new MinigameProgressService();

    public Result startStage(int stageNumber) {
        Result access = progressService.checkStageAccess(MinigameType.BEGHOULDED, stageNumber);
        if (!access.success()) {
            return access;
        }
        try {
            Beghouled game = new Beghouled(stageNumber);
            game.loadLevel();
            App.getInstance().setCurrentGame(game);
            App.getInstance().setCurrentMenu(Menu.BEGHOULDED);
            return success(
                "Beghouled stage " + stageNumber + " started.\n"
                    + "Target: to find " + game.getTargetMatches() + " matches.\n"
            );
        } catch (RuntimeException exception) {
            String message = exception.getMessage() == null
                ? "Unknown error."
                : exception.getMessage();
            return failure("Could not start Beghouled: " + message + "\n");
        }
    }

    public Result swapPlants(int firstX, int firstY, int secondX, int secondY) {
        Beghouled game = activeGame();
        if (game == null) {
            return failure("No active Beghouled game found.\n");
        }
        StringBuilder events = beginEvent(game);
        try {
            Beghouled.SwapOutcome outcome = game.swapPlants(firstX, firstY, secondX, secondY);
            StringBuilder output = new StringBuilder()
                .append("Plants at (").append(firstX).append(", ").append(firstY)
                .append(") and (").append(secondX).append(", ").append(secondY).append(") were swapped.\n")
                .append("Direct matches: ").append(outcome.directMatches()).append("\nCascade matches: ")
                .append(outcome.cascadeMatches()).append("\nSun gained: ").append(outcome.sunGained())
                .append("\nProgress: ").append(outcome.totalMatches()).append('/').append(game.getTargetMatches())
                .append(" matches.\n").append(events);
            if (outcome.boardReset()) {
                output.append("The board had no proper move and was reset.\n");
            }
            return resultAfterPossibleFinish(game, output.toString());
        } catch (IllegalArgumentException | IllegalStateException exception) {
            return failure(exception.getMessage() + "\n");
        }
    }

    public Result upgradePlants(String fromPlant, String toPlant) {
        Beghouled game = activeGame();
        if (game == null) {
            return failure("No active Beghouled game found.\n");
        }
        StringBuilder events = beginEvent(game);
        try {
            Beghouled.UpgradeOutcome outcome = game.upgradePlants(fromPlant, toPlant);
            String output = "Upgraded " + outcome.transformedCount() + " "
                + outcome.fromPlant() + " plant(s) to " + outcome.toPlant() + ".\n"
                + "Cost: " + outcome.cost() + " sun.\n" + "Remaining sun: " + outcome.remainingSun() + ".\n"
                + events;
            return resultAfterPossibleFinish(game, output);
        } catch (IllegalArgumentException | IllegalStateException exception) {
            return failure(exception.getMessage() + "\n");
        }
    }

    public Result advanceTime(int tickCount) {
        Beghouled game = activeGame();
        if (game == null) {
            return failure("No active Beghouled game found.\n");
        }
        if (tickCount <= 0) {
            return failure("Tick count must be positive.\n");
        }

        StringBuilder events = beginEvent(game);
        try {
            game.forward(tickCount);
        } catch (IllegalArgumentException | IllegalStateException exception) {
            return failure(exception.getMessage() + "\n");
        }
        String output = "Game advanced by " + tickCount + " ticks.\n" + events;
        return resultAfterPossibleFinish(game, output);
    }

    public Result showStatus() {
        Beghouled game = activeGame();
        if (game == null) {
            return failure("No active Beghouled game found.\n");
        }
        GameState state = game.getGameState();
        StringBuilder output = new StringBuilder()
            .append("===== BEGHOULDED STATUS =====\n")
            .append("Stage: ").append(game.getStage().getStageNumber()).append("\nDifficulty: ")
            .append(game.getStage().getDifficulty())
            .append("\nTick: ").append(state.getTickCounter())
            .append("\nSun: ").append(state.getSun())
            .append("\nMatches: ").append(game.getCompletedMatches()).append('/').append(game.getTargetMatches())
            .append("\nEndless waves started: ").append(game.getWaveNumber())
            .append("\nLiving zombies: ")
            .append(game.getLivingZombieCount())
            .append("\nCraters: ").append(game.getCraterCount())
            .append("\nLegal swap available: ").append(game.hasAnyLegalSwap() ? "yes" : "no")
            .append("\n\nPlant counts:\n");

        Map<String, Integer> counts = game.getPlantCounts();
        if (counts.isEmpty()) {
            output.append("none\n");
        } else {
            counts.forEach((name, count) -> output
                .append("  ").append(name)
                .append(": ").append(count).append('\n'));
        }
        return success(output.toString());
    }

    public Result showUpgrades() {
        Beghouled game = activeGame();
        if (game == null) {
            return failure("No active Beghouled game found.\n");
        }

        Map<String, Integer> counts = game.getPlantCounts();
        StringBuilder output = new StringBuilder()
            .append("===== BEGHOULDED UPGRADES =====\n")
            .append("Current sun: ")
            .append(game.getGameState().getSun()).append("\n");
        for (Beghouled.UpgradeRule rule : game.getAvailableUpgrades()) {
            output.append(rule.fromPlant())
                .append(" -> ").append(rule.toPlant())
                .append(" | cost: ").append(rule.cost())
                .append(" sun | currently on board: ").append(counts.getOrDefault(rule.fromPlant(), 0))
                .append('\n');
        }
        return success(output.toString());
    }
    private String getBeghouledPlantCode(Plant plant) {
        if (plant == null) {
            return "..";
        }
        String plantName = plant.getName().trim().toLowerCase(Locale.ROOT);
        return switch (plantName) {
            case "peashooter" ->
                "PS";
            case "repeater" ->
                "RP";
            case "mega gatling pea" ->
                "MG";
            case "snow pea" ->
                "SP";
            case "wall-nut" ->
                "WN";
            case "tall-nut" ->
                "TN";
            case "puff-shroom" ->
                "PF";
            case "fume-shroom" ->
                "FS";
            case "cabbage-pult" ->
                "CP";
            case "melon-pult" ->
                "MP";
            case "winter melon" ->
                "WM";
            case "bonk choy" ->
                "BC";
            default -> throw new IllegalStateException(
                "Unknown Beghouled plant: " + plant.getName()
            );
        };
    }
    public Result showMap() {
        Beghouled game = activeGame();
        if (game == null) {
            return failure(
                "No active Beghouled game found.\n"
            );
        }
        GameState state = game.getGameState();
        Board board = state.getBoard();
        Map<String, String> plantLegend = new TreeMap<>();
        for (Plant plant : board.getAllPlants()) {
            plantLegend.put(getBeghouledPlantCode(plant), plant.getName());
        }
        StringBuilder output = new StringBuilder();
        output.append("===== GAME STATUS =====\n")
            .append("Stage: ").append(game.getStage().getStageNumber()).append('\n')
            .append("Matches: ").append(game.getCompletedMatches()).append('/').append(game.getTargetMatches()).append('\n')
            .append("Sun: ").append(state.getSun()).append('\n')
            .append("Wave: ").append(game.getWaveNumber()).append('\n')
            .append("Tick: ").append(state.getTickCounter()).append('\n');
        output.append("\n===== BOARD =====\n")
            .append("Each cell contains 3 chars: [plant(2)][zombie].\n\n");
        appendBoardColumnHeader(output, board);
        for (int lane = 0; lane < board.getLaneCount(); lane++) {
            output.append("  Row ").append(lane + 1).append(": ");
            for (int column = 0; column < board.getColumnCount(); column++) {
                Tile tile = board.getTile(lane, column);
                String plantCode;
                if (tile.isCrater()) {
                    plantCode = "##";
                } else {
                    plantCode = getBeghouledPlantCode(tile.getTopPlant());
                }
                char zombieCode = tile.hasZombie(state) ? 'Z' : '.';
                output.append('[').append(plantCode).append(zombieCode).append("] ");
            }
            output.append('\n');
        }
        output.append("\nCell positions 1-2 (plant): ## = crater, .. = empty");
        for (Map.Entry<String, String> entry : plantLegend.entrySet()) {
            output.append(", ").append(entry.getKey()).append(" = ").append(entry.getValue());
        }
        output.append('\n').append("Cell position 3: Z=zombie, .=none\n");
        return success(output.toString());
    }
    public Result showCurrentMenu() {
        return success("You are now in the Beghouled minigame.\n");
    }

    public Result exitMenu() {
        Beghouled game = activeGame();
        App.getInstance().setCurrentGame(null);
        App.getInstance().setCurrentMenu(Menu.TRAVELLOG_MENU);
        if (game == null) {
            return success("You returned to the Travel Log.\n");
        }
        return success(
            "You left Beghouled stage " + game.getStage().getStageNumber() + " and returned to the Travel Log.\n"
        );
    }

    private StringBuilder beginEvent(Beghouled game) {
        StringBuilder events = new StringBuilder();
        game.getGameState().setEventLogger(message -> {
            events.append(message);
            if (!message.endsWith("\n")) {
                events.append('\n');
            }
        });
        return events;
    }

    private Result resultAfterPossibleFinish(Beghouled game, String message) {
        if (!game.getGameState().isFinished()) {
            return success(message);
        }

        boolean won = game.getGameState().isWon();
        int stageNumber = game.getStage().getStageNumber();
        App.getInstance().setCurrentGame(null);
        App.getInstance().setCurrentMenu(Menu.TRAVELLOG_MENU);

        if (won) {
            String progress = progressService.recordWin(MinigameType.BEGHOULDED, stageNumber);
            return success(
                message
                    + "You completed Beghouled stage "
                    + stageNumber
                    + " and returned to the Travel Log.\n"
                    + progress
            );
        }

        return failure(
            message
                + "You lost Beghouled stage " + stageNumber + " and returned to the Travel Log.\n"
        );
    }

    private Beghouled activeGame() {
        Game game = App.getInstance().getCurrentGame();
        if (game instanceof Beghouled beghouled) {
            return beghouled;
        }
        return null;
    }

    private Result success(String message) {
        return new Result(true, message, null);
    }

    private Result failure(String message) {
        return new Result(false, message, null);
    }
}
