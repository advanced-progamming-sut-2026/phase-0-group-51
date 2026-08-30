package views.graphical.screens.minigamesScreen.zombotany;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import models.Zombie.Zombie;
import views.graphical.gameplay.board.BoardTransform;

public class ZombotanyPeashooterActor extends Actor {
    private static final int FRAME_WIDTH = 181;

    private static final int FRAME_HEIGHT = 543;

    private static final int WALK_FRAME_COUNT = 8;

    private static final int EAT_FRAME_COUNT = 7;


    /*
     * مدت زمان هر Frame
     */
    private static final float FRAME_DURATION = 0.15f;


    /*
     * اندازه نمایش Sprite
     */
    private static final float SCALE = 0.35f;

    private final Zombie zombieModel;
    private final BoardTransform boardTransform;


    /*
     * Animations
     */
    private final Animation<TextureRegion> walkAnimation;

    private final Animation<TextureRegion> eatAnimation;


    /*
     * زمان Animation
     */
    private float stateTime;
    private boolean wasEating;



    public ZombotanyPeashooterActor(
            Zombie model,
            Texture walkSpriteSheet,
            Texture shootSpriteSheet,
            BoardTransform transform
    ) {
        this.zombieModel = model;
        this.boardTransform = transform;

        if (walkSpriteSheet == null) {
            throw new IllegalArgumentException(
                    "walkSpriteSheet cannot be null"
            );
        }
        if (shootSpriteSheet == null) {
            throw new IllegalArgumentException(
                    "shootSpriteSheet cannot be null"
            );
        }

        TextureRegion[][] walkSheetFrames =
                TextureRegion.split(
                        walkSpriteSheet,
                        FRAME_WIDTH,
                        FRAME_HEIGHT
                );
        TextureRegion[][] shootSheetFrames =
                TextureRegion.split(
                        shootSpriteSheet,
                        FRAME_WIDTH,
                        FRAME_HEIGHT
                );

        if (walkSheetFrames.length < 1
                || walkSheetFrames[0].length < WALK_FRAME_COUNT) {
            throw new IllegalArgumentException(
                    "Invalid Zombotany Peashooter walk sprite sheet. "
                            + "Expected at least 8 frames of 181x543."
            );
        }
        if (shootSheetFrames.length < 1
                || shootSheetFrames[0].length < EAT_FRAME_COUNT) {
            throw new IllegalArgumentException(
                    "Invalid Zombotany Peashooter shoot sprite sheet. "
                            + "Expected at least 7 frames of 181x543."
            );
        }

        TextureRegion[] walkFrames =
                new TextureRegion[WALK_FRAME_COUNT];

        for (int i = 0; i < WALK_FRAME_COUNT; i++) {
            walkFrames[i] = walkSheetFrames[0][i];
        }


        walkAnimation =
                new Animation<>(
                        FRAME_DURATION,
                        walkFrames
                );

        walkAnimation.setPlayMode(
                Animation.PlayMode.LOOP
        );


        /*
         * =====================================================
         * EAT / ATTACK
         * =====================================================
         *
         * Row 1:
         *
         * [0][1][2][3][4][5][6]
         *
         * Frame هشتم این ردیف عمداً استفاده نمی‌شود.
         */
        TextureRegion[] eatFrames =
            new TextureRegion[
                EAT_FRAME_COUNT
            ];


        for (
            int i = 0;
            i < EAT_FRAME_COUNT;
            i++
        ) {

            eatFrames[i] =
                shootSheetFrames[0][i];
        }


        eatAnimation =
                new Animation<>(
                        FRAME_DURATION,
                eatFrames
                );


        /*
         * تا زمانی که Zombie در حال خوردن است،
         * Animation باید تکرار شود.
         */
        eatAnimation.setPlayMode(
                Animation.PlayMode.LOOP
        );


        /*
         * اندازه Actor
         */
        setSize(
                FRAME_WIDTH * SCALE,
                FRAME_HEIGHT * SCALE
        );


        /*
         * وضعیت اولیه
         */
        stateTime = 0f;

        wasEating = false;
    }

    @Override
    public void act(float delta) {
        super.act(delta);


        /*
         * وضعیت فعلی Zombie
         */
        boolean eating = zombieModel.isEating();

        /*
         * =====================================================
         * Animation State Transition
         * =====================================================
         *
         * Walk -> Eat
         */
        if (eating && !wasEating) {
            stateTime = 0f;

            if (Gdx.app != null) {

                Gdx.app.log(
                    "ZombotanyAnimation",
                    "Peashooter zombie started EATING"
                );
            }
        }


        /*
         * Eat -> Walk
         */
        if (!eating && wasEating) {
            stateTime = 0f;

            if (Gdx.app != null) {

                Gdx.app.log(
                    "ZombotanyAnimation",
                    "Peashooter zombie stopped EATING"
                );
            }
        }


        /*
         * وضعیت قبلی را ذخیره کن.
         */
        wasEating = eating;

        /*
         * زمان Animation
         */
        stateTime +=
            Math.max(
                0f,
                delta
            );


        /*
         * =====================================================
         * Position
         * =====================================================
         *
         * این قسمت را با ZombieAnimationSystem اصلی
         * هماهنگ کردیم:
         *
         * (x + 0.5f)
         */
        float pixelX =
            boardTransform
                .getArea()
                .x()
                +
                (
                    zombieModel.getX()
                        + 0.5f
                )
                *
                boardTransform.tileWidth();


        /*
         * Y نیز مشابه سیستم اصلی.
         */
        float pixelY =
                boardTransform.tileY(
                        zombieModel.getLane()
                )
            +
            boardTransform.tileHeight()
                * 0.5f;


        setPosition(pixelX, pixelY);
    }

    @Override
    public void draw(
            Batch batch,
            float parentAlpha
    ) {

        /*
         * وضعیت واقعی Model را مستقیماً می‌خوانیم.
         */
        boolean eating =
                zombieModel.isEating();

        TextureRegion currentFrame;


        /*
         * =====================================================
         * EAT
         * =====================================================
         */
        if (eating) {
            currentFrame =
                eatAnimation.getKeyFrame(
                            stateTime,
                    true
                    );

        }

        /*
         * =====================================================
         * WALK
         * =====================================================
         */
        else {

            currentFrame =
                    walkAnimation.getKeyFrame(
                            stateTime,
                            true
                    );
        }


        /*
         * رسم Frame
         */
        batch.draw(
                currentFrame,
                getX(),
                getY(),
                getWidth(),
                getHeight()
        );
    }


    public Zombie getZombieModel() {
        return zombieModel;
    }
}
