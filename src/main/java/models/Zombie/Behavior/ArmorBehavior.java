package models.Zombie.Behavior;

import models.Zombie.Zombie;

import java.util.HashMap;
import java.util.Map;

public class ArmorBehavior implements ZombieBehavior {
    private final Map<ArmorType, Integer> DEFAULT_HP = new HashMap<>();
    private final ArmorType armorType;
    private int  armorHP;
    private boolean destroyed = false;

    public ArmorBehavior(ArmorType armorType) {
        this.armorType = armorType;
    }



    @Override
    public void onTick(Zombie zombie) {};

    @Override
    public int onHit(Zombie zombie, int rawDamage) {
        return 0;
    }
    public boolean isDestroyed()    { return destroyed; }
    public ArmorType getArmorType() { return armorType; }






    public enum ArmorType {
        CONE, BUCKET, BRICK, ICE_BLOCK, SHOULDER_CROWN,
        SARCOPHAGUS, SURFBOARD, KNIGHT_SHIELD
    }
}
