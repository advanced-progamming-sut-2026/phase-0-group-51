package views.graphical.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Scaling;
import graphics.PvzGame;
import models.games.ChapterTheme;
import models.App;
import models.User;
import Data.database.ProgressRepository;

public class ChapterMapScreen extends BaseScreen {

    private final ChapterTheme chapter;
    private ScrollPane mapScroll;
    private Group mapContainer;
    private Texture backgroundTexture;

    private static final float MAP_WIDTH = 2000f;
    private static final float MAP_HEIGHT = PvzGame.VIRTUAL_HEIGHT;

    private int currentActiveLevel = 2;

    private static class NodeConfig {
        float islandScale;
        float iconScale;
        float iconX;
        float iconY;

        float statueScale = 0.5f;
        float statueY = 0.55f;

        NodeConfig(float islandScale, float iconScale, float iconX, float iconY) {
            this.islandScale = islandScale;
            this.iconScale = iconScale;
            this.iconX = iconX;
            this.iconY = iconY;
        }

        NodeConfig withStatue(float scale, float y) {
            this.statueScale = scale;
            this.statueY = y;
            return this;
        }
    }

    public ChapterMapScreen(PvzGame game, ChapterTheme chapter) {
        super(game);
        this.chapter = chapter;
        User user = App.getInstance().getLoggedInUser();
        int[] progress = {1, 1};
        if (user != null) {
            progress = new ProgressRepository().getCurrentProgress(user.getId());
        }

        int chapterIndex = 1;
        if (chapter == ChapterTheme.FROSTBITE_CAVES) chapterIndex = 2;
        else if (chapter == ChapterTheme.BIG_WAVE_BEACH) chapterIndex = 3;
        else if (chapter == ChapterTheme.DARK_AGES) chapterIndex = 4;

        if (chapterIndex < progress[0]) {
            currentActiveLevel = 999;
        } else if (chapterIndex == progress[0]) {
            currentActiveLevel = progress[1];
        } else {
            currentActiveLevel = 0;
        }

        buildUi();
    }

    private void buildUi() {
        mapContainer = new Group();
        mapContainer.setSize(MAP_WIDTH, MAP_HEIGHT);
        backgroundTexture = new Texture(Gdx.files.internal(getBackgroundForChapter(chapter)));
        backgroundTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        Image bgImage = new Image(backgroundTexture);
        bgImage.setSize(MAP_WIDTH, MAP_HEIGHT);
        bgImage.setScaling(Scaling.stretch);
        bgImage.setTouchable(Touchable.disabled);
        mapContainer.addActor(bgImage);

        Drawable bgStatueDrawable = safeRegion(getBackgroundStatueId(chapter));
        if (bgStatueDrawable != null) {
            Image bgStatue = new Image(bgStatueDrawable);
            float bgScale = getBackgroundStatueScale(chapter);
            bgStatue.setSize(bgStatue.getWidth() * bgScale, bgStatue.getHeight() * bgScale);
            float statueX = (MAP_WIDTH - bgStatue.getWidth()) / 2f;
            float statueY = (MAP_HEIGHT - bgStatue.getHeight()) / 2f;
            bgStatue.setPosition(statueX, statueY);
            mapContainer.addActor(bgStatue);
        }

        Vector2[] nodePositions = getPositionsForChapter(chapter);

        for (int i = 0; i < nodePositions.length - 1; i++) {
            int currentLevel = i + 1;
            int nextLevel = i + 2;

            String currentIsland = getIslandId(chapter, currentLevel);
            String nextIsland = getIslandId(chapter, nextLevel);

            Vector2 startCenter = getIconCenter(currentLevel, nodePositions[i], currentIsland);
            Vector2 endCenter = getIconCenter(nextLevel, nodePositions[i + 1], nextIsland);

            createConnectingLine(startCenter, endCenter);
        }

        for (int i = 0; i < nodePositions.length; i++) {
            int levelNum = i + 1;
            String islandId = getIslandId(chapter, levelNum);

            Group node = createLevelNode(levelNum, nodePositions[i], islandId);
            mapContainer.addActor(node);
        }

        mapScroll = new ScrollPane(mapContainer);
        mapScroll.setFillParent(true);
        mapScroll.setScrollingDisabled(false, true);
        mapScroll.setOverscroll(false, false);
        stage.addActor(mapScroll);
    }

    private String getBackgroundForChapter(ChapterTheme chapter) {
        return "assets/backgrounds/adventure.jpeg";
    }

    private String getBackgroundStatueId(ChapterTheme chapter) {
        switch (chapter) {
            case ANCIENT_EGYPT:
                return "IMAGE_WORLDMAP_EGYPT_ISLAND14";
            case FROSTBITE_CAVES:
                return "IMAGE_WORLDMAP_ICEAGE_ISLAND1";
            case BIG_WAVE_BEACH:
                return "IMAGE_WORLDMAP_BEACH_ISLAND1";
            case DARK_AGES:
                return "IMAGE_WORLDMAP_DARK_ANIM22_ANIM22_534X1169";
            default:
                return "IMAGE_WORLDMAP_EGYPT_ISLAND14";
        }
    }

    private float getBackgroundStatueScale(ChapterTheme chapter) {
        switch (chapter) {
            case ANCIENT_EGYPT:
                return 0.8f;
            case FROSTBITE_CAVES:
                return 0.8f;
            case BIG_WAVE_BEACH:
                return 1.0f;
            case DARK_AGES:
                return 0.7f;
            default:
                return 1.0f;
        }
    }

    private String getIslandId(ChapterTheme chapter, int levelNum) {
        switch (chapter) {
            case ANCIENT_EGYPT:
                if (levelNum == 1) return "IMAGE_WORLDMAP_EGYPT_ISLAND1";
                if (levelNum == 3) return "IMAGE_WORLDMAP_EGYPT_ISLAND4";
                return "IMAGE_WORLDMAP_EGYPT_ISLAND5";

            case FROSTBITE_CAVES:
                if (levelNum == 1) return "IMAGE_WORLDMAP_ICEAGE_ANIM3_ANIM3_1307X1318";
                if (levelNum == 3) return "IMAGE_WORLDMAP_ICEAGE_ANIM28_ANIM28_271X337";
                if (levelNum == 4) return "IMAGE_WORLDMAP_ZOMBOSS_NODE_ICEAGE_ZOMBOSS_NODE_ICEAGE_1055X1280";
                return "IMAGE_WORLDMAP_ICEAGE_ANIM26_ANIM26_375X281";

            case BIG_WAVE_BEACH:
                if (levelNum == 1) return "IMAGE_WORLDMAP_BEACH_ANIM27_ANIM27_1362X953";
                if (levelNum == 3) return "IMAGE_WORLDMAP_BEACH_ANIM12_ANIM12_335X420";
                if (levelNum == 4) return "IMAGE_WORLDMAP_TWISTER_ISLAND84";
                return "IMAGE_WORLDMAP_BEACH_ANIM13_ANIM13_397X399";

            case DARK_AGES:
                if (levelNum == 1) return "IMAGE_WORLDMAP_DARK_ANIM1_ANIM1_1201X1413";
                if (levelNum == 3) return "IMAGE_WORLDMAP_DARK_ISLAND7";
                if (levelNum == 4) return "IMAGE_WORLDMAP_ZOMBOSS_NODE_DARK_ZOMBOSS_NODE_DARK_905X1096";
                return "IMAGE_WORLDMAP_DARK_ISLAND6";

            default:
                if (levelNum == 1) return "IMAGE_WORLDMAP_EGYPT_ISLAND1";
                if (levelNum == 3) return "PLACEHOLDER_EGYPT_ISLAND_3";
                return "IMAGE_WORLDMAP_EGYPT_ISLAND5";
        }
    }

    private String getStatueId(ChapterTheme chapter) {
        if (chapter == ChapterTheme.ANCIENT_EGYPT) {
            return "IMAGE_WORLDMAP_EGYPT_ISLAND3";
        }
        return null;
    }

    private Vector2[] getPositionsForChapter(ChapterTheme chapter) {
        if (chapter == ChapterTheme.ANCIENT_EGYPT) {
            return new Vector2[]{
                new Vector2(200f, 350f),
                new Vector2(600f, 300f),
                new Vector2(950f, 150f),
                new Vector2(1400f, 400f)
            };
        } else {
            return new Vector2[]{
                new Vector2(150f, 350f),
                new Vector2(750f, 300f),
                new Vector2(1100f, 200f),
                new Vector2(1450f, 300f)
            };
        }
    }

    private NodeConfig getNodeConfig(ChapterTheme chapter, int levelNum) {
        switch (chapter) {
            case ANCIENT_EGYPT:
                if (levelNum == 1) return new NodeConfig(0.70f, 0.5f, 0.70f, 0.45f);
                if (levelNum == 2) return new NodeConfig(0.75f, 0.5f, 0.50f, 0.75f);
                if (levelNum == 3) return new NodeConfig(0.75f, 0.5f, 0.50f, 0.75f);
                if (levelNum == 4) return new NodeConfig(0.75f, 0.5f, 0.50f, 0.75f).withStatue(0.5f, 0.55f);
                return new NodeConfig(0.75f, 0.5f, 0.50f, 0.75f);

            case FROSTBITE_CAVES:
                if (levelNum == 1) return new NodeConfig(0.40f, 0.5f, 0.62f, 0.48f);
                if (levelNum == 2) return new NodeConfig(0.55f, 0.5f, 0.48f, 0.80f);
                if (levelNum == 3) return new NodeConfig(0.55f, 0.5f, 0.48f, 0.80f);
                if (levelNum == 4) return new NodeConfig(0.40f, 0.5f, 0.50f, 0.45f);
                return new NodeConfig(0.55f, 0.5f, 0.48f, 0.80f);

            case BIG_WAVE_BEACH:
                if (levelNum == 1) return new NodeConfig(0.32f, 0.5f, 0.60f, 0.45f);
                if (levelNum == 2) return new NodeConfig(0.35f, 0.5f, 0.55f, 0.78f);
                if (levelNum == 3) return new NodeConfig(0.35f, 0.5f, 0.50f, 0.79f);
                if (levelNum == 4) return new NodeConfig(0.7f, 0.5f, 0.50f, 0.43f);
                return new NodeConfig(0.35f, 0.5f, 0.55f, 0.78f);

            case DARK_AGES:
                if (levelNum == 1) return new NodeConfig(0.55f, 0.5f, 0.78f, 0.33f);
                if (levelNum == 2) return new NodeConfig(0.65f, 0.5f, 0.50f, 0.75f);
                if (levelNum == 3) return new NodeConfig(0.65f, 0.5f, 0.50f, 0.75f);
                if (levelNum == 4) return new NodeConfig(0.50f, 0.5f, 0.50f, 0.38f);
                return new NodeConfig(0.65f, 0.5f, 0.50f, 0.75f);

            default:
                return new NodeConfig(0.65f, 0.5f, 0.50f, 0.75f);
        }
    }

    private Vector2 getIconCenter(int levelNum, Vector2 pos, String islandId) {
        NodeConfig cfg = getNodeConfig(chapter, levelNum);

        Drawable d = safeRegion(islandId);
        float islandW = d.getMinWidth() * cfg.islandScale;
        float islandH = d.getMinHeight() * cfg.islandScale;

        float iconW = 118f * cfg.iconScale;
        float iconH = 78f * cfg.iconScale;

        float localX = islandW * cfg.iconX - iconW / 2f;
        float localY = islandH * cfg.iconY - iconH / 2f;

        return new Vector2(pos.x + localX + iconW / 2f, pos.y + localY + iconH / 2f);
    }

    private Actor buildLevelIcon(int levelNum, float iconScale) {
        Drawable frame = safeRegion("IMAGE_WORLDMAP_LEVEL_NODE_LEVEL_NODE_118X40");

        Drawable circle;
        if (levelNum < currentActiveLevel) {
            circle = safeRegion("IMAGE_WORLDMAP_LEVEL_NODE_LEVEL_NODE_97X71_2");
        } else if (levelNum == currentActiveLevel) {
            circle = safeRegion("IMAGE_WORLDMAP_LEVEL_NODE_LEVEL_NODE_97X71");
        } else {
            circle = safeRegion("IMAGE_WORLDMAP_LEVEL_NODE_LEVEL_NODE_97X71_3");
        }

        float groupWidth = 118f * iconScale;
        float groupHeight = 78f * iconScale;
        WidgetGroup group = new WidgetGroup();
        group.setSize(groupWidth, groupHeight);

        if (levelNum == currentActiveLevel) {
            Drawable glow = safeRegion("IMAGE_WORLDMAP_LEVEL_NODE_LEVEL_NODE_58X344");
            if (glow != null) {
                Image glowImg = new Image(glow);
                glowImg.setSize(58f * iconScale, 344f * iconScale);
                float glowX = (groupWidth - glowImg.getWidth()) / 2f;
                float glowY = 30f * iconScale;
                glowImg.setPosition(glowX, glowY);
                glowImg.setColor(1f, 1f, 1f, 0.6f);
                glowImg.setTouchable(Touchable.disabled);
                group.addActor(glowImg);
            }
        }

        if (circle != null) {
            Image img = new Image(circle);
            img.setSize(97f * iconScale, 71f * iconScale);
            img.setPosition((groupWidth - (97f * iconScale)) / 2f, 8f * iconScale);
            group.addActor(img);
        }

        if (frame != null) {
            Image img = new Image(frame);
            img.setSize(118f * iconScale, 40f * iconScale);
            img.setPosition((groupWidth - (118f * iconScale)) / 2f, 0);
            group.addActor(img);
        }

        return group;
    }

    private Group createLevelNode(final int levelNum, Vector2 pos, String islandId) {
        Group node = new Group();
        NodeConfig cfg = getNodeConfig(chapter, levelNum);

        Image island = new Image(safeRegion(islandId));
        island.setSize(island.getWidth() * cfg.islandScale, island.getHeight() * cfg.islandScale);

        float islandW = island.getWidth();
        float islandH = island.getHeight();
        node.setSize(islandW, islandH);

        if (levelNum == 4 && chapter == ChapterTheme.ANCIENT_EGYPT) {
            String statueId = getStatueId(chapter);
            if (statueId != null) {
                Image statue = new Image(safeRegion(statueId));
                statue.setSize(statue.getWidth() * cfg.statueScale, statue.getHeight() * cfg.statueScale);

                float statueX = (islandW - statue.getWidth()) / 2f;
                float statueY = islandH * cfg.statueY;
                statue.setPosition(statueX, statueY);

                node.addActor(statue);
            }
        }

        node.addActor(island);

        Actor icon = buildLevelIcon(levelNum, cfg.iconScale);
        float iconW = 118f * cfg.iconScale;
        float iconH = 78f * cfg.iconScale;

        float localX = islandW * cfg.iconX - iconW / 2f;
        float localY = islandH * cfg.iconY - iconH / 2f;
        icon.setPosition(localX, localY);
        node.addActor(icon);

        Label lbl = new Label(String.valueOf(levelNum), labelStyle("medium_outline"));
        lbl.setAlignment(Align.center);
        lbl.setPosition(icon.getX() + iconW / 2f, icon.getY() + iconH / 2f + (4f * cfg.iconScale), Align.center);
        node.addActor(lbl);

        node.setPosition(pos.x, pos.y);

        if (levelNum <= currentActiveLevel) {
            node.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    game.showScreen(new GameScreen(game, chapter, levelNum));
                }
            });
        }

        return node;
    }

    private void createConnectingLine(Vector2 start, Vector2 end) {
        float distance = (float) Math.hypot(end.x - start.x, end.y - start.y);
        float angle = (float) Math.toDegrees(Math.atan2(end.y - start.y, end.x - start.x));

        Image line = new Image(safeRegion("IMAGE_WORLDMAP_MAP_PATH_MAP_PATH_135X16_3"));
        line.setSize(distance, 16f);
        line.setOrigin(0, 8f);
        line.setPosition(start.x, start.y - 8f);
        line.setRotation(angle);

        mapContainer.addActor(line);
    }
    private Drawable safeRegion(String id) {
        try {
            TextureRegion r = game.getTextureBank().region(id);
            return (r == null) ? game.getSkin().newDrawable("white_pixel", Color.DARK_GRAY) : new TextureRegionDrawable(r);
        } catch (Exception e) {
            return game.getSkin().newDrawable("white_pixel", Color.DARK_GRAY);
        }
    }

    private Label.LabelStyle labelStyle(String name) {
        try {
            return game.getSkin().get(name, Label.LabelStyle.class);
        } catch (Exception e) {
            return game.getSkin().get("default", Label.LabelStyle.class);
        }
    }

    @Override
    public void show() {
        super.show();
        game.showHud(0, 0, true, () -> game.showScreen(new ChapterSelectScreen(game)));

        stage.getViewport().update(
                Gdx.graphics.getWidth(),
                Gdx.graphics.getHeight(),
                true
        );

//        if (stage != null) {
//            // استفاده از InputMultiplexer برای اینکه هم کلیک‌های نقشه و هم دکمه‌های HUD کار کنند
//            Gdx.input.setInputProcessor(new InputMultiplexer(stage, Gdx.input.getInputProcessor()));
//            stage.getViewport().update(Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), true);
//        }
    }

    @Override
    public void resize(int width, int height) {
        super.resize(width, height);
        if (stage != null) {
            stage.getViewport().update(width, height, true);
        }
    }

    @Override
    public void render(float delta) {
        com.badlogic.gdx.utils.ScreenUtils.clear(0f, 0f, 0f, 1f);
        super.render(delta);
    }

    @Override
    public void dispose() {
        if (backgroundTexture != null) backgroundTexture.dispose();
        stage.dispose();
    }
}
