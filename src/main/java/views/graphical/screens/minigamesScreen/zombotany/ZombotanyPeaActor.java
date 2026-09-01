package views.graphical.screens.minigamesScreen.zombotany;

import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import graphics.PvzGame;
import views.graphical.animation.PamAnimationActor;
import views.graphical.gameplay.board.BoardTransform;

public class ZombotanyPeaActor extends Group {

    private static final String PEA_PAM =
            "768/INITIAL/EFFECTS/SLINGPEA_PROJECTILE/SLINGPEA_PROJECTILE.PAM";

    private static final String PEA_CLIP = "tier1";

    private static final float PEA_SCALE = 0.7f;

    private static final float SPEED_TILES_PER_SECOND = 6.5f;

    private static final float START_COLUMN_OFFSET = 0.35f;

    private static final float MOUTH_OFFSET_Y = 70f;

    private final BoardTransform transform;
    private final float worldY;
    private final float leftLimitX;
    private float worldX;

    public ZombotanyPeaActor(
            PvzGame game,
            BoardTransform transform,
            int lane,
            float column
    ) {
        this.transform = transform;

        setTransform(true);
        setTouchable(Touchable.disabled);

        game.getPamPlayer().loadSync(PEA_PAM);

        PamAnimationActor animation =
                game.createPamActor(PEA_PAM, PEA_CLIP, 0f, 0f, true);
        animation.setScale(PEA_SCALE);
        animation.setTouchable(Touchable.disabled);
        addActor(animation);

        this.worldX = transform.getArea().x()
                + (column + START_COLUMN_OFFSET) * transform.tileWidth();
        this.worldY = transform.tileY(lane)
                + transform.tileHeight() * 0.5f
                + MOUTH_OFFSET_Y;
        this.leftLimitX = transform.getArea().x()
                - transform.tileWidth();

        setPosition(worldX, worldY);
    }

    @Override
    public void act(float delta) {
        super.act(delta);

        worldX -= SPEED_TILES_PER_SECOND
                * transform.tileWidth()
                * Math.max(0f, delta);

        setPosition(worldX, worldY);

        if (worldX <= leftLimitX) {
            remove();
        }
    }
}
