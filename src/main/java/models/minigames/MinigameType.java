package models.minigames;

import lombok.Getter;

@Getter
public enum MinigameType {
    VASEBREAKER("Vasebreaker"),
    WALLNUT_BOWLING("Wall-nut Bowling"),
    IZOMBIE("I, Zombie"),
    BEGHOULDED("Beghouled"),
    ZOMBOTANY("Zombotany");
    private final String displayName;
    MinigameType(String displayName) {
        this.displayName = displayName;
    }

}
