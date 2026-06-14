package Data.loader;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PlantRegistry {
    private static final Map<Integer, PlantData> ALL = new HashMap<>();

    public static void register(PlantData data)  { ALL.put(data.id(), data); }
    public static PlantData get(int id)          { return ALL.get(id); }
    public static List<PlantData> getAll()       { return List.copyOf(ALL.values()); }
    public static boolean contains(int id)       { return ALL.containsKey(id); }
}
