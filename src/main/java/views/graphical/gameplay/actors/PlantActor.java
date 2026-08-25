package views.graphical.gameplay.actors;

import Data.loader.PlantAnimationData;
import Data.loader.PlantData;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import graphics.PvzGame;
import models.Plant.Plant;
import lombok.Getter;
import views.graphical.animation.PamAnimationActor;
import views.graphical.gameplay.effects.EffectPamFactory;

import java.util.List;

public class PlantActor extends Group {

    private static final float PREVIEW_ALPHA = 0.58f;
    public static final float BOARD_SCALE = 0.65f;

    private static final String PLANT_FOOD_EFFECT_PAM =
        "768/INITIAL/EFFECTS/PLANTFOOD_FX/PLANTFOOD_FX.PAM";

    private static final String FROST_LEVEL_ONE_ASSET =
        "IMAGE_EFFECTS_FROSTBITE_CHILL_PLANT_FROSTBITE_CHILL_PLANT_153X62";
    private static final String FROST_LEVEL_TWO_ASSET =
        "IMAGE_EFFECTS_FROSTBITE_CHILL_PLANT_FROSTBITE_CHILL_PLANT_153X79";
    // Kept as a runtime fallback because the supplied address had two leading I characters.
    private static final String FROST_LEVEL_TWO_ASSET_ALTERNATE =
        "IIMAGE_EFFECTS_FROSTBITE_CHILL_PLANT_FROSTBITE_CHILL_PLANT_153X79";

    private static final String ICE_BLOCK_PAM =
        "768/FULL/EFFECTS/FROSTBITE_ICE_BLOCK_PLANT/"
            + "FROSTBITE_ICE_BLOCK_PLANT.PAM";
    private static final String FREEZE_START_CLIP = "freeze_start";
    private static final String FREEZE_IDLE_CLIP = "freeze_idle";

    private static final String SHEEP_PAM =
        "768/FULL/EFFECTS/DARK_WIZARD_SHEEPENING/DARK_WIZARD_SHEEPENING.PAM";
    private static final String SHEEP_INTRO_CLIP = "animation";
    private static final String SHEEP_IDLE_CLIP = "idle";
    private static final String SHEEP_OUTRO_CLIP = "animation2";
    private static final float SHEEP_INTRO_SECONDS = 1.7f;
    private static final float SHEEP_OUTRO_SECONDS = 1.1f;
    private static final String OCTOPUS_PAM =
        "768/FULL/EFFECTS/ZOMBIE_OCTOPUS_PROJECTILE/ZOMBIE_OCTOPUS_PROJECTILE.PAM";
    private static final float OCTOPUS_FLIGHT_SECONDS = 0.3f;
    private static final float OCTOPUS_LAND_SECONDS = 0.9667f;
    private static final float OCTOPUS_DIE_SECONDS = 2.0f;
    private static final float OCTOPUS_SCALE = 1.12f;

    private static final String[] ICE_DAMAGE_PARTS = {
        "ice_block_damage0",
        "ice_block_damage1",
        "ice_block_damage2",
        "ice_block_damage3",
        "ice_block_damage4",
        "ice_block_damage5"
    };

    private static final float CHILL_OFFSET_X = 0f;
    private static final float CHILL_OFFSET_Y = -20f;
    private static final float ICE_BLOCK_OFFSET_X = 0f;
    private static final float ICE_BLOCK_OFFSET_Y = -20f;
    private static final float FREEZE_START_FALLBACK_DURATION = 0.8f;

    private static final float DAMAGE_FLASH_DURATION = 0.15f;
    private static final float DAMAGE_FLASH_ALPHA = 0.65f;
    private static final float DAMAGE_FLASH_COOLDOWN = 0.4f;
    private static final float PLANT_FOOD_EFFECT_SCALE = 1.35f;
    private static final float PLANT_FOOD_EFFECT_OFFSET_X = 20f;
    private static final float PLANT_FOOD_EFFECT_OFFSET_Y = 120f;
    // Key looked up in the plant's own "animations" map (plants.json) for a
    // plant-specific sprite animation to play while the plant is buffed by
    // Plant Food. Plants without this key just keep their normal animation.
    private static final String PLANT_FOOD_ANIMATION_KEY = "plantFood";

    private static final float SQUASH_ARC_HEIGHT = 55f;
    private static final float SQUASH_STATIONARY_FRACTION = 0.45f;
    private static final float SQUASH_ATTACK_FALLBACK_DURATION = 0.8f;
    private static final float SQUASH_JUMP_DOWN_FALLBACK_DURATION = 0.8f;

    private final PvzGame game;
    private String baseAnimationKey;

    private boolean temporaryAnimation;
    private boolean terminalAnimation;

    private boolean plantFoodAnimationActive;
    private PlantAnimationData plantFoodAnimationData;
    private boolean plantFoodAnimationPlaying;
    private float plantFoodAnimationTimeRemaining;

    private float animationTimeRemaining;
    @Getter
    private PlantData plantData;
    private PamAnimationActor animation;
    private PamAnimationActor plantFoodEffect;
    private PamAnimationActor octopusEffect;
    private PamAnimationActor sheepEffect;
    private boolean transformedShown;
    private int sheepPhase;
    private float sheepPhaseRemaining;

    private int octopusPhase;
    private float octopusPhaseRemaining;

    private Image frostChillEffect;
    private PamAnimationActor iceBlockEffect;
    private int shownFrostLevel;
    private int shownIceDamageStage = -1;
    private int syncedIceHealth;
    private boolean freezeStartPlaying;
    private float freezeStartRemaining;

    private float damageFlashCooldownRemaining;

    private boolean previewMode;

    private final Vector2 cursorPosition = new Vector2();

    public PlantActor(PvzGame game) {
        this.game = game;
        setTransform(true);

        setTouchable(Touchable.disabled);
        setVisible(false);
    }

    public void setPlant(PlantData plantData) {
        clearPlant();

        if (plantData == null) {
            return;
        }

        this.plantData = plantData;

        if (plantData.idlePamPath() == null
            || plantData.idlePamPath().isBlank()
            || plantData.idleClip() == null
            || plantData.idleClip().isBlank()) {
            return;
        }

        game.getPamPlayer().loadSync(
            plantData.idlePamPath()
        );

        animation = game.createPamActor(
            plantData.idlePamPath(),
            plantData.idleClip(),
            0f,
            0f,
            true
        );

        animation.setTouchable(Touchable.disabled);

        addActor(animation);

        applyVisualMode();

        setVisible(true);
    }

    public void setPreviewMode(boolean previewMode) {
        this.previewMode = previewMode;

        if (previewMode) {
            clearFrostVisual();
        }

        applyVisualMode();
    }

    private void applyVisualMode() {
        if (animation == null) {
            return;
        }

        if (previewMode) {
            animation.setColor(
                1f,
                1f,
                1f,
                PREVIEW_ALPHA
            );
            setScale(BOARD_SCALE);
        } else {
            animation.setColor(
                1f,
                1f,
                1f,
                1f
            );
            setScale(BOARD_SCALE);
        }
    }

    public void setPlantScale(float scale) {
        if (scale <= 0f) {
            throw new IllegalArgumentException(
                "Plant scale must be positive."
            );
        }

        setScale(scale);
    }

    public void flashDamage() {
        if (animation == null
            || previewMode
            || damageFlashCooldownRemaining > 0f) {
            return;
        }

        animation.flashAdditive(
            DAMAGE_FLASH_DURATION,
            DAMAGE_FLASH_ALPHA
        );

        damageFlashCooldownRemaining =
            DAMAGE_FLASH_COOLDOWN;
    }

    public void syncOctopusVisual(boolean hasOctopus) {
        if (previewMode) {
            if (octopusEffect != null) {
                octopusEffect.remove();
                octopusEffect = null;
            }
            octopusPhase = 0;
            return;
        }

        if (hasOctopus && octopusPhase == 0) {
            showOctopusClip("animation", false);
            octopusPhase = 1;
            octopusPhaseRemaining = OCTOPUS_FLIGHT_SECONDS;
        } else if (!hasOctopus && octopusPhase != 0 && octopusPhase != 4) {
            showOctopusClip("die", false);
            octopusPhase = 4;
            octopusPhaseRemaining = OCTOPUS_DIE_SECONDS;
        }
    }

    private void showOctopusClip(String clip, boolean loop) {
        if (octopusEffect != null) {
            octopusEffect.remove();
            octopusEffect = null;
        }
        try {
            octopusEffect = game.createPamActor(
                OCTOPUS_PAM,
                clip,
                0f,
                0f,
                loop
            );
            octopusEffect.setScale(OCTOPUS_SCALE, OCTOPUS_SCALE);
            octopusEffect.setTouchable(Touchable.disabled);
            addActor(octopusEffect);
            octopusEffect.toBack();
        } catch (RuntimeException ignored) {
            octopusEffect = null;
        }
    }

    private void updateOctopusSequence(float delta) {
        if (octopusPhase == 0 || octopusPhase == 3) {
            return;
        }
        octopusPhaseRemaining -= Math.max(0f, delta);
        if (octopusPhaseRemaining > 0f) {
            return;
        }
        if (octopusPhase == 1) {
            showOctopusClip("animation2", false);
            octopusPhase = 2;
            octopusPhaseRemaining = OCTOPUS_LAND_SECONDS;
        } else if (octopusPhase == 2) {
            showOctopusClip("animation4", true);
            octopusPhase = 3;
        } else if (octopusPhase == 4) {
            if (octopusEffect != null) {
                octopusEffect.remove();
                octopusEffect = null;
            }
            octopusPhase = 0;
        }
    }

    public void flashOctopusDamage() {
        if (octopusEffect == null
            || previewMode) {
            return;
        }

        octopusEffect.flashAdditive(
            DAMAGE_FLASH_DURATION,
            DAMAGE_FLASH_ALPHA
        );
    }

    public void playSquashJump(
        float targetX,
        float targetY,
        Runnable onLanding,
        Runnable onFinished
    ) {
        Runnable landing = onLanding == null
            ? () -> { }
            : onLanding;
        Runnable completion = onFinished == null
            ? () -> { }
            : onFinished;

        clearActions();

        if (!playOneShotAnimation("attack")) {
            setPosition(targetX, targetY);
            landing.run();
            completion.run();
            return;
        }

        float attackDuration = animationDuration(
            "attack",
            SQUASH_ATTACK_FALLBACK_DURATION
        );
        float jumpDownDuration = animationDuration(
            "jumpDown",
            SQUASH_JUMP_DOWN_FALLBACK_DURATION
        );

        float stationaryDuration =
            attackDuration * SQUASH_STATIONARY_FRACTION;
        float travelDuration = Math.max(
            0.05f,
            attackDuration - stationaryDuration
        );
        float firstHalfDuration = travelDuration * 0.5f;
        float secondHalfDuration = travelDuration - firstHalfDuration;

        float middleX = (getX() + targetX) * 0.5f;
        float middleY = Math.max(getY(), targetY) + SQUASH_ARC_HEIGHT;

        addAction(
            Actions.sequence(
                Actions.delay(stationaryDuration),
                Actions.moveTo(
                    middleX,
                    middleY,
                    firstHalfDuration,
                    Interpolation.sineOut
                ),

                Actions.moveTo(
                    targetX,
                    targetY,
                    secondHalfDuration,
                    Interpolation.sineIn
                ),
                Actions.run(() -> {
                    setPosition(targetX, targetY);
                    playOneShotAnimation("jumpDown");
                    landing.run();
                }),
                Actions.delay(jumpDownDuration),
                Actions.run(completion)
            )
        );
    }

    private boolean playOneShotAnimation(String key) {
        if (plantData == null
            || animation == null
            || !plantData.hasAnimation(key)) {
            return false;
        }

        PlantAnimationData data = plantData.animation(key);

        temporaryAnimation = false;
        terminalAnimation = false;
        animationTimeRemaining = 0f;

        animation.play(data.clip(), false);
        animation.restart();
        return true;
    }

    private float animationDuration(
        String key,
        float fallback
    ) {
        if (plantData == null || !plantData.hasAnimation(key)) {
            return fallback;
        }

        float duration = plantData.animation(key).duration();
        return duration > 0f ? duration : fallback;
    }
    public void playPlantFoodAnimation() {
        if (plantData == null
            || animation == null
            || !plantData.hasAnimation(
            PLANT_FOOD_ANIMATION_KEY
        )) {
            return;
        }

        if (terminalAnimation) {
            return;
        }

        PlantAnimationData data =
            plantData.animation(
                PLANT_FOOD_ANIMATION_KEY
            );

        plantFoodAnimationData = data;
        plantFoodAnimationPlaying = true;

        if (data.loop()) {
            plantFoodAnimationTimeRemaining = 0f;
        } else {
            plantFoodAnimationTimeRemaining =
                Math.max(
                    0f,
                    data.duration()
                );
        }

        temporaryAnimation = false;
        animationTimeRemaining = 0f;

        animation.play(
            data.clip(),
            data.loop()
        );

        animation.restart();
    }

    public void playPlantFoodEffect() {
        if (previewMode || plantData == null) {
            return;
        }

        if (plantFoodEffect != null) {
            plantFoodEffect.remove();
            plantFoodEffect = null;
        }

        try {
            EffectPamFactory.OneShot effect =
                EffectPamFactory.create(
                    game,
                    PLANT_FOOD_EFFECT_PAM,
                    PLANT_FOOD_EFFECT_SCALE,
                    1.0f,
                    "plantfood",
                    "plant_food",
                    "effect",
                    "animation",
                    "anim"
                );

            plantFoodEffect = effect.actor();
            plantFoodEffect.setPosition(
                PLANT_FOOD_EFFECT_OFFSET_X,
                PLANT_FOOD_EFFECT_OFFSET_Y
            );
            addActorAt(0, plantFoodEffect);

            plantFoodEffect.addAction(
                Actions.sequence(
                    Actions.delay(effect.duration()),
                    Actions.run(() -> plantFoodEffect = null),
                    Actions.removeActor()
                )
            );
        } catch (RuntimeException e) {
            if (Gdx.app != null) {
                Gdx.app.error(
                    "PlantActor",
                    "Could not play Plant Food effect.",
                    e
                );
            }
        }
    }

    public void syncFrost(int frostLevel, int iceHealth) {
        if (previewMode || animation == null) {
            clearFrostVisual();
            return;
        }

        int clampedLevel = Math.max(
            0,
            Math.min(Plant.MAX_FROST_LEVEL, frostLevel)
        );

        syncedIceHealth = Math.max(0, iceHealth);

        if (clampedLevel == 0) {
            clearFrostVisual();
            return;
        }

        if (clampedLevel == 1 || clampedLevel == 2) {
            showChillLevel(clampedLevel);
            return;
        }

        if (syncedIceHealth <= 0) {
            clearFrostVisual();
            return;
        }

        showFrozenIceBlock();
    }

    private void showChillLevel(int frostLevel) {
        removeIceBlockEffect();

        if (animation != null) {
            animation.resumeAnimation();
        }

        if (shownFrostLevel == frostLevel && frostChillEffect != null) {
            return;
        }

        removeChillEffect();

        TextureRegion region = chillRegion(frostLevel);

        frostChillEffect = new Image(
            new TextureRegionDrawable(region)
        );
        frostChillEffect.setTouchable(Touchable.disabled);

        float width = region.getRegionWidth();
        float height = region.getRegionHeight();

        frostChillEffect.setSize(width, height);
        frostChillEffect.setPosition(
            -width / 2f + CHILL_OFFSET_X,
            -height / 2f + CHILL_OFFSET_Y
        );

        /*
         * No setScale(...) here. The effect is inside PlantActor and therefore
         * inherits exactly the same scale as the plant (BOARD_SCALE by default).
         */
        addActor(frostChillEffect);
        shownFrostLevel = frostLevel;
        shownIceDamageStage = -1;
    }

    private TextureRegion chillRegion(int frostLevel) {
        if (frostLevel == 1) {
            TextureRegion region = tryRegion(FROST_LEVEL_ONE_ASSET);
            if (region != null) {
                return region;
            }
            throw new IllegalStateException(
                "Missing frost level 1 asset: "
                    + FROST_LEVEL_ONE_ASSET
            );
        }

        TextureRegion region = tryRegion(FROST_LEVEL_TWO_ASSET);
        if (region == null) {
            region = tryRegion(FROST_LEVEL_TWO_ASSET_ALTERNATE);
        }
        if (region != null) {
            return region;
        }

        throw new IllegalStateException(
            "Missing frost level 2 asset. Tried: "
                + FROST_LEVEL_TWO_ASSET
                + " and "
                + FROST_LEVEL_TWO_ASSET_ALTERNATE
        );
    }

    private TextureRegion tryRegion(String assetId) {
        try {
            return game.getTextureBank().region(assetId);
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private void showFrozenIceBlock() {
        removeChillEffect();

        if (animation != null) {
            animation.pauseAnimation();
        }

        if (iceBlockEffect == null || shownFrostLevel != Plant.MAX_FROST_LEVEL) {
            startFreezeAnimation();
            return;
        }

        if (!freezeStartPlaying) {
            applyIceDamageStage(
                resolveIceDamageStage(syncedIceHealth)
            );
        }
    }

    private void startFreezeAnimation() {
        removeIceBlockEffect();

        game.getPamPlayer().loadSync(ICE_BLOCK_PAM);

        iceBlockEffect = game.createPamActor(
            ICE_BLOCK_PAM,
            FREEZE_START_CLIP,
            ICE_BLOCK_OFFSET_X,
            ICE_BLOCK_OFFSET_Y,
            false
        );
        iceBlockEffect.setTouchable(Touchable.disabled);

        /*
         * No setScale(...) here either: the ice block inherits PlantActor's
         * exact plant scale.
         */
        iceBlockEffect.setVisibleParts(List.of());
        addActor(iceBlockEffect);

        shownFrostLevel = Plant.MAX_FROST_LEVEL;
        shownIceDamageStage = -1;
        freezeStartPlaying = true;
        freezeStartRemaining = freezeStartDuration();
    }

    private float freezeStartDuration() {
        try {
            return Math.max(
                0.05f,
                game.getPamPlayer().clipDurationSeconds(
                    ICE_BLOCK_PAM,
                    FREEZE_START_CLIP
                )
            );
        } catch (RuntimeException ignored) {
            return FREEZE_START_FALLBACK_DURATION;
        }
    }

    private void updateFreezeTransition(float delta) {
        if (!freezeStartPlaying || iceBlockEffect == null) {
            return;
        }

        freezeStartRemaining -= Math.max(0f, delta);

        if (freezeStartRemaining > 0f) {
            return;
        }

        freezeStartPlaying = false;

        iceBlockEffect.play(FREEZE_IDLE_CLIP, true);
        iceBlockEffect.restart();

        applyIceDamageStage(
            resolveIceDamageStage(syncedIceHealth)
        );
    }

    private int resolveIceDamageStage(int iceHealth) {
        int clampedHealth = Math.max(
            1,
            Math.min(Plant.ICE_MAX_HEALTH, iceHealth)
        );

        int damageTaken =
            Plant.ICE_MAX_HEALTH - clampedHealth;

        return Math.min(
            ICE_DAMAGE_PARTS.length - 1,
            damageTaken / 100
        );
    }

    private void applyIceDamageStage(int stage) {
        if (iceBlockEffect == null || freezeStartPlaying) {
            return;
        }

        int clampedStage = Math.max(
            0,
            Math.min(ICE_DAMAGE_PARTS.length - 1, stage)
        );

        if (shownIceDamageStage == clampedStage) {
            return;
        }

        shownIceDamageStage = clampedStage;

        iceBlockEffect.setVisibleParts(
            List.of(ICE_DAMAGE_PARTS[clampedStage])
        );
    }

    private void clearFrostVisual() {
        boolean hadFrostVisual =
            shownFrostLevel != 0
                || frostChillEffect != null
                || iceBlockEffect != null;

        removeChillEffect();
        removeIceBlockEffect();

        if (hadFrostVisual && animation != null) {
            animation.resumeAnimation();
        }

        shownFrostLevel = 0;
        shownIceDamageStage = -1;
        syncedIceHealth = 0;
        freezeStartPlaying = false;
        freezeStartRemaining = 0f;
    }

    private void removeChillEffect() {
        if (frostChillEffect != null) {
            frostChillEffect.remove();
            frostChillEffect = null;
        }
    }

    private void removeIceBlockEffect() {
        if (iceBlockEffect != null) {
            iceBlockEffect.remove();
            iceBlockEffect = null;
        }

        freezeStartPlaying = false;
        freezeStartRemaining = 0f;
        shownIceDamageStage = -1;
    }

    public void setBaseAnimation(String key) {
        if (key != null && key.equals(baseAnimationKey)) {
            return;
        }
        baseAnimationKey = key;
        if (!temporaryAnimation && !terminalAnimation) {
            if (plantFoodAnimationPlaying) {
                return;
            }
            if (key != null && plantData.hasAnimation(key)) {
                playAnimation(key);
            } else {
                playFallbackIdle();
            }
        }
    }

    public void syncPlantFoodAnimation(boolean active) {
        if (plantData == null || animation == null) {
            return;
        }

        if (active == plantFoodAnimationActive) {
            return;
        }

        plantFoodAnimationActive = active;

        if (active) {
            return;
        }

        // A one-shot animation is allowed to finish on its own.
        // Only a looping Plant Food clip depends on isOnPlantFood().
        if (plantFoodAnimationPlaying
            && plantFoodAnimationData != null
            && plantFoodAnimationData.loop()) {

            plantFoodAnimationPlaying = false;
            plantFoodAnimationData = null;
            plantFoodAnimationTimeRemaining = 0f;

            if (!temporaryAnimation && !terminalAnimation) {
                restoreIdleAnimation();
            }
        }
    }

    private void updatePlantFoodAnimation(
        float delta
    ) {
        if (!plantFoodAnimationPlaying
            || plantFoodAnimationData == null) {
            return;
        }

        if (plantFoodAnimationData.loop()) {
            return;
        }

        plantFoodAnimationTimeRemaining -=
            Math.max(0f, delta);

        if (plantFoodAnimationTimeRemaining > 0f) {
            return;
        }

        plantFoodAnimationPlaying = false;
        plantFoodAnimationData = null;
        plantFoodAnimationTimeRemaining = 0f;

        if (!temporaryAnimation
            && !terminalAnimation) {
            restoreIdleAnimation();
        }
    }

    private void restoreIdleAnimation() {
        if (plantFoodAnimationPlaying
            && plantFoodAnimationData != null) {

            animation.play(
                plantFoodAnimationData.clip(),
                plantFoodAnimationData.loop()
            );

            animation.restart();
            return;
        }

        if (baseAnimationKey != null
            && plantData.hasAnimation(baseAnimationKey)) {
            playAnimation(baseAnimationKey);
        } else {
            playFallbackIdle();
        }
    }
    private void playFallbackIdle() {
        if (plantData == null || animation == null) {
            return;
        }

        animation.play(
            plantData.idleClip(),
            true
        );
    }

    public void playTemporaryAnimation(String key) {
        if (plantData == null || animation == null || !plantData.hasAnimation(key)) {
            return;
        }
        if (plantFoodAnimationPlaying) {
            return;
        }
        PlantAnimationData data = plantData.animation(key);

        temporaryAnimation = true;
        terminalAnimation = false;
        animationTimeRemaining = data.duration();

        animation.play(data.clip(), data.loop());

        animation.restart();
    }
    public void playTerminalAnimation(String key) {
        if (plantData == null || animation == null || !plantData.hasAnimation(key)) {
            return;
        }

        PlantAnimationData data = plantData.animation(key);

        temporaryAnimation = false;
        terminalAnimation = true;
        animationTimeRemaining = data.duration();
        animation.play(data.clip(), data.loop());
        animation.restart();
    }

    private void playAnimation(String key) {
        if (plantData == null || animation == null || !plantData.hasAnimation(key)) {
            return;
        }

        PlantAnimationData data = plantData.animation(key);

        animation.play(data.clip(), data.loop());
    }

    public void syncTransformed(boolean transformed) {
        if (transformed == transformedShown) {
            return;
        }
        transformedShown = transformed;
        if (transformed) {
            showSheepClip(SHEEP_INTRO_CLIP, false);
            sheepPhase = 1;
            sheepPhaseRemaining = SHEEP_INTRO_SECONDS;
            if (animation != null) {
                animation.setVisible(false);
            }
        } else if (sheepPhase != 0) {
            showSheepClip(SHEEP_OUTRO_CLIP, false);
            sheepPhase = 3;
            sheepPhaseRemaining = SHEEP_OUTRO_SECONDS;
        }
    }

    private void showSheepClip(String clip, boolean loop) {
        if (sheepEffect != null) {
            sheepEffect.remove();
            sheepEffect = null;
        }
        game.getPamPlayer().loadSync(SHEEP_PAM);
        sheepEffect = game.createPamActor(
            SHEEP_PAM,
            clip,
            0f,
            0f,
            loop
        );
        sheepEffect.setTouchable(Touchable.disabled);
        addActor(sheepEffect);
        sheepEffect.toFront();
    }

    private void updateSheepTransform(float delta) {
        if (sheepPhase != 1 && sheepPhase != 3) {
            return;
        }
        sheepPhaseRemaining -= Math.max(0f, delta);
        if (sheepPhaseRemaining > 0f) {
            return;
        }
        if (sheepPhase == 1) {
            showSheepClip(SHEEP_IDLE_CLIP, true);
            sheepPhase = 2;
        } else {
            if (sheepEffect != null) {
                sheepEffect.remove();
                sheepEffect = null;
            }
            if (animation != null) {
                animation.setVisible(true);
            }
            sheepPhase = 0;
        }
    }

    public void clearPlant() {
        clearChildren();

        animation = null;
        plantFoodEffect = null;
        octopusEffect = null;
        sheepEffect = null;
        transformedShown = false;
        sheepPhase = 0;
        sheepPhaseRemaining = 0f;
        octopusPhase = 0;
        octopusPhaseRemaining = 0f;
        frostChillEffect = null;
        iceBlockEffect = null;
        plantData = null;

        shownFrostLevel = 0;
        shownIceDamageStage = -1;
        syncedIceHealth = 0;
        freezeStartPlaying = false;
        freezeStartRemaining = 0f;

        baseAnimationKey = null;
        temporaryAnimation = false;
        terminalAnimation = false;
        animationTimeRemaining = 0f;
        damageFlashCooldownRemaining = 0f;
        plantFoodAnimationActive = false;
        plantFoodAnimationData = null;
        plantFoodAnimationPlaying = false;
        plantFoodAnimationTimeRemaining = 0f;

        setVisible(false);
    }

    @Override
    public void act(float delta) {
        super.act(delta);

        if (damageFlashCooldownRemaining > 0f) {
            damageFlashCooldownRemaining = Math.max(
                0f,
                damageFlashCooldownRemaining - Math.max(0f, delta)
            );
        }

        if (!previewMode) {
            updateFreezeTransition(delta);
            updateAnimationState(delta);
            updatePlantFoodAnimation(delta);
            updateSheepTransform(delta);
            updateOctopusSequence(delta);
        }

        if (!previewMode
            || !isVisible()
            || getStage() == null) {
            return;
        }

        cursorPosition.set(
            Gdx.input.getX(),
            Gdx.input.getY()
        );

        getStage().screenToStageCoordinates(
            cursorPosition
        );

        setPosition(
            cursorPosition.x,
            cursorPosition.y
        );
    }
    private void updateAnimationState(
        float delta
    ) {
        if (!temporaryAnimation
            && !terminalAnimation) {
            return;
        }

        animationTimeRemaining -= delta;

        if (animationTimeRemaining > 0f) {
            return;
        }

        if (terminalAnimation) {
            remove();
            return;
        }

        temporaryAnimation = false;
        restoreIdleAnimation();
    }
}
