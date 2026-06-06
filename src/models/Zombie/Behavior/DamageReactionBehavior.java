package models.Zombie.Behavior;

import models.Zombie.Zombie;

public class DamageReactionBehavior implements ZombieBehavior {
    private final DamageReactionType reactionType;
    private boolean reacted = false;   // یه فلگ برا اینکه بدونیم اون ریکشن یبار اتفاق افتاده یا نه

    public DamageReactionBehavior(DamageReactionType type) {
        this.reactionType = type;
    }


    @Override
    public void onTick(Zombie zombie) {}

    @Override
    public int onHit(Zombie zombie, int rawDamage) {return 0;}



    public enum DamageReactionType {
        REFLECT_PROJECTILE,  // Juggler → reflects incoming projectile
        SUBMERGE_DODGE,      // Snorkel → ducks underwater, only takes damage when eating
        BOARD_ABSORB,        // Surfer  → surfboard absorbs first hits (shared with ArmorBehavior)
        ICE_CHILL_ATTACKER,  // IceBlock → chills plant that attacks it
        NEWSPAPER_RAGE,      // Newspaper → speed boost when newspaper destroyed
        PHARAOH_SPEED_BOOST  // Pharaoh → runs faster when sarcophagus broken
    }

}
