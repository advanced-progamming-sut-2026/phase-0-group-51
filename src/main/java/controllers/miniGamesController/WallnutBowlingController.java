package controllers.miniGamesController;

import controllers.GamingController;
import models.Board.Board;
import models.Board.Tile;
import models.games.GameState;
import models.App;
import models.Plant.WallNut;
import models.Result;
import models.Zombie.Zombie;
import models.enums.Menu;
import models.games.Game;
import models.games.TerminalMapRenderer;
import models.games.ZombieWaveManager;
import models.minigames.MinigameType;
import models.minigames.wallnutBowling.RollingWallnut;
import models.minigames.wallnutBowling.WallnutBowling;
import models.minigames.wallnutBowling.WallnutType;

import java.text.DecimalFormat;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class WallnutBowlingController extends GamingController {
    private static final DecimalFormat COORDINATE_FORMAT = new DecimalFormat("0.##");
    private final MinigameProgressService progressService =
        new MinigameProgressService();

    private String safeMessage(RuntimeException exception) {
        String message = exception.getMessage();
        return message == null || message.isBlank() ? "The operation failed." : message;
    }

    private Result success(String message) {
        return new Result(true, message, null);
    }

    private Result failure(String message) {
        return new Result(false, message, null);
    }
    public Result startStage(int stageNumber) {
        Result access = progressService.checkStageAccess(
            MinigameType.WALLNUT_BOWLING, stageNumber
        );
        if (!access.success()) {
            return access;
        }
        try {
            WallnutBowling game = new WallnutBowling(stageNumber);
            game.loadLevel();
            App.getInstance().setCurrentGame(game);
            App.getInstance().setCurrentMenu(Menu.WALLNUT_BOWLING);
            return success(
                "Wall-nut Bowling stage " + stageNumber + " started.\n"
                    + "The first walnut is already on the conveyor belt.\n"
                    + "Plant only in columns 1 through " + game.getRedLineColumn()
                    + ", before the red line.\n"
            );
        } catch (RuntimeException exception) {
            return failure("Could not start Wall-nut Bowling: "
                + safeMessage(exception) + "\n");
        }
    }

    public Result rollWallnut(int x, int y) {
        WallnutBowling game = activeGame();
        if (game == null) {
            return failure("No active Wall-nut Bowling game found.\n");
        }
        try {
            RollingWallnut wallnut = game.rollNextWallnut(x, y);
            return success(wallnut.getWallnutType().getName()
                + " was released at (" + x + ", " + y + ").\n");
        } catch (IllegalArgumentException | IllegalStateException exception) {
            return failure(safeMessage(exception) + "\n");
        }
    }

    public Result advanceTime(int tickCount) {
        WallnutBowling game = activeGame();
        if (game == null) {
            return failure("No active Wall-nut Bowling game found.\n");
        }
        StringBuilder events = new StringBuilder();
        game.getGameState().setEventLogger(message -> {
            events.append(message);
            if (!message.endsWith("\n")) events.append('\n');
        });

        try {
            game.forward(tickCount);
        } catch (IllegalArgumentException | IllegalStateException exception) {
            return failure(safeMessage(exception) + "\n");
        } finally {
            game.getGameState().setEventLogger(null);
        }
        String message = "Game advanced by " + tickCount + " ticks.\n" + events;
        return resultAfterPossibleFinish(game, message);
    }

    public Result showConveyor() {
        WallnutBowling game = activeGame();
        if (game == null) {
            return failure("No active Wall-nut Bowling game found.\n");
        }
        List<WallnutType> belt = game.getConveyorBelt();
        String contents = belt.isEmpty() ? "empty" : belt.stream()
                                                     .map(WallnutType::getName)
                                                     .collect(Collectors.joining(" -> "));
        int ticksRemaining = Math.max(0, game.getNextConveyorDeliveryTick() - game.getGameState().getTickCounter());
        return success("Conveyor: " + contents + "\n"
            + "Next delivery in " + ticksRemaining + " ticks.\n");
    }

    public Result showStatus() {
        WallnutBowling game = activeGame();
        if (game == null) {
            return failure("No active Wall-nut Bowling game found.\n");
        }
        ZombieWaveManager waveManager = game.getGameState().getZombieWaveManager();
        StringBuilder output = new StringBuilder();
        output.append("===== WALL-NUT BOWLING STATUS =====\n")
            .append("Stage: ").append(game.getStage().getStageNumber()).append('\n')
            .append("Difficulty: ").append(game.getStage().getDifficulty()).append('\n')
            .append("Current tick: ").append(game.getGameState().getTickCounter()).append('\n')
            .append("Red line: after column ").append(game.getRedLineColumn()).append('\n')
            .append("Wave: ").append(waveManager.getCurrentWaveNumber())
            .append('/').append(waveManager.getTotalWaves()).append('\n')
            .append("Living zombies: ").append(game.getLivingZombieCount()).append('\n')
            .append("Active walnuts: ").append(game.getRollingWallnuts().size()).append("\n\n");

        appendConveyor(output, game);
        appendRollingWallnuts(output, game);
        appendZombies(output, game);
        return success(output.toString());
    }

    private void appendConveyor(StringBuilder output, WallnutBowling game) {
        output.append("===== CONVEYOR =====\n");
        if (game.getConveyorBelt().isEmpty()) {
            output.append("empty\n\n");
            return;
        }
        int position = 1;
        for (WallnutType type : game.getConveyorBelt()) {
            output.append(position++)
                .append(". ")
                .append(type.getName())
                .append('\n');
        }
        output.append('\n');
    }

    private void appendRollingWallnuts(StringBuilder output, WallnutBowling game) {
        output.append("===== ROLLING WALNUTS =====\n");
        if (game.getRollingWallnuts().isEmpty()) {
            output.append("none\n\n");
            return;
        }
        for (RollingWallnut wallnut : game.getRollingWallnuts()) {
            output.append(wallnut.getWallnutType().getName())
                .append(" at (")
                .append(COORDINATE_FORMAT.format(wallnut.getX() + 1))
                .append(", ")
                .append(COORDINATE_FORMAT.format(wallnut.getY() + 1))
                .append("), direction: (")
                .append(COORDINATE_FORMAT.format(wallnut.getDirectionX()))
                .append(", ")
                .append(COORDINATE_FORMAT.format(wallnut.getDirectionY()))
                .append("), zombie hits: ")
                .append(wallnut.getZombieHitCount())
                .append('\n');
        }
        output.append('\n');
    }

    private void appendZombies(StringBuilder output, WallnutBowling game) {
        output.append("===== ZOMBIES =====\n");
        List<Zombie> zombies = game.getGameState().getZombiesInTheGame().stream()
            .filter(zombie -> !zombie.isDead())
            .sorted(Comparator.comparingInt(Zombie::getLane)
                .thenComparingDouble(Zombie::getX))
            .toList();
        if (zombies.isEmpty()) {
            output.append("none\n");
            return;
        }
        for (Zombie zombie : zombies) {
            output.append(zombie.getAlias())
                .append(" at (")
                .append(COORDINATE_FORMAT.format(zombie.getX() + 1))
                .append(", ")
                .append(zombie.getLane() + 1)
                .append("), health: ")
                .append(zombie.getHitpoints())
                .append('/')
                .append(zombie.getMaxHitpoints())
                .append('\n');
        }
    }

    private Result resultAfterPossibleFinish(WallnutBowling game, String message) {
        if (!game.getGameState().isFinished()) {
            return success(message);
        }

        boolean won = game.getGameState().isWon();
        int stageNumber = game.getStage().getStageNumber();
        App.getInstance().setCurrentGame(null);
        App.getInstance().setCurrentMenu(Menu.TRAVELLOG_MENU);

        String ending;
        if (won) {
            String progressMessage = progressService.recordWin(
                MinigameType.WALLNUT_BOWLING, stageNumber
            );
            ending = "You completed Wall-nut Bowling stage " + stageNumber
                + " and returned to the Travel Log.\n"
                + progressMessage;
        } else {
            ending = "You lost Wall-nut Bowling stage " + stageNumber
                + " and returned to the Travel Log.\n";
        }
        return success(message + ending);
    }

    public Result showMap() {
        WallnutBowling game = activeGame();
        if (game == null) {
            return failure("No active Wall-nut Bowling game found.\n");
        }
        GameState state = game.getGameState();
        ZombieWaveManager waveManager = state.getZombieWaveManager();

        String map = TerminalMapRenderer.render(
            "WALL-NUT BOWLING MAP",
            List.of(
                "Stage: " + game.getStage().getStageNumber(),
                "Difficulty: " + game.getStage().getDifficulty(),
                "Tick: " + state.getTickCounter(),
                "Wave: " + waveManager.getCurrentWaveNumber()
                    + "/" + waveManager.getTotalWaves(),
                "Zombies: " + game.getLivingZombieCount(),
                "Red Line: after column " + game.getRedLineColumn()
            ),
            "",
            state.getBoard(),
            tile -> buildWallnutMapCell(game, state, tile),
            lane -> "Row " + (lane + 1),
            lane -> wallnutMowerStatus(state, lane),
            game.getRedLineColumn()
        );

        return success(
            map
                + "\nCell: [base][zombie][sun][loot]\n"
                + "Base: O=rolling wallnut, P=plant, .=empty\n"
                + "Objects: Z=zombie, S=grounded sun, s=falling sun\n"
                + "Loot: C=coin, D=gem, O=pot, .=none\n"
                + "|| = red line\n"
        );
                }

    private String buildWallnutMapCell(
        WallnutBowling game,
        GameState state,
        Tile tile
    ) {
                String cell = buildFourCharacterCell(state, tile);
        if (hasRollingWallnutAt(
            game,
            tile.getLane(),
            tile.getColumn()
        )) {
            return 'O' + cell.substring(1);
                }
        return cell;
            }

    private String wallnutMowerStatus(GameState state, int lane) {
        return "Mower: "
            + (state.getLawnMowers()[lane].isDestroyed()
                ? "USED"
                : "READY");
    }

    private boolean hasRollingWallnutAt(WallnutBowling game, int lane, int column) {
        for (RollingWallnut wallnut : game.getRollingWallnuts()) {
            if (wallnut.isRemoved()) {
                continue;
            }
            if ((int) Math.floor(wallnut.getY()) == lane
                && (int) Math.floor(wallnut.getX()) == column) {
                return true;
            }
        }
        return false;
    }

    private WallnutBowling activeGame() {
        Game game = App.getInstance().getCurrentGame();
        return game instanceof WallnutBowling wallnutBowling ? wallnutBowling : null;
    }

    public Result showCurrentMenu() {
        return success("You are now in the Wall-nut Bowling minigame.\n");
    }

    public Result exitMenu() {
        WallnutBowling game = activeGame();
        App.getInstance().setCurrentGame(null);
        App.getInstance().setCurrentMenu(Menu.TRAVELLOG_MENU);
        if (game == null) {
            return success("You returned to the Travel Log.\n");
        }
        return success("You left Wall-nut Bowling stage "
            + game.getStage().getStageNumber()
            + " and returned to the Travel Log.\n");
    }

}
