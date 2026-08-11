package Data.loader;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import models.Zombie.ArmorDefinition;
import models.Zombie.Behavior.ZombieBehavior;
import models.Zombie.Behavior.ZombieBehaviorFactory;
import models.Zombie.Zombie;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

public class ZombieLoader {

    private final Map<String, ArmorDefinition> armorRegistry = new HashMap<>();
    private final Map<String, String> zombieCardAssets = new LinkedHashMap<>();
    private final Map<String, String> zombieIdlePamPaths = new LinkedHashMap<>();
    private final Map<String, String> zombieIdleClips = new LinkedHashMap<>();
    private final Map<String, List<String>> zombieIdleVisibleParts = new LinkedHashMap<>();
    private final Map<String, String> zombieWalkClips = new LinkedHashMap<>();
    private final Map<String, String> zombieToughness = new LinkedHashMap<>();
    private final Map<String, String> zombieSpeed = new LinkedHashMap<>();
    private final Map<String, String> zombieOverallDisc = new LinkedHashMap<>();
    private final Map<String, String> zombieFunDisc = new LinkedHashMap<>();
    private final Map<String, String> zombieDamage = new LinkedHashMap<>();
    private final Map<String, String> zombieWeakness = new LinkedHashMap<>();
    private final Map<String, String> zombieSpecial = new LinkedHashMap<>();

    private static JsonNode readResourceTree(String classpathResource) throws IOException {
        try (InputStream is = ZombieLoader.class.getResourceAsStream(classpathResource)) {
            if (is == null) {
                throw new IllegalStateException("Missing classpath resource " + classpathResource);
            }
            return new ObjectMapper().readTree(is);
        }
    }

    public void loadArmors(String jsonPath) throws Exception {
        JsonNode root = readResourceTree(jsonPath);

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
        JsonNode root = readResourceTree(jsonPath);

        Map<String, Zombie> result = new LinkedHashMap<>();

        for (JsonNode entry : root) {
            String alias    = entry.path("aliases").get(0).asText();
            String objclass = entry.path("objclass").asText();
            JsonNode d      = entry.path("objdata");
            String cardAssetId = entry.path("cardAssetId").asText();
            String idlePamPath = entry.path("idlePamPath").asText("");
            String idleClip = entry.path("idleClip").asText("idle");
            String walkClip = entry.path("walkClip").asText("walk");
            String toughness = entry.path("toughness").asText("");
            String speed = entry.path("speed").asText("");
            String overallDisc = entry.path("overallDisc").asText("");
            String funDisc = entry.path("funDisc").asText("");
            String damage = entry.path("damage").asText("");
            String weakness = entry.path("weakness").asText("");
            String special = entry.path("special").asText("");

            if (cardAssetId.isBlank()) {
                throw new IllegalStateException("Missing cardAssetId for zombie: " + alias);
            }
            if (idlePamPath.isBlank()) {
                throw new IllegalStateException("Missing idlePamPath for zombie: " + alias);
            }
            List<String> visibleParts = new ArrayList<>();

            JsonNode visiblePartsNode = entry.path("idleVisibleParts");

            if (visiblePartsNode.isArray()) {
                for (JsonNode part : visiblePartsNode) {
                    String address =
                            part.asText("");

                    if (!address.isBlank()) {
                        visibleParts.add(address);
                    }
                }
            }

            zombieCardAssets.put(alias, cardAssetId);
            zombieIdlePamPaths.put(alias, idlePamPath);
            zombieIdleClips.put(alias, idleClip);
            zombieIdleVisibleParts.put(alias, List.copyOf(visibleParts));
            zombieWalkClips.put(alias, walkClip);
            zombieToughness.put(alias, toughness);
            zombieSpeed.put(alias, speed);
            zombieOverallDisc.put(alias, overallDisc);
            zombieFunDisc.put(alias, funDisc);
            zombieDamage.put(alias, damage);
            zombieWeakness.put(alias, weakness);
            zombieSpecial.put(alias, special);

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
    public Map<String, String> getZombieCardAssets() {
        return Collections.unmodifiableMap(zombieCardAssets);
    }
    public Map<String, String> getZombieIdlePamPaths() {
        return Collections.unmodifiableMap(zombieIdlePamPaths);
    }
    public Map<String, String> getZombieIdleClips() {
        return Collections.unmodifiableMap(zombieIdleClips);
    }
    public Map<String, List<String>> getZombieIdleVisibleParts() {
        return Collections.unmodifiableMap(zombieIdleVisibleParts);
    }
    public Map<String, String> getZombieWalkClips() {
        return Collections.unmodifiableMap(zombieWalkClips);
    }
    public Map<String, String> getZombieToughness() {
        return Collections.unmodifiableMap(zombieToughness);
    }
    public Map<String, String> getZombieSpeed() {
        return Collections.unmodifiableMap(zombieSpeed);
    }
    public Map<String, String> getZombieOverallDisc() {
        return Collections.unmodifiableMap(zombieOverallDisc);
    }
    public Map<String, String> getZombieFunDisc() {
        return Collections.unmodifiableMap(zombieFunDisc);
    }
    public Map<String, String> getZombieDamage() {
        return Collections.unmodifiableMap(zombieDamage);
    }
    public Map<String, String> getZombieWeakness() {
        return Collections.unmodifiableMap(zombieWeakness);
    }
    public Map<String, String> getZombieSpecial() {
        return Collections.unmodifiableMap(zombieSpecial);
    }
}
