package network.protocol.quests;

import models.games.ChapterTheme;
import models.games.GameState;
import models.quests.QuestRunTracker;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class QuestRunSummary {
    private ChapterTheme chapter;
    private int difficultyLevel;
    private boolean won;
    private int sun;
    private int ticksPerSecond;
    private int firstWaveStartTick;
    private int plantsLost;
    private int plantsPlaced;
    private int explosivePlantsUsed;
    private int sunProducerPlantsUsed;
    private int totalKills;
    private int plantKills;
    private int nonPlantKills;
    private int mowerKills;
    private int firstColumnKillsWithoutMower;
    private boolean usedOnlyNightPlants;
    private boolean usedOnlySunProducers;
    private boolean symmetric;
    private boolean asymmetricExceptMiddle;
    private List<Integer> killTicks = new ArrayList<>();
    private Map<String, Integer> killsByPlantName = new HashMap<>();
    private Map<String, Integer> killsByFamily = new HashMap<>();
    private Set<String> usedFamilies = new HashSet<>();
    private Set<Integer> usedRows = new HashSet<>();
    private Set<Integer> usedColumns = new HashSet<>();

    public QuestRunSummary() {
    }

    public static QuestRunSummary from(
            GameState state,
            ChapterTheme chapter,
            int difficultyLevel,
            boolean won
    ) {
        if (state == null) {
            throw new IllegalArgumentException("Game state is required.");
        }

        QuestRunTracker tracker = state.getQuestTracker();
        QuestRunSummary summary = new QuestRunSummary();
        summary.chapter = chapter;
        summary.difficultyLevel = difficultyLevel;
        summary.won = won;
        summary.sun = state.getSun();
        summary.ticksPerSecond = state.getTicksPerSecond();
        summary.firstWaveStartTick = tracker.getFirstWaveStartTick();
        summary.plantsLost = tracker.getPlantsLost();
        summary.plantsPlaced = tracker.getPlantsPlaced();
        summary.explosivePlantsUsed = tracker.getExplosivePlantsUsed();
        summary.sunProducerPlantsUsed = tracker.getSunProducerPlantsUsed();
        summary.totalKills = tracker.getTotalKills();
        summary.plantKills = tracker.getPlantKills();
        summary.nonPlantKills = tracker.getNonPlantKills();
        summary.mowerKills = tracker.getMowerKills();
        summary.firstColumnKillsWithoutMower = tracker.getFirstColumnKillsWithoutMower();
        summary.usedOnlyNightPlants = tracker.usedOnlyNightPlants();
        summary.usedOnlySunProducers = tracker.usedOnlySunProducersExactly(
                Math.max(1, tracker.getPlantsPlaced())
        );
        summary.symmetric = tracker.isSymmetric(state.getBoard());
        summary.asymmetricExceptMiddle = tracker.isAsymmetricExceptMiddle(state.getBoard());
        summary.killTicks = new ArrayList<>(tracker.getKillTicks());
        summary.killsByPlantName = new HashMap<>(tracker.getKillsByPlantName());
        summary.killsByFamily = new HashMap<>(tracker.getKillsByFamily());
        summary.usedFamilies = new HashSet<>(tracker.getUsedFamilies());
        summary.usedRows = new HashSet<>(tracker.getUsedRows());
        summary.usedColumns = new HashSet<>(tracker.getUsedColumns());
        return summary;
    }

    public ChapterTheme getChapter() { return chapter; }
    public void setChapter(ChapterTheme chapter) { this.chapter = chapter; }
    public int getDifficultyLevel() { return difficultyLevel; }
    public void setDifficultyLevel(int difficultyLevel) { this.difficultyLevel = difficultyLevel; }
    public boolean isWon() { return won; }
    public void setWon(boolean won) { this.won = won; }
    public int getSun() { return sun; }
    public void setSun(int sun) { this.sun = sun; }
    public int getTicksPerSecond() { return ticksPerSecond; }
    public void setTicksPerSecond(int ticksPerSecond) { this.ticksPerSecond = ticksPerSecond; }
    public int getFirstWaveStartTick() { return firstWaveStartTick; }
    public void setFirstWaveStartTick(int firstWaveStartTick) { this.firstWaveStartTick = firstWaveStartTick; }
    public int getPlantsLost() { return plantsLost; }
    public void setPlantsLost(int plantsLost) { this.plantsLost = plantsLost; }
    public int getPlantsPlaced() { return plantsPlaced; }
    public void setPlantsPlaced(int plantsPlaced) { this.plantsPlaced = plantsPlaced; }
    public int getExplosivePlantsUsed() { return explosivePlantsUsed; }
    public void setExplosivePlantsUsed(int explosivePlantsUsed) { this.explosivePlantsUsed = explosivePlantsUsed; }
    public int getSunProducerPlantsUsed() { return sunProducerPlantsUsed; }
    public void setSunProducerPlantsUsed(int sunProducerPlantsUsed) { this.sunProducerPlantsUsed = sunProducerPlantsUsed; }
    public int getTotalKills() { return totalKills; }
    public void setTotalKills(int totalKills) { this.totalKills = totalKills; }
    public int getPlantKills() { return plantKills; }
    public void setPlantKills(int plantKills) { this.plantKills = plantKills; }
    public int getNonPlantKills() { return nonPlantKills; }
    public void setNonPlantKills(int nonPlantKills) { this.nonPlantKills = nonPlantKills; }
    public int getMowerKills() { return mowerKills; }
    public void setMowerKills(int mowerKills) { this.mowerKills = mowerKills; }
    public int getFirstColumnKillsWithoutMower() { return firstColumnKillsWithoutMower; }
    public void setFirstColumnKillsWithoutMower(int value) { this.firstColumnKillsWithoutMower = value; }
    public boolean isUsedOnlyNightPlants() { return usedOnlyNightPlants; }
    public void setUsedOnlyNightPlants(boolean usedOnlyNightPlants) { this.usedOnlyNightPlants = usedOnlyNightPlants; }
    public boolean isUsedOnlySunProducers() { return usedOnlySunProducers; }
    public void setUsedOnlySunProducers(boolean usedOnlySunProducers) { this.usedOnlySunProducers = usedOnlySunProducers; }
    public boolean isSymmetric() { return symmetric; }
    public void setSymmetric(boolean symmetric) { this.symmetric = symmetric; }
    public boolean isAsymmetricExceptMiddle() { return asymmetricExceptMiddle; }
    public void setAsymmetricExceptMiddle(boolean asymmetricExceptMiddle) { this.asymmetricExceptMiddle = asymmetricExceptMiddle; }
    public List<Integer> getKillTicks() { return killTicks; }
    public void setKillTicks(List<Integer> killTicks) { this.killTicks = killTicks == null ? new ArrayList<>() : new ArrayList<>(killTicks); }
    public Map<String, Integer> getKillsByPlantName() { return killsByPlantName; }
    public void setKillsByPlantName(Map<String, Integer> value) { this.killsByPlantName = value == null ? new HashMap<>() : new HashMap<>(value); }
    public Map<String, Integer> getKillsByFamily() { return killsByFamily; }
    public void setKillsByFamily(Map<String, Integer> value) { this.killsByFamily = value == null ? new HashMap<>() : new HashMap<>(value); }
    public Set<String> getUsedFamilies() { return usedFamilies; }
    public void setUsedFamilies(Set<String> value) { this.usedFamilies = value == null ? new HashSet<>() : new HashSet<>(value); }
    public Set<Integer> getUsedRows() { return usedRows; }
    public void setUsedRows(Set<Integer> value) { this.usedRows = value == null ? new HashSet<>() : new HashSet<>(value); }
    public Set<Integer> getUsedColumns() { return usedColumns; }
    public void setUsedColumns(Set<Integer> value) { this.usedColumns = value == null ? new HashSet<>() : new HashSet<>(value); }
}
