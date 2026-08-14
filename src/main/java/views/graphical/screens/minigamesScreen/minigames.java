package views.graphical.screens.minigamesScreen;

import Data.database.MinigameProgressRepository;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.WidgetGroup;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Scaling;

import controllers.miniGamesController.BeghouledController;
import controllers.miniGamesController.IZombieController;
import controllers.miniGamesController.VaseBreakerController;
import controllers.miniGamesController.WallnutBowlingController;
import controllers.miniGamesController.ZombotanyController;

import graphics.PvzGame;

import models.App;
import models.Result;
import models.User;
import models.minigames.MinigameType;

import views.graphical.screens.BaseScreen;
import views.graphical.screens.MainMenuScreen;
import views.graphical.screens.minigamesScreen.vaseBreaker.VaseBreakerScreen;

public class minigames extends BaseScreen {

    private static final float MAP_WIDTH = 2000f;
    private static final float MAP_HEIGHT =
            PvzGame.VIRTUAL_HEIGHT;
    private static final float WORLD_MAX_WIDTH = 700f;
    private static final float WORLD_MAX_HEIGHT = 420f;

    private static final float ICON_SCALE = 0.72f;
    private final MinigameType minigameType;
    private final MinigameProgressRepository progressRepository = new MinigameProgressRepository();

    private ScrollPane mapScroll;
    private Group mapContainer;

    private Texture backgroundTexture;
    private Texture worldTexture;

    private int highestUnlockedStage = 1;
    private int highestCompletedStage = 0;

    public minigames(PvzGame game, MinigameType minigameType) {
        super(game);
        this.minigameType = minigameType;
        loadProgress();
        buildUi();
    }

    private void loadProgress() {
        User user = App.getInstance().getLoggedInUser();
        if (user == null) {
            highestUnlockedStage = 1;
            highestCompletedStage = 0;
            return;
        }
        MinigameProgressRepository.Progress progress = progressRepository.getProgress(user.getId(), minigameType);
        highestUnlockedStage = progress.highestUnlockedStage();
        highestCompletedStage = progress.highestCompletedStage();
    }

    private void buildUi() {

        mapContainer = new Group();
        mapContainer.setSize(MAP_WIDTH, MAP_HEIGHT);

        backgroundTexture = new Texture(Gdx.files.internal("assets/backgrounds/adventure.jpeg"));
        backgroundTexture.setFilter(
                Texture.TextureFilter.Linear,
                Texture.TextureFilter.Linear
        );

        Image background = new Image(backgroundTexture);
        background.setBounds(
                0f,
                0f,
                MAP_WIDTH,
                MAP_HEIGHT
        );
        background.setScaling(Scaling.stretch);
        background.setTouchable(Touchable.disabled);
        mapContainer.addActor(background);

        WorldBounds world = addWorldImage();
        Vector2[] stagePositions =
                getStagePositions();

        for (int i = 0; i < stagePositions.length - 1; i++) {

            Vector2 start =
                    getStageIconCenter(
                            i + 1,
                            stagePositions[i]
                    );

            Vector2 end =
                    getStageIconCenter(
                            i + 2,
                            stagePositions[i + 1]
                    );

            createConnectingLine(start, end);
        }

        for (int i = 0; i < stagePositions.length; i++) {
            int stageNumber = i + 1;
            Group stageIsland = createStageIsland(stageNumber, stagePositions[i]);
            mapContainer.addActor(stageIsland);
        }

        mapScroll = new ScrollPane(mapContainer);

        mapScroll.setFillParent(true);
        mapScroll.setScrollingDisabled(false, true);
        mapScroll.setOverscroll(false, false);
        mapScroll.setFadeScrollBars(false);

        stage.addActor(mapScroll);

        mapScroll.layout();
        mapScroll.setScrollPercentX(0.5f);
    }
    private Vector2[] getStagePositions() {
        return new Vector2[]{
                new Vector2(180f, 370f),
                new Vector2(760f, 260f),
                new Vector2(1350f, 160f)
        };
    }
    private WorldBounds addWorldImage() {
        String path = getWorldImagePath();
        worldTexture = new Texture(Gdx.files.internal(path));
        worldTexture.setFilter(
                Texture.TextureFilter.Linear,
                Texture.TextureFilter.Linear
        );

        float originalWidth = worldTexture.getWidth();
        float originalHeight = worldTexture.getHeight();
        float scaleX = WORLD_MAX_WIDTH / originalWidth;
        float scaleY = WORLD_MAX_HEIGHT / originalHeight;
        float scale = Math.min(scaleX, scaleY);
        float worldWidth = originalWidth * scale;
        float worldHeight = originalHeight * scale;

        float worldX = (MAP_WIDTH - worldWidth) / 2f;
        float worldY = (MAP_HEIGHT - worldHeight) / 2f;

        Image worldImage = new Image(worldTexture);
        worldImage.setSize(
                worldWidth,
                worldHeight);

        worldImage.setPosition(
                worldX,
                worldY
        );

        worldImage.setScaling(Scaling.fit);
        worldImage.setTouchable(Touchable.disabled);

        mapContainer.addActor(worldImage);

        return new WorldBounds(worldX, worldY, worldWidth, worldHeight);
    }


    private String getWorldImagePath() {

        return switch (minigameType) {

            case VASEBREAKER ->
                    "assets/vase.png";

            case BEGHOULDED ->
                    "assets/beghouled.png";

            case IZOMBIE ->
                    "assets/izombie.png";

            case WALLNUT_BOWLING ->
                    "assets/wallnut.png";

            case ZOMBOTANY ->
                    "assets/zombotany.png";
        };
    }


    private Group createStageIsland(
            final int stageNumber,
            Vector2 position
    ) {

        Group group =
                new Group();


        Drawable islandDrawable =
                safeRegion(
                        getStageIslandId(
                                stageNumber
                        )
                );


        Image island =
                new Image(
                        islandDrawable
                );


        float islandWidth;
        float islandHeight;


        if (stageNumber == 1) {

            islandWidth = 330f;
            islandHeight = 280f;

        } else {

            islandWidth = 190f;
            islandHeight = 150f;
        }


        island.setSize(
                islandWidth,
                islandHeight
        );

        island.setTouchable(
                Touchable.disabled
        );


        group.setSize(
                islandWidth,
                islandHeight
        );

        group.addActor(
                island
        );


        Actor levelIcon =
                buildLevelIcon(
                        stageNumber
                );


        float iconWidth =
                118f * ICON_SCALE;

        float iconHeight =
                78f * ICON_SCALE;

        float iconX =
                (
                        islandWidth
                                - iconWidth
                ) / 2f;

        float iconY =
                islandHeight * 0.55f;


        levelIcon.setPosition(
                iconX,
                iconY
        );


        group.addActor(
                levelIcon
        );


        Label number =
                new Label(
                        String.valueOf(
                                stageNumber
                        ),
                        labelStyle(
                                "medium_outline"
                        )
                );


        number.setAlignment(
                Align.center
        );


        number.setPosition(
                iconX
                        + iconWidth / 2f,

                iconY
                        + iconHeight / 2f
                        + 4f,

                Align.center
        );


        number.setTouchable(
                Touchable.disabled
        );


        group.addActor(
                number
        );


        group.setPosition(
                position.x,
                position.y
        );


        if (stageNumber
                <= highestUnlockedStage) {

            group.setTouchable(
                    Touchable.enabled
            );

            group.addListener(
                    new ClickListener() {

                        @Override
                        public void clicked(
                                InputEvent event,
                                float x,
                                float y
                        ) {

                            startStage(
                                    stageNumber
                            );
                        }
                    }
            );

        } else {

            group.setTouchable(
                    Touchable.disabled
            );
        }


        return group;
    }

    private String getStageIslandId(
            int stageNumber
    ) {

        if (stageNumber == 1) {
            return "IMAGE_WORLDMAP_EGYPT_ISLAND1";
        }

        return "IMAGE_WORLDMAP_EGYPT_ISLAND5";
    }
    private Vector2 getStageIconCenter(
            int stageNumber,
            Vector2 islandPosition
    ) {

        float islandWidth =
                stageNumber == 1
                        ? 330f
                        : 190f;

        float islandHeight =
                stageNumber == 1
                        ? 280f
                        : 150f;


        return new Vector2(
                islandPosition.x
                        + islandWidth / 2f,

                islandPosition.y
                        + islandHeight * 0.70f
        );
    }
    private Actor buildLevelIcon(
            int stageNumber
    ) {

        Drawable frame = safeRegion("IMAGE_WORLDMAP_LEVEL_NODE_LEVEL_NODE_118X40");


        Drawable circle;


        if (stageNumber
                <= highestCompletedStage) {

            circle = safeRegion("IMAGE_WORLDMAP_LEVEL_NODE_LEVEL_NODE_97X71_2");
        }


        else if (stageNumber
                == highestUnlockedStage) {

            circle = safeRegion("IMAGE_WORLDMAP_LEVEL_NODE_LEVEL_NODE_97X71");
        }


        else {

            circle = safeRegion("IMAGE_WORLDMAP_LEVEL_NODE_LEVEL_NODE_97X71_3");
        }


        float groupWidth =
                118f * ICON_SCALE;

        float groupHeight =
                78f * ICON_SCALE;


        WidgetGroup group =
                new WidgetGroup();

        group.setSize(
                groupWidth,
                groupHeight
        );

        if (stageNumber
                == highestUnlockedStage
                && stageNumber
                > highestCompletedStage) {

            Drawable glow =
                    safeRegion(
                            "IMAGE_WORLDMAP_LEVEL_NODE_LEVEL_NODE_58X344"
                    );

            Image glowImage =
                    new Image(glow);

            glowImage.setSize(
                    58f * ICON_SCALE,
                    344f * ICON_SCALE
            );

            glowImage.setPosition(
                    (groupWidth
                            - glowImage.getWidth())
                            / 2f,

                    30f * ICON_SCALE
            );

            glowImage.setColor(
                    1f,
                    1f,
                    1f,
                    0.6f
            );

            glowImage.setTouchable(
                    Touchable.disabled
            );

            group.addActor(
                    glowImage
            );
        }

        Image circleImage =
                new Image(circle);

        circleImage.setSize(
                97f * ICON_SCALE,
                71f * ICON_SCALE
        );

        circleImage.setPosition(
                (
                        groupWidth
                                - 97f
                                * ICON_SCALE
                ) / 2f,

                8f * ICON_SCALE
        );

        circleImage.setTouchable(
                Touchable.disabled
        );

        group.addActor(
                circleImage
        );


        Image frameImage =
                new Image(frame);

        frameImage.setSize(
                118f * ICON_SCALE,
                40f * ICON_SCALE
        );

        frameImage.setPosition(
                (
                        groupWidth
                                - 118f
                                * ICON_SCALE
                ) / 2f,

                0f
        );

        frameImage.setTouchable(
                Touchable.disabled
        );

        group.addActor(
                frameImage
        );


        return group;
    }



    private void createConnectingLine(
            Vector2 start,
            Vector2 end
    ) {

        float distance =
                start.dst(end);

        float angle =
                (float) Math.toDegrees(
                        Math.atan2(
                                end.y - start.y,
                                end.x - start.x
                        )
                );


        Image line =
                new Image(
                        safeRegion(
                                "IMAGE_WORLDMAP_MAP_PATH_MAP_PATH_135X16_3"
                        )
                );


        line.setSize(
                distance,
                16f
        );

        line.setOrigin(
                0f,
                8f
        );

        line.setPosition(
                start.x,
                start.y - 8f
        );

        line.setRotation(
                angle
        );

        line.setTouchable(
                Touchable.disabled
        );

        mapContainer.addActor(
                line
        );
    }

    private void startStage(int stageNumber) {
        if (minigameType == MinigameType.VASEBREAKER) {
            Gdx.app.postRunnable(() -> game.showScreen(new VaseBreakerScreen(game, stageNumber)));
            return;
        }
        Result result;

        switch (minigameType) {

            case BEGHOULDED ->
                    result = new BeghouledController().startStage(stageNumber);

            case IZOMBIE ->
                    result = new IZombieController().startStage(stageNumber);


            case WALLNUT_BOWLING ->
                    result = new WallnutBowlingController().startStage(stageNumber);


            case ZOMBOTANY ->
                    result = new ZombotanyController().startStage(stageNumber);

            default -> {return;}
        }

        if (!result.success()) {
            game.notifyError(result.message());
            return;
        }

        game.notifyInfo(result.message());

    }


    private Drawable safeRegion(
            String id
    ) {

        try {

            TextureRegion region =
                    game.getTextureBank()
                            .region(id);

            if (region != null) {

                return new TextureRegionDrawable(
                        region
                );
            }

        } catch (Exception ignored) {
        }


        return game.getSkin()
                .newDrawable(
                        "white_pixel",
                        Color.DARK_GRAY
                );
    }


    private Label.LabelStyle labelStyle(
            String name
    ) {

        try {

            return game.getSkin().get(
                    name,
                    Label.LabelStyle.class
            );

        } catch (Exception exception) {

            return game.getSkin().get(
                    "default",
                    Label.LabelStyle.class
            );
        }
    }

    @Override
    public void show() {
        super.show();
        game.showHud(0, 0, true, () -> game.showScreen(new MainMenuScreen(game)));
        if (stage != null) {
            stage.getViewport().update(Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), true);
        }
    }


    @Override
    public void resize(int width, int height) {
        super.resize(width, height);
    }


    @Override
    public void render(float delta) {
        com.badlogic.gdx.utils.ScreenUtils.clear(0f, 0f, 0f, 1f);
        super.render(delta);
    }


    @Override
    public void dispose() {

        if (backgroundTexture != null) {
            backgroundTexture.dispose();
            backgroundTexture = null;
        }

        if (worldTexture != null) {
            worldTexture.dispose();
            worldTexture = null;
        }

        super.dispose();
    }

    private record WorldBounds(float x, float y, float width, float height) {
    }
}