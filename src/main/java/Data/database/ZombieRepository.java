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

    public void saveArmorDefinition(ArmorDefinition def) throws SQLException {
        String sql = """
            INSERT INTO armor_definition
                (alias, base_health, metallic, pass_damage, layer_thresholds)
            VALUES (?, ?, ?, ?, ?)
            ON CONFLICT(alias) DO UPDATE SET
                base_health      = excluded.base_health,
                metallic         = excluded.metallic,
                pass_damage      = excluded.pass_damage,
                layer_thresholds = excluded.layer_thresholds
        """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, def.alias);
            ps.setInt(2, def.baseHealth);
            ps.setInt(3, def.metallic ? 1 : 0);
            ps.setInt(4, def.passDamage ? 1 : 0);
            ps.setString(5, ZombieBehaviorFactory.serializeThresholds(def.layerThresholds));
            ps.executeUpdate();
        }
    }
    public void saveZombieTemplate(Zombie zombie) throws SQLException {
        boolean oldAutoCommit = conn.getAutoCommit();
        conn.setAutoCommit(false);
        try {
            saveZombieRow(zombie);
            List<ZombieBehavior> behaviors = zombie.getBehaviors();
            for (int i = 0; i < behaviors.size(); i++) {
                saveBehaviorRow(zombie.getAlias(), i, behaviors.get(i));
            }
            conn.commit();
        } catch (SQLException e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(oldAutoCommit);
        }
    }
    private void saveZombieRow(Zombie zombie) throws SQLException {
        String sql = """
            INSERT INTO zombie_template
                (alias, hitpoints, speed, eat_dps, wave_point_cost, weight)
            VALUES (?, ?, ?, ?, ?, ?)
            ON CONFLICT(alias) DO UPDATE SET
                hitpoints      = excluded.hitpoints,
                speed          = excluded.speed,
                eat_dps        = excluded.eat_dps,
                wave_point_cost= excluded.wave_point_cost,
                weight         = excluded.weight
        """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, zombie.getAlias());
            ps.setFloat(2, zombie.getMaxHitpoints());
            ps.setFloat(3, zombie.getBaseSpeed());
            ps.setFloat(4, zombie.getBaseEatDPS());
            ps.setFloat(5, zombie.getWavePointCost());
            ps.setInt(6, zombie.getWeight());
            ps.executeUpdate();
        }
    }

    private void saveBehaviorRow(String alias, int order, ZombieBehavior behavior) throws SQLException {
        String sql = """
            INSERT INTO zombie_behavior_template
                (zombie_alias, behavior_type, chain_order,
                 armor_alias,
                 ranged_type, interval_ticks, range, extra_param,
                 summon_type, summon_alias, summon_count, hp_threshold,
                 reaction_type, param1, param2,
                 movement_type, movement_param,
                 world_effect_type, effect_interval, effect_count,
                 aura_type, aura_radius, aura_interval,
                 death_effect_type, death_spawn_alias, death_spawn_count,
                 transform_type, transform_interval, transform_range
                 sun_steal_max_amount)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?,
                    ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?,?)
        """;

        if (!(behavior instanceof PersistableBehavior pb)) {
            throw new IllegalArgumentException(
                "Behavior is not persistable: " + behavior.getClass().getSimpleName());
        }

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, alias);
            ps.setString(2, pb.behaviorType());
            ps.setInt(3, order);
            for (int i = 4; i <= 29; i++) ps.setNull(i, Types.NULL);
            pb.applyToStatement(ps);
            ps.executeUpdate();
        }
    }

    public Zombie loadZombie(String alias) throws SQLException {
        Zombie zombie = loadTemplate(alias);
        for (ZombieBehavior behavior : loadBehaviors(alias)) {
            zombie.addBehavior(behavior);
        }
        return zombie;
    }

    public Map<String, Zombie> loadAllZombies() throws SQLException {
        Map<String, Zombie> result = new LinkedHashMap<>();

        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT alias FROM zombie_template ORDER BY alias")) {
            while (rs.next()) {
                String alias = rs.getString("alias");
                result.put(alias, loadZombie(alias));
            }
        }

        return result;
    }

    private Zombie loadTemplate(String alias) throws SQLException {
        String sql = """
            SELECT alias, hitpoints, speed, eat_dps, wave_point_cost, weight
            FROM zombie_template WHERE alias = ?
        """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, alias);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) throw new IllegalArgumentException("No zombie template: " + alias);
                return new Zombie(
                    rs.getString("alias"),
                    rs.getFloat("hitpoints"),
                    rs.getFloat("speed"),
                    rs.getFloat("eat_dps"),
                    rs.getFloat("wave_point_cost"),
                    rs.getInt("weight")
                );
            }
        }
    }

    private List<ZombieBehavior> loadBehaviors(String alias) throws SQLException {
        String sql = """
            SELECT bt.*,
                   ad.base_health, ad.metallic, ad.pass_damage, ad.layer_thresholds
            FROM zombie_behavior_template bt
            LEFT JOIN armor_definition ad ON bt.armor_alias = ad.alias
            WHERE bt.zombie_alias = ?
            ORDER BY bt.chain_order ASC
        """;

        List<ZombieBehavior> result = new ArrayList<>();

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, alias);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ZombieBehavior behavior = ZombieBehaviorFactory.fromResultSet(rs);
                    if (behavior != null) result.add(behavior);
                }
            }
        }
        return result;
    }
}
