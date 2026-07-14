package data.loader;

import models.plant.PlantTag;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public final class PlantLoader {
    private PlantLoader() {}

    public static void load() {
        PlantRegistry.clear();
        loadPlants("/plants.json");
    }

    private static void loadPlants(String path) {
        JSONArray arr = readArray(path);
        for (int i = 0; i < arr.length(); i++) {
            JSONObject obj = arr.getJSONObject(i);
            PlantRegistry.register(new PlantData(
                    obj.getInt("id"), obj.getString("name"), obj.getString("category"),
                    parseTags(obj.optJSONArray("tags")), obj.getInt("cost"), obj.getInt("baseHp"),
                    obj.getInt("damage"), obj.optString("damageExpression", String.valueOf(obj.getInt("damage"))),
                    obj.optString("baseAbility", ""), obj.optString("plantFoodEffect", ""),
                    obj.getDouble("actionInterval"), obj.getDouble("recharge"),
                    obj.optDouble("projectileSpeed", 0.5), obj.optString("lvl2", ""),
                    obj.optString("lvl3", ""), obj.optString("lvl4", ""), parseUpgrades(obj.optJSONArray("upgrades"))
            ));
        }
    }

    private static List<UpgradeData> parseUpgrades(JSONArray arr) {
        List<UpgradeData> result = new ArrayList<>();
        if (arr == null) return result;
        for (int i = 0; i < arr.length(); i++) {
            JSONObject up = arr.getJSONObject(i);
            List<StatModifierData> modifiers = new ArrayList<>();
            JSONArray mods = up.optJSONArray("modifiers");
            if (mods != null) {
                for (int j = 0; j < mods.length(); j++) {
                    JSONObject m = mods.getJSONObject(j);
                    modifiers.add(new StatModifierData(m.getString("stat"), m.getString("operation"), m.getDouble("value")));
                }
            }
            result.add(new UpgradeData(up.getInt("level"), up.optString("description", ""), modifiers));
        }
        return result;
    }

    private static List<PlantTag> parseTags(JSONArray arr) {
        List<PlantTag> tags = new ArrayList<>();
        if (arr == null) return tags;
        for (int i = 0; i < arr.length(); i++) {
            String value = arr.getString(i).trim().toUpperCase().replace('-', '_').replace(' ', '_');
            try { tags.add(PlantTag.valueOf(value)); }
            catch (IllegalArgumentException e) { throw new IllegalStateException("Unknown plant tag: " + value, e); }
        }
        return tags;
    }

    private static JSONArray readArray(String path) {
        try (InputStream is = PlantLoader.class.getResourceAsStream(path)) {
            if (is == null) throw new IllegalStateException("Missing classpath resource " + path);
            return new JSONArray(new String(is.readAllBytes(), StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new RuntimeException("Failed to load " + path, e);
        }
    }
}
