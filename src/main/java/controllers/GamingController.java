package controllers;

import data.loader.PlantData;
import data.loader.PlantRegistry;
import models.App;
import models.Board.Board;
import models.Board.Tile;
import models.plant.Plant;
import models.Result;
import models.zombie.Behavior.ArmorBehavior;
import models.zombie.Zombie;
import models.games.Game;
import models.games.GameState;
import models.items.Mower;
import models.sun.Sun;

import java.text.DecimalFormat;
import java.util.*;

public class GamingController {
    private final Map<Integer, Integer> cooldownUntilTick = new HashMap<>();
    private static final DecimalFormat COORDINATE_FORMAT = new DecimalFormat("0.##");
    private String formatCoordinate(double coordinate) {
        return COORDINATE_FORMAT.format(coordinate);
    }
    private String formatSeconds(int ticks, int ticksPerSecond) {
        double seconds = (double) ticks / ticksPerSecond;
        return COORDINATE_FORMAT.format(seconds);
    }
    private Result success(String message) {
        return new Result(true, message, null);
    }

    private Result failure(String message) {
        return new Result(false, message, null);
    }
    public Result advanceTime(int tick){
        StringBuilder sb = new StringBuilder();
        Game game = App.getInstance().getCurrentGame();
        if (game == null || game.getGameState() == null) {
            return new Result(false, "No active game found.\n", null);
        }
        sb.append("Game Advanced By ").append(tick).append(" ticks.\n");
        game.getGameState().setEventLogger(message -> sb.append(message));
            game.forward(tick);
        if (game.getGameState().isFinished()) {
            models.App.getInstance().setCurrentMenu(models.enums.Menu.GAME_MENU);
            models.App.getInstance().setCurrentGame(null);
            if (game.getGameState().isWon()) {
                sb.append("Game ended! You returned to the Main Menu.\n");
            } else {
                sb.append("Game Over! You returned to the Main Menu.\n");
            }
        }
            return new Result(true, sb.toString(), null);
    }
    public Result plantPlant(String plantType,int x, int y){
        Game game = App.getInstance().getCurrentGame();
        if (game == null) return failure("No active game found.\n");
        GameState state = game.getGameState();
        if (state == null) return failure("No active level found.\n");
        Tile tile = state.getBoard().getTileAtUserCoordinates(x - 1, y - 1);
        if (tile == null) return failure("Coordinates are outside the map.\n");
        PlantData selected = PlantRegistry.getByName(plantType);
        if (selected == null) return failure("Unknown plant.\n");
        if (!game.getSelectedPlantsForThisGame().contains(selected)) {
            return failure("This plant is not selected for this level.\n");
        }
        if (!tile.isOccupiable()) {
            return tileOccupationFailure(tile);
        }
        int availableAt = state.getPlantCooldownEnd(selected.id());
        if (state.getTickCounter() < availableAt) {
            int ticksLeft = availableAt - state.getTickCounter();
            return failure("Plant is recharging for " + ticksLeft + " more ticks.\n");
        }
        if (state.getSun() < selected.cost()) {
            return failure("Not enough sun." + selected.name()
                    + " costs "
                    + selected.cost()
                    + " suns.\n");
        }
//        state.plantPlant(, tile);
        state.startPlantCooldown(selected);
        state.setSun(state.getSun()-selected.cost());
        return success(
                selected.name()
                        + " planted at ("
                        + x
                        + ", "
                        + y
                        + ").\n"
        );
    }
    private Result tileOccupationFailure(Tile tile) {
        if (tile.hasPlant())
            return failure("This tile already contains a plant.\n");

        if (tile.hasGrave())
            return failure("A grave blocks this tile.\n");

        if (tile.isIceBlocked())
            return failure("This tile is blocked by ice.\n");

        return failure("This tile cannot be occupied.\n");
    }
    public Result pluckPlants(int x, int y){
        Game game = App.getInstance().getCurrentGame();
        if (game == null) return failure("No active game found.\n");
        Tile tile = game.getGameState().getBoard().getTileAtUserCoordinates(x,y);
        if (tile == null) return failure("Coordinates are outside the map.\n");
        if (!tile.hasPlant()) return failure("There is no plant at (" + x + ", " + y + ").\n");

        String name = tile.getPlant().getName();
        tile.removePlant();
        return success(name + " was plucked from (" + x + ", " + y + ").\n");
    }
    public Result feedPlants(){}
    public Result zombiesInfo(){
        Game game = App.getInstance().getCurrentGame();
        if (game == null) return failure("No active game found.\n");
        List<Zombie> zombies = new ArrayList<>(game.getGameState().getZombiesInTheGame());
        zombies.removeIf(Zombie::isDead);
        zombies.sort(Comparator.comparingInt(Zombie::getLane).thenComparingDouble(Zombie::getX));
        if (zombies.isEmpty()) return success("There are no active zombies on the map.\n");
        StringBuilder output = new StringBuilder();
        for (Zombie zombie : zombies) {
            output.append(zombie.getAlias()).append(":\n")
                    .append("position: ")
                    .append(formatCoordinate(zombie.getX() + 1)).append(", ")
                    .append(zombie.getLane() + 1).append('\n')
                    .append("health: ").append(zombie.getHitpoints())
                    .append('/').append(zombie.getMaxHitpoints()).append('\n')
                    .append("armor:\n");

            boolean hasArmor = false;
            for (ArmorBehavior armor : zombie.getBehaviors().stream()
                    .filter(ArmorBehavior.class::isInstance).map(ArmorBehavior.class::cast).toList()) {
                if (!armor.isGone()) {
                    hasArmor = true;
                    output.append("  ").append(armor.getDefinition().getAlias())
                            .append(": ").append(armor.getCurrentHP()).append('\n');
                }
            }
            if (!hasArmor) output.append("  none\n");
            output.append("effects:\n");
            if (zombie.getEffects().isEmpty()) {
                output.append("  none\n");
            } else {
                zombie.getEffects().forEach((effect, ticks) -> output
                        .append("  ").append(effect).append(": ")
                        .append(formatSeconds(ticks, game.getGameState().getTicksPerSecond()))
                        .append("s\n"));
            }
            output.append('\n');
        }
        return success(output.toString());
    }
     public Result showPlantsStatus(){
         Game game = App.getInstance().getCurrentGame();
         if (game == null) {
             return failure("No active game found.\n");
         }
         GameState state = game.getGameState();
         StringBuilder output = new StringBuilder();
         output.append("===== PLANTS STATUS =====\n\n");
         for (PlantData plant : game.getSelectedPlantsForThisGame()) {
             int availableAt = cooldownUntilTick.getOrDefault(plant.id(), 0);
             int ticksLeft = Math.max(0, availableAt - state.getTickCounter());

             boolean enoughSun = state.getSun() >= plant.cost();
             output.append(plant.name())
                     .append(":\n");
             output.append("  sun cost: ")
                     .append(plant.cost())
                     .append('\n');
             output.append("  available: ")
                     .append(
                             ticksLeft == 0 && enoughSun
                                     ? "yes"
                                     : "no"
                     )
                     .append('\n');
             if (ticksLeft > 0) {
                 output.append("  cooldown remaining: ")
                         .append(ticksLeft)
                         .append(" ticks\n");
             }
             output.append('\n');
         }
         return success(output.toString());
     }

    public Result showMap(){
        Game game = App.getInstance().getCurrentGame();
        if (game == null) {
            return failure("No active game found.\n");
        }
        GameState state = game.getGameState();
        if (state == null) {
            return failure("No active level found.\n");
        }
        StringBuilder output = new StringBuilder();
        output.append(gameStatus(state));
        output.append(legend());
        output.append(lawnMowerStatus(state));
        output.append(boardDisplay(state));
        output.append(zombiePositions(state));
        return success(output.toString());
        }
    private String gameStatus(GameState state) {
        StringBuilder output = new StringBuilder();
        int wave = state.getZombieWaveManager() == null ? 0 : state.getZombieWaveManager().getCurrentWaveNumber();
        output.append("===== GAME STATUS =====\n");
        output.append("Wave: ")
                .append(wave)
                .append('\n');
        output.append("Sun: ")
                .append(state.getSun())
                .append('\n');
        // TODO: ADD PLANT FOOD LATER
        output.append("Tick: ")
                .append(state.getTickCounter())
                .append("\n\n");
        return output.toString();
    }
    private String legend() {
        return """
            ===== LEGEND =====
            [P] Plant
            [Z] Zombie
            [B] Plant + Zombie collision
            [G] Grave
            [I] Ice blocked tile
            [.] Empty tile
            """;
    }
    private String lawnMowerStatus(GameState state) {
        StringBuilder output = new StringBuilder();
        output.append("===== LAWN MOWERS =====\n");
        for (int lane = 0; lane < state.getBoard().getLaneCount(); lane++) {
            Mower mower = state.getLawnMowers()[lane];
            output.append("Row ")
                    .append(lane + 1)
                    .append(": ");
            if (mower.isDestroyed()) {
                output.append("DESTROYED");
            }
            else if (mower.isActivated()) {
                output.append("USED");
            }
            else {
                output.append("AVAILABLE");
            }
            output.append('\n');
        }
        return output.append('\n').toString();
    }
    private String boardDisplay(GameState state) {
        Board board = state.getBoard();
        StringBuilder output = new StringBuilder();
        output.append("===== BOARD =====\n");
        output.append("      ");
        for (int column = 0;
             column < board.getColumnCount();
             column++) {
            output.append(String.format(
                    "%4d",
                    column + 1
            ));
        }
        output.append('\n');
        for (int lane = 0; lane < board.getLaneCount(); lane++) {
            output.append(
                    String.format("R%-3d ", lane + 1));
            for (int column = 0; column < board.getColumnCount(); column++) {
                Tile tile = board.getTile(lane, column);
                output.append("[").append(tileSymbol(tile, state)).append("] ");
            }
            output.append('\n');
        }
        return output.toString();
    }
    private char tileSymbol(Tile tile, GameState state) {
        boolean zombieHere = tile.hasZombie(state);
        if (tile.hasPlant() && zombieHere) {
            return 'B';
        }
        if (tile.hasPlant()) {
            return 'P';
        }
        if (zombieHere) {
            return 'Z';
        }
        if (tile.hasGrave()) {
            return 'G';
        }
        if (tile.isIceBlocked()) {
            return 'I';
        }
        return '.';
    }
    private String zombiePositions(GameState state) {
        StringBuilder output = new StringBuilder();
        output.append("\n===== ZOMBIE POSITIONS =====\n");
        for (Zombie zombie : state.getZombiesInTheGame()) {
            if (zombie.isDead()) {
                continue;
            }
            output.append(zombie.getAlias())
                    .append(" -> ")
                    .append("x=")
                    .append(String.format(
                            Locale.US,
                            "%.2f",
                            zombie.getX()
                    ))
                    .append(", y=")
                    .append(String.format(
                            Locale.US,
                            "%.2f",
                            zombie.getLane()*Tile.TILEHEIGHT
                    ))
                    .append('\n');
        }
        return output.toString();
    }
    public Result showTileStatus(int x,int y){
        Game game = App.getInstance().getCurrentGame();
        if (game == null) return failure("No active game found.\n");
        GameState state = game.getGameState();
        Tile tile = state.getBoard().getTileAtUserCoordinates( x, y);
        if (tile == null) return failure("Coordinates are outside the map.\n");

        StringBuilder output = new StringBuilder()
                .append("Tile (").append(x).append(", ").append(y).append("):\n")
                .append("terrain: ");
        if (tile.hasGrave()) output.append("grave");
        else if (tile.isIceBlocked()) output.append("ice-blocked");
        else if (tile.isFrosted()) output.append("frosted");
        else output.append("normal");
        output.append('\n');

        if (tile.hasPlant()) {
            Plant plant = tile.getPlant();
            output.append("plant: ").append(plant.getName()).append('\n')
                    .append("plant health: ").append(plant.getCurrentHP())
                    .append('/').append(plant.getPlantStat().maxHp()).append('\n')
                    .append("plant level: ").append(plant.getLevel()).append('\n')
                    .append("plant food active: ").append(plant.isOnPlantFood() ? "yes" : "no").append('\n');
        } else {
            output.append("plant: none\n");
        }

        List<Zombie> zombies = tile.getZombies(state);
        if (zombies.isEmpty() || !tile.hasZombie(state)) {
            output.append("zombies: none\n");
        } else {
            output.append("zombies:\n");
            for (Zombie zombie : zombies) {
                output.append("  ").append(zombie.getAlias())
                        .append(" - health ").append(zombie.getHitpoints())
                        .append('/').append(zombie.getMaxHitpoints()).append('\n');
            }
        }
        return success(output.toString());

    }
    public Result zombiesDrop(){}
    public Result showSunAmount(GameState gameState){
        return new Result(true,"You have "+gameState.getSun()+" suns.\n",null );
    }
    public Result cheatAddSun(GameState gameState,int n){
        gameState.increaseSunBalance(n);
        return new Result(true,gameState.getSun()+" suns added.\n",null );
    }
    public Result collectSun(GameState gameState,int x,int y){
        Sun targetSun = null;
        for (Sun sun : gameState.getBoard().getActiveSuns()) {
            float sunGroundY = sun.getLane() * Tile.TILEHEIGHT;
            if (Math.abs(sun.getX() - x) < 0.1 && Math.abs(sun.getY() - y) < 0.1) {
                targetSun = sun;
                break;
            }
        }

        if (targetSun != null) {
            boolean success = gameState.getBoard().collectSun(targetSun,gameState);
            if(!success){
                return new Result(false,"Sun has expired or collected before.\n",null);
            }
        } else {
            return new Result(false,"No sun found at given coordinates.\n",null);
        }
        return new Result(true,"Sun collected successfully.\n",null);
    }
}
