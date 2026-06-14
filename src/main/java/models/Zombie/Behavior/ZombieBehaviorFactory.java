package models.Zombie.Behavior;

import com.fasterxml.jackson.databind.JsonNode;
import models.Zombie.ArmorDefinition;
import models.Zombie.Behavior.*;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
public class ZombieBehaviorFactory {
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
        return new SummonBehavior(
            SummonBehavior.SummonType.valueOf(rs.getString("summon_type")),
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
        return new MovementBehavior(
            MovementBehavior.MovementType.valueOf(rs.getString("movement_type")),
            rs.getFloat("movement_param")
        );
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

    public static List<ZombieBehavior> fromJson(
        String objclass,
        JsonNode d,
        Map<String, ArmorDefinition> armorRegistry) {

        List<ZombieBehavior> behaviors = new ArrayList<>();

        switch (objclass) {

            case "ZombiePropertySheet" ->
                resolveArmorProps(d, armorRegistry, behaviors);

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

                behaviors.add(new SummonBehavior(
                    SummonBehavior.SummonType.IMP_THROW, "ZombieImp", 1, (int)(hp * throwPercent)));
                behaviors.add(new DeathEffectBehavior(
                    DeathEffectBehavior.DeathEffectType.SPAWN_IMP, "ZombieImp", 1));
            }

            case "ZombieRaProps" -> {
                int maxSun = d.path("MaxClaimedSunCurrency").asInt(250);
                behaviors.add(new RangedAttackBehavior(
                    RangedAttackBehavior.RangedAttackType.SUN_STEAL, 60, 999, maxSun));
                behaviors.add(new AuraBehavior(
                    AuraBehavior.AuraType.STEAL_SUN_PASSIVE, 0, 120));
            }

            case "ZombieExplorerProps" ->
                behaviors.add(new RangedAttackBehavior(
                    RangedAttackBehavior.RangedAttackType.TORCH_FIRE,
                    45, d.path("MaxTorchReach").asInt(37)));

            case "ZombieTombRaiserProps" -> {
                int interval = (int)(d.path("TimeBetweenRaisings").asDouble(6) * 60);
                behaviors.add(new RangedAttackBehavior(
                    RangedAttackBehavior.RangedAttackType.BONE_THROW, interval, 999));
                behaviors.add(new WorldEffectBehavior(
                    WorldEffectBehavior.WorldEffectType.SPAWN_TOMB,
                    interval, d.path("NumberOfTombsToSpawn").asInt(2)));
                behaviors.add(new DeathEffectBehavior(
                    DeathEffectBehavior.DeathEffectType.TOMBSTONE_CRUMBLE));
            }

            case "ZombieIceAgeDodoProps" -> {
                List<String> flyOver = new ArrayList<>();
                for (JsonNode p : d.path("PlantsToFlyOver").path("List"))
                    flyOver.add(p.asText());
                behaviors.add(new MovementBehavior(MovementBehavior.MovementType.FLY_OVER, flyOver));
            }

            case "ZombieIceAgeHunterProps" ->
                behaviors.add(new RangedAttackBehavior(
                    RangedAttackBehavior.RangedAttackType.SNOWBALL,
                    30,
                    d.path("FarAttackRange").asInt(4),
                    d.path("SnowballsPerBarrage").asInt(3)
                ));

            case "ZombieIceAgeTroglobiteProps" -> {
                behaviors.add(new MovementBehavior(MovementBehavior.MovementType.PUSH_ICE_BLOCK));
                behaviors.add(new WorldEffectBehavior(
                    WorldEffectBehavior.WorldEffectType.FREEZE_COLUMN,
                    30, d.path("NumberOfIceblocksToSpawnWith").asInt(3)));
            }

            case "ZombieBeachFishermanProps" ->
                behaviors.add(new RangedAttackBehavior(
                    RangedAttackBehavior.RangedAttackType.HOOK_PULL,
                    (int)(d.path("DelayBetweenCasting").asDouble(2.5) * 60),
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
                    RangedAttackBehavior.RangedAttackType.JUGGLE_BALL, 35, 5));
                behaviors.add(new DamageReactionBehavior(
                    DamageReactionBehavior.DamageReactionType.REFLECT_PROJECTILE, 0.75f));
            }

            case "ZombieDarkWizardProps" -> {
                behaviors.add(new RangedAttackBehavior(
                    RangedAttackBehavior.RangedAttackType.SPELL_SHEEP, 60, 4));
                behaviors.add(new TransformBehavior(
                    TransformBehavior.TransformType.SHEEP_TRANSFORM, 60, 4));
            }

            case "ZombieDarkKingProps" -> {
                int interval = (int)(d.path("DelayBetweenKnightings").asDouble(2.5) * 60);
                int area     = d.path("KnightingAreaX").asInt(4);
                behaviors.add(new AuraBehavior(AuraBehavior.AuraType.BUFF_SPEED_NEARBY,  area, interval));
                behaviors.add(new AuraBehavior(AuraBehavior.AuraType.BUFF_DAMAGE_NEARBY, area, interval));
            }

            case "ZombieCrystalSkullProps" ->
                behaviors.add(new RangedAttackBehavior(
                    RangedAttackBehavior.RangedAttackType.LASER_BEAM,
                    (int)(d.path("LaserCooldownTime").asDouble(5) * 60),
                    999,
                    d.path("LaserBeamDamage").asInt(4001)
                ));

            case "ZombieProspectorProps" ->
                behaviors.add(new MovementBehavior(MovementBehavior.MovementType.PROSPECTOR_JUMP));

            case "ZombiePianoProps" ->
                behaviors.add(new MovementBehavior(
                    MovementBehavior.MovementType.PIANO_CRUSH,
                    (float) d.path("FastMoveSpeed").asDouble(0.4)));

            case "ZombieModernAllStarProps" ->
                behaviors.add(new MovementBehavior(
                    MovementBehavior.MovementType.TACKLE_RUN,
                    (float) d.path("RunningSpeedScale").asDouble(0.5)));

            case "ZombieLostCityJaneProps" ->
                behaviors.add(new DamageReactionBehavior(
                    DamageReactionBehavior.DamageReactionType.REFLECT_PROJECTILE, 1.0f));

            case "ZombieArcadeProps" ->
                behaviors.add(new MovementBehavior(MovementBehavior.MovementType.PUSH_PLANT_BACK));

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


}
