package models.items;

import models.Zombie.Zombie;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public class Wave {
    private final int waveNumber;
    private final float difficulty;
    private final boolean finalWave;
    private final List<Zombie> zombies = new ArrayList<>();
    private int initialTotalHealth = 0;

    public Wave(int waveNumber, float difficulty, boolean finalWave) {
        this.waveNumber = waveNumber;
        this.difficulty = difficulty;
        this.finalWave = finalWave;
    }

    public void addZombie(Zombie zombie) {
        zombies.add(zombie);
        initialTotalHealth += zombie.getMaxHitpoints();
    }

    public int remainingHealth() {
        int sum = 0;
        for (Zombie zombie : zombies) {
            if (!zombie.isDead()) {
                sum += zombie.getHitpoints();
            }
        }
        return sum;
    }

    //when 75% is gone
    public boolean isBroken() {
        return initialTotalHealth == 0
            || remainingHealth() <= initialTotalHealth * 0.25f;
    }

    public boolean allDead() {
        for (Zombie zombie : zombies) {
            if (!zombie.isDead()) {
                return false;
            }
        }
        return true;
    }

    public int getWaveNumber() {
        return waveNumber;
    }

    public float getDifficulty() {
        return difficulty;
    }

    public boolean isFinalWave() {
        return finalWave;
    }

    public List<Zombie> getZombies() {
        return Collections.unmodifiableList(zombies);
    }

    public int getInitialTotalHealth() {
        return initialTotalHealth;
    }
}
