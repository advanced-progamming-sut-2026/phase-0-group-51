package Data.loader;

import models.Plant.PlantTag;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class PlantLoader {

    public static void load() {
        loadPlants("/plants.json");
    }

    private static void loadPlants(String path) {
        JSONArray arr = readArray(path);
        for (int i = 0; i < arr.length(); i++) {
            JSONObject obj = arr.getJSONObject(i);
            PlantRegistry.register(new PlantData(
                    obj.getInt("id"),
                    obj.getString("name"),
                    obj.getString("category"),
                    parseTags(obj.getJSONArray("tags")),
                    obj.getInt("cost"),
                    obj.getInt("baseHp"),
                    obj.getInt("damage"),
                    obj.getDouble("actionInterval"),
                    obj.getInt("recharge"),
                    obj.getString("lvl2"),
                    obj.getString("lvl3"),
                    obj.getString("lvl4")
            ));
        }
    }

    private static List<PlantTag> parseTags(JSONArray arr) {
        List<PlantTag> tags = new ArrayList<>();
        for (int i = 0; i < arr.length(); i++) {
            try {
                tags.add(PlantTag.valueOf(arr.getString(i)));
            } catch (IllegalArgumentException e) {
                System.out.println("Unknown tag: " + arr.getString(i));
            }
        }
        return tags;
    }

    private static JSONArray readArray(String path) {
        try {
            InputStream is = PlantLoader.class.getResourceAsStream(path);
            String content = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            return new JSONArray(content);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load " + path, e);
        }
    }


}