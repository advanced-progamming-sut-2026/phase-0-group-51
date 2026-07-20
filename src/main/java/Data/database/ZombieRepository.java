package Data.database;

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
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


public class ZombieRepository {
    private final Connection conn;

    public ZombieRepository() throws SQLException {
        this.conn = DataBaseManager.getConnection();
        ensureTables();
    }

    private void ensureTables() throws SQLException {
        try (Statement st = conn.createStatement()) {
            st.executeUpdate("""
                CREATE TABLE IF NOT EXISTS armor_definition (
                    alias            TEXT PRIMARY KEY,
                    base_health      INTEGER NOT NULL,
                    metallic         INTEGER NOT NULL DEFAULT 0,
                    pass_damage      INTEGER NOT NULL DEFAULT 0,
                    layer_thresholds TEXT
                )
             """);
            st.executeUpdate("""
                CREATE TABLE IF NOT EXISTS zombie_template (
                    alias           TEXT PRIMARY KEY, hitpoints       REAL NOT NULL,
                    speed           REAL NOT NULL, eat_dps         REAL NOT NULL,
                    wave_point_cost REAL NOT NULL,  weight          INTEGER NOT NULL
                )
            """);
            st.executeUpdate("""
                CREATE TABLE IF NOT EXISTS zombie_behavior_template (
                    zombie_alias       TEXT NOT NULL,
                    behavior_type      TEXT NOT NULL,  chain_order        INTEGER NOT NULL,
                    armor_alias        TEXT,  ranged_type        TEXT,
                    interval_ticks     INTEGER,  range              INTEGER,
                    extra_param        INTEGER,summon_type        TEXT,
                    summon_alias       TEXT,  summon_count       INTEGER,
                    hp_threshold       INTEGER,  reaction_type      TEXT,
                    param1             REAL, param2             REAL,
                    movement_type      TEXT, movement_param     REAL,
                    movement_targets   TEXT, world_effect_type  TEXT,
                    effect_interval    INTEGER,effect_count       INTEGER,
                    aura_type          TEXT, aura_radius        REAL,
                    aura_interval      INTEGER,aura_armor_aliases TEXT,
                    death_effect_type  TEXT,  death_spawn_alias  TEXT,
                    death_spawn_count  INTEGER, transform_type     TEXT,
                    transform_interval INTEGER,  transform_range    INTEGER,
                    push_type          TEXT, object_hp          INTEGER,
                    object_count       INTEGER,    spawn_alias        TEXT,
                    spawn_count        INTEGER, speed_scale        REAL,
                    kill_hypnotized    INTEGER,  running_speed_scale REAL,
                    repeating          INTEGER, torch_reach        INTEGER,
                    explosion_delay    INTEGER,  max_amount         INTEGER,
                    steal_interval     INTEGER, detect_range       INTEGER,
                    steal_duration     INTEGER, steal_per_second   INTEGER,
                    PRIMARY KEY (zombie_alias, chain_order)
                )
            """);
        }
    }

    // ------------------------------------------------------------------ save

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
            ps.setString(1, def.getAlias());
            ps.setInt(2, def.getBaseHealth());
            ps.setInt(3, def.isMetallic() ? 1 : 0);
            ps.setInt(4, def.isPassDamage() ? 1 : 0);
            ps.setString(5, ZombieBehaviorFactory.serializeThresholds(def.getLayerThresholds()));
            ps.executeUpdate();
        }
    }

    public void saveZombieTemplate(Zombie zombie) throws SQLException {
        boolean oldAutoCommit = conn.getAutoCommit();
        conn.setAutoCommit(false);
        try {
            saveZombieRow(zombie);
            deleteBehaviorRows(zombie.getAlias());
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

    public void saveAll(Map<String, Zombie> zombies) throws SQLException {
        for (Zombie zombie : zombies.values()) {
            saveZombieTemplate(zombie);
        }
    }

    private void saveZombieRow(Zombie zombie) throws SQLException {
        String sql = """
            INSERT INTO zombie_template
                (alias, hitpoints, speed, eat_dps, wave_point_cost, weight)
            VALUES (?, ?, ?, ?, ?, ?)
            ON CONFLICT(alias) DO UPDATE SET
                hitpoints       = excluded.hitpoints,
                speed           = excluded.speed,
                eat_dps         = excluded.eat_dps,
                wave_point_cost = excluded.wave_point_cost,
                weight          = excluded.weight
        """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, zombie.getAlias());
            ps.setFloat(2, zombie.getMaxHitpoints());
            ps.setFloat(3, zombie.getBaseSpeed());
            ps.setFloat(4, zombie.getBaseEatDps());
            ps.setFloat(5, zombie.getWavePointCost());
            ps.setInt(6, zombie.getWeight());
            ps.executeUpdate();
        }
    }

    private void deleteBehaviorRows(String alias) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
            "DELETE FROM zombie_behavior_template WHERE zombie_alias = ?")) {
            ps.setString(1, alias);
            ps.executeUpdate();
        }
    }

    private void saveBehaviorRow(String alias, int order, ZombieBehavior behavior) throws SQLException {
        if (!(behavior instanceof PersistableBehavior pb)) {
            throw new IllegalArgumentException(
                "Behavior is not persistable: " + behavior.getClass().getSimpleName());
        }

        // armor definitions live in their own table and must exist before the
        // behavior row references them by alias
        if (behavior instanceof ArmorBehavior b) {
            saveArmorDefinition(b.getDefinition());
        } else if (behavior instanceof AuraBehavior b) {
            for (ArmorDefinition def : b.getKnightArmors()) {
                saveArmorDefinition(def);
            }
        }

        Map<String, Object> cols = new LinkedHashMap<>();
        cols.put("zombie_alias", alias);
        cols.put("behavior_type", pb.behaviorType());
        cols.put("chain_order", order);
        pb.applyToStatement(cols);
        insertBehaviorRow(cols);
    }

    private void insertBehaviorRow(Map<String, Object> cols) throws SQLException {
        String columns = String.join(", ", cols.keySet());
        String placeholders = String.join(", ", Collections.nCopies(cols.size(), "?"));
        String sql = "INSERT INTO zombie_behavior_template (" + columns + ") VALUES (" + placeholders + ")";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            int i = 1;
            for (Object value : cols.values()) {
                switch (value) {
                    case null           -> ps.setNull(i, Types.NULL);
                    case Integer number -> ps.setInt(i, number);
                    case Float number   -> ps.setFloat(i, number);
                    default             -> ps.setString(i, value.toString());
                }
                i++;
            }
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
        List<String> aliases = new ArrayList<>();

        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT alias FROM zombie_template ORDER BY alias")) {
            while (rs.next()) {
                aliases.add(rs.getString("alias"));
            }
        }
        for (String alias : aliases) {
            result.put(alias, loadZombie(alias));
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
        List<String[]> pendingAuras = new ArrayList<>();

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, alias);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    if ("AURA".equals(rs.getString("behavior_type"))) {
                        pendingAuras.add(new String[]{
                            rs.getString("aura_type"),
                            String.valueOf(rs.getFloat("aura_radius")),
                            String.valueOf(rs.getInt("aura_interval")),
                            rs.getString("aura_armor_aliases")
                        });
                        result.add(null);
                        continue;
                    }
                    ZombieBehavior behavior = ZombieBehaviorFactory.fromResultSet(rs);
                    if (behavior != null) result.add(behavior);
                }
            }
        }

        int auraIndex = 0;
        for (int i = 0; i < result.size(); i++) {
            if (result.get(i) == null) {
                String[] aura = pendingAuras.get(auraIndex++);
                result.set(i, buildAuraWithArmors(aura));
            }
        }
        result.removeIf(java.util.Objects::isNull);
        return result;
    }

    private AuraBehavior buildAuraWithArmors(String[] aura) throws SQLException {
        List<ArmorDefinition> armors = new ArrayList<>();
        if (aura[3] != null && !aura[3].isBlank()) {
            for (String armorAlias : aura[3].split(",")) {
                ArmorDefinition def = loadArmorDefinition(armorAlias.trim());
                if (def != null) armors.add(def);
            }
        }
        return new AuraBehavior(
            AuraBehavior.AuraType.valueOf(aura[0]),
            Float.parseFloat(aura[1]),
            Integer.parseInt(aura[2]),
            armors
        );
    }

    public ArmorDefinition loadArmorDefinition(String alias) throws SQLException {
        String sql = "SELECT * FROM armor_definition WHERE alias = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, alias);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                return new ArmorDefinition(
                    rs.getString("alias"),
                    rs.getInt("base_health"),
                    rs.getInt("metallic") == 1,
                    rs.getInt("pass_damage") == 1,
                    ZombieBehaviorFactory.deserializeThresholds(rs.getString("layer_thresholds"))
                );
            }
        }
    }
}
