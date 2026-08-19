package views.graphical.gameplay.manager;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import graphics.PvzGame;
import models.effects.VisualEffectEvent;
import views.graphical.animation.PamAnimationActor;
import views.graphical.gameplay.board.BoardArea;
import views.graphical.gameplay.board.BoardTransform;
import views.graphical.gameplay.effects.EffectPamFactory;

public final class WorldEffectManager extends Group {

    private static final String EXPLOSION_PAM =
        "768/INITIAL/EFFECTS/TELEPORTATO_EXPLOSION/TELEPORTATO_EXPLOSION.PAM";

    private static final String PROJECTILE_HIT_PAM =
        "768/INITIAL/EFFECTS/PVINE_PROJECTILE_HIT/PVINE_PROJECTILE_HIT.PAM";

    private static final float EXPLOSION_SCALE = 0.75f;
    private static final float PROJECTILE_HIT_SCALE = 0.50f;

    private final PvzGame game;
    private final BoardTransform transform;

    public WorldEffectManager(
        PvzGame game,
        BoardTransform transform
    ) {
        this.game = game;
        this.transform = transform;
        setTouchable(Touchable.disabled);
    }

    public void play(VisualEffectEvent event) {
        if (event == null) {
            return;
        }

        switch (event.type()) {
            case PLANT_EXPLOSION -> playExplosion(event);
            case PROJECTILE_IMPACT -> playProjectileImpact(event);
            case ICY_WIND -> {
            }
        }
    }

    private void playExplosion(VisualEffectEvent event) {
        playOneShot(
            EXPLOSION_PAM,
            EXPLOSION_SCALE,
            0.8f,
            event.posX(),
            event.posY(),
            "explosion",
            "explode",
            "attack",
            "effect",
            "animation",
            "anim"
        );
    }

    private void playProjectileImpact(VisualEffectEvent event) {
        playOneShot(
            PROJECTILE_HIT_PAM,
            PROJECTILE_HIT_SCALE,
            0.3f,
            event.posX(),
            event.posY(),
            "hit",
            "impact",
            "projectile_hit",
            "effect",
            "animation",
            "anim"
        );
    }

    private void playOneShot(
        String pamPath,
        float scale,
        float fallbackDuration,
        double posX,
        double posY,
        String... clipCandidates
    ) {
        try {
            EffectPamFactory.OneShot effect =
                EffectPamFactory.create(
                    game,
                    pamPath,
                    scale,
                    fallbackDuration,
                    clipCandidates
                );

            PamAnimationActor actor = effect.actor();
            positionActor(actor, posX, posY);
            addActor(actor);

            actor.addAction(
                Actions.sequence(
                    Actions.delay(effect.duration()),
                    Actions.removeActor()
                )
            );
        } catch (RuntimeException e) {
            if (Gdx.app != null) {
                Gdx.app.error(
                    "WorldEffectManager",
                    "Could not play effect: " + pamPath,
                    e
                );
            }
        }
    }

    private void positionActor(
        PamAnimationActor actor,
        double posX,
        double posY
    ) {
        BoardArea area = transform.getArea();

        float x =
            area.x()
                + ((float) posX + 0.5f)
                * transform.tileWidth();

        float y =
            area.y()
                + (
                BoardTransform.ROWS
                    - 1f
                    - (float) posY
                    + 0.5f
            )
                * transform.tileHeight();

        actor.setPosition(x, y);
    }
}
