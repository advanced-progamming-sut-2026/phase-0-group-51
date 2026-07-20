package Data.loader;

import Data.database.ZombieRepository;
import models.Zombie.Zombie;

import java.sql.SQLException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;


public class ZombieRegistry {
    private static final Map<String, Zombie> TEMPLATES = new LinkedHashMap<>();

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

    public static Map<String, Zombie> getTemplates() {
        return Collections.unmodifiableMap(TEMPLATES);
    }
}
