package models.plant;

import models.games.GameState;

public enum PlantTag {
    DAY{},
    NIGHT{},
    SHROOM{},
    WRAMP_UP{},
    PEA{},
    ICE{},
    FIRE{},
    STACK{},
    CHARGE{},
    MAGIC{},
    POISON{},
    WATER{},
    AOE{},
    TRAP{},
    MOVE_ZOMBIES{},
    SUN{},
    EXPLOSIVE{};
    public void onTakeDamage(Plant plant, GameState state){}
    public void onTick(Plant plant, GameState state){}
}
