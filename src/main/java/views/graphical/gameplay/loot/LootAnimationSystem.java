package views.graphical.gameplay.loot;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import graphics.PvzGame;
import models.Zombie.Zombie;
import models.enums.LootType;
import models.items.DroppedLoot;
import pvz.libpvz.pam.PamPlayer;
import views.graphical.animation.PamAnimationActor;
import views.graphical.gameplay.board.BoardTransform;
import views.graphical.gameplay.zombie.ZombieAnimationSystem;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class LootAnimationSystem {

    private static final String PLANT_FOOD_PAM =
        "768/INITIAL/EFFECTS/PLANTFOOD_PICKUP/PLANTFOOD_PICKUP.PAM";

    private static final String GEM_PAM =
        "768/INITIAL/EFFECTS/COIN_DIAMOND/COIN_DIAMOND.PAM";

    private static final String COIN_PAM =
        "768/INITIAL/EFFECTS/COIN_GOLD/COIN_GOLD.PAM";

    private static final String POT_IMAGE =
        "IMAGE_ZEN_GARDEN_GROWING_PLANT_SLOT_GROWING_PLANT_SLOT_184X161";

    private static final float PLANT_FOOD_SCALE = 0.4f;
    private static final float GEM_SCALE = 0.25f;
    private static final float COIN_SCALE = 0.3f;
    private static final float POT_SCALE = 0.45f;

    private final PamPlayer pamPlayer;
    private final Stage worldStage;
    private final BoardTransform boardTransform;
    private final ZombieAnimationSystem zombieAnimationSystem;
    private final PvzGame game;

    private final Map<DroppedLoot, Actor> pickupActors =
        new HashMap<>();

    public LootAnimationSystem(
        PamPlayer pamPlayer,
        Stage worldStage,
        BoardTransform boardTransform,
        ZombieAnimationSystem zombieAnimationSystem,
        PvzGame game
    ) {
        this.pamPlayer = pamPlayer;
        this.worldStage = worldStage;
        this.boardTransform = boardTransform;
        this.zombieAnimationSystem = zombieAnimationSystem;
        this.game = game;

        pamPlayer.loadSync(PLANT_FOOD_PAM);
        pamPlayer.loadSync(GEM_PAM);
        pamPlayer.loadSync(COIN_PAM);
    }

    public void sync(
        Collection<Zombie> zombies,
        Collection<DroppedLoot> loots
    ) {
        syncLoot(loots);
    }

    private void syncLoot(Collection<DroppedLoot> loots) {

        if (loots == null) {
            return;
        }

        for (DroppedLoot loot : loots) {

            if (loot == null) {
                continue;
            }

            Actor actor = pickupActors.get(loot);

            if (actor == null) {

                actor = createActor(loot);

                if (actor == null) {
                    continue;
                }

                if (actor instanceof PamAnimationActor pamActor) {
                    pamActor.restart();
                }

                pickupActors.put(loot, actor);
                worldStage.addActor(actor);
            }

            float x =
                boardTransform.getArea().x()
                    + loot.getX()
                    * boardTransform.tileWidth();

            float y =
                boardTransform.tileY(
                    loot.getLane()
                );

            if (actor instanceof Image) {
                actor.setPosition(
                    x,
                    y
                );
            } else {
                actor.setPosition(x, y);
            }

            actor.toFront();
        }

        Iterator<Map.Entry<DroppedLoot, Actor>> iterator =
            pickupActors.entrySet().iterator();

        while (iterator.hasNext()) {

            Map.Entry<DroppedLoot, Actor> entry =
                iterator.next();

            if (!loots.contains(entry.getKey())) {
                entry.getValue().remove();
                iterator.remove();
            }
        }
    }

    private Actor createActor(DroppedLoot loot) {

        switch (loot.getType()) {

            case PLANT_FOOD: {
                PamAnimationActor actor =
                    new PamAnimationActor(
                        pamPlayer,
                        PLANT_FOOD_PAM,
                        "idle",
                        true
                    );

                actor.setScale(
                    PLANT_FOOD_SCALE,
                    PLANT_FOOD_SCALE
                );

                return actor;
            }

            case GEM: {
                PamAnimationActor actor =
                    new PamAnimationActor(
                        pamPlayer,
                        GEM_PAM,
                        "idle",
                        true
                    );

                actor.setScale(
                    GEM_SCALE,
                    GEM_SCALE
                );

                return actor;
            }

            case COIN: {
                PamAnimationActor actor =
                    new PamAnimationActor(
                        pamPlayer,
                        COIN_PAM,
                        "animation",
                        true
                    );

                actor.setScale(
                    COIN_SCALE,
                    COIN_SCALE
                );

                return actor;
            }

            case POT: {

                Image pot =
                    new Image(
                        new TextureRegionDrawable(
                            game.getTextureBank()
                                .region(POT_IMAGE)
                        )
                    );

                pot.setScale(POT_SCALE);
                pot.setTouchable(Touchable.disabled);

                return pot;
            }

            default:
                return null;
        }
    }

    public void clear() {

        for (Actor actor : pickupActors.values()) {
            actor.remove();
        }

        pickupActors.clear();
    }
}
