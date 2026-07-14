package models.Zombie;

public enum ZombieType {
    // Basic / Armored (objclass: ZombiePropertySheet)
    DEFAULT("ZombieDefault"),
    ARMOR_1("ZombieArmor1"),
    ARMOR_2("ZombieArmor2"),
    ARMOR_4("ZombieArmor4"),
    DARK_ARMOR_3("ZombieDarkArmor3"),
    IMP("ZombieImp"),
    DARK_IMP_DRAGON("ZombieDarkImpDragon"),

    // Gargantuar
    GARGANTUAR("ZombieGargantuar"),

    //  Ancient Egypt
    RA("ZombieRa"),
    EXPLORER("ZombieExplorer"),
    TOMB_RAISER("ZombieTombRaiser"),

    // Frostbite Caves
    ICE_AGE_DODO("ZombieIceAgeDodo"),
    ICE_AGE_HUNTER("ZombieIceAgeHunter"),
    ICE_AGE_TROGLOBITE("ZombieIceAgeTroglobite"),

    // Big Wave Beach
    BEACH_FISHERMAN("ZombieBeachFisherman"),
    BEACH_OCTOPUS("ZombieBeachOctopus"),
    BEACH_SNORKEL("ZombieBeachSnorkel"),

    // Dark Ages
    DARK_JUGGLER("ZombieDarkJuggler"),
    WIZARD("ZombieWizard"),
    DARK_KING("ZombieDarkKing"),

    // Modern Day
    MODERN_ALL_STAR("ZombieModernAllStar"),
    LOST_CITY_JANE("ZombieLostCityJane"),
    CRYSTAL_SKULL("ZombieCrystalSkull"),
    PROSPECTOR("ZombieProspector"),
    PIANO("ZombiePiano"),
    NEWSPAPER("ZombieNewspaper"),
    ARCADE("ZombieArcade"),

    // Extra zombies from the project document
    BARREL_ROLLER("ZombieBarrelRoller"),
    TURQUOISE("ZombieTurquoise");

    private final String alias;

    ZombieType(String alias) {
        this.alias = alias;
    }

    public String getAlias() {
        return alias;
    }

    public static ZombieType fromAlias(String alias) {
        for (ZombieType type : values()) {
            if (type.alias.equals(alias)) return type;
        }
        throw new IllegalArgumentException("Unknown zombie alias: " + alias);
    }
}
