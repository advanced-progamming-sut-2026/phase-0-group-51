package models.zombie.Behavior;

import java.util.Map;


public interface PersistableBehavior extends ZombieBehavior {
    String behaviorType();

    void applyToStatement(Map<String, Object> cols);
}
