package models.Board;

import models.Plant.Plant;
import models.Zombie.Zombie;
import models.projectile.Projectile;

import java.util.ArrayList;
import java.util.List;

public class Board {
    private final int laneCount;
    private final int columnCount;

    private final List<Plant> plants;
    private final List<Zombie> zombies;
    private final List<Projectile> projectiles;

    public Board(int laneCount, int columnCount) {
        this.laneCount    = laneCount;
        this.columnCount  = columnCount;
        this.plants       = new ArrayList<>();
        this.zombies      = new ArrayList<>();
        this.projectiles  = new ArrayList<>();
    }
}
