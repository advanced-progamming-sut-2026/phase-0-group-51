package models.minigames.wallnutBowling;

import lombok.Getter;

@Getter
public enum WallnutType {
    BOWLING("Bowling Walnut"),
    EXPLODE("Explode-O-Nut"),
    BIG_WALLNUT("Giant Walnut");
    private final String name;
    WallnutType(String name) {
        this.name = name;
    }

}
