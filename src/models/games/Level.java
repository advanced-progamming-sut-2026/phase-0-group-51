package models.games;

import models.games.LevelType;

public record Level(
        int levelNumber,
        LevelType type
) {}