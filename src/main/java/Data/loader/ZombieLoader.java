package Data.loader;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import models.Zombie.*;
import models.Zombie.Behavior.*;
import java.io.File;
import java.util.*;


public class ZombieLoader {

    private final Map<String, ArmorDefinition> armorRegistry = new HashMap<>();

    public void loadArmors(String jsonPath) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(new File(jsonPath));

        for (JsonNode entry : root) {
            String alias = entry.path("aliases").get(0).asText();
            JsonNode d   = entry.path("objdata");

            int     hp         = d.path("BaseHealth").asInt(300);
            boolean metallic   = false;
            boolean passDamage = false;

            for (JsonNode flag : d.path("ArmorFlags")) {
                String f = flag.asText();
                if (f.equals("metallic"))   metallic   = true;
                if (f.equals("passdamage")) passDamage = true;
            }

            List<Float> thresholds = new ArrayList<>();
            for (JsonNode t : d.path("ArmorLayerHealth"))
                thresholds.add((float) t.asDouble());

            ArmorDefinition def = new ArmorDefinition(alias, hp, metallic, passDamage, thresholds);

            armorRegistry.put(alias, def);
            armorRegistry.put(alias + "@ArmorTypes", def);
        }
    }


    public Map<String, Zombie> loadZombies(String jsonPath) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(new File(jsonPath));

        Map<String, Zombie> result = new LinkedHashMap<>();

        for (JsonNode entry : root) {
            String alias    = entry.path("aliases").get(0).asText();
            String objclass = entry.path("objclass").asText();
            JsonNode d      = entry.path("objdata");

            int   hp     = d.path("Hitpoints").asInt(190);
            float speed  = (float) d.path("Speed").asDouble(0.185);
            float dps    = (float) d.path("EatDPS").asDouble(100);
            int   wpc    = d.path("WavePointCost").asInt(100);
            int   weight = d.path("Weight").asInt(1000);

            Zombie zombie = new Zombie(alias, hp, speed, dps, wpc, weight);

            addBehaviors(zombie, objclass, d);

            result.put(alias, zombie);
        }
        return result;
    }

    private void addBehaviors(Zombie zombie, String objclass, JsonNode d) {
        switch (objclass) {

            case "ZombiePropertySheet" -> {
                for (JsonNode ap : d.path("ZombieArmorProps")) {
                    ArmorDefinition def = resolveRTID(ap.asText());
                    if (def != null) {
                        zombie.addBehavior(new ArmorBehavior(def));
                    }
                }
            }

            case "ZombieNewspaperProps" -> {
                for (JsonNode ap : d.path("ZombieArmorProps")) {
                    ArmorDefinition def = resolveRTID(ap.asText());
                    if (def != null) zombie.addBehavior(new ArmorBehavior(def));
                }
                float speedScale  = (float) d.path("EnragedSpeedScale").asDouble(4.0);
                float damageScale = (float) d.path("EnragedDamageScale").asDouble(4.0);
                zombie.addBehavior(
                    new DamageReactionBehavior(DamageReactionBehavior.DamageReactionType.NEWSPAPER_RAGE,
                        speedScale, damageScale));
            }

            case "ZombieGargantuarProps" -> {
                float throwPercent = 0.5f;
                JsonNode layers = d.path("HealthThresholdToImpAmmoLayers");
                if (layers.isArray() && !layers.isEmpty()) {
                    throwPercent = (float) layers.get(0)
                        .path("HealthPercentThrowImp").asDouble(0.5);
                }
                int    hp          = d.path("Hitpoints").asInt(3600);
                int    hpThreshold = (int)(hp * throwPercent);
                String impAlias    = "ZombieImp";

                zombie.addBehavior(
                    new SummonBehavior(SummonBehavior.SummonType.IMP_THROW, impAlias, 1, hpThreshold));
                zombie.addBehavior(
                    new DeathEffectBehavior(DeathEffectBehavior.DeathEffectType.SPAWN_IMP, impAlias, 1));
            }

            case "ZombieRaProps" -> {
                int maxSun = d.path("MaxClaimedSunCurrency").asInt(250);
                zombie.addBehavior(
                    new RangedAttackBehavior(RangedAttackBehavior.RangedAttackType.SUN_STEAL, 60, 999, maxSun));
                zombie.addBehavior(
                    new AuraBehavior(AuraBehavior.AuraType.STEAL_SUN_PASSIVE, 0, 120));
            }

            case "ZombieExplorerProps" -> {
                int reach = d.path("MaxTorchReach").asInt(37);
                zombie.addBehavior(
                    new RangedAttackBehavior(RangedAttackBehavior.RangedAttackType.TORCH_FIRE, 45, reach));
            }

            case "ZombieTombRaiserProps" -> {
                int   numTombs = d.path("NumberOfTombsToSpawn").asInt(2);
                float raisingCd= (float) d.path("TimeBetweenRaisings").asDouble(6);
                int   interval = (int)(raisingCd * 60);
                zombie.addBehavior(
                    new RangedAttackBehavior(RangedAttackBehavior.RangedAttackType.BONE_THROW, interval, 999));
                zombie.addBehavior(
                    new WorldEffectBehavior(WorldEffectBehavior.WorldEffectType.SPAWN_TOMB, interval, numTombs));
                zombie.addBehavior(
                    new DeathEffectBehavior(DeathEffectBehavior.DeathEffectType.TOMBSTONE_CRUMBLE));
            }

            case "ZombieIceAgeDodoProps" -> {
                List<String> flyOver = new ArrayList<>();
                for (JsonNode p : d.path("PlantsToFlyOver").path("List"))
                    flyOver.add(p.asText());
                zombie.addBehavior(new MovementBehavior(MovementBehavior.MovementType.FLY_OVER, flyOver));
            }

            case "ZombieIceAgeHunterProps" -> {
                int farRange = d.path("FarAttackRange").asInt(4);
                int barrage  = d.path("SnowballsPerBarrage").asInt(3);
                zombie.addBehavior(
                    new RangedAttackBehavior(RangedAttackBehavior.RangedAttackType.SNOWBALL, 30, farRange, barrage));
            }

            case "ZombieIceAgeTroglobiteProps" -> {
                int numBlocks = d.path("NumberOfIceblocksToSpawnWith").asInt(3);
                zombie.addBehavior(new MovementBehavior(MovementBehavior.MovementType.PUSH_ICE_BLOCK));
                zombie.addBehavior(
                    new WorldEffectBehavior(WorldEffectBehavior.WorldEffectType.FREEZE_COLUMN, 30, numBlocks));
            }

            case "ZombieBeachFishermanProps" -> {
                int   maxRange = d.path("CastingAreaMaxRange").asInt(8);
                float delay    = (float) d.path("DelayBetweenCasting").asDouble(2.5);
                zombie.addBehavior(
                    new RangedAttackBehavior(RangedAttackBehavior.RangedAttackType.HOOK_PULL,
                        (int)(delay * 60), maxRange));
            }

            case "ZombieBeachOctopusProps" ->
                zombie.addBehavior(
                    new RangedAttackBehavior(RangedAttackBehavior.RangedAttackType.OCTOPUS_NET, 40, 3));

            case "ZombieBeachSnorkelProps" -> {
                zombie.addBehavior(new MovementBehavior(MovementBehavior.MovementType.UNDERGROUND));
                zombie.addBehavior(
                    new DamageReactionBehavior(DamageReactionBehavior.DamageReactionType.SUBMERGE_DODGE));
            }

            case "ZombieDarkJugglerProps" -> {
                zombie.addBehavior(
                    new RangedAttackBehavior(RangedAttackBehavior.RangedAttackType.JUGGLE_BALL, 35, 5));
                zombie.addBehavior(
                    new DamageReactionBehavior(DamageReactionBehavior.DamageReactionType.REFLECT_PROJECTILE, 0.75f));
            }

            case "ZombieDarkWizardProps" -> {
                zombie.addBehavior(
                    new RangedAttackBehavior(RangedAttackBehavior.RangedAttackType.SPELL_SHEEP, 60, 4));
                zombie.addBehavior(
                    new TransformBehavior(TransformBehavior.TransformType.SHEEP_TRANSFORM, 60, 4));
            }

            case "ZombieDarkKingProps" -> {
                float delay   = (float) d.path("DelayBetweenKnightings").asDouble(2.5);
                int   area    = d.path("KnightingAreaX").asInt(4);
                int   interval= (int)(delay * 60);
                zombie.addBehavior(
                    new AuraBehavior(AuraBehavior.AuraType.BUFF_SPEED_NEARBY,  area, interval));
                zombie.addBehavior(
                    new AuraBehavior(AuraBehavior.AuraType.BUFF_DAMAGE_NEARBY, area, interval));
            }

            case "ZombieCrystalSkullProps" -> {
                int   damage   = d.path("LaserBeamDamage").asInt(4001);
                float cooldown = (float) d.path("LaserCooldownTime").asDouble(5);
                zombie.addBehavior(
                    new RangedAttackBehavior(RangedAttackBehavior.RangedAttackType.LASER_BEAM,
                        (int)(cooldown * 60), 999, damage));
            }

            case "ZombieProspectorProps" ->
                zombie.addBehavior(new MovementBehavior(MovementBehavior.MovementType.PROSPECTOR_JUMP));


            case "ZombiePianoProps" -> {
                float fastSpeed = (float) d.path("FastMoveSpeed").asDouble(0.4);
                zombie.addBehavior(new MovementBehavior(MovementBehavior.MovementType.PIANO_CRUSH, fastSpeed));
            }

            case "ZombieModernAllStarProps" -> {
                float runScale = (float) d.path("RunningSpeedScale").asDouble(0.5);
                zombie.addBehavior(new MovementBehavior(MovementBehavior.MovementType.TACKLE_RUN, runScale));
            }

            case "ZombieLostCityJaneProps" ->
                zombie.addBehavior(
                    new DamageReactionBehavior(DamageReactionBehavior.DamageReactionType.REFLECT_PROJECTILE, 1.0f));

            case "ZombieArcadeProps" ->
                zombie.addBehavior(new MovementBehavior(MovementBehavior.MovementType.PUSH_PLANT_BACK));

            default -> {
            }
        }
    }

    private ArmorDefinition resolveRTID(String rtid) {
        String key = rtid.replace("RTID(", "").replace(")", "").trim();
        return armorRegistry.get(key);
    }
}
