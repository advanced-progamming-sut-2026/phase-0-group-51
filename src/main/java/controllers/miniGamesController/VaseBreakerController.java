package controllers.miniGamesController;

import controllers.GamingController;
import models.App;
import models.minigames.vaseBreaker.Brain;
import models.minigames.vaseBreaker.Vase;
import models.Result;
import models.enums.Menu;
import models.games.Game;
import models.minigames.vaseBreaker.DroppedSeedPacket;
import models.minigames.vaseBreaker.VaseBreaker;
import models.Plant.Plant;
import models.Zombie.Zombie;

import java.text.DecimalFormat;
import java.util.Comparator;
import java.util.Map;

public class VaseBreakerController extends GamingController {
    private static final DecimalFormat COORDINATE_FORMAT = new DecimalFormat("0.##");
    public Result startStage(int stageNumber) {
        if (stageNumber < 1 || stageNumber > 3) {
            return failure("Vasebreaker level must be 1, 2, or 3.\n");
        }
        try {
            VaseBreaker game = new VaseBreaker(stageNumber);
            game.loadLevel();
            App.getInstance().setCurrentGame(game);
            App.getInstance().setCurrentMenu(Menu.VASE_BREAKER);
            return success(
                    "Vasebreaker level " + stageNumber + " started.\n" + "Break all vases and defeat every zombie.\n"
                            + "Do not let a zombie eat a brain.\n"
            );
        } catch (RuntimeException exception) {
            return failure(
                    "Could not start Vasebreaker: \n"
            );
        }
    }

    public Result breakVase(int x, int y) {
        VaseBreaker game = activeGame();

        if (game == null) {
            return failure("No active Vasebreaker game found.\n");}
        try {
            VaseBreaker.BreakOutcome outcome =
                    game.breakVase(x, y);

            String message = switch (outcome.contentType()) {
                case EMPTY ->
                        "The vase at (" + x + ", " + y + ") was empty.\n";

                case SEED_PACKET ->
                        "The vase at (" + x + ", " + y + ") dropped a " + outcome.contentName() + " seed packet.\n"
                                + "It will disappear in " + game.packetLifetimeTicks() + " ticks.\n";

                case ZOMBIE ->
                        "The vase at (" + x + ", " + y + ") released " + outcome.contentName() + ".\n";

                case GARGANTUAR ->
                        "The Gargantuar vase at (" + x + ", " + y + ") released " + outcome.contentName() + "!\n";
            };

            return resultAfterPossibleFinish(game, message);
        } catch (IllegalArgumentException | IllegalStateException exception) {
            return failure("failure in breaking vase\n");
        }
    }

    public Result pickUpSeedPacket(int x, int y) {
        VaseBreaker game = activeGame();
        if (game == null) {
            return failure("No active Vasebreaker game found.\n");
        }
        try {
            String plantName = game.pickUpSeedPacket(x, y);
            int currentAmount = game.getPacketInventory().getOrDefault(plantName, 0);
            return success(
                    "Picked up the "
                            + plantName
                            + " seed packet at ("
                            + x + ", " + y + ").\n"
                            + "You now have "
                            + currentAmount
                            + " "
                            + plantName
                            + " packet(s).\n"
            );
        } catch (IllegalArgumentException | IllegalStateException exception) {
            return failure("failure in pick " + "\n");
        }
    }

    public Result plantPacket(
            String plantType,
            int x,
            int y
    ) {
        VaseBreaker game = activeGame();

        if (game == null) {
            return failure("No active Vasebreaker game found.\n");
        }

        if (plantType == null || plantType.isBlank()) {
            return failure("Plant type cannot be empty.\n");
        }

        try {
            Plant plant = game.plantFromPacket(plantType.trim(), x, y);
            return success(plant.getName() + " was planted from a seed packet at (" + x + ", " + y + ").\n");
        } catch (IllegalArgumentException | IllegalStateException exception) {
            return failure("failure in planting packet\n");
        }
    }

    public Result advanceTime(int tickCount) {
        VaseBreaker game = activeGame();
        if (game == null) {
            return failure("No active Vasebreaker game found.\n");
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

        return resultAfterPossibleFinish(
                game,
                output.toString()
        );
    }

    public Result showStatus() {
        VaseBreaker game = activeGame();

        if (game == null) {
            return failure("No active Vasebreaker game found.\n");
        }
        int currentTick = game.getGameState().getTickCounter();
        StringBuilder output = new StringBuilder();
        output.append("===== VASEBREAKER STATUS =====\n");
        output.append("Stage: ")
                .append(game.getStage().getStageNumber())
                .append('\n');

        output.append("Difficulty: ")
                .append(game.getStage().getDifficulty())
                .append('\n');

        output.append("Current tick: ")
                .append(currentTick)
                .append('\n');

        output.append("Remaining vases: ")
                .append(game.getRemainingVaseCount())
                .append('\n');

        output.append("Living zombies: ")
                .append(game.getLivingZombieCount())
                .append("\n\n");

        appendBrainStatus(output, game);
        appendVaseStatus(output, game);
        appendDroppedPackets(output, game, currentTick);
        appendPacketInventory(output, game);
        appendZombieStatus(output, game);
        return success(output.toString());
    }

    private void appendBrainStatus(
            StringBuilder output,
            VaseBreaker game
    ) {
        output.append("===== BRAINS =====\n");

        for (Brain brain : game.getBrains()) {
            output.append("Row ")
                    .append(brain.getRow())
                    .append(": ")
                    .append(
                            brain.isEaten()
                                    ? "EATEN"
                                    : "SAFE"
                    )
                    .append('\n');
        }

        output.append('\n');
    }

    private void appendVaseStatus(
            StringBuilder output,
            VaseBreaker game
    ) {
        output.append("===== UNBROKEN VASES =====\n");

        boolean found = false;

        for (Vase vase : game.getVases()
                .stream()
                .filter(candidate -> !candidate.isBroken())
                .sorted(
                        Comparator.comparingInt(Vase::getY)
                                .thenComparingInt(Vase::getX)
                )
                .toList()) {

            found = true;

            output.append("(")
                    .append(vase.getX())
                    .append(", ")
                    .append(vase.getY())
                    .append("): ")
                    .append(visibleVaseName(vase))
                    .append('\n');
        }

        if (!found) {
            output.append("none\n");
        }

        output.append('\n');
    }

    private void appendDroppedPackets(
            StringBuilder output,
            VaseBreaker game,
            int currentTick
    ) {
        output.append("===== DROPPED SEED PACKETS =====\n");

        if (game.getDroppedSeedPackets().isEmpty()) {
            output.append("none\n\n");
            return;
        }

        for (DroppedSeedPacket packet
                : game.getDroppedSeedPackets()) {

            output.append(packet.getPlantName())
                    .append(" at (")
                    .append(packet.getX())
                    .append(", ")
                    .append(packet.getY())
                    .append("), expires in ")
                    .append(
                            packet.ticksRemaining(currentTick)
                    )
                    .append(" ticks\n");
        }

        output.append('\n');
    }

    private void appendPacketInventory(
            StringBuilder output,
            VaseBreaker game
    ) {
        output.append("===== PACKET INVENTORY =====\n");

        if (game.getPacketInventory().isEmpty()) {
            output.append("empty\n\n");
            return;
        }

        for (Map.Entry<String, Integer> entry
                : game.getPacketInventory().entrySet()) {

            output.append(entry.getKey())
                    .append(": ")
                    .append(entry.getValue())
                    .append('\n');
        }

        output.append('\n');
    }

    private void appendZombieStatus(
            StringBuilder output,
            VaseBreaker game
    ) {
        output.append("===== ZOMBIES =====\n");

        boolean found = false;

        for (Zombie zombie
                : game.getGameState()
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
                    .append(
                            COORDINATE_FORMAT.format(
                                    zombie.getX() + 1
                            )
                    )
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

    private String visibleVaseName(Vase vase) {
        return switch (vase.getVaseType()) {
            case SIMPLE -> "simple vase (?)";
            case PLANT -> "plant vase";
            case GARGANTUAR -> "Gargantuar vase";
        };
    }

    private Result resultAfterPossibleFinish(VaseBreaker game, String message) {
        if (!game.getGameState().isFinished()) {
            return success(message);
        }
        boolean won = game.getGameState().isWon();
        int stageNumber = game.getStage().getStageNumber();
         // TODO:Save stage completion here when progress persistence is implemented.
        App.getInstance().setCurrentGame(null);
        App.getInstance().setCurrentMenu(
                Menu.TRAVELLOG_MENU
        );

        String ending;
        if (won) {
            ending =
                    "You completed Vasebreaker stage " + stageNumber + " and returned to the Travel Log.\n";
        } else {
            ending = "You lost Vasebreaker stage " + stageNumber + " and returned to the Travel Log.\n";
        }
        return success(message + ending);
    }

    private VaseBreaker activeGame() {
        Game game = App.getInstance().getCurrentGame();
        if (game instanceof VaseBreaker vaseBreaker) {
            return vaseBreaker;
        }
        return null;
    }

    private Result success(String message) {
        return new Result(true, message, null);
    }

    private Result failure(String message) {
        return new Result(false, message, null);
    }

    public Result showCurrentMenu(){
        return new Result(true
                ,"You are now in the vase breaker minigame.\n",null);
    }
    public Result exitMenu(){
        VaseBreaker game = activeGame();
        if (game == null) {
            App.getInstance().setCurrentMenu(Menu.TRAVELLOG_MENU);
            return success("You returned to the Travel Log.\n");
        }
        App.getInstance().setCurrentGame(null);
        App.getInstance().setCurrentMenu(Menu.TRAVELLOG_MENU);
        return success(
                "You left the Vasebreaker level" + game.getStage().getStageNumber() +
                        " and returned to the Travel Log.\n"
        );
    }

}
