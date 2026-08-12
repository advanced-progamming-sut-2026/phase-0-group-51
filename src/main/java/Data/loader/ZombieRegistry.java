package Data.loader;

import Data.database.ZombieRepository;
import models.Zombie.Zombie;

import java.sql.SQLException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


public class ZombieRegistry {
    private static final Map<String, Zombie> TEMPLATES = new LinkedHashMap<>();
    private static final Map<String, String> CARD_ASSETS = new LinkedHashMap<>();
    private static final Map<String, String> IDLE_PAM_PATHS = new LinkedHashMap<>();
    private static final Map<String, String> IDLE_CLIPS = new LinkedHashMap<>();
    private static final Map<String, List<String>> IDLE_VISIBLE_PARTS = new LinkedHashMap<>();
    private static final Map<String, String> WALK_CLIPS = new LinkedHashMap<>();
    private static final Map<String, String> TOUGHNESS = new LinkedHashMap<>();
    private static final Map<String, String> SPEED = new LinkedHashMap<>();
    private static final Map<String, String> OVERALL_DISC = new LinkedHashMap<>();
    private static final Map<String, String> FUN_DISC = new LinkedHashMap<>();
    private static final Map<String, String> DAMAGE = new LinkedHashMap<>();
    private static final Map<String, String> WEAKNESS = new LinkedHashMap<>();
    private static final Map<String, String> SPECIAL = new LinkedHashMap<>();

    public ZombieRegistry(ZombieRepository repository) throws SQLException {
        init(repository.loadAllZombies());
    }

    /**
     * Populates the registry straight from the bundled JSON resources.
     * Must be called once at startup (see App's constructor) - nothing else
     * currently seeds this registry, so without this call every lookup in
     * getTemplate()/spawn() silently returns null / throws, and
     * ZombieWaveManager never spawns any zombies.
     */
    public static void load() {
        try {
            ZombieLoader loader = new ZombieLoader();
            loader.loadArmors("/ArmorTypeData.json");
            Map<String, Zombie> loaded = loader.loadZombies("/zombies.json");
            init(loaded);
            CARD_ASSETS.clear();
            CARD_ASSETS.putAll(loader.getZombieCardAssets());
            IDLE_PAM_PATHS.clear();
            IDLE_PAM_PATHS.putAll(loader.getZombieIdlePamPaths());
            IDLE_CLIPS.clear();
            IDLE_CLIPS.putAll(loader.getZombieIdleClips());
            IDLE_VISIBLE_PARTS.clear();
            IDLE_VISIBLE_PARTS.putAll(loader.getZombieIdleVisibleParts());
            WALK_CLIPS.clear();
            WALK_CLIPS.putAll(loader.getZombieWalkClips());
            TOUGHNESS.clear();
            TOUGHNESS.putAll(loader.getZombieToughness());
            SPEED.clear();
            SPEED.putAll(loader.getZombieSpeed());
            OVERALL_DISC.clear();
            OVERALL_DISC.putAll(loader.getZombieOverallDisc());
            FUN_DISC.clear();
            FUN_DISC.putAll(loader.getZombieFunDisc());
            DAMAGE.clear();
            DAMAGE.putAll(loader.getZombieDamage());
            WEAKNESS.clear();
            WEAKNESS.putAll(loader.getZombieWeakness());
            SPECIAL.clear();
            SPECIAL.putAll(loader.getZombieSpecial());
        } catch (Exception e) {
            throw new RuntimeException("Failed to load zombies.json", e);
        }
    }

    public static void init(Map<String, Zombie> loaded) {
        TEMPLATES.clear();
        TEMPLATES.putAll(loaded);
    }

    public static Zombie getTemplate(String alias) {
        return TEMPLATES.get(alias);
    }

    public static Zombie spawn(String alias) {
        Zombie template = TEMPLATES.get(alias);
        if (template == null) {
            throw new IllegalArgumentException("Unknown zombie alias: " + alias);
        }
        return template.copy();
    }
    public static String getCardAssetId(String alias) {
        return CARD_ASSETS.get(alias);
    }
    public static String getIdlePamPath(String alias) {
        return IDLE_PAM_PATHS.get(alias);
    }
    public static String getIdleClip(String alias) {
        return IDLE_CLIPS.getOrDefault(alias, "idle");
    }
    public static String getWalkClip(String alias) {
        return WALK_CLIPS.getOrDefault(alias, "walk");
    }

    public static List<String> getIdleVisibleParts(String alias) {
        return IDLE_VISIBLE_PARTS.getOrDefault(alias, List.of());
    }
    public static String getToughness(String alias) {
        return TOUGHNESS.getOrDefault(alias, "");
    }
    public static String getSpeed(String alias) {
        return SPEED.getOrDefault(alias, "");
    }
    public static String getOverallDisc(String alias) {
        return OVERALL_DISC.getOrDefault(alias, "");
    }
    public static String getFunDisc(String alias) {
        return FUN_DISC.getOrDefault(alias, "");
    }
    public static String getDamage(String alias) {
        return DAMAGE.getOrDefault(alias, "");
    }
    public static String getWeakness(String alias) {
        return WEAKNESS.getOrDefault(alias, "");
    }
    public static String getSpecial(String alias) {
        return SPECIAL.getOrDefault(alias, "");
    }

    public static Map<String, Zombie> getTemplates() {
        return Collections.unmodifiableMap(TEMPLATES);
    }
}
