package Data.loader;

import models.games.ChapterTheme;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class PlantRegistry {
    private static final Map<Integer, PlantData> ALL = new HashMap<>();

    public static final int DEFAULT_PURCHASE_COST = 2000;
    private static final int REGISTERED_PLANT_COUNT = 69;
    private static final Map<Integer, UnlockRule> UNLOCK_RULES =
            createUnlockRules();

    public enum UnlockType {
        STARTER,
        CHAPTER,
        LEVEL,
        PURCHASE
    }

    public record UnlockRule(
            UnlockType type,
            ChapterTheme chapter,
            int level,
            int purchaseCost
    ) {
        public UnlockRule {
            if (type == null) {
                throw new IllegalArgumentException(
                        "Plant unlock type cannot be null."
                );
            }
            if ((type == UnlockType.CHAPTER || type == UnlockType.LEVEL)
                    && chapter == null) {
                throw new IllegalArgumentException(
                        "Adventure plant unlocks must specify a chapter."
                );
            }
            if (type == UnlockType.LEVEL && level < 1) {
                throw new IllegalArgumentException(
                        "Level plant unlocks must specify a positive level."
                );
            }
            if (type == UnlockType.PURCHASE && purchaseCost <= 0) {
                throw new IllegalArgumentException(
                        "Purchasable plants must have a positive price."
                );
            }
        }

        public static UnlockRule starter() {
            return new UnlockRule(UnlockType.STARTER, null, 0, 0);
        }

        public static UnlockRule chapter(ChapterTheme chapter) {
            return new UnlockRule(UnlockType.CHAPTER, chapter, 0, 0);
        }

        public static UnlockRule level(ChapterTheme chapter, int level) {
            return new UnlockRule(UnlockType.LEVEL, chapter, level, 0);
        }

        public static UnlockRule purchase(int purchaseCost) {
            return new UnlockRule(
                    UnlockType.PURCHASE,
                    null,
                    0,
                    purchaseCost
            );
        }

        public boolean isPurchasable() {
            return type == UnlockType.PURCHASE;
        }

        public String description() {
            return switch (type) {
                case STARTER -> "Starter plant";
                case CHAPTER -> "Unlocks with " + chapter.getName();
                case LEVEL -> "Unlocks after " + chapter.getName()
                        + " Level " + level;
                case PURCHASE -> "Purchase-only: "
                        + purchaseCost + " coins";
            };
        }
    }

    public static void register(PlantData data) {
        ALL.put(data.id(), data);
    }

    public static void clear() {
        ALL.clear();
    }

    public static PlantData get(int id) {
        return ALL.get(id);
    }

    public static List<PlantData> getAll() {
        return List.copyOf(ALL.values());
    }

    public static boolean contains(int id) {
        return ALL.containsKey(id);
    }

    public static PlantData getByName(String name) {
        for (PlantData data : ALL.values()) {
            if (data.name().equalsIgnoreCase(name)) {
                return data;
            }
        }
        return null;
    }

    public static PlantData getById(int plantId) {
        for (PlantData data : ALL.values()) {
            if (data.id() == plantId) {
                return data;
            }
        }
        return null;
    }

    public static UnlockRule getUnlockRule(int plantId) {
        UnlockRule rule = UNLOCK_RULES.get(plantId);
        if (rule == null) {
            throw new IllegalArgumentException(
                    "No unlock rule exists for plant id " + plantId + "."
            );
        }
        return rule;
    }

    public static List<Integer> getStarterPlantIds() {
        return plantIdsWithType(UnlockType.STARTER);
    }

    public static List<Integer> getChapterPlantIds(ChapterTheme chapter) {
        return UNLOCK_RULES.entrySet().stream()
                .filter(entry -> entry.getValue().type() == UnlockType.CHAPTER)
                .filter(entry -> entry.getValue().chapter() == chapter)
                .map(Map.Entry::getKey)
                .toList();
    }

    public static List<Integer> getLevelRewardPlantIds(
            ChapterTheme chapter,
            int level
    ) {
        return UNLOCK_RULES.entrySet().stream()
                .filter(entry -> entry.getValue().type() == UnlockType.LEVEL)
                .filter(entry -> entry.getValue().chapter() == chapter)
                .filter(entry -> entry.getValue().level() == level)
                .map(Map.Entry::getKey)
                .toList();
    }

    public static List<Integer> getPurchasablePlantIds() {
        return plantIdsWithType(UnlockType.PURCHASE);
    }

    public static List<Integer> getAdventurePlantIds() {
        List<Integer> result = new ArrayList<>();
        for (Map.Entry<Integer, UnlockRule> entry
                : UNLOCK_RULES.entrySet()) {
            if (!entry.getValue().isPurchasable()) {
                result.add(entry.getKey());
            }
        }
        return List.copyOf(result);
    }

    private static List<Integer> plantIdsWithType(UnlockType type) {
        return UNLOCK_RULES.entrySet().stream()
                .filter(entry -> entry.getValue().type() == type)
                .map(Map.Entry::getKey)
                .toList();
    }

    private static Map<Integer, UnlockRule> createUnlockRules() {
        Map<Integer, UnlockRule> rules = new LinkedHashMap<>();

        addStarterPlants(rules);
        addAncientEgyptPlants(rules);
        addFrostbitePlants(rules);
        addBigWaveBeachPlants(rules);
        addDarkAgesPlants(rules);
        addPurchaseOnlyPlants(rules);

        if (rules.size() != REGISTERED_PLANT_COUNT) {
            throw new IllegalStateException(
                    "Expected unlock rules for " + REGISTERED_PLANT_COUNT
                            + " plants, but found " + rules.size() + "."
            );
        }
        return Collections.unmodifiableMap(rules);
    }

    private static void addStarterPlants(Map<Integer, UnlockRule> rules) {
        putAll(rules, UnlockRule.starter(), 1, 6, 25, 30, 44);
    }

    private static void addAncientEgyptPlants(
            Map<Integer, UnlockRule> rules
    ) {
        putAll(rules, level(ChapterTheme.ANCIENT_EGYPT, 1), 38, 60);
        putAll(rules, level(ChapterTheme.ANCIENT_EGYPT, 2), 7, 39);
        putAll(rules, level(ChapterTheme.ANCIENT_EGYPT, 3), 8, 32);
        putAll(rules, level(ChapterTheme.ANCIENT_EGYPT, 4), 2);
    }

    private static void addFrostbitePlants(
            Map<Integer, UnlockRule> rules
    ) {
        putAll(
                rules,
                UnlockRule.chapter(ChapterTheme.FROSTBITE_CAVES),
                59
        );
        putAll(rules, level(ChapterTheme.FROSTBITE_CAVES, 1), 29);
        putAll(rules, level(ChapterTheme.FROSTBITE_CAVES, 2), 31, 45);
        putAll(rules, level(ChapterTheme.FROSTBITE_CAVES, 3), 28);
        putAll(rules, level(ChapterTheme.FROSTBITE_CAVES, 4), 4, 10);
    }

    private static void addBigWaveBeachPlants(
            Map<Integer, UnlockRule> rules
    ) {
        putAll(
                rules,
                UnlockRule.chapter(ChapterTheme.BIG_WAVE_BEACH),
                58
        );
        putAll(rules, level(ChapterTheme.BIG_WAVE_BEACH, 1), 37);
        putAll(rules, level(ChapterTheme.BIG_WAVE_BEACH, 2), 16, 46);
        putAll(rules, level(ChapterTheme.BIG_WAVE_BEACH, 3), 11, 27);
        putAll(rules, level(ChapterTheme.BIG_WAVE_BEACH, 4), 13);
    }

    private static void addDarkAgesPlants(
            Map<Integer, UnlockRule> rules
    ) {
        putAll(
                rules,
                UnlockRule.chapter(ChapterTheme.DARK_AGES),
                3
        );
        putAll(rules, level(ChapterTheme.DARK_AGES, 1), 23, 24);
        putAll(rules, level(ChapterTheme.DARK_AGES, 2), 12, 51);
        putAll(rules, level(ChapterTheme.DARK_AGES, 3), 26, 47);
        putAll(rules, level(ChapterTheme.DARK_AGES, 4), 40, 53);
    }

    private static void addPurchaseOnlyPlants(
            Map<Integer, UnlockRule> rules
    ) {
        UnlockRule purchase = UnlockRule.purchase(DEFAULT_PURCHASE_COST);
        putAll(
                rules,
                purchase,
                5, 9, 14, 15, 17, 18, 19, 20, 21, 22,
                33, 34, 35, 36, 41, 42, 43, 48, 49, 50,
                52, 54, 55, 56, 57,
                61, 62, 63, 64, 65, 66, 67, 68, 69
        );
    }

    private static UnlockRule level(ChapterTheme chapter, int level) {
        return UnlockRule.level(chapter, level);
    }

    private static void putAll(
            Map<Integer, UnlockRule> rules,
            UnlockRule rule,
            int... plantIds
    ) {
        for (int plantId : plantIds) {
            UnlockRule previous = rules.put(plantId, rule);
            if (previous != null) {
                throw new IllegalStateException(
                        "Plant id " + plantId
                                + " has more than one unlock rule."
                );
            }
        }
    }
}
