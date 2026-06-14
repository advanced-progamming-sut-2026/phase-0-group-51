package Data.loader;

import Data.database.ZombieRepository;
import models.Zombie.Zombie;

import java.sql.SQLException;
import java.util.Map;

public class ZombieRegistry {
    private final Map<String, Zombie> templates;

    public ZombieRegistry(ZombieRepository repository) throws SQLException {
        this.templates = repository.loadAllZombies();
    }

    public Zombie getTemplate(String alias) {
        Zombie zombie = templates.get(alias);

        if (zombie == null) {
            throw new IllegalArgumentException("Unknown zombie alias: " + alias);
        }

        return zombie;
    }
}
