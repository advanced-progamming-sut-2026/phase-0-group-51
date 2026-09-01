package network.protocol.match;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class MatchSnapshot {

    private String matchId;
    private int tick;
    private int ticksPerSecond = 10;
    private int remainingTicks;
    private String status;

    private int plantSun;
    private int zombieSun;

    private Map<String, Integer> plantCooldownTicks = new LinkedHashMap<>();
    private Map<String, Integer> plantCooldownTotalTicks = new LinkedHashMap<>();
    private Map<String, Integer> zombieCooldownTicks = new LinkedHashMap<>();
    private Map<String, Integer> zombieCooldownTotalTicks = new LinkedHashMap<>();

    private List<PlantNetState> plants = new ArrayList<>();
    private List<ZombieNetState> zombies = new ArrayList<>();
    private List<ProjectileNetState> projectiles = new ArrayList<>();
    private List<BrainNetState> brains = new ArrayList<>();

    public MatchSnapshot() {
    }

    public MatchSnapshot(String matchId, int tick, int remainingTicks, String status,
                         int plantSun, int zombieSun,
                         List<PlantNetState> plants, List<ZombieNetState> zombies,
                         List<ProjectileNetState> projectiles, List<BrainNetState> brains) {
        this(
                matchId,
                tick,
                10,
                remainingTicks,
                status,
                plantSun,
                zombieSun,
                new LinkedHashMap<>(),
                new LinkedHashMap<>(),
                new LinkedHashMap<>(),
                new LinkedHashMap<>(),
                plants,
                zombies,
                projectiles,
                brains
        );
    }

    public MatchSnapshot(
            String matchId,
            int tick,
            int ticksPerSecond,
            int remainingTicks,
            String status,
            int plantSun,
            int zombieSun,
            Map<String, Integer> plantCooldownTicks,
            Map<String, Integer> plantCooldownTotalTicks,
            Map<String, Integer> zombieCooldownTicks,
            Map<String, Integer> zombieCooldownTotalTicks,
            List<PlantNetState> plants,
            List<ZombieNetState> zombies,
            List<ProjectileNetState> projectiles,
            List<BrainNetState> brains
    ) {
        this.matchId = matchId;
        this.tick = tick;
        this.ticksPerSecond = Math.max(1, ticksPerSecond);
        this.remainingTicks = remainingTicks;
        this.status = status;
        this.plantSun = plantSun;
        this.zombieSun = zombieSun;
        this.plantCooldownTicks = safeMap(plantCooldownTicks);
        this.plantCooldownTotalTicks = safeMap(plantCooldownTotalTicks);
        this.zombieCooldownTicks = safeMap(zombieCooldownTicks);
        this.zombieCooldownTotalTicks = safeMap(zombieCooldownTotalTicks);
        this.plants = plants == null ? new ArrayList<>() : plants;
        this.zombies = zombies == null ? new ArrayList<>() : zombies;
        this.projectiles = projectiles == null ? new ArrayList<>() : projectiles;
        this.brains = brains == null ? new ArrayList<>() : brains;
    }

    private static Map<String, Integer> safeMap(Map<String, Integer> value) {
        return value == null
                ? new LinkedHashMap<>()
                : new LinkedHashMap<>(value);
    }

    public String getMatchId() { return matchId; }
    public void setMatchId(String matchId) { this.matchId = matchId; }

    public int getTick() { return tick; }
    public void setTick(int tick) { this.tick = tick; }

    public int getTicksPerSecond() { return Math.max(1, ticksPerSecond); }
    public void setTicksPerSecond(int ticksPerSecond) {
        this.ticksPerSecond = Math.max(1, ticksPerSecond);
    }

    public int getRemainingTicks() { return remainingTicks; }
    public void setRemainingTicks(int remainingTicks) { this.remainingTicks = remainingTicks; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public int getPlantSun() { return plantSun; }
    public void setPlantSun(int plantSun) { this.plantSun = plantSun; }

    public int getZombieSun() { return zombieSun; }
    public void setZombieSun(int zombieSun) { this.zombieSun = zombieSun; }

    public Map<String, Integer> getPlantCooldownTicks() {
        return plantCooldownTicks;
    }

    public void setPlantCooldownTicks(Map<String, Integer> plantCooldownTicks) {
        this.plantCooldownTicks = safeMap(plantCooldownTicks);
    }

    public Map<String, Integer> getPlantCooldownTotalTicks() {
        return plantCooldownTotalTicks;
    }

    public void setPlantCooldownTotalTicks(Map<String, Integer> plantCooldownTotalTicks) {
        this.plantCooldownTotalTicks = safeMap(plantCooldownTotalTicks);
    }

    public Map<String, Integer> getZombieCooldownTicks() {
        return zombieCooldownTicks;
    }

    public void setZombieCooldownTicks(Map<String, Integer> zombieCooldownTicks) {
        this.zombieCooldownTicks = safeMap(zombieCooldownTicks);
    }

    public Map<String, Integer> getZombieCooldownTotalTicks() {
        return zombieCooldownTotalTicks;
    }

    public void setZombieCooldownTotalTicks(Map<String, Integer> zombieCooldownTotalTicks) {
        this.zombieCooldownTotalTicks = safeMap(zombieCooldownTotalTicks);
    }

    public List<PlantNetState> getPlants() { return plants; }
    public void setPlants(List<PlantNetState> plants) { this.plants = plants; }

    public List<ZombieNetState> getZombies() { return zombies; }
    public void setZombies(List<ZombieNetState> zombies) { this.zombies = zombies; }

    public List<ProjectileNetState> getProjectiles() { return projectiles; }
    public void setProjectiles(List<ProjectileNetState> projectiles) { this.projectiles = projectiles; }

    public List<BrainNetState> getBrains() { return brains; }
    public void setBrains(List<BrainNetState> brains) { this.brains = brains; }
}
