package models.Zombie.Behavior;

import com.fasterxml.jackson.databind.JsonNode;
import models.Zombie.ArmorDefinition;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ZombieBehaviorFactory {
    private static final int TICKS_PER_SECOND = 10;

    public static ZombieBehavior fromResultSet(ResultSet rs) throws SQLException {
        String type = rs.getString("behavior_type");

        return switch (type) {
            case "ARMOR"           -> buildArmor(rs);
            case "RANGED_ATTACK"   -> buildRangedAttack(rs);
            case "SUMMON"          -> buildSummon(rs);
            case "DAMAGE_REACTION" -> buildDamageReaction(rs);
            case "MOVEMENT"        -> buildMovement(rs);
            case "WORLD_EFFECT"    -> buildWorldEffect(rs);
            case "AURA"            -> buildAura(rs);
            case "DEATH_EFFECT"    -> buildDeathEffect(rs);
            case "TRANSFORM"       -> buildTransform(rs);
            case "PUSH_OBJECT"     -> buildPushObject(rs);
            case "CONTACT_KILL"    -> buildContactKill(rs);
            case "TORCH"           -> new TorchBehavior(rs.getInt("torch_reach"));
            case "DYNAMITE"        -> new DynamiteBehavior(rs.getInt("explosion_delay"));
            case "SUN_STEAL"       -> new SunStealBehavior(rs.getInt("max_amount"), rs.getInt("steal_interval"));
            case "TURQUOISE_LASER" -> new TurquoiseLaserBehavior(
                rs.getInt("detect_range"), rs.getInt("steal_duration"), rs.getInt("steal_per_second"));
            default                -> null;
        };
    }

    private static ZombieBehavior buildArmor(ResultSet rs) throws SQLException {
        ArmorDefinition def = new ArmorDefinition(
            rs.getString("armor_alias"),
            rs.getInt("base_health"),
            rs.getInt("metallic")    == 1,
            rs.getInt("pass_damage") == 1,
            deserializeThresholds(rs.getString("layer_thresholds"))
        );
        return new ArmorBehavior(def);
    }

    private static ZombieBehavior buildRangedAttack(ResultSet rs) throws SQLException {
        return new RangedAttackBehavior(
            RangedAttackBehavior.RangedAttackType.valueOf(rs.getString("ranged_type")),
            rs.getInt("interval_ticks"),
            rs.getInt("range"),
            rs.getInt("extra_param")
        );
    }

    private static ZombieBehavior buildSummon(ResultSet rs) throws SQLException {
        return new ImpThrowBehavior(
            ImpThrowBehavior.SummonType.valueOf(rs.getString("summon_type")),
            rs.getString("summon_alias"),
            rs.getInt("summon_count"),
            rs.getInt("hp_threshold")
        );
    }

    private static ZombieBehavior buildDamageReaction(ResultSet rs) throws SQLException {
        return new DamageReactionBehavior(
            DamageReactionBehavior.DamageReactionType.valueOf(rs.getString("reaction_type")),
            rs.getFloat("param1"),
            rs.getFloat("param2")
        );
    }

    private static ZombieBehavior buildMovement(ResultSet rs) throws SQLException {
        MovementBehavior.MovementType type =
            MovementBehavior.MovementType.valueOf(rs.getString("movement_type"));
        String targets = rs.getString("movement_targets");
        if (targets != null && !targets.isBlank()) {
            return new MovementBehavior(type, List.of(targets.split(",")));
        }
        return new MovementBehavior(type, rs.getFloat("movement_param"));
    }

    private static ZombieBehavior buildWorldEffect(ResultSet rs) throws SQLException {
        return new WorldEffectBehavior(
            WorldEffectBehavior.WorldEffectType.valueOf(rs.getString("world_effect_type")),
            rs.getInt("effect_interval"),
            rs.getInt("effect_count")
        );
    }

    private static ZombieBehavior buildAura(ResultSet rs) throws SQLException {
        return new AuraBehavior(
            AuraBehavior.AuraType.valueOf(rs.getString("aura_type")),
            rs.getFloat("aura_radius"),
            rs.getInt("aura_interval")
        );
    }

    private static ZombieBehavior buildDeathEffect(ResultSet rs) throws SQLException {
        return new DeathEffectBehavior(
            DeathEffectBehavior.DeathEffectType.valueOf(rs.getString("death_effect_type")),
            rs.getString("death_spawn_alias"),
            rs.getInt("death_spawn_count")
        );
    }

    private static ZombieBehavior buildTransform(ResultSet rs) throws SQLException {
        return new TransformBehavior(
            TransformBehavior.TransformType.valueOf(rs.getString("transform_type")),
            rs.getInt("transform_interval"),
            rs.getInt("transform_range")
        );
    }

    private static ZombieBehavior buildPushObject(ResultSet rs) throws SQLException {
        return new PushObjectBehavior(
            PushObjectBehavior.PushType.valueOf(rs.getString("push_type")),
            rs.getInt("object_hp"),
            rs.getInt("object_count"),
            rs.getString("spawn_alias"),
            rs.getInt("spawn_count")
        );
    }

    private static ZombieBehavior buildContactKill(ResultSet rs) throws SQLException {
        return new InstantKillBehavior(
            rs.getFloat("speed_scale"),
            rs.getInt("kill_hypnotized") == 1,
            rs.getFloat("running_speed_scale"),
            rs.getInt("repeating") == 1
        );
    }

    public static List<ZombieBehavior> fromJson(
        String alias,
        String objclass,
        JsonNode d,
        Map<String, ArmorDefinition> armorRegistry) {

        List<ZombieBehavior> behaviors = new ArrayList<>();

        switch (objclass) {

            case "ZombiePropertySheet" -> {
                if (alias.equals("ZombieDarkArmor3")) {
                    // Knight: crown + shoulder armor
                    addArmorByAlias("CrownDefault@ArmorTypes", armorRegistry, behaviors);
                    addArmorByAlias("ShoulderArmorDefault@ArmorTypes", armorRegistry, behaviors);
                } else {
                    resolveArmorProps(d, armorRegistry, behaviors);
                }
                if (alias.equals("ZombieDarkImpDragon")) {
                    // Imp dragon: immune to fire projectiles
                    behaviors.add(new DamageReactionBehavior(
                        DamageReactionBehavior.DamageReactionType.FIRE_IMMUNE));
                }
            }

            case "ZombieNewspaperProps" -> {
                resolveArmorProps(d, armorRegistry, behaviors);
                behaviors.add(new DamageReactionBehavior(
                    DamageReactionBehavior.DamageReactionType.NEWSPAPER_RAGE,
                    (float) d.path("EnragedSpeedScale").asDouble(4.0),
                    (float) d.path("EnragedDamageScale").asDouble(4.0)
                ));
            }

            case "ZombieGargantuarProps" -> {
                int    hp           = d.path("Hitpoints").asInt(3600);
                float  throwPercent = 0.5f;
                JsonNode layers     = d.path("HealthThresholdToImpAmmoLayers");
                if (layers.isArray() && !layers.isEmpty())
                    throwPercent = (float) layers.get(0).path("HealthPercentThrowImp").asDouble(0.5);

                // Throws its imp to the 3rd column when it drops to half health.
                behaviors.add(new ImpThrowBehavior(
                    ImpThrowBehavior.SummonType.IMP_THROW, "ZombieImp", 1, (int) (hp * throwPercent)));
                // Smashes every plant in one hit instead of eating.
                behaviors.add(new InstantKillBehavior(1.0f, true, 0f, true));
            }

            case "ZombieRaProps" -> {
                int maxAmount = d.path("MaxClaimedSunCurrency").asInt(250);
                behaviors.add(new SunStealBehavior(maxAmount, 20));
            }

            case "ZombieExplorerProps" ->
                behaviors.add(new TorchBehavior(
                    Math.max(0, d.path("MaxTorchReach").asInt(1) - 1)));

            case "ZombieTombRaiserProps" -> {
                int interval = (int) (d.path("TimeBetweenRaisings").asDouble(6) * TICKS_PER_SECOND);
                behaviors.add(new WorldEffectBehavior(
                    WorldEffectBehavior.WorldEffectType.SPAWN_TOMB,
                    interval, d.path("NumberOfTombsToSpawn").asInt(2)));
            }

            case "ZombieIceAgeDodoProps" ->
                // Curated list of obstacles that exist in plants.csv (walls,
                // row-movers, traps). See MovementBehavior.DODO_FLY_OVER_PLANTS.
                behaviors.add(new MovementBehavior(
                    MovementBehavior.MovementType.FLY_OVER,
                    MovementBehavior.DODO_FLY_OVER_PLANTS));

            case "ZombieIceAgeHunterProps" ->
                behaviors.add(new RangedAttackBehavior(
                    RangedAttackBehavior.RangedAttackType.SNOWBALL,
                    30,
                    d.path("FarAttackRange").asInt(4),
                    d.path("SnowballsPerBarrage").asInt(3)
                ));

            case "ZombieIceAgeTroglobiteProps" ->
                // Pushes ice blocks (600 HP each, like frozen tiles) that crush plants.
                behaviors.add(new PushObjectBehavior(
                    PushObjectBehavior.PushType.ICE_BLOCK,
                    600,
                    d.path("NumberOfIceblocksToSpawnWith").asInt(3)));

            case "ZombieBeachFishermanProps" ->
                behaviors.add(new RangedAttackBehavior(
                    RangedAttackBehavior.RangedAttackType.HOOK_PULL,
                    (int) (d.path("DelayBetweenCasting").asDouble(2.5) * TICKS_PER_SECOND),
                    d.path("CastingAreaMaxRange").asInt(8)
                ));

            case "ZombieBeachOctopusProps" ->
                behaviors.add(new RangedAttackBehavior(
                    RangedAttackBehavior.RangedAttackType.OCTOPUS_NET, 40, 3));

            case "ZombieBeachSnorkelProps" -> {
                behaviors.add(new MovementBehavior(MovementBehavior.MovementType.UNDERGROUND));
                behaviors.add(new DamageReactionBehavior(
                    DamageReactionBehavior.DamageReactionType.SUBMERGE_DODGE));
            }

            case "ZombieDarkJugglerProps" -> {
                behaviors.add(new RangedAttackBehavior(
                    RangedAttackBehavior.RangedAttackType.JUGGLE_BALL, 35, 5, 20));
                behaviors.add(new DamageReactionBehavior(
                    DamageReactionBehavior.DamageReactionType.REFLECT_PROJECTILE,
                    1.5f));  // spin speed scale (moves faster while spinning)
            }

            case "ZombieDarkWizardProps" ->
                behaviors.add(new TransformBehavior(
                    TransformBehavior.TransformType.SHEEP_TRANSFORM, 60, 4));

            case "ZombieDarkKingProps" -> {
                int interval = (int) (d.path("DelayBetweenKnightings").asDouble(2.5) * TICKS_PER_SECOND);
                int area     = d.path("KnightingAreaX").asInt(4);
                List<ArmorDefinition> knightArmors = new ArrayList<>();
                ArmorDefinition crown    = armorRegistry.get("CrownDefault@ArmorTypes");
                ArmorDefinition shoulder = armorRegistry.get("ShoulderArmorDefault@ArmorTypes");
                if (crown != null)    knightArmors.add(crown);
                if (shoulder != null) knightArmors.add(shoulder);
                behaviors.add(new AuraBehavior(
                    AuraBehavior.AuraType.KNIGHT_NEARBY, area, interval, knightArmors));
            }

            case "ZombieCrystalSkullProps" ->
                behaviors.add(new RangedAttackBehavior(
                    RangedAttackBehavior.RangedAttackType.LASER_BEAM,
                    (int) (d.path("LaserCooldownTime").asDouble(5) * TICKS_PER_SECOND),
                    d.path("LaserBeamLength").asInt(999),
                    d.path("LaserBeamDamage").asInt(4001)
                ));

            case "ZombieProspectorProps" -> {
                behaviors.add(new MovementBehavior(MovementBehavior.MovementType.PROSPECTOR_JUMP));
                // The dynamite was never attached before -> the jump never happened.
                behaviors.add(new DynamiteBehavior(
                    (int) (d.path("LaunchCountdown").asDouble(10) * TICKS_PER_SECOND)));
            }

            case "ZombiePianoProps" -> {
                behaviors.add(new MovementBehavior(
                    MovementBehavior.MovementType.PIANO_CRUSH,
                    (float) d.path("FastMoveSpeed").asDouble(0.4)));
                // Its music makes zombies switch lanes.
                behaviors.add(new WorldEffectBehavior(
                    WorldEffectBehavior.WorldEffectType.RANDOM_LANE_SWAP, 30, 1));
            }

            case "ZombieModernAllStarProps" ->
                // Runs fast, tackles the first plant, then walks very slowly.
                behaviors.add(new InstantKillBehavior(
                    0.3f, true,
                    (float) d.path("RunningSpeedScale").asDouble(2.5)));

            case "ZombieLostCityJaneProps" ->
                // Parasol: blocks lobbed projectiles.
                behaviors.add(new DamageReactionBehavior(
                    DamageReactionBehavior.DamageReactionType.DEFLECT_LOBBER));

            case "ZombieArcadeProps" ->
                behaviors.add(new PushObjectBehavior(
                    PushObjectBehavior.PushType.ARCADE_MACHINE, 1100, 1));

            case "ZombieBarrelRollerProps" ->
                // Pushes a barrel; when it breaks, two imps jump out.
                behaviors.add(new PushObjectBehavior(
                    PushObjectBehavior.PushType.BARREL, 1100, 1, "ZombieImp", 2));

            case "ZombieTurquoiseProps" ->
                // Steals 25 sun/s for 5s from 4 tiles away, then fires its laser.
                behaviors.add(new TurquoiseLaserBehavior(4, 5, 25));

            default ->
                System.out.println("[ZombieBehaviorFactory] Unknown objclass: " + objclass);
        }

        return behaviors;
    }

    private static void resolveArmorProps(
        JsonNode d,
        Map<String, ArmorDefinition> armorRegistry,
        List<ZombieBehavior> behaviors) {

        for (JsonNode ap : d.path("ZombieArmorProps")) {
            String key = ap.asText()
                .replace("RTID(", "")
                .replace(")", "")
                .trim();
            ArmorDefinition def = armorRegistry.get(key);
            if (def != null) {
                behaviors.add(new ArmorBehavior(def));
            }
        }
    }

    public static String serializeThresholds(List<Float> thresholds) {
        if (thresholds == null || thresholds.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < thresholds.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(thresholds.get(i));
        }
        return sb.toString();
    }

    public static List<Float> deserializeThresholds(String value) {
        List<Float> result = new ArrayList<>();
        if (value == null || value.isBlank()) return result;
        for (String part : value.split(",")) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) result.add(Float.parseFloat(trimmed));
        }
        return result;
    }

    private static void addArmorByAlias(
        String armorKey,
        Map<String, ArmorDefinition> armorRegistry,
        List<ZombieBehavior> behaviors) {

        ArmorDefinition def = armorRegistry.get(armorKey);
        if (def != null) {
            behaviors.add(new ArmorBehavior(def));
        }
    }
}
