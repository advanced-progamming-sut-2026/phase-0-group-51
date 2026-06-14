package models.Zombie.Behavior;

import java.sql.PreparedStatement;
import java.sql.SQLException;

public interface PersistableBehavior extends ZombieBehavior{
    String behaviorType();
    void applyToStatement(PreparedStatement ps) throws SQLException;
}
