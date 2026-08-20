package views.graphical.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Scaling;
import graphics.PvzGame;
import models.games.ChapterTheme;
import views.graphical.gameplay.manager.AudioManager;
import views.graphical.screens.ChapterMapScreen;
import views.graphical.screens.GameScreen;

public class GameOverPopup extends Table {

    private final PvzGame game;
    private final ChapterTheme theme;
    private final int levelNumber;
    private final String titleText;
    private final String exitText;
    private final String retryText;

    private final Runnable exitAction;
    private final Runnable retryAction;
    public GameOverPopup(PvzGame game, ChapterTheme theme, int levelNumber) {
    this(game, "THE ZOMBIES\nATE YOUR\nBRAINS!", "EXIT TO MAP", () -> Gdx.app.postRunnable(() -> game.showScreen(new ChapterMapScreen(game, theme))), "RETRY",
            () -> Gdx.app.postRunnable(() -> game.showScreen(new GameScreen(game, theme, levelNumber))));
    }
    public GameOverPopup(PvzGame game, String titleText, String exitText, Runnable exitAction, String retryText, Runnable retryAction) {

        this.game = game;
        this.theme = null;
        this.levelNumber = 0;
        this.titleText = titleText;
        this.exitText = exitText;
        this.exitAction = exitAction;
        this.retryText = retryText;
        this.retryAction = retryAction;

        setFillParent(true);

        setBackground(
                game.getSkin().newDrawable("white_pixel", new Color(0f, 0f, 0f, 1f))
        );

        getColor().a = 0f;
        addAction(Actions.fadeIn(2f));
        buildUi();
        AudioManager.getInstance().playMusic("assets/sounds/GameOver.mp3");
    }
    private void buildUi() {
        BitmapFont terrorFont = game.getSkin().getFont("FBUSV8C5EI_2");
        Label.LabelStyle titleStyle = new Label.LabelStyle(terrorFont, Color.valueOf("39FF14"));

        Label titleLbl = new Label(titleText, titleStyle);
        titleLbl.setAlignment(Align.center);
        titleLbl.setFontScale(2.1f);

        TextureRegion brainRegion = game.getTextureBank().region("IMAGE_UI_GAMEOVER_FAIL_SCREEN_BRAIN_ONLY");
        Image brainImg = new Image(brainRegion);
        brainImg.setScaling(Scaling.fit);

        Table btnTable = new Table();

        TextButton exitBtn = createCustomButton(exitText, "IMAGE_UI_GENERIC_BROWNBUTTON");
        exitBtn.addListener(
                new ClickListener() {
                    @Override
                    public void clicked(InputEvent event, float x, float y) {
                        if (exitAction != null) {
                            exitAction.run();
                        }
                    }
                }
        );


        TextButton retryBtn = createCustomButton(retryText, "IMAGE_UI_GENERIC_PURPLEBUTTON");
        retryBtn.addListener(
                new ClickListener() {
                    @Override
                    public void clicked(InputEvent event, float x, float y) {
                        if (retryAction != null) {
                            retryAction.run();
                        }
                    }
                }
        );

        btnTable.add(exitBtn).size(180, 60).padRight(30);
        btnTable.add(retryBtn).size(180, 60);

        add(titleLbl).padTop(30).padBottom(30).row();
        add(brainImg).size(350, 280).row();

        add(btnTable).expandY().bottom().padBottom(60);
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
}
