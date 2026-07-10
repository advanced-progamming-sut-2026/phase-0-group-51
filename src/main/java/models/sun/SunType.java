package models.sun;

import lombok.Getter;
import lombok.Setter;

@Getter
public enum SunType {
    ORDINARY(25, 300),
    SPECIAL(100, 300),
    RADIOACTIVE(25, 150);

    private final int amount;
    private final int lifeSeconds;

    SunType(int amount,int lifeSeconds){
        this.amount = amount;
        this.lifeSeconds = lifeSeconds;
    }

    public int getLifeTicks(){
        return lifeSeconds * 10;
    }
}
