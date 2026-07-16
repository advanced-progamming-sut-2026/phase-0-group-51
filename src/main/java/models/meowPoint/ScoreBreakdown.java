package models.meowPoint;

public record ScoreBreakdown (int zombieValuePoints,
  int quickKillBonus, int simultaneousKillBonus, int fastWaveBonus,
  int gardenPreservationBonus, int total, boolean won
) {
    public String format() {
        return "=== MEOWPOINT RESULT =====\n"
                + "Zombie value points: " + zombieValuePoints + "\n"
                + "Quick-kill bonus: " + quickKillBonus + "\n"
                + "Simultaneous-kill bonus: " + simultaneousKillBonus + "\n"
                + "Fast-wave bonus: " + fastWaveBonus + "\n"
                + "Garden-preservation bonus: "
                + gardenPreservationBonus + "\n"
                + "Result: " + (won ? "WIN" : "LOSS") + "\n"
                + "Total MeowPoint: " + total + "\n";
    }
}
