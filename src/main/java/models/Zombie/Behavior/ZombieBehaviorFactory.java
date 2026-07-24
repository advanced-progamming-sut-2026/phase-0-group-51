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
    private static final int HUNTER_SNOWBALL_INTERVAL_TICKS = TICKS_PER_SECOND;

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
        JsonNode data,
        Map<String, ArmorDefinition> armorRegistry) {

        List<ZombieBehavior> behaviors = new ArrayList<>();
        if (addGeneralBehavior(
            alias, objclass, data, armorRegistry, behaviors
        )) {
            return behaviors;
        }
        if (addAncientOrFrostbiteBehavior(
            objclass, data, behaviors
        )) {
            return behaviors;
        }
        if (addBeachOrDarkBehavior(
            objclass, data, armorRegistry, behaviors
        )) {
            return behaviors;
        }
        if (addRemainingBehavior(objclass, data, behaviors)) {
            return behaviors;
        }
        System.out.println(
            "[ZombieBehaviorFactory] Unknown objclass: " + objclass
        );
        return behaviors;
    }

    private static boolean addGeneralBehavior(
        String alias,
        String objclass,
        JsonNode data,
        Map<String, ArmorDefinition> armorRegistry,
        List<ZombieBehavior> behaviors
    ) {
        switch (objclass) {
            case "ZombiePropertySheet" -> addPropertySheetBehavior(
                alias, data, armorRegistry, behaviors
            );
            case "ZombieNewspaperProps" -> addNewspaperBehavior(
                data, armorRegistry, behaviors
            );
            case "ZombieGargantuarProps" -> addGargantuarBehavior(
                data, behaviors
            );
            case "ZombieRaProps" -> behaviors.add(
                new SunStealBehavior(
                    data.path("MaxClaimedSunCurrency").asInt(250),
                    20
                )
            );
            default -> {
                return false;
            }
        }
        return true;
    }

    private static void addPropertySheetBehavior(
        String alias,
        JsonNode data,
        Map<String, ArmorDefinition> armorRegistry,
        List<ZombieBehavior> behaviors
    ) {
        if (alias.equals("ZombieDarkArmor3")) {
            addArmorByAlias(
                "CrownDefault@ArmorTypes",
                armorRegistry,
                behaviors
            );
            addArmorByAlias("ShoulderArmorDefault@ArmorTypes", armorRegistry, behaviors);
        } else {
            resolveArmorProps(data, armorRegistry, behaviors);
        }
        if (alias.equals("ZombieDarkImpDragon")) {
            // Imp dragon: immune to fire projectiles
            behaviors.add(new DamageReactionBehavior(
                DamageReactionBehavior.DamageReactionType.FIRE_IMMUNE));
        }
    }

    private static void addNewspaperBehavior(
        JsonNode data,
        Map<String, ArmorDefinition> armorRegistry,
        List<ZombieBehavior> behaviors
    ) {
        resolveArmorProps(data, armorRegistry, behaviors);
        behaviors.add(new DamageReactionBehavior(
            DamageReactionBehavior.DamageReactionType.NEWSPAPER_RAGE,
            (float) data.path("EnragedSpeedScale").asDouble(4.0),
            (float) data.path("EnragedDamageScale").asDouble(4.0)
        ));
    }

    private static void addGargantuarBehavior(
        JsonNode data,
        List<ZombieBehavior> behaviors
    ) {
        int hitpoints = data.path("Hitpoints").asInt(3600);
        float  throwPercent = 0.5f;
        JsonNode layers = data.path("HealthThresholdToImpAmmoLayers");
        if (layers.isArray() && !layers.isEmpty()) {
            throwPercent = (float) layers.get(0)
                .path("HealthPercentThrowImp")
                .asDouble(0.5);
        }
        behaviors.add(new ImpThrowBehavior(
            ImpThrowBehavior.SummonType.IMP_THROW,
            "ZombieImp",
            1,
            (int) (hitpoints * throwPercent)
        ));
        behaviors.add(new InstantKillBehavior(1.0f, true, 0f, true));
    }

    private static boolean addAncientOrFrostbiteBehavior(
        String objclass,
        JsonNode data,
        List<ZombieBehavior> behaviors
    ) {
        switch (objclass) {
            case "ZombieExplorerProps" -> addExplorerBehavior(
                data, behaviors
            );
            case "ZombieTombRaiserProps" -> addTombRaiserBehavior(
                data, behaviors
            );
            case "ZombieIceAgeDodoProps" -> addDodoBehavior(behaviors);
            case "ZombieIceAgeHunterProps" -> addHunterBehavior(
                data, behaviors
            );
            case "ZombieIceAgeTroglobiteProps" -> addTroglobiteBehavior(
                data, behaviors
            );
            default -> {
                return false;
            }
        }
        return true;
    }

    private static void addExplorerBehavior(
        JsonNode data,
        List<ZombieBehavior> behaviors
    ) {
        behaviors.add(new TorchBehavior(
            Math.max(0, data.path("MaxTorchReach").asInt(1) - 1)
        ));
    }

    private static void addTombRaiserBehavior(
        JsonNode data,
        List<ZombieBehavior> behaviors
    ) {
        int interval = (int) (
            data.path("TimeBetweenRaisings").asDouble(6)
                * TICKS_PER_SECOND
        );
        behaviors.add(new WorldEffectBehavior(
            WorldEffectBehavior.WorldEffectType.SPAWN_TOMB,
            interval,
            data.path("NumberOfTombsToSpawn").asInt(2)
        ));
    }

    private static void addDodoBehavior(
        List<ZombieBehavior> behaviors
    ) {
        behaviors.add(new MovementBehavior(
            MovementBehavior.MovementType.FLY_OVER,
            MovementBehavior.DODO_FLY_OVER_PLANTS
        ));
    }

    private static void addHunterBehavior(
        JsonNode data,
        List<ZombieBehavior> behaviors
    ) {
        behaviors.add(new RangedAttackBehavior(
            RangedAttackBehavior.RangedAttackType.SNOWBALL,
            HUNTER_SNOWBALL_INTERVAL_TICKS,
            data.path("FarAttackRange").asInt(4),
            data.path("SnowballsPerBarrage").asInt(3)
        ));
    }

    private static void addTroglobiteBehavior(
        JsonNode data,
        List<ZombieBehavior> behaviors
    ) {
        behaviors.add(new PushObjectBehavior(
            PushObjectBehavior.PushType.ICE_BLOCK,
            600,
            data.path("NumberOfIceblocksToSpawnWith").asInt(3)
        ));
    }

    private static boolean addBeachOrDarkBehavior(
        String objclass,
        JsonNode data,
        Map<String, ArmorDefinition> armorRegistry,
        List<ZombieBehavior> behaviors
    ) {
        switch (objclass) {
            case "ZombieBeachFishermanProps" -> addFishermanBehavior(
                data, behaviors
            );
            case "ZombieBeachOctopusProps" -> addOctopusBehavior(behaviors);
            case "ZombieBeachSnorkelProps" -> addSnorkelBehavior(behaviors);
            case "ZombieDarkJugglerProps" -> addJugglerBehavior(behaviors);
            case "ZombieDarkWizardProps" -> addWizardBehavior(behaviors);
            case "ZombieDarkKingProps" -> addDarkKingBehavior(
                data, armorRegistry, behaviors
            );
            default -> {
                return false;
            }
        }
        return true;
    }

    private static void addFishermanBehavior(
        JsonNode data,
        List<ZombieBehavior> behaviors
    ) {
        int interval = (int) (
            data.path("DelayBetweenCasting").asDouble(2.5)
                * TICKS_PER_SECOND
        );
        behaviors.add(new RangedAttackBehavior(
            RangedAttackBehavior.RangedAttackType.HOOK_PULL,
            interval,
            data.path("CastingAreaMaxRange").asInt(8)
        ));
    }

    private static void addOctopusBehavior(
        List<ZombieBehavior> behaviors
    ) {
        behaviors.add(new RangedAttackBehavior(
            RangedAttackBehavior.RangedAttackType.OCTOPUS_NET,
            40,
            3
        ));
    }

    private static void addSnorkelBehavior(
        List<ZombieBehavior> behaviors
    ) {
        behaviors.add(new MovementBehavior(MovementBehavior.MovementType.UNDERGROUND));
        behaviors.add(new DamageReactionBehavior(
            DamageReactionBehavior.DamageReactionType.SUBMERGE_DODGE));
    }

    private static void addJugglerBehavior(
        List<ZombieBehavior> behaviors
    ) {
        behaviors.add(new RangedAttackBehavior(
            RangedAttackBehavior.RangedAttackType.JUGGLE_BALL, 35, 5, 20));
        behaviors.add(new DamageReactionBehavior(
            DamageReactionBehavior.DamageReactionType.REFLECT_PROJECTILE,
            1.5f
        ));
    }

    private static void addWizardBehavior(
        List<ZombieBehavior> behaviors
    ) {
        behaviors.add(new TransformBehavior(
            TransformBehavior.TransformType.SHEEP_TRANSFORM,
            60,
            4
        ));
    }

    private static void addDarkKingBehavior(
        JsonNode data,
        Map<String, ArmorDefinition> armorRegistry,
        List<ZombieBehavior> behaviors
    ) {
        int interval = (int) (
            data.path("DelayBetweenKnightings").asDouble(2.5)
                * TICKS_PER_SECOND
        );
        int area = data.path("KnightingAreaX").asInt(4);
        List<ArmorDefinition> knightArmors = new ArrayList<>();
        ArmorDefinition crown    = armorRegistry.get("CrownDefault@ArmorTypes");
        ArmorDefinition shoulder = armorRegistry.get("ShoulderArmorDefault@ArmorTypes");
        if (crown != null) {
            knightArmors.add(crown);
        }
        if (shoulder != null) {
            knightArmors.add(shoulder);
        }
        behaviors.add(new AuraBehavior(
            AuraBehavior.AuraType.KNIGHT_NEARBY,
            area,
            interval,
            knightArmors
        ));
    }

    private static boolean addRemainingBehavior(
        String objclass,
        JsonNode data,
        List<ZombieBehavior> behaviors
    ) {
        switch (objclass) {
            case "ZombieCrystalSkullProps" -> addCrystalSkullBehavior(
                data, behaviors
            );
            case "ZombieProspectorProps" -> addProspectorBehavior(
                data, behaviors
            );
            case "ZombiePianoProps" -> addPianoBehavior(data, behaviors);
            case "ZombieModernAllStarProps" -> addAllStarBehavior(
                data, behaviors
            );
            case "ZombieLostCityJaneProps" -> addJaneBehavior(behaviors);
            case "ZombieArcadeProps" -> addArcadeBehavior(behaviors);
            case "ZombieBarrelRollerProps" -> addBarrelBehavior(behaviors);
            case "ZombieTurquoiseProps" -> behaviors.add(
                new TurquoiseLaserBehavior(4, 5, 25)
            );
            default -> {
                return false;
            }
        }
        return true;
    }

    private static void addCrystalSkullBehavior(
        JsonNode data,
        List<ZombieBehavior> behaviors
    ) {
        int cooldown = (int) (
            data.path("LaserCooldownTime").asDouble(5)
                * TICKS_PER_SECOND
        );
        behaviors.add(new RangedAttackBehavior(
            RangedAttackBehavior.RangedAttackType.LASER_BEAM,
            cooldown,
            data.path("LaserBeamLength").asInt(999),
            data.path("LaserBeamDamage").asInt(4001)
        ));


    }

    private static void addProspectorBehavior(
        JsonNode data,
        List<ZombieBehavior> behaviors
    ) {
        behaviors.add(new MovementBehavior(
            MovementBehavior.MovementType.PROSPECTOR_JUMP
        ));
        int delay = (int) (
            data.path("LaunchCountdown").asDouble(10)
                * TICKS_PER_SECOND
        );
        behaviors.add(new DynamiteBehavior(delay));
    }

    private static void addPianoBehavior(
        JsonNode data,
        List<ZombieBehavior> behaviors
    ) {
        behaviors.add(new MovementBehavior(
            MovementBehavior.MovementType.PIANO_CRUSH,
            (float) data.path("FastMoveSpeed").asDouble(0.4)
        ));
        behaviors.add(new WorldEffectBehavior(
            WorldEffectBehavior.WorldEffectType.RANDOM_LANE_SWAP, 30, 1));
    }

    private static void addAllStarBehavior(
        JsonNode data,
        List<ZombieBehavior> behaviors
    ) {
        behaviors.add(new InstantKillBehavior(
            0.3f, true,
            (float) data.path("RunningSpeedScale").asDouble(2.5)
        ));
    }

    private static void addJaneBehavior(
        List<ZombieBehavior> behaviors
    ) {
        behaviors.add(new DamageReactionBehavior(
            DamageReactionBehavior.DamageReactionType.DEFLECT_LOBBER
        ));
    }

    private static void addArcadeBehavior(
        List<ZombieBehavior> behaviors
    ) {
        behaviors.add(new PushObjectBehavior(
            PushObjectBehavior.PushType.ARCADE_MACHINE,
            1100,
            1
        ));
    }

    private static void addBarrelBehavior(
        List<ZombieBehavior> behaviors
    ) {
        behaviors.add(new PushObjectBehavior(
            PushObjectBehavior.PushType.BARREL,
            1100,
            1,
            "ZombieImp",
            2
        ));
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
