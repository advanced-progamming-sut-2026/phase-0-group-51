package models.Plant;

import models.Zombie.Zombie;
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
    MOVEZOMBIES{},
    SUN{},
    EXPLOSIVE{};
    public void onTakeDamage(Plant plant, GameState state){}
    public void onTick(Plant plant, GameState state){}
}
