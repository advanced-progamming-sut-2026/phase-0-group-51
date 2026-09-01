package network.protocol.match;

import java.util.ArrayList;
import java.util.List;

public class MatchSnapshot {

    private String matchId;
    private int tick;
    private int remainingTicks;
    private String status;

    private int plantSun;
    private int zombieSun;

    private List<PlantNetState> plants = new ArrayList<>();
    private List<ZombieNetState> zombies = new ArrayList<>();
    private List<ProjectileNetState> projectiles = new ArrayList<>();
    private List<BrainNetState> brains = new ArrayList<>();
    private List<VisualEffectNetState> visualEffects = new ArrayList<>();

    public MatchSnapshot() {
    }

    public MatchSnapshot(String matchId, int tick, int remainingTicks, String status,
                         int plantSun, int zombieSun,
                         List<PlantNetState> plants, List<ZombieNetState> zombies,
                         List<ProjectileNetState> projectiles, List<BrainNetState> brains) {
        this(matchId, tick, remainingTicks, status, plantSun, zombieSun,
            plants, zombies, projectiles, brains, new ArrayList<>());
    }

    public MatchSnapshot(String matchId, int tick, int remainingTicks, String status,
                         int plantSun, int zombieSun,
                         List<PlantNetState> plants, List<ZombieNetState> zombies,
                         List<ProjectileNetState> projectiles, List<BrainNetState> brains,
                         List<VisualEffectNetState> visualEffects) {
        this.matchId = matchId;
        this.tick = tick;
        this.remainingTicks = remainingTicks;
        this.status = status;
        this.plantSun = plantSun;
        this.zombieSun = zombieSun;
        this.plants = plants;
        this.zombies = zombies;
        this.projectiles = projectiles;
        this.brains = brains;
        this.visualEffects = visualEffects;
    }

    public String getMatchId() { return matchId; }
    public void setMatchId(String matchId) { this.matchId = matchId; }

    public int getTick() { return tick; }
    public void setTick(int tick) { this.tick = tick; }

    public int getRemainingTicks() { return remainingTicks; }
    public void setRemainingTicks(int remainingTicks) { this.remainingTicks = remainingTicks; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public int getPlantSun() { return plantSun; }
    public void setPlantSun(int plantSun) { this.plantSun = plantSun; }

    public int getZombieSun() { return zombieSun; }
    public void setZombieSun(int zombieSun) { this.zombieSun = zombieSun; }

    public List<PlantNetState> getPlants() { return plants; }
    public void setPlants(List<PlantNetState> plants) { this.plants = plants; }

    public List<ZombieNetState> getZombies() { return zombies; }
    public void setZombies(List<ZombieNetState> zombies) { this.zombies = zombies; }

    public List<ProjectileNetState> getProjectiles() { return projectiles; }
    public void setProjectiles(List<ProjectileNetState> projectiles) { this.projectiles = projectiles; }

    public List<BrainNetState> getBrains() { return brains; }
    public void setBrains(List<BrainNetState> brains) { this.brains = brains; }

    public List<VisualEffectNetState> getVisualEffects() { return visualEffects; }
    public void setVisualEffects(List<VisualEffectNetState> visualEffects) { this.visualEffects = visualEffects; }
}
