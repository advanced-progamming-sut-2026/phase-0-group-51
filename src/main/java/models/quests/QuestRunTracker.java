package models.quests;

import Data.loader.PlantData;
import Data.loader.PlantRegistry;
import models.Board.Board;
import models.Board.Tile;
import models.Plant.Plant;
import models.Plant.PlantTag;
import models.Zombie.Zombie;
import models.games.ChapterTheme;
import models.games.TimeOfTheDay;
import models.items.Mower;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Collects facts about one adventure run. It does not access the database. */
public class QuestRunTracker {
    private int firstWaveStartTick = -1;
    private int plantsLost;
    private int plantsPlaced;
    private int explosivePlantsUsed;
    private int sunProducerPlantsUsed;
    private int totalKills;
    private int plantKills;
    private int nonPlantKills;
    private int mowerKills;
    private int firstColumnKillsWithoutMower;
    private boolean usedOnlyNightPlants = true;
    private boolean usedOnlySunProducers = true;

    private final List<Integer> killTicks = new ArrayList<>();
    private final Map<String, Integer> killsByPlantName = new HashMap<>();
    private final Map<String, Integer> killsByFamily = new HashMap<>();
    private final Set<String> usedFamilies = new HashSet<>();
    private final Set<Integer> usedRows = new HashSet<>();
    private final Set<Integer> usedColumns = new HashSet<>();

    public void recordFirstWaveStart(int tick) {
        if (firstWaveStartTick < 0) {
            firstWaveStartTick = tick;
        }
    }

    public void recordPlantPlaced(Plant plant) {
        if (plant == null) {
            return;
        }
        plantsPlaced++;
        usedRows.add(plant.getPosY() + 1);
        usedColumns.add(plant.getPosX() + 1);

        String family = plantFamily(plant);
        usedFamilies.add(normalize(family));

        boolean explosive = family.equalsIgnoreCase("Explosive")
                || plant.hasTag(PlantTag.EXPLOSIVE);
        if (explosive) {
            explosivePlantsUsed++;
        }

        boolean sunProducer = family.equalsIgnoreCase("SunProducer")
                || family.equalsIgnoreCase("Sun Producer")
                || plant.hasTag(PlantTag.SUN);
        if (sunProducer) {
            sunProducerPlantsUsed++;
        } else {
            usedOnlySunProducers = false;
        }

        boolean nightPlant = plant.hasTag(PlantTag.NIGHT) || plant.hasTag(PlantTag.SHROOM);
        if (!nightPlant) {
            usedOnlyNightPlants = false;
        }
    }

    public void recordPlantLost(Plant plant) {
        if (plant != null) {
            plantsLost++;
        }
    }

    public void recordZombieKill(
            Zombie zombie,
            QuestKillSourceType sourceType,
            Plant sourcePlant,
            int tick,
            Mower laneMower
    ) {
        if (zombie == null || !zombie.isQuestEligible()
                || sourceType == QuestKillSourceType.CHEAT) {
            return;
        }
        totalKills++;
        if (sourceType == QuestKillSourceType.PLANT && sourcePlant != null) {
            plantKills++;
            String name = normalize(sourcePlant.getName());
            String family = normalize(plantFamily(sourcePlant));
            killsByPlantName.merge(name, 1, Integer::sum);
            killsByFamily.merge(family, 1, Integer::sum);
        } else {
            nonPlantKills++;
        }
        if (sourceType == QuestKillSourceType.MOWER) {
            mowerKills++;
        }
        if (firstWaveStartTick >= 0) {
            killTicks.add(tick);
        }
        boolean mowerGone = laneMower != null
                && (laneMower.isActivated() || laneMower.isDestroyed());
        if (zombie.getX() >= 0 && zombie.getX() < 1.0f
                && mowerGone && sourceType != QuestKillSourceType.MOWER) {
            firstColumnKillsWithoutMower++;
        }
    }

    public int getPlantsLost() {
        return plantsLost;
    }

    public int getTotalKills() {
        return totalKills;
    }

    public int getPlantKills() {
        return plantKills;
    }

    public int getNonPlantKills() {
        return nonPlantKills;
    }

    public int getMowerKills() {
        return mowerKills;
    }

    public int getFastKills(int seconds, int ticksPerSecond) {
        if (firstWaveStartTick < 0 || seconds <= 0 || ticksPerSecond <= 0) {
            return 0;
        }
        int deadline = firstWaveStartTick + seconds * ticksPerSecond;
        int count = 0;
        for (int tick : killTicks) {
            if (tick < deadline) {
                count++;
            }
        }
        return count;
    }

    public int getExplosivePlantsUsed() {
        return explosivePlantsUsed;
    }

    public int getFirstColumnKillsWithoutMower() {
        return firstColumnKillsWithoutMower;
    }

    public int getKillsByPlantName(String plantName) {
        return killsByPlantName.getOrDefault(normalize(plantName), 0);
    }

    public boolean onlyPlantKillersByName(String plantName) {
        return plantKills > 0 && getKillsByPlantName(plantName) == plantKills;
    }

    public boolean onlyPlantKillersFromFamily(String family) {
        return plantKills > 0
                && killsByFamily.getOrDefault(normalize(family), 0) == plantKills;
    }

    public boolean usedFamily(String family) {
        return usedFamilies.contains(normalize(family));
    }

    public boolean usedOnlyNightPlants() {
        return plantsPlaced > 0 && usedOnlyNightPlants;
    }

    public boolean usedOnlySunProducersExactly(int count) {
        return plantsPlaced == count
                && sunProducerPlantsUsed == count
                && usedOnlySunProducers;
    }

    public boolean neverUsedColumn(int oneBasedColumn) {
        return !usedColumns.contains(oneBasedColumn);
    }

    public boolean neverUsedRow(int oneBasedRow) {
        return !usedRows.contains(oneBasedRow);
    }

    public boolean isDayLevel(ChapterTheme chapter) {
        return chapter != null && chapter.getTimeOfTheDay() == TimeOfTheDay.DAY;
    }

    /** Row 1 mirrors row 5 and row 2 mirrors row 4; the middle row is ignored. */
    public boolean isSymmetric(Board board) {
        if (board == null || countPlants(board) == 0) {
            return false;
        }
        for (int upper = 0; upper < board.getLaneCount() / 2; upper++) {
            int lower = board.getLaneCount() - 1 - upper;
            for (int column = 0; column < board.getColumnCount(); column++) {
                if (!samePlant(board.getTile(upper, column), board.getTile(lower, column))) {
                    return false;
                }
            }
        }
        return true;
    }

    /** The middle row is ignored; at least one off-middle plant must break mirror symmetry. */
    public boolean isAsymmetricExceptMiddle(Board board) {
        if (board == null) {
            return false;
        }
        boolean hasOffMiddlePlant = false;
        for (int upper = 0; upper < board.getLaneCount() / 2; upper++) {
            int lower = board.getLaneCount() - 1 - upper;
            for (int column = 0; column < board.getColumnCount(); column++) {
                if (plantAt(board.getTile(upper, column)) != null
                        || plantAt(board.getTile(lower, column)) != null) {
                    hasOffMiddlePlant = true;
                }
            }
        }
        return hasOffMiddlePlant && !isSymmetric(board);
    }

    private int countPlants(Board board) {
        int count = 0;
        for (int row = 0; row < board.getLaneCount(); row++) {
            for (int column = 0; column < board.getColumnCount(); column++) {
                if (plantAt(board.getTile(row, column)) != null) {
                    count++;
                }
            }
        }
        return count;
    }

    private boolean samePlant(Tile first, Tile second) {
        Plant a = plantAt(first);
        Plant b = plantAt(second);
        if (a == null || b == null) {
            return a == b;
        }
        return a.getName().equalsIgnoreCase(b.getName());
    }

    private Plant plantAt(Tile tile) {
        return tile == null ? null : tile.getPlant();
    }

    private String plantFamily(Plant plant) {
        PlantData data = PlantRegistry.getById(plant.getId());
        if (data != null && data.category() != null && !data.category().isBlank()) {
            return data.category();
        }
        return plant.getPlantType().getClass().getSimpleName();
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT)
                .replace("-", "").replace("_", "").replace(" ", "");
    }
}
