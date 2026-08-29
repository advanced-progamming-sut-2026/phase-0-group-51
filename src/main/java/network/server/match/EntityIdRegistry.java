package network.server.match;

import java.util.IdentityHashMap;
import java.util.Map;


public final class EntityIdRegistry {

    private final Map<Object, Integer> ids = new IdentityHashMap<>();
    private int nextId = 1;

    public int idFor(Object entity) {
        Integer existing = ids.get(entity);
        if (existing != null) {
            return existing;
        }
        int assigned = nextId++;
        ids.put(entity, assigned);
        return assigned;
    }

    public void forget(Object entity) {
        ids.remove(entity);
    }

    public void clear() {
        ids.clear();
        nextId = 1;
    }
}
