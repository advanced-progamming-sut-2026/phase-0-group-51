package network.client;

import models.App;
import models.User;
import network.protocol.shop.ShopDailyOfferDto;
import network.protocol.shop.ShopItemDto;
import network.protocol.shop.ShopPlantStateDto;
import network.protocol.shop.ShopResponse;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class ClientShopState {
    private static boolean loaded;
    private static List<ShopItemDto> catalogue = List.of();
    private static ShopDailyOfferDto dailyOffer;
    private static Map<Integer, ShopPlantStateDto> plants = Map.of();

    private ClientShopState() {
    }

    public static synchronized void apply(ShopResponse response) {
        if (response == null || !response.isSuccess()) {
            return;
        }

        catalogue = List.copyOf(
                response.getCatalogue() == null
                        ? List.of()
                        : response.getCatalogue()
        );
        dailyOffer = response.getDailyOffer();

        Map<Integer, ShopPlantStateDto> replacement = new LinkedHashMap<>();
        if (response.getPlants() != null) {
            for (ShopPlantStateDto state : response.getPlants()) {
                if (state != null) {
                    replacement.put(state.getPlantId(), state);
                }
            }
        }
        plants = Map.copyOf(replacement);
        loaded = true;

        Set<Integer> unlockedIds = new LinkedHashSet<>(plants.keySet());
        ClientPlantOwnershipState.replaceWith(unlockedIds);

        User user = App.getInstance().getLoggedInUser();
        if (user != null) {
            user.setCoins(response.getCoins());
            user.setGems(response.getGems());
            user.setPlantFoodNum(response.getPlantFood());
        }
    }

    public static synchronized boolean isLoaded() {
        return loaded;
    }

    public static synchronized List<ShopItemDto> getCatalogue() {
        return new ArrayList<>(catalogue);
    }

    public static synchronized ShopDailyOfferDto getDailyOffer() {
        return dailyOffer;
    }

    public static synchronized List<ShopPlantStateDto> getPlants() {
        return new ArrayList<>(plants.values());
    }

    public static synchronized ShopPlantStateDto getPlant(int plantId) {
        return plants.get(plantId);
    }

    public static synchronized Set<Integer> unlockedPlantIds() {
        return new LinkedHashSet<>(plants.keySet());
    }

    public static synchronized Map<Integer, Integer> plantLevels() {
        Map<Integer, Integer> result = new LinkedHashMap<>();
        plants.forEach((id, state) -> result.put(id, state.getLevel()));
        return result;
    }

    public static synchronized Map<Integer, Integer> seedPackets() {
        Map<Integer, Integer> result = new LinkedHashMap<>();
        plants.forEach((id, state) -> result.put(id, state.getSeedPackets()));
        return result;
    }

    public static synchronized boolean hasBoost(int plantId) {
        ShopPlantStateDto state = plants.get(plantId);
        return state != null && state.isBoosted();
    }

    public static synchronized int plantLevel(int plantId) {
        ShopPlantStateDto state = plants.get(plantId);
        return state == null ? 1 : Math.max(1, state.getLevel());
    }

    public static synchronized int seedPacketCount(int plantId) {
        ShopPlantStateDto state = plants.get(plantId);
        return state == null ? 0 : Math.max(0, state.getSeedPackets());
    }

    public static synchronized void clear() {
        loaded = false;
        catalogue = List.of();
        dailyOffer = null;
        plants = Map.of();
    }
}
