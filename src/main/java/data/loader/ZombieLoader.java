package data.loader;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import models.zombie.ArmorDefinition;
import models.zombie.Behavior.ZombieBehavior;
import models.zombie.Behavior.ZombieBehaviorFactory;
import models.zombie.Zombie;

import java.io.File;
import java.util.*;

public class ZombieLoader {

    private final Map<String, ArmorDefinition> armorRegistry = new HashMap<>();

    public void loadArmors(String jsonPath) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(new File(jsonPath));

        for (JsonNode entry : root) {
            String alias   = entry.path("aliases").get(0).asText();
            JsonNode d     = entry.path("objdata");

            int     hp         = d.path("BaseHealth").asInt(300);
            boolean metallic   = false;
            boolean passDamage = false;

            for (JsonNode flag : d.path("ArmorFlags")) {
                if (flag.asText().equals("metallic"))   metallic   = true;
                if (flag.asText().equals("passdamage")) passDamage = true;
            }

            List<Float> thresholds = new ArrayList<>();
            for (JsonNode t : d.path("ArmorLayerHealth"))
                thresholds.add((float) t.asDouble());

            ArmorDefinition def = new ArmorDefinition(alias, hp, metallic, passDamage, thresholds);
            armorRegistry.put(alias, def);
            armorRegistry.put(alias + "@ArmorTypes", def);
        }
    }

    public Map<String, Zombie> loadZombies(String jsonPath) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(new File(jsonPath));

        Map<String, Zombie> result = new LinkedHashMap<>();

        for (JsonNode entry : root) {
            String alias    = entry.path("aliases").get(0).asText();
            String objclass = entry.path("objclass").asText();
            JsonNode d      = entry.path("objdata");

            Zombie zombie = new Zombie(
                alias,
                (float) d.path("Hitpoints").asDouble(190),
                (float) d.path("Speed").asDouble(0.185),
                (float) d.path("EatDPS").asDouble(100),
                (float) d.path("WavePointCost").asDouble(100),
                d.path("Weight").asInt(1000)
            );

            for (ZombieBehavior behavior : ZombieBehaviorFactory.fromJson(alias, objclass, d, armorRegistry)) {
                zombie.addBehavior(behavior);
            }

            result.put(alias, zombie);
        }

        return result;
    }

    public Map<String, ArmorDefinition> getArmorRegistry() {
        return armorRegistry;
    }
}
