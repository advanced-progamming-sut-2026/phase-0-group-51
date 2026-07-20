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
import models.Plant.PlantTag;
import models.Plant.Modifier;
import models.Result;
import models.User;
import models.Zombie.Behavior.ArmorBehavior;
import models.Zombie.Zombie;
import models.Zombie.ZombieType;
import models.enums.Menu;
import models.games.Game;
import models.games.GameState;
import models.games.ScoringGame;
import models.games.ancientEgypt.Grave;
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
    private static final int LILY_PAD_ID = 58;
    private static final int PUMPKIN_ID = 50;
    private static final int IMITATER_ID = 56;
    private static final int HOT_POTATO_ID = 59;
    private static final int GRAVE_BUSTER_ID = 60;
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
        if (game.isPreparingPlantWhatYouGet()) {
            return failure(
                    "Time is paused during Plant_What_You_Get preparation. "
                            + "Plant your plants, then use "
                            + "'start zombie waves'.\n"
            );
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
        PlantData imitaterTarget = parseImitaterTarget(plantType);
        PlantData selected = imitaterTarget == null
                ? PlantRegistry.getByName(plantType)
                : PlantRegistry.getById(IMITATER_ID);
        if (selected == null) {
            return failure("Unknown plant.\n");
        }
        if (selected.id() == IMITATER_ID && imitaterTarget == null) {
            return failure(
                    "Use Imitater:<plant name> to choose what Imitater copies.\n"
            );
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
        if (imitaterTarget != null) {
            if (!game.getSelectedPlantsForThisGame().contains(imitaterTarget)) {
                return failure("The copied plant must also be selected for this level.\n");
            }
            return placeImitaterCopy(
                    state,
                    tile,
                    selected,
                    imitaterTarget,
                    x,
                    y
            );
        }
        if (selected.id() == HOT_POTATO_ID) {
            return placeHotPotato(state, tile, selected, x, y);
        }
        if (selected.id() == GRAVE_BUSTER_ID) {
            return placeGraveBuster(state, tile, selected, x, y);
        }
        if (selected.id() == PUMPKIN_ID) {
            return placePumpkin(state, tile, selected, x, y);
        }
        if (game.isPlantWhatYouGetLevel() && isSunProducer(selected)) {
            return failure("Sun-producing plants are forbidden in Plant What You Get.\n");
        }
        if (tile.isWater()) {
            return placePlantOnWater(state, tile, selected, x, y);
        }
        if (isLilyPad(selected) || selected.tags().contains(PlantTag.WATER)) {
            return failure(selected.name() + " can only be planted on water.\n");
        }
        if (canStackSelectedPlant(selected, tile)) {
            return createAndStackPlant(state, tile, selected, x, y);
        }
        if (!tile.isOccupiable()) {
            return tileOccupationFailure(tile);
        }
        return createAndPlacePlant(state, tile, selected, x, y);
    }
    private Result placePlantOnWater(GameState state, Tile tile, PlantData selected, int x, int y) {
        if (!isLilyPad(selected) && canStackSelectedPlant(selected, tile)) {
            return createAndStackPlant(state, tile, selected, x, y);
        }
        if (isLilyPad(selected)) {
            if (tile.hasPlant()) {
                return failure("This water tile already contains a plant or Lily Pad.\n");
            }
            return createAndPlaceLilyPad(state, tile, selected, x, y);
        }
        if (selected.tags().contains(PlantTag.WATER)) {
        if (!tile.isOccupiable()) {
            return tileOccupationFailure(tile);
        }
        return createAndPlacePlant(state, tile, selected, x, y);
    }
        if (tile.hasLilyPad() && !tile.hasTopPlant()) {
            return createAndPlacePlantOnLilyPad(state, tile, selected, x, y);
        }
        return failure(
                "A non-aquatic plant needs an empty Lily Pad on this water tile.\n"
        );
    }

    private Result createAndPlaceLilyPad(
            GameState state, Tile tile,
            PlantData selected, int x, int y
    ) {
        Result cooldownFailure = cooldownFailure(state, selected);
        if (cooldownFailure != null) {
            return cooldownFailure;
        }
        Plant lilyPad = createPlantForCurrentUser(selected);
        try {
            state.plantLilyPad(lilyPad, tile);
            startCooldownIfRequired(state, lilyPad);
        } catch (IllegalArgumentException | IllegalStateException exception) {
            return failure(exception.getMessage() + ".\n");
        }
        return success(
                selected.name() + " planted on water at (" + x + ", " + y + ").\n"
                        + activateStoredBoost(selected, lilyPad, state)
        );
    }

    private Result createAndPlacePlantOnLilyPad(
            GameState state,
            Tile tile,
            PlantData selected,
            int x,
            int y
    ) {
        Result cooldownFailure = cooldownFailure(state, selected);
        if (cooldownFailure != null) {
            return cooldownFailure;
        }
        Plant plant = createPlantForCurrentUser(selected);
        try {
            state.plantOnLilyPad(plant, tile);
            startCooldownIfRequired(state, plant);
        } catch (IllegalArgumentException | IllegalStateException exception) {
            return failure(exception.getMessage() + ".\n");
        }
        return success(
                selected.name() + " planted on a Lily Pad at ("
                        + x + ", " + y + ").\n"
                        + activateStoredBoost(selected, plant, state)
        );
    }

    private Result cooldownFailure(GameState state, PlantData selected) {
        if (rechargeDisabledDuringPreparation()) {
            return null;
        }
        int availableAt = state.getPlantCooldownEnd(selected.id());
        if (state.getTickCounter() >= availableAt) {
            return null;
        }
        int ticksLeft = availableAt - state.getTickCounter();
        return failure(
                "Plant is recharging for "
                        + formatSeconds(ticksLeft, state.getTicksPerSecond())
                        + " more seconds.\n"
        );
    }

    private void startCooldownIfRequired(GameState state, Plant plant) {
        if (!rechargeDisabledDuringPreparation()) {
            state.startPlantCooldown(plant);
        }
    }

    private boolean rechargeDisabledDuringPreparation() {
        Game game = App.getInstance().getCurrentGame();
        return game != null && game.isPreparingPlantWhatYouGet();
    }

    private boolean isLilyPad(PlantData plant) {
        return plant != null && plant.id() == LILY_PAD_ID;
    }

    private PlantData parseImitaterTarget(String input) {
        if (input == null) {
            return null;
        }
        String prefix = "imitater:";
        String normalized = input.trim();
        if (!normalized.toLowerCase(Locale.ROOT).startsWith(prefix)) {
            return null;
        }
        String copiedName = normalized.substring(prefix.length()).trim();
        if (copiedName.isEmpty()) {
            return null;
        }
        PlantData target = PlantRegistry.getByName(copiedName);
        if (target == null || target.id() == IMITATER_ID) {
            return null;
        }
        return target;
    }

    private Result placeImitaterCopy(
            GameState state,
            Tile tile,
            PlantData imitater,
            PlantData target,
            int x,
            int y
    ) {
        Result cooldown = cooldownFailure(state, imitater);
        if (cooldown != null) {
            return cooldown;
        }
        User user = App.getInstance().getLoggedInUser();
        int imitaterLevel = user == null ? 1
                : PlantRepository.loadPlantLevels(user.getId())
                .getOrDefault(IMITATER_ID, 1);
        int targetLevel = user == null ? 1
                : PlantRepository.loadPlantLevels(user.getId())
                .getOrDefault(target.id(), 1);
        Plant copy = Modifier.createImitaterCopy(
                target,
                targetLevel,
                imitaterLevel
        );
        try {
            if (target.id() == HOT_POTATO_ID) {
                boolean hasFrozenPlant = tile.getPlants().stream()
                        .anyMatch(Plant::isFrozenByIce);
                if (!tile.isIceBlocked() && !hasFrozenPlant) {
                    throw new IllegalStateException(
                            "Hot Potato must be used on ice or a frozen plant"
                    );
                }
                state.useInstantPlantOnTile(copy, tile);
            } else if (target.id() == GRAVE_BUSTER_ID) {
                state.plantOnGrave(copy, tile);
            } else if (target.id() == PUMPKIN_ID) {
                state.plantPumpkin(copy, tile);
            } else if (target.id() == LILY_PAD_ID) {
                state.plantLilyPad(copy, tile);
            } else if (tile.isWater()
                    && copy.hasTag(PlantTag.WATER)) {
                state.plantPlant(copy, tile);
            } else if (tile.isWater() && tile.hasLilyPad()
                    && !tile.hasTopPlant()) {
                state.plantOnLilyPad(copy, tile);
            } else {
                state.plantPlant(copy, tile);
            }
            state.startPlantCooldown(copy);
        } catch (IllegalArgumentException | IllegalStateException exception) {
            return failure(exception.getMessage() + ".\n");
        }
        return success("Imitater copied " + target.name() + " at ("
                + x + ", " + y + ").\n");
    }

    private Result placeHotPotato(
            GameState state,
            Tile tile,
            PlantData selected,
            int x,
            int y
    ) {
        boolean hasFrozenPlant = tile.getPlants().stream()
                .anyMatch(Plant::isFrozenByIce);
        if (!tile.isIceBlocked() && !hasFrozenPlant) {
            return failure("Hot Potato must be used on ice or a frozen plant.\n");
        }
        return placeSpecialInstantPlant(state, tile, selected, x, y);
    }

    private Result placeGraveBuster(
            GameState state,
            Tile tile,
            PlantData selected,
            int x,
            int y
    ) {
        if (!tile.hasGrave()) {
            return failure("Grave Buster must be planted on a grave.\n");
        }
        Result cooldown = cooldownFailure(state, selected);
        if (cooldown != null) {
            return cooldown;
        }
        Plant plant = createPlantForCurrentUser(selected);
        try {
            state.plantOnGrave(plant, tile);
            startCooldownIfRequired(state, plant);
        } catch (IllegalArgumentException | IllegalStateException exception) {
            return failure(exception.getMessage() + ".\n");
        }
        return success("Grave Buster started eating the grave at ("
                + x + ", " + y + ").\n");
    }

    private Result placePumpkin(
            GameState state,
            Tile tile,
            PlantData selected,
            int x,
            int y
    ) {
        if (!tile.hasPlant() || tile.hasPumpkin()) {
            return failure("Pumpkin must cover an existing plant.\n");
        }
        Result cooldown = cooldownFailure(state, selected);
        if (cooldown != null) {
            return cooldown;
        }
        Plant pumpkin = createPlantForCurrentUser(selected);
        try {
            state.plantPumpkin(pumpkin, tile);
            startCooldownIfRequired(state, pumpkin);
        } catch (IllegalArgumentException | IllegalStateException exception) {
            return failure(exception.getMessage() + ".\n");
        }
        return success("Pumpkin covered the plant at (" + x + ", " + y + ").\n");
    }

    private Result placeSpecialInstantPlant(
            GameState state,
            Tile tile,
            PlantData selected,
            int x,
            int y
    ) {
        Result cooldown = cooldownFailure(state, selected);
        if (cooldown != null) {
            return cooldown;
        }
        Plant plant = createPlantForCurrentUser(selected);
        try {
            state.useInstantPlantOnTile(plant, tile);
            startCooldownIfRequired(state, plant);
        } catch (IllegalArgumentException | IllegalStateException exception) {
            return failure(exception.getMessage() + ".\n");
        }
        return success(selected.name() + " activated at ("
                + x + ", " + y + ").\n");
    }

    private boolean isSunProducer(PlantData plant) {
        if (plant == null) {
            return false;
        }
        String category = plant.category() == null ? "" : plant.category().replaceAll("[^A-Za-z]", "")
                .toLowerCase(Locale.ROOT);

        return category.equals("sunproducer") || plant.tags().contains(PlantTag.SUN);
    }

    private boolean canStackSelectedPlant(PlantData selected, Tile tile) {
        return tile.hasPlant()
                && selected.tags().contains(PlantTag.STACK)
                && tile.getPlant().getId() == selected.id();
    }

    private Result createAndStackPlant(GameState state, Tile tile, PlantData selected, int x, int y) {
        int availableAt = state.getPlantCooldownEnd(selected.id());
        if (!rechargeDisabledDuringPreparation()
                && state.getTickCounter() < availableAt) {
            int ticksLeft = availableAt - state.getTickCounter();
            return failure("Plant is recharging for "
                    + formatSeconds(ticksLeft, state.getTicksPerSecond()) + " more seconds.\n");
        }
        Plant addition = createPlantForCurrentUser(selected);
        Plant existing = tile.getPlant();
        try {
            state.stackPlant(addition, existing);
            if (!rechargeDisabledDuringPreparation()) {
            state.startPlantCooldown(addition);
            }
        } catch (IllegalArgumentException | IllegalStateException exception) {
            return failure(exception.getMessage() + ".\n");
        }
        String message = selected.name() + " stacked at (" + x + ", " + y + ").\n";
        User user = App.getInstance().getLoggedInUser();
        if (user != null && PlantBoostRepository.hasBoost(user.getId(), selected.id())) {
            existing.feed(state);
            PlantBoostRepository.consumeBoost(user.getId(), selected.id());
            message += "The stored boost for " + selected.name() + " was activated on the stacked plant.\n";
        }
        return success(message);
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
        boolean plantStillExists = !plant.isMarkedForRemoval()
                && state.getBoard().getTileForPlant(plant) != null;
        if (!plantStillExists) {
            return "The stored boost was kept because "
                    + selected.name() + " is an instant-use plant.\n";
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
        if (!rechargeDisabledDuringPreparation()
                && state.getTickCounter() < availableAt) {
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
            if (!rechargeDisabledDuringPreparation()) {
            state.startPlantCooldown(plant);
            }
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
        if (tile.isWater()) {
            return failure("This water tile requires an aquatic plant or a Lily Pad.\n");
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
        if (state.isProtectedPlant(plant)) {
            return failure(
                    "This plant is protected in Save Our Seeds and cannot be plucked.\n"
            );
        }
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
        Plant plant = tile.getPlant();
        if (plant.isMarkedForRemoval() || plant.isDead()) {
            return failure("This plant can no longer receive plant food.\n");
        }
        if (plant.isFrozenByIce() || plant.hasOctopus()) {
            return failure("A covered plant cannot receive plant food.\n");
        }
        if (plant.isOnPlantFood()) {
            return failure("This plant is already using plant food.\n");
        }
        if (!state.consumePlantFood()) {
            return failure("You do not have any plant food.\n");
        }
        plant.feed(state);
        return success(plant.getName() + " was fed plant food; you have "
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
        appendTileSuns(output, tile, state);
        return success(output.toString());
    }

    private void appendTileTerrain(StringBuilder output, Tile tile) {
        output.append("terrain: ");
        if (tile.hasGrave()) {
            Grave grave = tile.getGrave();
            if (grave.isHasPlantFood()) {
                output.append("plant-food grave\n")
                        .append("grave reward: 1 plant food\n");
            } else if (grave.isHasSun()) {
                output.append("sun grave\n")
                        .append("grave reward: 50 sun\n");
            } else {
                output.append("normal grave\n");
            }
            output.append("grave health: ")
                    .append(grave.getHealth())
                    .append("/700\n");
        } else if (tile.isIceBlocked()) {
            output.append("ice-blocked\n");
        } else if (tile.getIceFloorDirection() != null) {
            output.append("ice-floor-")
                    .append(tile.getIceFloorDirection().name().toLowerCase(Locale.ROOT))
                    .append('\n');
        } else if (tile.isWater() && tile.isLowShore()) {
            output.append("flooded low shore\n");
        } else if (tile.isWater()) {
            output.append("water\n");
        } else if (tile.isLowShore()) {
            output.append("dry low shore\n");
        } else if (tile.isFrosted()) {
            output.append("frosted\n");
        } else {
            output.append("normal\n");
        }
    }

    private void appendTilePlant(StringBuilder output, Tile tile) {
        if (!tile.hasTopPlant() && !tile.hasLilyPad()) {
            output.append("plant: none\n");
            return;
        }
        if (tile.hasTopPlant()) {
            appendPlantDetails(output, "plant", tile.getTopPlant());
        } else {
            output.append("plant: none\n");
        }
        if (tile.hasLilyPad()) {
            appendPlantDetails(output, "lily pad", tile.getLilyPadPlant());
        }
    }

    private void appendPlantDetails(
            StringBuilder output,
            String label,
            Plant plant
    ) {
        output.append(label).append(": ").append(plant.getName()).append('\n')
                .append(label).append(" health: ").append(plant.getCurrentHP())
                .append('/').append(plant.getPlantStat().maxHp()).append('\n')
                .append(label).append(" level: ").append(plant.getLevel()).append('\n')
                .append(label).append(" frost level: ")
                .append(plant.getFrostLevel()).append("/3\n")
                .append(label).append(" plant food active: ")
                .append(plant.isOnPlantFood() ? "yes" : "no").append('\n');
        if (plant.isFrozenByIce()) {
            output.append(label).append(" ice: ").append(plant.getIceHealth())
                    .append('/').append(Plant.ICE_MAX_HEALTH).append('\n');
        }
    }

    private void appendTileZombies(StringBuilder output, Tile tile, GameState state) {
        List<Zombie> zombies = getZombiesAtTile(
                state, tile.getLane(), tile.getColumn()
        );
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

    private void appendTileSuns(StringBuilder output, Tile tile, GameState state) {
        List<Sun> suns = getSunsAtTile( state.getBoard(), tile.getLane(), tile.getColumn()
        );
        if (suns.isEmpty()) {
            output.append("suns: none\n");
            return;
        }
        output.append("suns:\n");
        for (Sun sun : suns) {
            output.append("  ")
                    .append(sun.getSunType())
                    .append(" - amount ")
                    .append(sun.getAmount())
                    .append(" - ")
                    .append(sun.isGrounded() ? "grounded" : "falling")
                    .append('\n');
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

    public Result startZombieWaves() {
        Game game = App.getInstance().getCurrentGame();
        if (game == null || game.getGameState() == null) {
            return failure("No active game found.\n");
        }
        if (!game.isPlantWhatYouGetLevel()) {
            return failure("This command is only available in Plant What You Get.\n");
        }
        if (!game.isPreparingPlantWhatYouGet()) {
            return failure("Zombie waves have already started.\n");
        }
        game.getGameState().setEventLogger(null);
        if (!game.startZombieWaves()) {
            return failure("Zombie waves could not be started.\n");
        }
        return success("First Zombie waves started.\n");
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
        if (state.isSaveOurSeedsActive()) {
            output.append(state.getSaveOurSeedsStatus()).append('\n')
                    .append("WARNING: rows marked with ! contain protected plants.\n");
        }
        if (game.isPreparingPlantWhatYouGet()) {
            output.append("PREPARATION - no recharge; use 'start zombie waves'.\n");
        } else if (game.isPlantWhatYouGetLevel()) {
            output.append("ZOMBIE WAVES - recharge active.\n");
        }
        if (state.getBoard().getWaterColumnCount() > 0) {
            output.append("Water: rightmost ")
                    .append(state.getBoard().getWaterColumnCount())
                    .append(" columns. Maximum tide reaches column 6.\n");
        }
        if (game instanceof ScoringGame scoringGame) {
            output.append("MeowPoint: ").append(scoringGame.getScoreTracker().currentTotal()).append('\n');}
        if (state.hasDeadline()) {
            output.append("Dead Line: before column ")
                    .append(state.getDeadlineColumn())
                    .append(".\n");
        }

        if (game.isConveyorBeltLevel()) {
            appendConveyorSummary(output, game);
        }
                output.append("\n===== LAWN MOWERS =====\n");
        for (int lane = 0; lane < state.getBoard().getLaneCount(); lane++) {
            Mower mower = state.getLawnMowers()[lane];
            output.append("Row ").append(lane + 1).append(": ")
                    .append(mower.isDestroyed() ? "USED" : "AVAILABLE").append('\n');
        }

        Board board = state.getBoard();
        output.append("\n===== BOARD =====\n")
                .append("Each cell contains  3 chars: ")
                .append("[base][zombie][sun].\n\n");

        appendBoardColumnHeader(output, board);

        for (int lane = 0; lane < board.getLaneCount(); lane++) {
            output.append(state.isProtectedRow(lane) ? "! Row " : "  Row ")
                    .append(lane + 1).append(": ");
            for (int column = 0; column < board.getColumnCount(); column++) {
                Tile tile = board.getTile(lane, column);
                output.append('[')
                        .append(buildThreeCharacterCell(state, tile))
                        .append("] ");
            }
            output.append('\n');
        }

        output.append("\nCell position 1 (base): ")
                .append("E=protected plant, P=land plant, F=frozen plant, A=aquatic plant, ")
                .append("Y=plant on Lily Pad, G=normal grave, ")
                .append("S=sun grave, Q=plant-food grave, I=ice block, ")
                .append("C=crater, U=ice floor up, D=ice floor down, ")
                .append("L=Lily Pad, W=water, B=flooded low shore, ")
                .append("T=dry low shore, .=empty\n")
                .append("Cell position 2: Z=zombie, .=none\n")
                .append("Cell position 3: S=collectible/grounded sun, ")
                .append("s=falling sun, .=none\n")
                .append("Examples: [PZS]=plant + zombie + grounded sun, ")
                .append("[SZ.]=sun grave + zombie, ")
                .append("[Q.S]=plant-food grave + grounded sun.\n");

        return success(output.toString());
    }

    private void appendConveyorSummary(StringBuilder output, Game game) {
        output.append("Conveyor: ");
        List<PlantData> belt = game.getConveyorBeltPlants();
        if (belt.isEmpty()) {
            output.append("empty");
        } else {
            for (int i = 0; i < belt.size(); i++) {
                if (i > 0) {
                    output.append(" -> ");
                }
                output.append(belt.get(i).name());
            }
        }
        output.append("\nNext conveyor delivery: ")
                .append(game.getTicksUntilNextConveyorDelivery())
                .append(" ticks\n");
    }

    private void appendBoardColumnHeader(StringBuilder output, Board board) {
        output.append("       ");
        for (int column = 0; column < board.getColumnCount(); column++) {
            output.append(String.format(Locale.ROOT, "%-6d", column + 1));
        }
        output.append('\n');
    }

    private String buildThreeCharacterCell(GameState state, Tile tile) {
        char base = getBaseMapSymbol(state, tile);
        char zombie = getZombiesAtTile(
                state, tile.getLane(), tile.getColumn()
        ).isEmpty() ? '.' : 'Z';
        char sun = getSunMapSymbol(
                state.getBoard(), tile.getLane(), tile.getColumn()
        );
        return new String(new char[]{base, zombie, sun});
    }

    private char getBaseMapSymbol(GameState state, Tile tile) {
        if (tile.hasTopPlant()) {
            if (state.isProtectedPlant(tile.getTopPlant())) {
                return 'E';
            }
            if (tile.getTopPlant().isFrozenByIce()) {
                return 'F';
            }
            if (tile.isWater() && tile.hasLilyPad()) {
                return 'Y';
            }
            if (tile.isWater()) {
                return 'A';
            }
            return 'P';
        }
        if (tile.hasLilyPad()) {
            return 'L';
        }
        if (tile.hasGrave()) {
            Grave grave = tile.getGrave();
            if (grave.isHasPlantFood()) {
                return 'Q';
            }
            if (grave.isHasSun()) {
                return 'S';
            }
            return 'G';
        }
        if (tile.isIceBlocked()) {
            return 'I';
        }
        if (tile.isCrater()) {
            return 'C';
        }
        if (tile.getIceFloorDirection() != null) {
            return tile.getIceFloorDirection().name().equals("UP")
                    ? 'U'
                    : 'D';
        }
        if (tile.isWater() && tile.isLowShore()) {
            return 'B';
        }
        if (tile.isWater()) {
            return 'W';
        }
        if (tile.isLowShore()) {
            return 'T';
        }
        return '.';
    }

    private List<Zombie> getZombiesAtTile(
            GameState state, int lane, int column
    ) {
        List<Zombie> result = new ArrayList<>();
        if (state == null || state.getZombiesInTheGame() == null) {
            return result;
        }
        for (Zombie zombie : state.getZombiesInTheGame()) {
            if (zombie == null || zombie.isDead()) {
                continue;
            }
            if (zombie.getLane() != lane) {
                continue;
            }
            int zombieColumn = (int) Math.floor(zombie.getX());
            if (zombieColumn == column) {
                result.add(zombie);
            }
        }
        return result;
    }

    private List<Sun> getSunsAtTile(
            Board board, int lane, int column
    ) {
        List<Sun> result = new ArrayList<>();
        if (board == null || board.getActiveSuns() == null) {
            return result;
        }
        for (Sun sun : board.getActiveSuns()) {
            if (sun == null || !sun.isActive()) {
                continue;
            }
            if (sun.getLane() != lane) {
                continue;
            }
            int sunColumn = (int) Math.floor(sun.getX());
            if (sunColumn == column) {
                result.add(sun);
            }
        }
        return result;
    }

    private char getSunMapSymbol(
            Board board, int lane, int column
    ) {
        List<Sun> suns = getSunsAtTile(board, lane, column);
        if (suns.isEmpty()) {
            return '.';
        }
        for (Sun sun : suns) {
            if (sun.isGrounded()) {
                return 'S';
            }
        }
        return 's';
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
