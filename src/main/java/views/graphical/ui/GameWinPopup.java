package views.graphical.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import graphics.PvzGame;
import models.games.ChapterTheme;
import views.graphical.screens.ChapterMapScreen;
import views.graphical.screens.GameScreen;

public class GameWinPopup extends Table {

    private final PvzGame game;
    private final ChapterTheme theme;
    private final int levelNumber;
    private Texture bgTexture;
    private Drawable bgDrawable;

    public GameWinPopup(PvzGame game, ChapterTheme theme, int levelNumber) {
        this.game = game;
        this.theme = theme;
        this.levelNumber = levelNumber;

        setFillParent(true);

        try {
            bgTexture = new Texture(Gdx.files.internal("assets/backgrounds/adventure.jpeg"));
            bgDrawable = new TextureRegionDrawable(new TextureRegion(bgTexture));
        } catch (Exception e) {
            bgDrawable = game.getSkin().newDrawable("white_pixel", new Color(0f, 0f, 0f, 0.9f));
        }

        getColor().a = 0f;
        addAction(Actions.fadeIn(2.0f));

        buildUi();
    }

    private void buildUi() {
        BorderedPanel boardPanel = new BorderedPanel(game, Color.valueOf("F5DEB3"));
        Table content = boardPanel.getContent();
        content.pad(25f, 30f, 30f, 30f);

        Label.LabelStyle textStyle = new Label.LabelStyle();
        if (game.getSkin().has("medium_outline", Label.LabelStyle.class)) {
            textStyle = game.getSkin().get("medium_outline", Label.LabelStyle.class);
        } else {
            textStyle.font = game.getSkin().getFont("default-font");
            textStyle.fontColor = Color.WHITE;
        }

        Label titleLbl = new Label("Level Complete!", textStyle);
        titleLbl.setColor(Color.valueOf("FF8C00"));
        titleLbl.setFontScale(2.0f);
        titleLbl.setAlignment(Align.center);
        Image separatorLine = new Image(game.getSkin().newDrawable("white_pixel", Color.valueOf("D2B48C")));

        Label msgLbl = new Label("You defeated the zombies!", textStyle);
        msgLbl.setColor(Color.valueOf("556B2F"));
        msgLbl.setFontScale(1.2f);
        msgLbl.setAlignment(Align.center);

        Table btnTable = new Table();

        TextButton exitBtn = createCustomButton("EXIT TO MAP", "IMAGE_UI_GENERIC_BROWNBUTTON");
        exitBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                Gdx.app.postRunnable(() -> {
                    game.showScreen(new ChapterMapScreen(game, theme));
                });
            }
        });

        TextButton nextBtn = createCustomButton("NEXT LEVEL", "IMAGE_UI_GENERIC_PURPLEBUTTON");
        nextBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                Gdx.app.postRunnable(() -> {
                    game.showScreen(new GameScreen(game, theme, levelNumber + 1));
                });
            }
        });

        btnTable.add(exitBtn).size(180, 55).padRight(30);
        btnTable.add(nextBtn).size(180, 55);

        content.add(titleLbl).padBottom(15f).row();
        content.add(separatorLine).growX().height(3f).padBottom(20f).row();
        content.add(msgLbl).width(650f).height(100f).center().row();
        content.add(btnTable).padBottom(10);

        boardPanel.pack();
        add(boardPanel).center();
    }

    @Override
    public void draw(Batch batch, float parentAlpha) {
        if (bgDrawable != null) {
            Color color = getColor();
            batch.setColor(color.r, color.g, color.b, color.a * parentAlpha);
            bgDrawable.draw(batch, getX(), getY(), getWidth(), getHeight());
            batch.setColor(Color.WHITE);
        }
        super.draw(batch, parentAlpha);
    }

    private TextButton createCustomButton(String text, String bgRegionId) {
        TextButton.TextButtonStyle style = new TextButton.TextButtonStyle();

        if (game.getSkin().has("medium_outline", Label.LabelStyle.class)) {
            style.font = game.getSkin().get("medium_outline", Label.LabelStyle.class).font;
        } else {
            style.font = game.getSkin().getFont("default-font");
        }
        style.fontColor = Color.WHITE;

        TextureRegion bgRegion = game.getTextureBank().region(bgRegionId);
        if (bgRegion != null) {
            style.up = new TextureRegionDrawable(bgRegion);
            style.down = new TextureRegionDrawable(bgRegion).tint(Color.LIGHT_GRAY);
        }

        TextButton btn = new TextButton(text, style);
        btn.getLabel().setFontScale(0.85f);
        return btn;
    }

    @Override
    public boolean remove() {
        if (bgTexture != null) {
            bgTexture.dispose();
        }
        return super.remove();
    }
}
