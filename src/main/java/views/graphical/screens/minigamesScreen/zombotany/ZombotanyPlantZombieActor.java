package views.graphical.screens.minigamesScreen.zombotany;

import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.scenes.scene2d.Actor;
import models.Zombie.Zombie;
import pvz.libpvz.pam.PamPlayer;
import views.graphical.animation.PamAnimationActor;
import views.graphical.gameplay.board.BoardTransform;

import java.util.List;

public class ZombotanyPlantZombieActor extends Actor {

    private static final String BODY_PAM =
            "768/INITIAL/ZOMBIE/ZOMBIE_TUTORIAL/ZOMBIE_TUTORIAL.PAM";

    private static final float BODY_SCALE = 0.61f;
    private static final float HEAD_SCALE = 0.30f;
    private static final float HEAD_DX_TILES = 0.0f;
    private static final float HEAD_DY_TILES = 0.55f;

    private final Zombie model;
    private final BoardTransform transform;
    private final PamAnimationActor body;
    private final PamAnimationActor head;
    private final String walkClip;
    private final String eatClip;
    private boolean wasEating;

    public ZombotanyPlantZombieActor(
            Zombie model,
            BoardTransform transform,
            PamPlayer pamPlayer,
            String plantPam
    ) {
        this.model = model;
        this.transform = transform;

        pamPlayer.loadSync(BODY_PAM);
        List<String> bodyClips = pamPlayer.clips(BODY_PAM);
        this.walkClip = pickClip(bodyClips, "walk");
        this.eatClip = pickClip(bodyClips, "eat");

        this.body = new PamAnimationActor(pamPlayer, BODY_PAM, walkClip, true);
        this.body.setScale(BODY_SCALE, BODY_SCALE);

        pamPlayer.loadSync(plantPam);
        String headClip = pickClip(pamPlayer.clips(plantPam), "idle");
        this.head = new PamAnimationActor(pamPlayer, plantPam, headClip, true);
        this.head.setScale(HEAD_SCALE, HEAD_SCALE);
    }

    private static String pickClip(List<String> clips, String wanted) {
        if (clips == null || clips.isEmpty()) {
            return wanted;
        }
        for (String clip : clips) {
            if (clip.equalsIgnoreCase(wanted)) {
                return clip;
            }
        }
        for (String clip : clips) {
            if (clip.toLowerCase().contains(wanted.toLowerCase())) {
                return clip;
            }
        }
        return clips.get(0);
    }

    @Override
    public void act(float delta) {
        super.act(delta);

        boolean eating = model.isEating();
        if (eating != wasEating) {
            body.play(eating ? eatClip : walkClip, true);
            wasEating = eating;
        }

        body.act(delta);
        head.act(delta);

        float x = transform.getArea().x()
                + (model.getX() + 0.5f) * transform.tileWidth();
        float y = transform.tileY(model.getLane())
                + transform.tileHeight() * 0.5f;

        body.setPosition(x, y);
        head.setPosition(
                x + HEAD_DX_TILES * transform.tileWidth(),
                y + HEAD_DY_TILES * transform.tileHeight()
        );
    }

    @Override
    public void draw(Batch batch, float parentAlpha) {
        body.draw(batch, parentAlpha);
        head.draw(batch, parentAlpha);
    }
}
