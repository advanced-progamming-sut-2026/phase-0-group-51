package graphics;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import lombok.Getter;
import lombok.Setter;
import models.App;
import pvz.libpvz.textures.TextureBank;
import pvz.skin.PvzSkin;
import views.graphical.animation.PamAnimationActor;
import views.graphical.screens.BaseScreen;
import views.graphical.screens.BootScreen;
import views.graphical.screens.FirstScreen;
import views.graphical.ui.GlobalUiLayer;
import pvz.libpvz.pam.PamPlayer;
import network.client.ClientNetworkManager;
import network.client.ClientAuthState;
import network.client.ClientSessionTokenStore;
import network.protocol.auth.LoginResponse;
import views.graphical.screens.MainMenuScreen;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    private PamPlayer pamPlayer;

    private ClientNetworkManager networkManager;
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
        networkManager = new ClientNetworkManager();

        textureBank = new TextureBank("768", assetsFolder);
        pamPlayer = new PamPlayer(textureBank, assetsFolder);
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

        startInitialScreen();
    }
    private void startInitialScreen() {
        String savedToken =
                ClientSessionTokenStore.load();

        if (savedToken == null) {
            showFirstMenu();
            return;
        }

        showScreen(
                new BootScreen(this)
        );

        restoreSavedSession(savedToken);
    }
    private void restoreSavedSession(
            String token
    ) {
        networkManager
                .ensureConnectedAsync()
                .thenCompose(
                        ignored ->
                                sendResumeSession(token)
                )
                .whenComplete(
                        (response, throwable) ->
                                Gdx.app.postRunnable(
                                        () ->
                                                finishSessionRestore(
                                                        response,
                                                        throwable
                                                )
                                )
                );
    }
    private CompletableFuture<LoginResponse>
    sendResumeSession(
            String token
    ) {
        try {
            return networkManager
                    .getAccountClientService()
                    .resumeSession(token);

        } catch (IOException | RuntimeException exception) {
            return failedFuture(exception);
        }
    }
    private void finishSessionRestore(
            LoginResponse response,
            Throwable throwable
    ) {
        if (throwable != null) {
            /*
             * Important:
             * A network failure does NOT mean that the token
             * is invalid.
             *
             * Keep the saved token so it can be retried later.
             */
            showFirstMenu();

            notifyError(
                    "Could not restore saved login: "
                            + rootMessage(throwable)
            );

            return;
        }

        if (response == null
                || !response.isSuccess()) {

            /*
             * The server was reachable and specifically
             * rejected the token.
             *
             * Therefore it really is invalid.
             */
            ClientSessionTokenStore.clear();

            showFirstMenu();

            notifyInfo(
                    response == null
                            ? "Saved login session is no longer valid."
                            : response.getMessage()
            );

            return;
        }

        ClientAuthState.applyLogin(
                response.getUser()
        );

        showScreen(
                new MainMenuScreen(this)
        );
    }
    private static <T> CompletableFuture<T> failedFuture(
            Throwable throwable
    ) {
        CompletableFuture<T> future =
                new CompletableFuture<>();

        future.completeExceptionally(throwable);

        return future;
    }
    private static String rootMessage(
            Throwable throwable
    ) {
        Throwable current = throwable;

        while (current.getCause() != null) {
            current = current.getCause();
        }

        String message =
                current.getMessage();

        if (message == null
                || message.isBlank()) {
            return current
                    .getClass()
                    .getSimpleName();
        }

        return message;
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

        if (Gdx.input != null) {
            Gdx.input.setInputProcessor(
                inputMultiplexer
            );
        }
    }
    public void render() {

        if (textureBank != null) {
            textureBank.update();
        }

        super.render();

        if (globalUiLayer != null) {
            globalUiLayer.render(
                Gdx.graphics.getDeltaTime()
            );
        }
    }

    public PamAnimationActor createPamActor(
        String pamPath,
        String clip,
        float x,
        float y,
        boolean loop
    ) {
        return createPamActor(
            pamPath,
            clip,
            x,
            y,
            loop,
            List.of()
        );
    }
    public PamAnimationActor createPamActor(
        String pamPath,
        String clip,
        float x,
        float y,
        boolean loop,
        List<String> visibleParts
    ) {
        PamAnimationActor actor =
            new PamAnimationActor(
                pamPlayer,
                pamPath,
                clip,
                loop
            );

        actor.setPosition(x, y);
        actor.setVisibleParts(visibleParts);

        return actor;
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

        if (networkManager != null) {
            networkManager.close();
            networkManager = null;
        }

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
