package controllers;

import Data.database.PlantBoostRepository;
import Data.database.PlantRepository;
import Data.loader.PlantData;
import Data.loader.PlantRegistry;
import Data.loader.ZombieRegistry;
import models.App;
import models.Board.Board;
import models.Board.Tile;
import models.Plant.Plant;
import models.Plant.PlantFactory;
import models.Result;
import models.User;
import models.Zombie.Behavior.ArmorBehavior;
import models.Zombie.Zombie;
import models.Zombie.ZombieType;
import models.enums.Menu;
import models.games.Game;
import models.games.GameState;
import models.games.ScoringGame;
import models.items.Mower;
import models.quests.QuestService;
import models.sun.Sun;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class GamingController {
    private static final DecimalFormat COORDINATE_FORMAT = new DecimalFormat("0.##");

    private Result success(String message) {
        return new Result(true, message, null);
    }

    private Result failure(String message) {
        return new Result(false, message, null);
    }

    private GameState activeState() {
        Game game = App.getInstance().getCurrentGame();
        return game == null ? null : game.getGameState();
    }

    private String formatCoordinate(double coordinate) {
        return COORDINATE_FORMAT.format(coordinate);
    }

    private String formatSeconds(int ticks, int ticksPerSecond) {
        return COORDINATE_FORMAT.format((double) ticks / ticksPerSecond);
    }

    public Result advanceTime(int ticks) {
        if (ticks <= 0) {
            return failure("Tick count must be positive.\n");
        }
        Game game = App.getInstance().getCurrentGame();
        if (game == null || game.getGameState() == null) {
            return failure("No active game found.\n");
        }

        GameState state = game.getGameState();
        StringBuilder output = new StringBuilder()
                .append("Game advanced by ")
                .append(ticks)
                .append(" ticks.\n");
        state.setEventLogger(output::append);
        game.forward(ticks);

        if (state.isFinished()) {
            Menu destination = game instanceof ScoringGame ? Menu.MAIN_MENU : Menu.GAME_MENU;
            App.getInstance().setCurrentMenu(destination);
            App.getInstance().setCurrentGame(null);
            String destinationName = game instanceof ScoringGame ? "Main Menu" : "Game Menu";
            output.append(state.isWon() ? "Game ended! " : "Game over! ")
                    .append("You returned to the ")
                    .append(destinationName)
                    .append(".\n");
        }
        return success(output.toString());
    }

    public Result plantPlant(String plantType, int x, int y) {
        Game game = App.getInstance().getCurrentGame();
        if (game == null || game.getGameState() == null) {
            return failure("No active game found.\n");
        }
        GameState state = game.getGameState();
        Tile tile = state.getBoard()
                .getTileAtUserCoordinates(x - 1, y - 1);
        if (tile == null) {
            return failure("Coordinates are outside the map.\n");
        }
        PlantData selected = PlantRegistry.getByName(plantType);
        if (selected == null) {
            return failure("Unknown plant.\n");
        }
        if (game.isConveyorBeltLevel()) {
            if (!game.hasConveyorPlant(selected)) {
                return failure(selected.name() + " is not currently on the conveyor belt.\n");
            }
            if (!tile.isOccupiable()) {
                return tileOccupationFailure(tile);
            }
            return createAndPlaceConveyorPlant(game, state, tile, selected, x, y);
        }
        if (!game.getSelectedPlantsForThisGame().contains(selected)) {
            return failure("This plant is not selected for this level.\n");
        }
        if (!tile.isOccupiable()) {
            return tileOccupationFailure(tile);
        }
        return createAndPlacePlant(state, tile, selected, x, y);
    }
    private Result createAndPlaceConveyorPlant(Game game, GameState state, Tile tile, PlantData selected, int x, int y
    ) {
        Plant plant = createPlantForCurrentUser(selected);
        try {
            state.plantPlantWithoutSunCost(plant, tile);
        } catch (IllegalArgumentException | IllegalStateException exception) {
            return failure(exception.getMessage() + ".\n");
        }
        if (!game.consumeConveyorPlant(selected)) {
            //محض اطمینانه وگرنه این اتفاق نمیوفته احتمالا هیچوقت
            state.pluckPlant(plant, tile);
            return failure("The conveyor plant was no longer available.\n");
        }
        String message = selected.name() + " was planted from the conveyor at ("
                + x + ", " + y + ") for 0 sun.\n";
        return success(message + activateStoredBoost(selected, plant, state));
    }
    private String activateStoredBoost(PlantData selected, Plant plant, GameState state) {
        User user = App.getInstance().getLoggedInUser();
        if (user == null || !PlantBoostRepository.hasBoost(user.getId(), selected.id())) {
            return "";
        }
        plant.feed(state);
        PlantBoostRepository.consumeBoost(user.getId(), selected.id());
        return "The stored boost for " + selected.name() + " was activated.\n";
    }

    private Plant createPlantForCurrentUser(PlantData selected) {
        User user = App.getInstance().getLoggedInUser();
        int level = user == null ? 1 : PlantRepository.loadPlantLevels(user.getId())
                .getOrDefault(selected.id(), 1);
        return PlantFactory.create(selected, level);
    }

    private Result createAndPlacePlant(GameState state, Tile tile, PlantData selected, int x, int y) {
        int availableAt = state.getPlantCooldownEnd(selected.id());
        if (state.getTickCounter() < availableAt) {
            int ticksLeft = availableAt - state.getTickCounter();
            String seconds = formatSeconds(
                    ticksLeft,
                    state.getTicksPerSecond()
            );
            return failure("Plant is recharging for " + seconds + " more seconds.\n");
        }
        User user = App.getInstance().getLoggedInUser();
        int level = user == null
                ? 1
                : PlantRepository.loadPlantLevels(user.getId())
                .getOrDefault(selected.id(), 1);
        Plant plant = PlantFactory.create(selected, level);
        int cost = plant.getPlantStat().cost();
        if (state.getSun() < cost) {
            return failure(
                    "Not enough sun. " + selected.name() + " costs " + cost + " suns.\n");
        }
        try {
            state.plantPlant(plant, tile);
            state.startPlantCooldown(plant);
        } catch (IllegalArgumentException | IllegalStateException exception) {
            return failure(exception.getMessage() + ".\n");
        }
        String message = selected.name() + " planted at (" + x + ", " + y + ").\n";
        if (user != null
                && PlantBoostRepository.hasBoost(
                user.getId(),
                selected.id()
        )) {
            boolean plantStillExists = tile.getPlant() == plant
                    && !plant.isMarkedForRemoval();
            if (plantStillExists) {
                plant.feed(state);
                PlantBoostRepository.consumeBoost(user.getId(), selected.id());
                message += "The stored boost for "
                        + selected.name()
                        + " was activated.\n";
            } else {
                message += "The stored boost was kept because "
                        + selected.name()
                        + " is an instant-use plant.\n";
            }
        }
        return success(message);
    }

    private Result tileOccupationFailure(Tile tile) {
        if (tile.hasPlant()) {
            return failure("This tile already contains a plant.\n");
        }
        if (tile.hasGrave()) {
            return failure("A grave blocks this tile.\n");
        }
        if (tile.isIceBlocked()) {
            return failure("This tile is blocked by ice.\n");
        }
        if (tile.getIceFloorDirection() != null) {
            return failure("An ice floor cannot be planted on.\n");
        }
        return failure("This tile cannot be occupied.\n");
    }

    public Result pluckPlant(int x, int y) {
        GameState state = activeState();
        if (state == null) {
            return failure("No active game found.\n");
        }
        Tile tile = state.getBoard().getTileAtUserCoordinates(x - 1, y - 1);
        if (tile == null) {
            return failure("Coordinates are outside the map.\n");
        }
        if (!tile.hasPlant()) {
            return failure("There is no plant at (" + x + ", " + y + ").\n");
        }

        Plant plant = tile.getPlant();
        state.pluckPlant(plant, tile);
        return success(plant.getName() + " was plucked from (" + x + ", " + y + ").\n");
    }

    public Result feedPlant(int x, int y) {
        GameState state = activeState();
        if (state == null) {
            return failure("No active game found.\n");
        }
        Tile tile = state.getBoard().getTileAtUserCoordinates(x - 1, y - 1);
        if (tile == null) {
            return failure("Coordinates are outside the map.\n");
        }
        if (!tile.hasPlant()) {
            return failure("There is no plant at (" + x + ", " + y + ").\n");
        }
        if (!state.consumePlantFood()) {
            return failure("You do not have any plant food.\n");
        }
        tile.getPlant().feed(state);
        return success(tile.getPlant().getName() + " was fed plant food; you have "
                + state.getPlantFoodCount() + " plant foods now.\n");
    }

    public Result cheatAddPlantFood() {
        if (scoringGameIsActive()) {
            return failure("Cheats are disabled in the Scoring Game.\n");
        }

        GameState state = activeState();
        if (state == null) {
            return failure("No active game found.\n");
        }
        if (!state.addPlantFood()) {
            return failure("You already have the maximum of 3 plant foods.\n");
        }
        return success("One plant food was added; you have "
                + state.getPlantFoodCount() + " plant foods now.\n");
    }

    public Result zombiesInfo() {
        GameState state = activeState();
        if (state == null) {
            return failure("No active game found.\n");
        }
        List<Zombie> zombies = new ArrayList<>(state.getZombiesInTheGame());
        zombies.removeIf(Zombie::isDead);
        zombies.sort(Comparator.comparingInt(Zombie::getLane).thenComparingDouble(Zombie::getX));
        if (zombies.isEmpty()) {
            return success("There are no active zombies on the map.\n");
        }
        StringBuilder output = new StringBuilder();
        for (Zombie zombie : zombies) {
            appendZombieInfo(output, zombie, state);
        }
        return success(output.toString());
    }

    private void appendZombieInfo(StringBuilder output, Zombie zombie, GameState state) {
        output.append(zombie.getAlias()).append(":\n")
                .append("position: ")
                .append(formatCoordinate(zombie.getX() + 1)).append(", ")
                .append(zombie.getLane() + 1).append('\n')
                .append("health: ").append(zombie.getHitpoints())
                .append('/').append(zombie.getMaxHitpoints()).append('\n');
        if (zombie.hasIceShell()) {
            output.append("ice shell: ").append(zombie.getIceShellHealth())
                    .append('/').append(Zombie.ICE_SHELL_MAX_HEALTH).append('\n');
        }
        appendArmorInfo(output, zombie);
        appendEffectInfo(output, zombie, state);
        output.append('\n');
    }

    private void appendArmorInfo(StringBuilder output, Zombie zombie) {
        output.append("armor:\n");
        boolean hasArmor = false;
        for (ArmorBehavior armor : zombie.getBehaviors().stream()
                .filter(ArmorBehavior.class::isInstance)
                .map(ArmorBehavior.class::cast)
                .toList()) {
            if (!armor.isGone()) {
                hasArmor = true;
                output.append("  ").append(armor.getDefinition().getAlias())
                        .append(": ").append(armor.getCurrentHP()).append('\n');
            }
        }
        if (!hasArmor) {
            output.append("  none\n");
        }
    }

    private void appendEffectInfo(StringBuilder output, Zombie zombie, GameState state) {
        output.append("effects:\n");
        if (zombie.getEffects().isEmpty()) {
            output.append("  none\n");
            return;
        }
        zombie.getEffects().forEach((effect, effectTicks) -> output
                .append("  ").append(effect).append(": ")
                .append(formatSeconds(effectTicks, state.getTicksPerSecond()))
                .append("s\n"));
    }

    public Result showConveyor() {
        Game game = App.getInstance().getCurrentGame();
        if (game == null || game.getGameState() == null) {
            return failure("No active game found.\n");
        }
        if (!game.isConveyorBeltLevel()) {
            return failure("The current level does not use a conveyor belt.\n");
        }
        List<PlantData> belt = game.getConveyorBeltPlants();
        StringBuilder output = new StringBuilder("===== CONVEYOR BELT =====\n");
        if (belt.isEmpty()) {
            output.append("empty\n");
        } else {
            for (int i = 0; i < belt.size(); i++) {
                output.append(i + 1)
                        .append(". ")
                        .append(belt.get(i).name())
                        .append('\n');
            }
        }
        int ticksRemaining = game.getTicksUntilNextConveyorDelivery();
        output.append("Next delivery in ")
                .append(ticksRemaining)
                .append(" ticks (")
                .append(formatSeconds(
                        ticksRemaining,
                        game.getGameState().getTicksPerSecond()
                ))
                .append(" seconds).\n");
        return success(output.toString());
    }
    public Result showPlantStatus() {
        Game game = App.getInstance().getCurrentGame();
        if (game == null || game.getGameState() == null) {
            return failure("No active game found.\n");
        }
        if (game.isConveyorBeltLevel()) {
            return showConveyor();
        }
        GameState state = game.getGameState();
        User user = App.getInstance().getLoggedInUser();
        Map<Integer, Integer> levels = user == null
                ? Map.of()
                : PlantRepository.loadPlantLevels(user.getId());

        StringBuilder output = new StringBuilder("===== PLANT STATUS =====\n\n");
        for (PlantData data : game.getSelectedPlantsForThisGame()) {
            Plant plant = PlantFactory.create(data, levels.getOrDefault(data.id(), 1));
            int ticksLeft = Math.max(0,
                    state.getPlantCooldownEnd(data.id()) - state.getTickCounter());
            boolean enoughSun = state.getSun() >= plant.getPlantStat().cost();
            output.append(data.name()).append(":\n")
                    .append("  level: ").append(plant.getLevel()).append('\n')
                    .append("  sun cost: ").append(plant.getPlantStat().cost()).append('\n')
                    .append("  available: ")
                    .append(ticksLeft == 0 && enoughSun ? "yes" : "no")
                    .append('\n');
            if (ticksLeft > 0) {
                output.append("  cooldown remaining: ")
                        .append(formatSeconds(ticksLeft, state.getTicksPerSecond()))
                        .append(" seconds\n");
            }
            if (!enoughSun) {
                output.append("  reason: not enough sun\n");
            }
            output.append('\n');
        }
        return success(output.toString());
    }

    public Result showTileStatus(int x, int y) {
        GameState state = activeState();
        if (state == null) {
            return failure("No active game found.\n");
        }
        Tile tile = state.getBoard().getTileAtUserCoordinates(x - 1, y - 1);
        if (tile == null) {
            return failure("Coordinates are outside the map.\n");
        }
        StringBuilder output = new StringBuilder()
                .append("Tile (").append(x).append(", ").append(y).append("):\n");
        appendTileTerrain(output, tile);
        appendTilePlant(output, tile);
        appendTileZombies(output, tile, state);
        return success(output.toString());
    }

    private void appendTileTerrain(StringBuilder output, Tile tile) {
        output.append("terrain: ");
        if (tile.hasGrave()) {
            output.append("grave\n")
                    .append("grave health: ").append(tile.getGrave().getHealth()).append("/700\n");
        } else if (tile.isIceBlocked()) {
            output.append("ice-blocked\n");
        } else if (tile.getIceFloorDirection() != null) {
            output.append("ice-floor-")
                    .append(tile.getIceFloorDirection().name().toLowerCase(Locale.ROOT))
                    .append('\n');
        } else if (tile.isFrosted()) {
            output.append("frosted\n");
        } else {
            output.append("normal\n");
        }
    }

    private void appendTilePlant(StringBuilder output, Tile tile) {
        if (!tile.hasPlant()) {
            output.append("plant: none\n");
            return;
        }
        Plant plant = tile.getPlant();
        output.append("plant: ").append(plant.getName()).append('\n')
                .append("plant health: ").append(plant.getCurrentHP())
                .append('/').append(plant.getPlantStat().maxHp()).append('\n')
                .append("plant level: ").append(plant.getLevel()).append('\n')
                .append("frost level: ").append(plant.getFrostLevel()).append("/3\n")
                .append("plant food active: ")
                .append(plant.isOnPlantFood() ? "yes" : "no").append('\n');
        if (plant.isFrozenByIce()) {
            output.append("plant ice: ").append(plant.getIceHealth())
                    .append('/').append(Plant.ICE_MAX_HEALTH).append('\n');
        }
    }

    private void appendTileZombies(StringBuilder output, Tile tile, GameState state) {
        List<Zombie> zombies = tile.getZombies(state);
        if (zombies.isEmpty()) {
            output.append("zombies: none\n");
            return;
        }
        output.append("zombies:\n");
        for (Zombie zombie : zombies) {
            output.append("  ").append(zombie.getAlias())
                    .append(" - health ").append(zombie.getHitpoints())
                    .append('/').append(zombie.getMaxHitpoints());
            if (zombie.hasIceShell()) {
                output.append(" - ice ").append(zombie.getIceShellHealth())
                        .append('/').append(Zombie.ICE_SHELL_MAX_HEALTH);
            }
            output.append('\n');
        }
    }

    public Result showSunAmount() {
        GameState state = activeState();
        if (state == null) {
            return failure("No active game found.\n");
        }
        return success("You have " + state.getSun() + " suns.\n");
    }

    public Result cheatAddSun(int amount) {
        if (scoringGameIsActive()) {
            return failure("Cheats are disabled in the Scoring Game.\n");
        }

        GameState state = activeState();
        if (state == null) {
            return failure("No active game found.\n");
        }
        if (amount <= 0) {
            return failure("Sun amount must be positive.\n");
        }
        state.increaseSunBalance(amount);
        return success(amount + " suns added; you have " + state.getSun() + " suns now.\n");
    }

    public Result collectSun(int x, int y) {
        GameState state = activeState();
        if (state == null) {
            return failure("No active game found.\n");
        }
        int column = x - 1;
        int lane = y - 1;
        if (state.getBoard().getTileAtUserCoordinates(column, lane) == null) {
            return failure("Coordinates are outside the map.\n");
        }

        Sun target = null;
        for (Sun sun : state.getBoard().getActiveSuns()) {
            if ((int) Math.floor(sun.getX()) == column && sun.getLane() == lane) {
                target = sun;
                break;
            }
        }
        if (target == null) {
            return failure("No sun found at given coordinates.\n");
        }
        int sunBeforeCollection = state.getSun();
        if (!state.getBoard().collectSun(target, state)) {
            return failure("Sun has expired or was already collected.\n");
        }
        int collectedAmount = Math.max(0, state.getSun() - sunBeforeCollection);
        QuestService.getInstance().recordSunCollected(
                App.getInstance().getLoggedInUser(), collectedAmount);
        return success("Sun collected successfully; you have " + state.getSun() + " suns now.\n");
    }

    public Result removeCooldowns() {
        if (scoringGameIsActive()) {
            return failure("Cheats are disabled in the Scoring Game.\n");
        }

        GameState state = activeState();
        if (state == null) {
            return failure("No active game found.\n");
        }
        state.clearPlantCooldowns();
        return success("All plant cooldowns were removed.\n");
    }

    public Result releaseNuke() {
        if (scoringGameIsActive()) {
            return failure("Cheats are disabled in the Scoring Game.\n");
        }

        GameState state = activeState();
        if (state == null || state.getZombieWaveManager() == null) {
            return failure("No active game found.\n");
        }
        state.getZombieWaveManager().releaseTheNuke();
        return success("All active zombies were killed.\n");
    }

    public Result spawnZombie(String requestedType, int x, int y) {
        if (scoringGameIsActive()) {
            return failure("Cheats are disabled in the Scoring Game.\n");
        }

        GameState state = activeState();
        if (state == null) {
            return failure("No active game found.\n");
        }
        if (state.getBoard().getTileAtUserCoordinates(x - 1, y - 1) == null) {
            return failure("Coordinates are outside the map.\n");
        }

        String alias = resolveZombieAlias(requestedType);
        if (alias == null) {
            return failure("Unknown zombie type.\n");
        }
        Zombie zombie;
        try {
            zombie = ZombieRegistry.spawn(alias);
        } catch (IllegalArgumentException exception) {
            return failure(exception.getMessage() + ".\n");
        }
        zombie.setX(x - 1);
        zombie.setLane(y - 1);
        zombie.setQuestEligible(false);
        state.addZombie(zombie);
        return success("Zombie " + zombie.getAlias() + " spawned at (" + x + ", " + y + ").\n");
    }

    private String resolveZombieAlias(String requestedType) {
        for (String alias : ZombieRegistry.getTemplates().keySet()) {
            if (alias.equalsIgnoreCase(requestedType.trim())) {
                return alias;
            }
        }
        String enumName = requestedType.trim().toUpperCase(Locale.ROOT).replace(' ', '_');
        try {
            return ZombieType.valueOf(enumName).getAlias();
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    public Result showMap() {
        Game game = App.getInstance().getCurrentGame();
        GameState state = activeState();
        if (state == null) {return failure("No active game found.\n");}
        StringBuilder output = new StringBuilder();
        int wave = state.getZombieWaveManager() == null ? 0 : state.getZombieWaveManager().getCurrentWaveNumber();
        output.append("===== GAME STATUS =====\n").append("Wave: ").append(wave).append('\n')
                .append("Sun: ").append(state.getSun()).append('\n')
                .append("Plant food: ").append(state.getPlantFoodCount()).append('\n')
                .append("Tick: ").append(state.getTickCounter()).append("\n");
        if (game instanceof ScoringGame scoringGame) {
            output.append("MeowPoint: ").append(scoringGame.getScoreTracker().currentTotal()).append('\n');}
        if (state.hasDeadline()) {
            output.append("Dead Line: before column ")
                    .append(state.getDeadlineColumn())
                    .append("; don't you dare miss the dead line .\n");
        }
        if (game.isConveyorBeltLevel()) {
            output.append("Conveyor: ");
            List<PlantData> belt = game.getConveyorBeltPlants();
            if (belt.isEmpty()) {output.append("empty");
            } else {
                for (int i = 0; i < belt.size(); i++) {
                    if (i > 0) {output.append(" -> ");}
                    output.append(belt.get(i).name());}}
            output.append("\nNext conveyor delivery: ")
                    .append(game.getTicksUntilNextConveyorDelivery()).append(" ticks\n");
        }
                output.append("\n===== LAWN MOWERS =====\n");
        for (int lane = 0; lane < state.getBoard().getLaneCount(); lane++) {
            Mower mower = state.getLawnMowers()[lane];
            output.append("Row ").append(lane + 1).append(": ")
                    .append(mower.isDestroyed() ? "USED" : "AVAILABLE").append('\n');
        }
        output.append("\n===== BOARD =====\n");
        Board board = state.getBoard();
        for (int lane = 0; lane < board.getLaneCount(); lane++) {
            output.append("Row ").append(lane + 1).append(": ");
            for (int column = 0; column < board.getColumnCount(); column++) {
                if (state.hasDeadline()
                        && column == state.getDeadlineColumn() - 1) {
                    output.append("|| ");
                }
                Tile tile = board.getTile(lane, column);
                char symbol = '.';
                boolean hasZombie = tile.hasZombie(state);
                if (tile.hasPlant() && hasZombie) symbol = 'B';
                else if (tile.hasPlant() && tile.getPlant().isFrozenByIce()) symbol = 'F';
                else if (tile.hasPlant()) symbol = 'P';
                else if (hasZombie) symbol = 'Z';else if (tile.hasGrave()) symbol = 'G';
                else if (tile.isIceBlocked()) symbol = 'I';
                else if (tile.getIceFloorDirection() != null) {
                    symbol = tile.getIceFloorDirection().name().equals("UP") ? '^' : 'v';}
                output.append('[').append(symbol).append("] ");}
            output.append('\n');}
        output.append("Legend: P=plant, F=frozen plant, Z=zombie, G=grave, ")
                .append("I=ice block, ^=slide up, v=slide down, ||=Dead Line\n");
        return success(output.toString());
    }
    private boolean scoringGameIsActive() {
        return App.getInstance().getCurrentGame() instanceof ScoringGame;
    }
    public Result showScore() {
        Game game = App.getInstance().getCurrentGame();
        if (!(game instanceof ScoringGame scoringGame)) {
            return failure(
                    "This command is only available in the Scoring Game.\n"
            );
        }
        return success(scoringGame.getScoreTracker().liveSummary());
    }

    public Result showScoringRules() {
        Game game = App.getInstance().getCurrentGame();
        if (!(game instanceof ScoringGame scoringGame)) {
            return failure("This command is only available in the Scoring Game.\n");
        }
        return success(scoringGame.showScoringRules());
    }
}
