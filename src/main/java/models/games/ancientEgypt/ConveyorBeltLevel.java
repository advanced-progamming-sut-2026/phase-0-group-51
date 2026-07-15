package models.games.ancientEgypt;

import Data.loader.PlantData;
import lombok.Getter;
import lombok.Setter;
import models.enums.commands.GameCommands;
import models.games.GameState;

import java.util.*;
@Setter
@Getter
public class ConveyorBeltLevel {
    public static final int DELIVERY_SECONDS = 12;
    private final List<PlantData> deliveryPool;
    private final Random random;
    private final Deque<PlantData> belt = new ArrayDeque<>();
    private int nextDeliveryTick;
    public ConveyorBeltLevel(List<PlantData> deliveryPool) {
        this(deliveryPool, new Random());
    }
    public ConveyorBeltLevel(List<PlantData> deliveryPool, Random random) {
        Objects.requireNonNull(deliveryPool, "Conveyor delivery pool cannot be null.");
        Objects.requireNonNull(random, "Random cannot be null.");
        List<PlantData> validPlants = deliveryPool.stream().filter(Objects::nonNull).distinct().toList();
        this.deliveryPool = List.copyOf(validPlants);
        this.random = random;
    }

    public void initialize(GameState state) {
        Objects.requireNonNull(state, "Game state cannot be null.");
        belt.clear();
        deliverPlant(state);
        nextDeliveryTick = state.getTickCounter() + DELIVERY_SECONDS * state.getTicksPerSecond();
    }

    public void onTick(GameState state) {
        Objects.requireNonNull(state, "Game state cannot be null.");
        int intervalTicks = DELIVERY_SECONDS * state.getTicksPerSecond();

        while (state.getTickCounter() >= nextDeliveryTick) {
            deliverPlant(state);
            nextDeliveryTick += intervalTicks;
        }
    }

    public boolean contains(PlantData plantData) {
        if (plantData == null) {
            return false;
        }
        return belt.stream().anyMatch(item -> item.id() == plantData.id());
    }

    public boolean consume(PlantData plantData) {
        if (plantData == null) {
            return false;
        }

        Iterator<PlantData> iterator = belt.iterator();
        while (iterator.hasNext()) {
            if (iterator.next().id() == plantData.id()) {
                iterator.remove();
                return true;
            }
        }
        return false;
    }

    public List<PlantData> getPlants() {
        return Collections.unmodifiableList(new ArrayList<>(belt));
    }

    public int getTicksUntilNextDelivery(GameState state) {
        Objects.requireNonNull(state, "Game state cannot be null.");
        return Math.max(0, nextDeliveryTick - state.getTickCounter());
    }

    private void deliverPlant(GameState state) {
        PlantData delivered = deliveryPool.get(random.nextInt(deliveryPool.size()));
        belt.addLast(delivered);
        state.logEvent(delivered.name() + " arrived on the conveyor belt.\n");
    }
}
