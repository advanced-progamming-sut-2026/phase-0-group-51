package graphics;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import lombok.Getter;
import lombok.Setter;
import models.App;
import pvz.libpvz.textures.TextureBank;
import pvz.skin.PvzSkin;
import views.graphical.screens.BaseScreen;
import views.graphical.screens.BootScreen;
import views.graphical.screens.FirstScreen;
import views.graphical.ui.GlobalUiLayer;
@Getter
@Setter
public final class PvzGame extends Game {

    public static final float VIRTUAL_WIDTH = 1280f;
    public static final float VIRTUAL_HEIGHT = 720f;

    private final InputMultiplexer inputMultiplexer =
            new InputMultiplexer();

    private SpriteBatch batch;
    private Skin skin;
    private GlobalUiLayer globalUiLayer;
    private TextureBank textureBank;
    @Override
    public void create() {
        batch = new SpriteBatch();
        skin = PvzSkin.get();
        applyLinearToSkin(skin);
        String assetsPath = System.getProperty("pvz.assets");
        if (assetsPath == null || assetsPath.isBlank()) {
            throw new IllegalStateException(
                    "pvz.assets is not configured."
            );
        }
        FileHandle assetsFolder = Gdx.files.absolute(assetsPath);
        if (!assetsFolder.exists() || !assetsFolder.isDirectory()) {
            throw new IllegalStateException(
                    "Invalid PVZ assets directory: " + assetsPath
            );
        }
        App.getInstance();

        textureBank = new TextureBank("738", assetsFolder);
        globalUiLayer = new GlobalUiLayer(
                this,
                skin
        );
        globalUiLayer.resize(
                Gdx.graphics.getWidth(),
                Gdx.graphics.getHeight()
        );

        Gdx.input.setInputProcessor(
                inputMultiplexer
        );
                showFirstMenu();
    }

    public void showScreen(BaseScreen nextScreen) {
        if (nextScreen == null) {
            throw new IllegalArgumentException(
                    "nextScreen cannot be null"
            );
        }

        Screen previousScreen = getScreen();

        if (previousScreen == nextScreen) {
            return;
        }

        setScreen(nextScreen);

        configureInput(
                nextScreen.getInputProcessor()
        );

        if (previousScreen != null) {
            previousScreen.dispose();
        }
    }

    private void configureInput(
            InputProcessor screenInputProcessor
    ) {
        inputMultiplexer.clear();

        if (globalUiLayer != null) {
            inputMultiplexer.addProcessor(
                    globalUiLayer.getInputProcessor()
            );
        }

        if (screenInputProcessor != null) {
            inputMultiplexer.addProcessor(
                    screenInputProcessor
            );
        }
    }

    @Override
    public void render() {
        super.render();
        textureBank.update();

        if (globalUiLayer != null) {
            globalUiLayer.render(
                    Gdx.graphics.getDeltaTime()
            );
        }
    }

    @Override
    public void resize(
            int width,
            int height
    ) {
        super.resize(width, height);

        if (globalUiLayer != null) {
            globalUiLayer.resize(
                    width,
                    height
            );
        }
    }

    public void showHud(
            int coins,
            int gems,
            boolean showBackButton,
            Runnable backAction
    ) {
        globalUiLayer.showHud(
                coins,
                gems,
                showBackButton,
                backAction
        );
    }

    public void hideHud() {
        globalUiLayer.hideHud();
    }

    public void updateCurrencies(
            int coins,
            int gems
    ) {
        globalUiLayer.updateCurrencies(
                coins,
                gems
        );
    }

    public void notifyInfo(String message) {
        globalUiLayer.notifyInfo(message);
    }

    public void notifyError(String message) {
        globalUiLayer.notifyError(message);
    }

    public SpriteBatch getBatch() {
        if (batch == null) {
            throw new IllegalStateException(
                    "SpriteBatch is not initialized yet."
            );
        }

        return batch;
    }

    public Skin getSkin() {
        if (skin == null) {
            throw new IllegalStateException(
                    "Skin is not initialized yet."
            );
        }

        return skin;
    }

    @Override
    public void dispose() {
        Screen currentScreen = getScreen();

        if (currentScreen != null) {
            currentScreen.dispose();
        }

        if (globalUiLayer != null) {
            globalUiLayer.dispose();
            globalUiLayer = null;
        }

        if (skin != null) {
            skin.dispose();
            skin = null;
        }

        if (batch != null) {
            batch.dispose();
            batch = null;
        }
    }
    public void showFirstMenu(){
        showScreen(new FirstScreen(this));
    }
    private void applyLinearToSkin(Skin skin) {
        if (skin.getAtlas() != null) {
            for (Texture texture : skin.getAtlas().getTextures()) {
                texture.setFilter(
                        Texture.TextureFilter.Linear,
                        Texture.TextureFilter.Linear
                );
            }
        }

        for (BitmapFont font : skin.getAll(BitmapFont.class).values()) {
            for (TextureRegion region : font.getRegions()) {
                region.getTexture().setFilter(
                        Texture.TextureFilter.Linear,
                        Texture.TextureFilter.Linear
                );
            }
        }
    }
}