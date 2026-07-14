package data.loader;

import data.database.ZombieRepository;
import models.zombie.Zombie;

import java.sql.SQLException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;


public class ZombieRegistry {
    private static final Map<String, Zombie> templates = new LinkedHashMap<>();

    public ZombieRegistry(ZombieRepository repository) throws SQLException {
        init(repository.loadAllZombies());
    }

    public static void init(Map<String, Zombie> loaded) {
        templates.clear();
        templates.putAll(loaded);
    }

    public static Zombie getTemplate(String alias) {
        return templates.get(alias);
    }

    public static Zombie spawn(String alias) {
        Zombie template = templates.get(alias);
        if (template == null) {
            throw new IllegalArgumentException("Unknown zombie alias: " + alias);
        }
        return template.copy();
    }

    public static Map<String, Zombie> getTemplates() {
        return Collections.unmodifiableMap(templates);
    }
}
