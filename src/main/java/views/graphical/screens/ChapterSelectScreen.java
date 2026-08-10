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
import views.graphical.ui.SettingsPopup;

import java.util.List;

public class ChapterSelectScreen extends BaseScreen {

    private Texture backgroundTexture;
    private Table content;

    private static final ChapterTheme[] CHAPTERS = {
        ChapterTheme.ANCIENT_EGYPT,
        ChapterTheme.FROSTBITE_CAVES,
        ChapterTheme.BIG_WAVE_BEACH,
        ChapterTheme.DARK_AGES
    };

    private static final float ISLAND_W = 310f;
    private static final float ISLAND_H = 360f;
    private static final String CHILI_ON  = "IMAGE_UI_PENNY_PURSUITS_COMMON_EASY_ICON_SMALL";
    private static final String CHILI_OFF = "IMAGE_UI_PENNY_PURSUITS_COMMON_EASY_HOLLOW_ICON_SMALL";

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
        backgroundTexture = new Texture(Gdx.files.internal("assets/backgrounds/adventure.jpeg"));
        backgroundTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);

        content = new Table();
        content.setFillParent(true);
        content.setBackground(new TextureRegionDrawable(new TextureRegion(backgroundTexture)));

        rebuildContent();
        stage.addActor(content);
    }

    private void rebuildContent() {
        content.clear();
        content.top();
        content.add().height(72f).row();
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





    private Table buildCenterIsland(final ChapterTheme chapter) {
        boolean isUnlocked = (chapter == CHAPTERS[0]);
        Actor island = islandImage(chapter, ISLAND_W, ISLAND_H);

        Stack stack = new Stack();
        Table imgWrap = new Table();

        if (!isUnlocked) {
            island.setColor(0.15f, 0.15f, 0.15f, 1f);
        }

        imgWrap.add(island).size(ISLAND_W, ISLAND_H);
        stack.add(imgWrap);

        if (!isUnlocked) {
            Table lockWrap = new Table();
            Drawable lockDrawable = safeRegion("IMAGE_UI_CHOOSER_SLOT_LOCK_SMALL_SLOT_LOCK_SMALL_71X94");
            if (lockDrawable != null) {
                Image lock = new Image(lockDrawable);
                lock.setScaling(Scaling.fit);
                lockWrap.add(lock).size(80, 106).center().padBottom(80f);
            } else {
                Label lockLbl = new Label("LOCKED", labelStyle("medium_outline"));
                lockLbl.setColor(Color.RED);
                lockWrap.add(lockLbl).center().padBottom(80f);
            }
            stack.add(lockWrap);
        }

        Table info = new Table();
        info.bottom();

        Label title = new Label(chapter.name, labelStyle("big_outline"));
        title.setColor(Color.WHITE);
        info.add(title).padBottom(4).row();

        if (isUnlocked) {
            info.add(buildDifficulty(chapterDifficulty(chapter))).padBottom(6).row();

            int total = chapter.levels.size();
            int done = Math.min(2, total);
            info.add(buildProgress(done, total)).padBottom(10).row();

            TextButton reviewBtn = new TextButton("REVIEW", game.getSkin(), "purple");
            reviewBtn.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    game.showScreen(new ChapterMapScreen(game, chapter));
                }
            });

            info.add(reviewBtn).width(160).height(48).padBottom(10);
        } else {
            info.add().height(77f).row();

            TextButton reviewBtn = new TextButton("REVIEW", game.getSkin(), "purple");
            reviewBtn.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    game.showScreen(new ChapterMapScreen(game, chapter));
                }
            });

            info.add(reviewBtn).width(160).height(48).padBottom(10);
        }

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

        Stack cardStack = new Stack();
        cardStack.setTouchable(Touchable.enabled);

        Actor island = islandImage(chapter, w, h);

        if (chapter != CHAPTERS[0]) {
            island.setColor(0.15f, 0.15f, 0.15f, alpha);
            cardStack.add(island);

            Table lockWrap = new Table();
            Drawable lockDrawable = safeRegion("IMAGE_UI_CHOOSER_SLOT_LOCK_SMALL_SLOT_LOCK_SMALL_71X94");
            if (lockDrawable != null) {
                Image lock = new Image(lockDrawable);
                lock.setScaling(Scaling.fit);
                lockWrap.add(lock).size(60 * scale, 80 * scale).center();
            } else {
                Label lockLbl = new Label("LOCKED", labelStyle("medium_outline"));
                lockLbl.setColor(Color.RED);
                lockWrap.add(lockLbl).center();
            }
            cardStack.add(lockWrap);
        } else {
            island.getColor().a = alpha;
            cardStack.add(island);
        }

        Table card = new Table();
        card.add(cardStack).size(w, h).bottom();
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
                t.add(new Image(chiliImg)).size(25).padLeft(2);
            } else {
                Label chili = new Label(on ? "\u2588" : "\u2591", labelStyle("medium_outline"));
                chili.setColor(on ? peppermint() : grey());
                t.add(chili).padLeft(2);
            }
        }
        return t;
    }

    private Actor stagesIcon() {
        Drawable frame      = safeRegion("IMAGE_WORLDMAP_LEVEL_NODE_LEVEL_NODE_118X40");
        Drawable circle     = safeRegion("IMAGE_WORLDMAP_LEVEL_NODE_LEVEL_NODE_97X71_2");

        if (frame == null && circle == null) {
            Table ph = new Table();
            ph.setBackground(game.getSkin().newDrawable("white_pixel", Color.CYAN));
            ph.setSize(47.2f, 34f);
            return ph;
        }

        float s = 0.4f;
        float groupWidth = 118 * s;
        float groupHeight = 78 * s;

        WidgetGroup group = new WidgetGroup();
        group.setSize(groupWidth, groupHeight);

        if (circle != null) {
            Image img = new Image(circle);
            img.setSize(97 * s, 71 * s);
            img.setPosition((groupWidth - img.getWidth()) / 2f, 8 * s);
            group.addActor(img);
        }

        if (frame != null) {
            Image img = new Image(frame);
            img.setSize(118 * s, 40 * s);
            img.setPosition((groupWidth - img.getWidth()) / 2f, 0);
            group.addActor(img);
        }

        return group;
    }

    private Table buildProgress(int done, int total) {
        Table t = new Table();

        Label lbl = new Label(done + "/" + total, labelStyle("medium_outline"));
        lbl.setColor(Color.WHITE);
        lbl.setAlignment(Align.center);

        Table textWrap = new Table();
        Drawable sliceBg = safeRegion("IMAGE_UI_HUD_INGAME_BACKGROUND_3SLICE");
        if (sliceBg != null) {
            textWrap.setBackground(sliceBg);
        } else {
            textWrap.setBackground(game.getSkin().newDrawable("white_pixel", new Color(0, 0, 0, 0.5f)));
        }

        textWrap.add(lbl).pad(6, 26, 6, 12).align(Align.center);
        textWrap.pack();

        Actor icon = stagesIcon();

        WidgetGroup group = new WidgetGroup();

        float textW = textWrap.getPrefWidth();
        float textH = textWrap.getPrefHeight();
        float iconW = icon != null ? icon.getWidth() : 0;
        float iconH = icon != null ? icon.getHeight() : 0;

        float overlap = iconW * 0.45f;
        float totalW = textW + iconW - overlap;
        float totalH = Math.max(textH, iconH);

        group.setSize(totalW, totalH);

        textWrap.setPosition(iconW - overlap, (totalH - textH) / 2f);
        textWrap.setSize(textW, textH);
        group.addActor(textWrap);

        if (icon != null) {
            icon.setPosition(0, (totalH - iconH) / 2f);
            group.addActor(icon);
        }

        t.add(group).size(totalW, totalH).padBottom(10);
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
        if (stage != null) {
            stage.getViewport().update(width, height, true);
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
