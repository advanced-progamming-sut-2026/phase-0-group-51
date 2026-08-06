package views.graphical.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.ScreenUtils;
import graphics.PvzGame;

public final class FirstScreen extends BaseScreen {
    private Stack root;
    private Texture backgroundTexture;
    private static final String EXIT_NORMAL_ID =
            "IMAGE_UI_DRAPER_CLOSE_BUTTON";

    private static final String EXIT_PRESSED_ID =
            "IMAGE_UI_DRAPER_CLOSE_BUTTON_DOWN";
    private static final String PLAY_ID ="IMAGE_UI_GENERIC_SM_PURPLE_BTN_NORMAL";
    private static final String PLAY_ID_PRESSED ="IMAGE_UI_GENERIC_SM_PURPLE_BTN_DOWN";
    private static final String NEW_PLAYER_ID ="IMAGE_UI_MAINMENU_MM_SETTINGS_TAB";
    public FirstScreen(PvzGame game) {
        super(game);
        buildUi();
    }
    private void buildUi(){
        root = new Stack();
        root.setFillParent(true);
        backgroundTexture = new Texture(
                Gdx.files.internal("assets/backgrounds/FirstBG.png")
        );

        backgroundTexture.setFilter(
                Texture.TextureFilter.Linear,
                Texture.TextureFilter.Linear
        );

        Image backgroundImage = new Image(backgroundTexture);
        backgroundImage.setFillParent(true);
        backgroundImage.setTouchable(Touchable.disabled);
        Table content = new Table();
        content.bottom().left();
        root.add(backgroundImage);
        ImageButton exitButton = createExitButton();
        TextButton signIn = createSignInButton();
        TextButton newPlayer = createNewPlayerButton();
        exitButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                Gdx.app.exit();
            }
        });
        signIn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                game.showScreen(new LoginScreen(game));
            }
        });
        newPlayer.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                game.showScreen(new SignupScreen(game));
            }
        });
        Table topRow = new Table();
        Table bottomRow = new Table();

        topRow.add(newPlayer)
                .width(390f)
                .height(50f)
                .padLeft(450f)
                .padBottom(20f);

        bottomRow.add(exitButton)
                .width(60f)
                .height(60f)
                .padLeft(80f)
                .padBottom(-37f);

        bottomRow.add(signIn)
                .width(220f)
                .height(100f)
                .padLeft(400f)
                .padBottom(10f);

        content.add(topRow)
                .left()
                .row();

        content.add(bottomRow)
                .left();

        root.add(content);
        stage.addActor(root);
    }
    @Override
    public void show(){
        game.hideHud();

    }
    @Override
    public void render(float delta){
         ScreenUtils.clear(0.05f,0.05f,0.05f,1f);
         super.render(delta);
    }
    @Override
    public void hide(){

    }
    private ImageButton createExitButton() {
        TextureRegion normalRegion = game.getTextureBank().region(EXIT_NORMAL_ID);
        TextureRegion pressedRegion = game.getTextureBank().region(EXIT_PRESSED_ID);

        ImageButton.ImageButtonStyle style = new ImageButton.ImageButtonStyle();
        style.imageUp = new TextureRegionDrawable(normalRegion);
        style.imageDown = new TextureRegionDrawable(pressedRegion);
        style.imageOver = new TextureRegionDrawable(pressedRegion);

        return new ImageButton(style);
    }
    private TextButton createSignInButton() {
        TextureRegion normalRegion = game.getTextureBank().region(PLAY_ID);
        TextureRegion pressedRegion = game.getTextureBank().region(PLAY_ID_PRESSED);


        TextButton.TextButtonStyle style =
                new TextButton.TextButtonStyle(game.getSkin().get(TextButton.TextButtonStyle.class));

        style.up = new TextureRegionDrawable(normalRegion);
        style.down = new TextureRegionDrawable(pressedRegion);
        style.over = new TextureRegionDrawable(normalRegion);

        return new TextButton("Sign In", style);
    }
    private TextButton createNewPlayerButton() {
        TextureRegion normalRegion = game.getTextureBank().region(NEW_PLAYER_ID);
        TextButton.TextButtonStyle style = new TextButton.TextButtonStyle(game.getSkin().get(TextButton.TextButtonStyle.class));
        style.up = new TextureRegionDrawable(normalRegion);
        style.down = new TextureRegionDrawable(normalRegion);
        style.over = new TextureRegionDrawable(normalRegion);

        return new TextButton("New Player", style);
    }

}

