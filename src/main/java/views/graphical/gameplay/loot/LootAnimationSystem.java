package views.graphical.gameplay.loot;

import com.badlogic.gdx.scenes.scene2d.Stage;
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

    private static final String PICKUP_CLIP =
        "idle";

    private static final float PICKUP_SCALE =
        0.4f;


    private final PamPlayer pamPlayer;
    private final Stage worldStage;
    private final BoardTransform boardTransform;
    private final ZombieAnimationSystem zombieAnimationSystem;


    private final Map<DroppedLoot, PamAnimationActor> pickupActors =
        new HashMap<>();



    public LootAnimationSystem(
        PamPlayer pamPlayer,
        Stage worldStage,
        BoardTransform boardTransform,
        ZombieAnimationSystem zombieAnimationSystem
    ) {

        this.pamPlayer = pamPlayer;
        this.worldStage = worldStage;
        this.boardTransform = boardTransform;
        this.zombieAnimationSystem = zombieAnimationSystem;


        pamPlayer.loadSync(
            PLANT_FOOD_PAM
        );
    }




    public void sync(
        Collection<Zombie> zombies,
        Collection<DroppedLoot> loots
    ) {

        syncPlantFoodPickups(
            loots
        );
    }





    private void syncPlantFoodPickups(
        Collection<DroppedLoot> loots
    ) {

        if (loots == null) {
            return;
        }


        for (DroppedLoot loot : loots) {


            if (loot == null
                || loot.getType() != LootType.PLANT_FOOD) {

                continue;
            }



            PamAnimationActor actor =
                pickupActors.get(
                    loot
                );



            if (actor == null) {


                actor =
                    new PamAnimationActor(
                        pamPlayer,
                        PLANT_FOOD_PAM,
                        PICKUP_CLIP,
                        true
                    );


                actor.restart();


                actor.setScale(
                    PICKUP_SCALE,
                    PICKUP_SCALE
                );


                pickupActors.put(
                    loot,
                    actor
                );


                worldStage.addActor(
                    actor
                );
            }

            actor.setPosition(

                boardTransform.getArea().x()
                    + loot.getX()
                    * boardTransform.tileWidth(),

                boardTransform.tileY(
                    loot.getLane()
                )
            );


            actor.toFront();
        }

        Iterator<Map.Entry<DroppedLoot, PamAnimationActor>> iterator =
            pickupActors.entrySet().iterator();


        while (iterator.hasNext()) {

            Map.Entry<DroppedLoot, PamAnimationActor> entry =
                iterator.next();


            if (!loots.contains(entry.getKey())) {

                entry.getValue().remove();

                iterator.remove();
            }
        }
    }





    public void clear() {

        for (PamAnimationActor actor :
            pickupActors.values()) {

            actor.remove();
        }


        pickupActors.clear();
    }
}
