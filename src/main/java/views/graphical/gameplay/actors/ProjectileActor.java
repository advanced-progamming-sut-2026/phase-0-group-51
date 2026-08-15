package views.graphical.gameplay.actors;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import graphics.PvzGame;
import models.Plant.PlantTag;
import models.projectile.Projectile;
import views.graphical.animation.PamAnimationActor;

public class ProjectileActor extends Group {

    private static final String PEA_PAM =
            "768/INITIAL/EFFECTS/SLINGPEA_PROJECTILE/SLINGPEA_PROJECTILE.PAM";

    private static final String PEA_CLIP =
            "tier1";

    private static final String CABBAGE_PAM =
            "768/INITIAL/EFFECTS/CABBAGEPULT_PLANTFOOD_PROJECTILE/CABBAGEPULT_PLANTFOOD_PROJECTILE.PAM";

    private static final String CABBAGE_CLIP =
            "plantfood_cabbage";

    private static final float PEA_SCALE = 0.7f;
    private static final float CABBAGE_SCALE = 0.4f;

    private final Projectile projectile;
    private final PamAnimationActor animation;

    public ProjectileActor(
            PvzGame game,
            Projectile projectile
    ) {
        this.projectile = projectile;

        setTransform(true);
        setTouchable(Touchable.disabled);

        String pamPath =
                resolvePamPath(projectile);

        String clip =
                resolveClip(projectile);

        game.getPamPlayer().loadSync(
                pamPath
        );

        animation =
                game.createPamActor(
                        pamPath,
                        clip,
                        0f,
                        0f,
                        true
                );

        animation.setScale(
                resolveScale(projectile)
        );

        animation.setTouchable(
                Touchable.disabled
        );
        Image originMarker =
                new Image(
                        game.getSkin()
                                .newDrawable(
                                        "white_pixel",
                                        Color.RED
                                )
                );

        originMarker.setBounds(
                -3f,
                -3f,
                6f,
                6f
        );

        originMarker.setTouchable(
                Touchable.disabled
        );

        addActor(originMarker);
        addActor(animation);

        addActor(animation);
    }

    public void setProjectilePosition(
            float x,
            float y
    ) {
        setPosition(
                x,
                y
        );
    }

    private String resolvePamPath(
            Projectile projectile
    ) {
        if (isCabbageTestProjectile(projectile)) {
            return CABBAGE_PAM;
        }

        return PEA_PAM;
    }

    private String resolveClip(
            Projectile projectile
    ) {
        if (isCabbageTestProjectile(projectile)) {
            return CABBAGE_CLIP;
        }

        return PEA_CLIP;
    }

    private float resolveScale(
            Projectile projectile
    ) {
        if (isCabbageTestProjectile(projectile)) {
            return CABBAGE_SCALE;
        }

        return PEA_SCALE;
    }

    private boolean isCabbageTestProjectile(
            Projectile projectile
    ) {
        return projectile.getTargetX() != null
                && projectile.getTargetY() != null;
    }
}