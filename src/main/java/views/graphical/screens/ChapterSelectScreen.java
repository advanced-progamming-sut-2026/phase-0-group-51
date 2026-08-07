package views.graphical.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
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

import java.util.List;

public class ChapterSelectScreen extends BaseScreen {

    private Stack root;
    private Texture backgroundTexture;
    private Table content;

    private static final ChapterTheme[] CHAPTERS = {
        ChapterTheme.ANCIENT_EGYPT,
        ChapterTheme.FROSTBITE_CAVES,
        ChapterTheme.BIG_WAVE_BEACH,
        ChapterTheme.DARK_AGES
    };

    private static final float ISLAND_W = 260f;
    private static final float ISLAND_H = 300f;

    private static final String GEM_ICON  = "IMAGE_UI_GENERIC_BUTTONS_PREMIUM_NORMAL";
    private static final String COIN_ICON = "IMAGE_UI_GENERIC_BUTTONS_COIN_BUY_NORMAL";

    private static final String CHILI_ON  = "IMAGE_UI_PENNY_PURSUITS_COMMON_EASY_ICON_SMALL";
    private static final String CHILI_OFF = "IMAGE_UI_PENNY_PURSUITS_COMMON_EASY_HOLLOW_ICON_SMALL";

    private static final String[][] TOP_LEFT_ICONS = {
        {"IMAGE_UI_HUD_WORLDMAP_BUTTONS_HUD_BACK_NORMAL",       "IMAGE_UI_HUD_WORLDMAP_BUTTONS_HUD_BACK_SELECTED",       "BACK"},
        {"IMAGE_UI_HUD_WORLDMAP_BUTTONS_HUD_ALMANAC_NORMAL",    "IMAGE_UI_HUD_WORLDMAP_BUTTONS_HUD_ALMANAC_SELECTED",    "BOOK"},
        {"IMAGE_UI_GENERIC_BUTTON_HUD_MINIGAMES_NORMAL",        "IMAGE_UI_GENERIC_BUTTON_HUD_MINIGAMES_SELECTED",        "POT"},
        {"IMAGE_UI_GENERIC_BUTTONS_HUD_ZG_NORMAL", "IMAGE_UI_GENERIC_BUTTONS_HUD_ZG_SELECTED", "GRN"},
    };


    private String islandRegion(ChapterTheme chapter) {
        switch (chapter) {
            case ANCIENT_EGYPT:   return "IMAGE_UI_UNIVERSE_WORLDS_EGYPT";
            case FROSTBITE_CAVES: return "IMAGE_UI_UNIVERSE_WORLDS_ICEAGE";
            case BIG_WAVE_BEACH:  return "IMAGE_UI_UNIVERSE_WORLDS_BEACH";
            case DARK_AGES:       return "IMAGE_UI_UNIVERSE_WORLDS_DARK";
            default:              return "IMAGE_UI_UNIVERSE_WORLDS_EGYPT";
        }
    }

    private int selectedIndex = 0;

    public ChapterSelectScreen(PvzGame game) {
        super(game);
        buildUi();
    }

    private void buildUi() {
        root = new Stack();
        root.setFillParent(true);

        backgroundTexture = new Texture(Gdx.files.internal("assets/backgrounds/adventure.png"));
        backgroundTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        Image backgroundImage = new Image(backgroundTexture);
        backgroundImage.setFillParent(true);
        backgroundImage.setTouchable(Touchable.disabled);
        root.add(backgroundImage);

        content = new Table();
        content.setFillParent(true);
        root.add(content);

        rebuildContent();
        stage.addActor(root);
    }

    private void rebuildContent() {
        content.clear();
        content.top();

        Table topBar = new Table();
        topBar.add(buildTopLeftIcons()).left().top();
        topBar.add().expandX();
        topBar.add(buildTopRight()).right().top();
        content.add(topBar).growX().pad(10).row();

        Table islandsRow = new Table();

        final int SIDE = 2;
        for (int offset = -SIDE; offset <= SIDE; offset++) {
            int idx = selectedIndex + offset;
            final int steps = Math.abs(offset);

            if (offset == 0) {

                islandsRow.add(buildCenterIsland(CHAPTERS[idx]))
                    .padLeft(20).padRight(20).top();
            } else if (idx >= 0 && idx < CHAPTERS.length) {

                final int targetIdx = idx;
                Actor side = buildDepthIsland(CHAPTERS[idx], steps);
                side.addListener(new ClickListener() {
                    @Override public void clicked(InputEvent e, float x, float y) {
                        selectedIndex = targetIdx;
                        rebuildContent();
                    }
                });
                float dropDown = (steps == 1) ? 80f : 120f;
                float sideGap  = 5f;
                islandsRow.add(side).padLeft(sideGap).padRight(sideGap).top().padTop(dropDown);
            } else {
                float emptyW = (steps == 1) ? ISLAND_W * 0.78f : ISLAND_W * 0.78f * 0.78f;
                islandsRow.add().width(emptyW).padLeft(5).padRight(5);
            }
        }

        content.add(islandsRow).expand().center().row();

        content.add(buildPageDots()).padBottom(18);
    }

    private void goNext() { selectedIndex = (selectedIndex + 1) % CHAPTERS.length; rebuildContent(); }
    private void goPrev() { selectedIndex = (selectedIndex - 1 + CHAPTERS.length) % CHAPTERS.length; rebuildContent(); }

    private Table buildTopLeftIcons() {
        Table icons = new Table();
        for (String[] entry : TOP_LEFT_ICONS) {
            icons.add(gameIcon(entry[0], entry[1], entry[2])).size(52).padRight(6);
        }
        return icons;
    }

    private Actor gameIcon(String normalRegion, String selectedRegion, String fallbackLabel) {
        Drawable normal = safeRegion(normalRegion);
        Drawable selected = safeRegion(selectedRegion);
        if (normal != null) {
            Drawable sel = (selected != null) ? selected : normal;
            ImageButton.ImageButtonStyle st = new ImageButton.ImageButtonStyle();
            st.imageUp = normal;
            st.imageOver = sel;
            st.imageDown = sel;
            return new ImageButton(st);
        }
        Table ph = new Table();
        ph.setBackground(game.getSkin().newDrawable("white_pixel", new Color(1, 1, 1, 0.18f)));
        Label l = new Label(fallbackLabel, labelStyle("medium"));
        l.setColor(Color.WHITE);
        ph.add(l);
        return ph;
    }

    private static final String SHOP_ICON_NORMAL   = "IMAGE_UI_HUD_WORLDMAP_BUTTONS_HUD_STORE_NORMAL";
    private static final String SHOP_ICON_SELECTED = "IMAGE_UI_HUD_WORLDMAP_BUTTONS_HUD_STORE_SELECTED";

    private Table buildTopRight() {
        Table bar = new Table();

        Drawable gem = safeRegion(GEM_ICON);
        if (gem != null) bar.add(new Image(gem)).size(30).padRight(5);
        bar.add(numberLabel("21")).padRight(8);

        Drawable coin = safeRegion(COIN_ICON);
        if (coin != null) bar.add(new Image(coin)).size(30).padRight(5);
        bar.add(numberLabel("770")).padRight(8);

        bar.add(gameIcon(SHOP_ICON_NORMAL, SHOP_ICON_SELECTED, "SHOP")).size(48);

        return bar;
    }


    private Table buildCenterIsland(ChapterTheme chapter) {
        Actor island = islandImage(chapter, ISLAND_W, ISLAND_H);

        Table info = new Table();
        info.bottom();

        Label title = new Label(chapter.name, labelStyle("big_outline"));
        title.setColor(Color.WHITE);
        info.add(title).padBottom(4).row();

        info.add(buildDifficulty(chapterDifficulty(chapter))).padBottom(6).row();

        int total = chapter.levels.size();
        int done = Math.min(2, total);
        info.add(buildProgress(done, total)).padBottom(10).row();

        TextButton reviewBtn = new TextButton("REVIEW", game.getSkin(), "purple");
        info.add(reviewBtn).width(160).height(48).padBottom(10);

        Stack stack = new Stack();
        Table imgWrap = new Table();
        imgWrap.add(island).size(ISLAND_W, ISLAND_H);
        stack.add(imgWrap);

        Table infoWrap = new Table();
        infoWrap.add(info).expand().bottom().padBottom(-90f);
        stack.add(infoWrap);

        Table card = new Table();
        card.add(stack);
        return card;
    }

    private Actor buildDepthIsland(ChapterTheme chapter, int steps) {
        float scale = (float) Math.pow(0.78, steps);
        float w = ISLAND_W * scale;
        float h = ISLAND_H * scale;

        float alpha = (steps == 1) ? 0.85f : 0.6f;

        Table card = new Table();
        card.setTouchable(Touchable.enabled);
        Actor island = islandImage(chapter, w, h);
        island.getColor().a = alpha;
        card.add(island).size(w, h).bottom();
        return card;
    }

    private Actor islandImage(ChapterTheme chapter, float w, float h) {
        Drawable d = safeRegion(islandRegion(chapter));
        if (d != null) {
            Image img = new Image(d);
            img.setScaling(Scaling.fit);
            return img;
        }
        Table placeholder = new Table();
        placeholder.setBackground(game.getSkin().newDrawable("white_pixel", themeColor(chapter)));
        Label lbl = new Label(chapter.name, labelStyle("medium_outline"));
        lbl.setColor(Color.WHITE);
        lbl.setWrap(true);
        lbl.setAlignment(Align.center);
        placeholder.add(lbl).width(w - 20).center();
        return placeholder;
    }

    private Table buildDifficulty(int level) {
        Table t = new Table();
        Label lbl = new Label("Difficulty ", labelStyle("medium_outline"));
        lbl.setColor(Color.WHITE);
        t.add(lbl).padRight(4);
        for (int i = 1; i <= 5; i++) {
            boolean on = i <= level;
            Drawable chiliImg = safeRegion(on ? CHILI_ON : CHILI_OFF);
            if (chiliImg != null) {
                t.add(new Image(chiliImg)).size(20).padLeft(2);
            } else {
                Label chili = new Label(on ? "\u2588" : "\u2591", labelStyle("medium_outline"));
                chili.setColor(on ? peppermint() : grey());
                t.add(chili).padLeft(2);
            }
        }
        return t;
    }

    private static final String STAGES_FRAME  = "IMAGE_WORLDMAP_LEVEL_NODE_LEVEL_NODE_118X40_2";
    private static final String STAGES_CIRCLE = "IMAGE_WORLDMAP_LEVEL_NODE_LEVEL_NODE_97X71_2";

    private Actor stagesIcon() {
        Drawable frame  = safeRegion(STAGES_FRAME);
        Drawable circle = safeRegion(STAGES_CIRCLE);
        if (frame == null && circle == null) return null;

        Stack st = new Stack();
        if (frame != null) {
            Table fw = new Table();
            fw.add(new Image(frame)).size(30);
            st.add(fw);
        }
        if (circle != null) {
            Table cw = new Table();
            cw.add(new Image(circle)).size(22);
            st.add(cw);
        }
        return st;
    }

    private Table buildProgress(int done, int total) {
        Table t = new Table();

        Actor icon = stagesIcon();
        if (icon != null) t.add(icon).size(30).padRight(6);

        ProgressBar bar = new ProgressBar(0, total, 1, false,
            game.getSkin().get("default-horizontal", ProgressBar.ProgressBarStyle.class));
        bar.setValue(done);
        t.add(bar).width(130).padRight(8);

        Table fractionWrap = new Table();
        fractionWrap.setBackground(game.getSkin().newDrawable("white_pixel", new Color(0, 0, 0, 0.45f)));
        Label lbl = new Label(done + "/" + total, labelStyle("medium_outline"));
        lbl.setColor(Color.WHITE);
        fractionWrap.add(lbl).pad(2, 8, 2, 8);
        t.add(fractionWrap);

        return t;
    }

    private Table buildPageDots() {
        Table t = new Table();
        for (int i = 0; i < CHAPTERS.length; i++) {
            Label dot = new Label("\u25CF", labelStyle("medium"));
            dot.setColor(i == selectedIndex ? Color.WHITE : new Color(1, 1, 1, 0.35f));
            t.add(dot).padLeft(6);
        }
        return t;
    }

    private Label numberLabel(String text) {
        Label l = new Label(text, labelStyle("medium_outline"));
        l.setColor(Color.WHITE);
        return l;
    }

    private Label.LabelStyle labelStyle(String name) {
        try {
            return game.getSkin().get(name, Label.LabelStyle.class);
        } catch (Exception e) {
            return game.getSkin().get("default", Label.LabelStyle.class);
        }
    }

    private Color peppermint() {
        try { return game.getSkin().getColor("PlantFamilyPeppermint"); }
        catch (Exception e) { return Color.RED; }
    }

    private Color grey() {
        try { return game.getSkin().getColor("Grey"); }
        catch (Exception e) { return Color.GRAY; }
    }

    private int chapterDifficulty(ChapterTheme chapter) {
        List<ChapterTheme> order = List.of(CHAPTERS);
        return Math.min(5, order.indexOf(chapter) + 2);
    }

    private Color themeColor(ChapterTheme chapter) {
        switch (chapter) {
            case ANCIENT_EGYPT:   return new Color(0.85f, 0.7f, 0.3f, 1f);
            case FROSTBITE_CAVES: return new Color(0.5f, 0.8f, 0.95f, 1f);
            case BIG_WAVE_BEACH:  return new Color(0.3f, 0.6f, 0.85f, 1f);
            case DARK_AGES:       return new Color(0.4f, 0.35f, 0.55f, 1f);
            default:              return new Color(0.5f, 0.5f, 0.5f, 1f);
        }
    }

    private Drawable safeRegion(String id) {
        try {
            TextureRegion r = game.getTextureBank().region(id);
            return (r == null) ? null : new TextureRegionDrawable(r);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public void render(float delta) {
        com.badlogic.gdx.utils.ScreenUtils.clear(0.05f, 0.05f, 0.05f, 1f);
        super.render(delta);
    }

    @Override
    public void dispose() {
        if (backgroundTexture != null) backgroundTexture.dispose();
        stage.dispose();
    }
}
