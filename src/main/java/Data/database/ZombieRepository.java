package Data.database;

import Data.database.DataBaseManager;
import models.Zombie.ArmorDefinition;
import models.Zombie.Behavior.*;
import models.Zombie.Zombie;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ZombieRepository {
    private final Connection conn;

    public ZombieRepository() throws SQLException {
        this.conn = DataBaseManager.getConnection();
    }

    public Map<String, Zombie> loadAllZombies() {
        return null;
    }
}
