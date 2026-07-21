package controllers.miniGamesController;

import controllers.GamingController;
import models.App;
import models.Board.Board;
import models.Board.Tile;
import models.Result;
import models.Zombie.Zombie;
import models.Zombie.ZombieType;
import models.enums.Menu;
import models.games.Game;
import models.games.GameState;
import models.games.TerminalMapRenderer;
import models.minigames.MinigameType;
import models.minigames.iZombie.IZombie;
import models.minigames.vaseBreaker.Brain;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class IZombieController extends GamingController {
    private final MinigameProgressService progressService =
        new MinigameProgressService();

    public Result startStage(int stageNumber) {
        Result access = progressService.checkStageAccess(MinigameType.IZOMBIE, stageNumber);
        if (!access.success()) {
            return access;
        }
        try {
            IZombie game = new IZombie(stageNumber);
            game.loadLevel();
            App.getInstance().setCurrentGame(game);
            App.getInstance().setCurrentMenu(Menu.IZOMBIE);
            return success(
                "I, Zombie level " + stageNumber + " started.\n"
                    + "Place zombies to the right of the red line (column "
                    + (IZombie.RED_LINE_COLUMN + 1) + " and beyond).\n"
                    + "Eat all " + game.getBrains().size() + " brains to win.\n"
                    + "You start with " + game.getGameState().getSun() + " sun.\n"
            );
        } catch (RuntimeException exception) {
            return failure("Could not start I, Zombie: \n");
        }
    }

    public Result placeZombie(String zombieName, int x, int y) {
        IZombie game = activeGame();
        if (game == null) {
            return failure("No active I, Zombie game found.\n");
        }
        if (zombieName == null || zombieName.isBlank()) {
            return failure("Zombie name cannot be empty.\n");
        }
        try {
            Zombie zombie = game.placeZombie(zombieName.trim(), x, y);
            return resultAfterPossibleFinish(
                game,
                zombie.getAlias() + " was placed at (" + x + ", " + y + ").\n"
                    + "Remaining sun: " + game.getGameState().getSun() + ".\n"
            );
        } catch (IllegalArgumentException | IllegalStateException exception) {
            return failure(exception.getMessage() + "\n");
        }
    }

    public Result advanceTime(int tickCount) {
        IZombie game = activeGame();
        if (game == null) {
            return failure("No active I, Zombie game found.\n");
        }
        if (tickCount <= 0) {
            return failure("Tick count must be positive.\n");
        }
        StringBuilder events = new StringBuilder();
        game.getGameState().setEventLogger(message -> {
            events.append(message);
            if (!message.endsWith("\n")) {
                events.append('\n');
            }
        });
        try {
            game.forward(tickCount);
        } catch (IllegalArgumentException | IllegalStateException exception) {
            return failure("failure\n");
        }
        StringBuilder output = new StringBuilder();
        output.append("Game advanced by ")
            .append(tickCount)
            .append(" ticks.\n");
        output.append(events);
        return resultAfterPossibleFinish(game, output.toString());
    }

    public Result showStatus() {
        IZombie game = activeGame();
        if (game == null) {
            return failure("No active I, Zombie game found.\n");
        }
        int currentTick = game.getGameState().getTickCounter();
        StringBuilder output = new StringBuilder();
        output.append("===== I, ZOMBIE STATUS =====\n");
        output.append("Stage: ")
            .append(game.getStage().getStageNumber())
            .append('\n');
        output.append("Difficulty: ")
            .append(game.getStage().getDifficulty())
            .append('\n');
        output.append("Current tick: ")
            .append(currentTick)
            .append('\n');
        output.append("Sun: ")
            .append(game.getGameState().getSun())
            .append('\n');
        output.append("Remaining brains: ")
            .append(game.getRemainingBrainCount())
            .append('\n');
        output.append("Living sun producers: ")
            .append(game.getLivingSunProducerCount())
            .append('\n');
        output.append("Living zombies: ")
            .append(game.getLivingZombieCount())
            .append("\n\n");
        appendBrainStatus(output, game);
        appendRoster(output, game);
        appendZombieStatus(output, game);
        return success(output.toString());
    }

    public Result showRoster() {
        IZombie game = activeGame();
        if (game == null) {
            return failure("No active I, Zombie game found.\n");
        }
        StringBuilder output = new StringBuilder();
        appendRoster(output, game);
        output.append("Current sun: ")
            .append(game.getGameState().getSun())
            .append('\n');
        return success(output.toString());
    }

    public Result showMap() {
        IZombie game = activeGame();
        if (game == null) {
            return failure("No active I, Zombie game found.\n");
        }
        GameState state = game.getGameState();

        String map = TerminalMapRenderer.render(
            "I, ZOMBIE MAP",
            List.of(
                "Stage: " + game.getStage().getStageNumber(),
                "Sun: " + state.getSun(),
                "Tick: " + state.getTickCounter(),
                "Brains: " + game.getRemainingBrainCount(),
                "Zombies: " + game.getLivingZombieCount(),
                "Red Line: before column "
                    + (IZombie.RED_LINE_COLUMN + 1)
            ),
            "",
            state.getBoard(),
            tile -> buildIZombieCell(state, tile),
            lane -> "Row " + (lane + 1),
            lane -> iZombieBrainStatus(game, lane),
            IZombie.RED_LINE_COLUMN
        );

        return success(
            map
                + "\nCell: [base][zombie][sun][loot]\n"
                + "Base: P=plant, F=frozen plant, .=empty\n"
                + "Zombie: Z=normal, S=sun producer, .=none\n"
                + "Sun: S=grounded, s=falling, .=none\n"
                + "Loot: C=coin, D=gem, O=pot, .=none\n"
                + "|| = red line\n"
        );
            }

    private String iZombieBrainStatus(IZombie game, int lane) {
        Brain brain = game.getBrains().get(lane);
        return "Brain: " + (brain.isEaten() ? "EATEN" : "SAFE");
    }

    private String buildIZombieCell(GameState state, Tile tile) {
        char base = getBaseMapSymbol(state, tile);
        char zombieChar = '.';
        for (Zombie zombie : getZombiesAtTile(state, tile.getLane(), tile.getColumn())) {
            if (IZombie.SUN_PRODUCER_ALIAS.equals(zombie.getAlias())) {
                if (zombieChar == '.') {
                    zombieChar = 'S';
                }
            } else {
                zombieChar = 'Z';
            }
        }
        char sun = getSunMapSymbol(
            state.getBoard(), tile.getLane(), tile.getColumn()
        );
        char loot = getLootMapSymbol(
            state.getBoard(), tile.getLane(), tile.getColumn()
        );
        return new String(new char[]{base, zombieChar, sun, loot});
    }

    private long tileKey(int lane, int column) {
        return ((long) lane << 32) | (column & 0xffffffffL);
    }

    private void appendBrainStatus(StringBuilder output, IZombie game) {
        output.append("===== BRAINS =====\n");
        for (Brain brain : game.getBrains()) {
            output.append("Row ")
                .append(brain.getRow())
                .append(": ")
                .append(brain.isEaten() ? "EATEN" : "SAFE")
                .append('\n');
        }
        output.append('\n');
    }

    private void appendRoster(StringBuilder output, IZombie game) {
        output.append("===== ZOMBIE ROSTER =====\n");
        for (Map.Entry<String, Integer> entry : game.getRoster().entrySet()) {
            output.append(entry.getKey())
                .append(": ")
                .append(entry.getValue())
                .append(" sun\n");
        }
        output.append('\n');
    }

    private void appendZombieStatus(StringBuilder output, IZombie game) {
        output.append("===== ZOMBIES =====\n");
        boolean found = false;
        for (Zombie zombie : game.getGameState()
            .getZombiesInTheGame()
            .stream()
            .filter(candidate -> !candidate.isDead())
            .sorted(
                Comparator.comparingInt(Zombie::getLane)
                    .thenComparingDouble(Zombie::getX)
            )
            .toList()) {
            found = true;
            output.append(zombie.getAlias())
                .append(" at (")
                .append(zombie.getColumn() + 1)
                .append(", ")
                .append(zombie.getLane() + 1)
                .append("), health: ")
                .append(zombie.getHitpoints())
                .append('/')
                .append(zombie.getMaxHitpoints())
                .append('\n');
        }
        if (!found) {
            output.append("none\n");
        }
    }


    private Result resultAfterPossibleFinish(IZombie game, String message) {
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
                MinigameType.IZOMBIE, stageNumber
            );
            ending = "You completed I, Zombie stage " + stageNumber
                + " and returned to the Travel Log.\n"
                + progressMessage;
        } else {
            ending = "You lost I, Zombie stage " + stageNumber + " and returned to the Travel Log.\n";
        }
        return success(message + ending);
    }

    private IZombie activeGame() {
        Game game = App.getInstance().getCurrentGame();
        if (game instanceof IZombie iZombie) {
            return iZombie;
        }
        return null;
    }

    private Result success(String message) {
        return new Result(true, message, null);
    }

    private Result failure(String message) {
        return new Result(false, message, null);
    }

    public Result showCurrentMenu() {
        return new Result(true,
            "You are now in the I, Zombie minigame.\n", null);
    }

    public Result exitMenu() {
        IZombie game = activeGame();
        if (game == null) {
            App.getInstance().setCurrentMenu(Menu.TRAVELLOG_MENU);
            return success("You returned to the Travel Log.\n");
        }
        App.getInstance().setCurrentGame(null);
        App.getInstance().setCurrentMenu(Menu.TRAVELLOG_MENU);
        return success(
            "You left the I, Zombie level " + game.getStage().getStageNumber()
                + " and returned to the Travel Log.\n"
        );
    }
}
