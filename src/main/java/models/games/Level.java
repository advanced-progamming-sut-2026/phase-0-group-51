package models.games;

import models.games.LevelType;
import models.items.Wave;

import java.util.List;

public record Level(
        int levelNumber,
       LevelType type,
        List<Wave> waves
) {}