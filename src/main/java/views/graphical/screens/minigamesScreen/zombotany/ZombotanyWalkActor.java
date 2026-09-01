package views.graphical.screens.minigamesScreen.zombotany;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import models.Zombie.Zombie;
import views.graphical.gameplay.board.BoardTransform;

public class ZombotanyWalkActor extends Actor {

    private static final int FRAME_HEIGHT = 543;
    private static final float FRAME_DURATION = 0.3f;
    private static final float VERTICAL_DROP = 34f;

    private final Zombie model;
    private final BoardTransform transform;
    private final float scale;
    private final Animation<TextureRegion> walkAnimation;
    private float stateTime;

    public ZombotanyWalkActor(
            Zombie model,
            Texture walkSpriteSheet,
            int frameWidth,
            int frameCount,
            BoardTransform transform,
            float scale
    ) {
        if (walkSpriteSheet == null) {
            throw new IllegalArgumentException("walkSpriteSheet cannot be null");
        }
        this.model = model;
        this.transform = transform;
        this.scale = scale;

        TextureRegion[][] sheet =
                TextureRegion.split(walkSpriteSheet, frameWidth, FRAME_HEIGHT);
        if (sheet.length < 1 || sheet[0].length < frameCount) {
            throw new IllegalArgumentException("Invalid Zombotany walk sprite sheet.");
        }

        TextureRegion[] frames = new TextureRegion[frameCount];
        for (int i = 0; i < frameCount; i++) {
            frames[i] = sheet[0][i];
        }

        walkAnimation = new Animation<>(FRAME_DURATION, frames);
        walkAnimation.setPlayMode(Animation.PlayMode.LOOP);

        setSize(frameWidth * scale, FRAME_HEIGHT * scale);
        stateTime = 0f;
    }

    @Override
    public void act(float delta) {
        super.act(delta);
        stateTime += Math.max(0f, delta);

        float pixelX = transform.getArea().x()
                + (model.getX() + 0.5f) * transform.tileWidth();
        float pixelY = transform.tileY(model.getLane())
                + transform.tileHeight() * 0.5f
                - VERTICAL_DROP;

        setPosition(pixelX, pixelY);
    }

    @Override
    public void draw(Batch batch, float parentAlpha) {
        TextureRegion frame = walkAnimation.getKeyFrame(stateTime, true);
        batch.draw(
                frame,
                getX(),
                getY(),
                frame.getRegionWidth() * scale,
                frame.getRegionHeight() * scale
        );
    }

    public Zombie getZombieModel() {
        return model;
    }
}
