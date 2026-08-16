package views.graphical.gameplay.actors;

import Data.loader.ProjectileVisualData;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import graphics.PvzGame;
import models.projectile.Projectile;
import views.graphical.animation.PamAnimationActor;

public class ProjectileActor extends Group {

    private final Projectile projectile;
    private final PamAnimationActor animation;

    public ProjectileActor(
            PvzGame game,
            Projectile projectile,
            ProjectileVisualData visual
    ) {
        this.projectile = projectile;

        setTransform(true);
        setTouchable(Touchable.disabled);

        game.getPamPlayer().loadSync(visual.pamPath());

        animation = game.createPamActor(
                visual.pamPath(),
                visual.clip(),
                0f,
                0f,
                true
        );

        animation.setScale(visual.scale());
        animation.setTouchable(Touchable.disabled);
        addActor(animation);
    }

    public void setProjectilePosition(
            float x,
            float y
    ) {
        setPosition(x, y);
    }
}
