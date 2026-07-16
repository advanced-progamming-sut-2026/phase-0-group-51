package models.meowPoint;

import models.Zombie.ZombieType;
import models.games.Level;
import models.games.LevelType;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

public class ScoringRules {
    public static final int FIXED_DIFFICULTY = 3;
    public static final int ZOMBIE_BASE_POINTS = 50;
    public static final int QUICK_KILL_MAX_BONUS = 300;
    public static final int QUICK_KILL_WINDOW_SECONDS = 30;
    public static final int SAME_TICK_EXTRA_KILL_BONUS = 100;
    public static final int FAST_WAVE_MAX_BONUS = 500;
    public static final int FAST_WAVE_WINDOW_SECONDS = 60;
    public static final int WIN_BONUS = 500;
    public static final int UNUSED_MOWER_BONUS = 200;
    public static final int SURVIVING_PLANT_BONUS = 25;
    public static final int SUN_DIVISOR = 5;

    private ScoringRules() {
    }

    public static LocalDate currentDate() {
        return LocalDate.now();
    }
    public static long dailyZombieSeed(LocalDate date) {
        if (date == null) {
            throw new IllegalArgumentException("Date cannot be null.");
        }
        return date.toEpochDay() * 100L + 1L;
    }
    public static long dailySunSeed(LocalDate date) {
        if (date == null) {
            throw new IllegalArgumentException("Date cannot be null.");
        }
        return date.toEpochDay() * 100L + 2L;
    }
    public static int dateKey(LocalDate date) {
        return date.getYear() * 10_000
                + date.getMonthValue() * 100
                + date.getDayOfMonth();
    }

    public static Level dailyLevel() {
        return new Level(
                1,
                LevelType.NORMAL,
                5,
                1000f,
                100,
                List.of(
                        ZombieType.DEFAULT,
                        ZombieType.ARMOR_1,
                        ZombieType.ARMOR_2,
                        ZombieType.ARMOR_4,
                        ZombieType.IMP,
                        ZombieType.RA,
                        ZombieType.EXPLORER,
                        ZombieType.TOMB_RAISER
                )
        );
    }

    public static String describe(LocalDate date) {
        return "===== SCORING GAME RULES =====\n"
                + "Daily challenge : " + date + "\n"
                + "1. Zombie value: 50 + rounded zombie wave cost.\n"
                + "2. Quick kill: up to 300 points, falling to zero after 30 seconds.\n"
                + "3. Simultaneous kills: every extra kill in one tick adds 100 x previous kills.\n"
                + "4. Fast wave: up to 500 points for clearing a wave; zero after 60 seconds.\n"
                + "5. Garden preservation after a win: 500 + 200 per unused mower"
                + " + 25 per surviving plant + final sun / 5.\n";
    }

}
