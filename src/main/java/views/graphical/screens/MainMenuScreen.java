package views.graphical.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Scaling;
import controllers.MainMenuController;
import graphics.PvzGame;
import models.Result;
import models.minigames.MinigameType;
import views.graphical.gameplay.manager.AudioManager;
import views.graphical.screens.minigamesScreen.meowPoint.MeowPointScreen;
import views.graphical.screens.minigamesScreen.minigames;
import views.graphical.ui.*;
import views.graphical.ui.leaderBoard.LeaderBoardPopup;
import network.client.ClientAuthState;
import network.protocol.auth.LogoutResponse;

import java.io.IOException;

public class MainMenuScreen extends BaseScreen{
    private ProfilePopup profilePopup;
    private LeaderBoardPopup leaderBoardPopup;
    private Stack root;
    private Texture backgroundTexture;
    private boolean logoutInFlight;
    private static final String EXIT_NORMAL_ID = "IMAGE_UI_DRAPER_CLOSE_BUTTON";
    private static final String EXIT_PRESSED_ID = "IMAGE_UI_DRAPER_CLOSE_BUTTON_DOWN";
    private static final String PROFILE = "IMAGE_UI_MAINMENU_MM_PLAYERICON";
    private static final String GAME_ICON = "IMAGE_UI_MAINMENU_PVZ2_LOGO_HORIZONTAL";
    private static final String VASE_BREAKER = "IMAGE_UI_FEATURE_UNLOCK_FEATURE_KEY_ART_VASEBREAKER";
    private static final String leaderBoard = "IMAGE_UI_GAMECENTER_ICON";
    private static final String Dokme = "IMAGE_UI_GENERIC_NAVDOT";
    private static final String Dokme_FILL = "IMAGE_UI_GENERIC_NAVDOT_FILL";
    private final java.util.List<ImageButton> cards = new java.util.ArrayList<>();
    private final java.util.List<Image> navDots = new java.util.ArrayList<>();

    private Group carouselGroup;
    private Table dotsTable;

    private int currentCardIndex = 0;
    public MainMenuScreen(PvzGame game) {
        super(game);
        buildUi();

    }
    private void buildUi() {
        root = new Stack();
        root.setFillParent(true);
        backgroundTexture = new Texture(
                Gdx.files.internal("assets/backgrounds/MainMenuBG.png")
        );

        backgroundTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);

        Image backgroundImage = new Image(backgroundTexture);

        backgroundImage.setFillParent(true);
        backgroundImage.setTouchable(Touchable.disabled);

        carouselGroup = new Group();
        carouselGroup.setTouchable(Touchable.childrenOnly);
        dotsTable = new Table();
        cards.clear();

        cards.add(createCards("assets/backgrounds/Adventure.png"));
        cards.add(createCards("assets/backgrounds/vase.png"));
        cards.add(createCards("assets/backgrounds/Beghouled.png"));
        cards.add(createCards("assets/backgrounds/Zombotany.png"));
        cards.add(createCards("assets/backgrounds/Wallnut.png"));
        cards.add(createCards("assets/backgrounds/Izombie.png"));
        cards.add(createCards("assets/backgrounds/Meow.png"));

        Table content = new Table();
        content.center().center();


        content.setWidth(450f);
        ImageButton exitButton = createExitButton();
        ImageButton profile = createProfileButton();
        profile.setSize(102f, 102f);
        ImageButton leaderboardButton = createLeaderboardButton();

        cards.get(0).addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
              game.showScreen(new ChapterSelectScreen(game));
            }
        });
        cards.get(1).addListener(
                new ChangeListener() {
                    @Override
                    public void changed(
                            ChangeEvent event,
                            Actor actor
                    ) {
                        game.showScreen(new minigames(game, MinigameType.VASEBREAKER));
                    }
                }
        );


        cards.get(2).addListener(
                new ChangeListener() {
                    @Override
                    public void changed(
                            ChangeEvent event,
                            Actor actor
                    ) {
                        game.showScreen(new minigames(game, MinigameType.BEGHOULDED));
                    }
                }
        );


        cards.get(3).addListener(
                new ChangeListener() {
                    @Override
                    public void changed(
                            ChangeEvent event,
                            Actor actor
                    ) {
                        game.showScreen(new minigames(game, MinigameType.ZOMBOTANY));
                    }
                }
        );


        cards.get(4).addListener(
                new ChangeListener() {
                    @Override
                    public void changed(
                            ChangeEvent event,
                            Actor actor
                    ) {
                        game.showScreen(new minigames(game, MinigameType.WALLNUT_BOWLING));
                    }
                }
        );


        cards.get(5).addListener(
                new ChangeListener() {
                    @Override
                    public void changed(
                            ChangeEvent event,
                            Actor actor
                    ) {
                        game.showScreen(new minigames(game, MinigameType.IZOMBIE));}
                }
        );
        cards.get(6).addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                Gdx.app.postRunnable(() ->
                        game.showScreen(new MeowPointScreen(game))
                );
            }
        });


        exitButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                Gdx.app.exit();
            }
        });
        profile.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                toggleProfile();
            }
        });
        leaderboardButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                toggleLeaderboard();
            }
        });
        Container<ImageButton> container4 = new Container<>(exitButton);
        container4.setFillParent(true);
        container4.bottom().left();
        container4.padBottom(15);
        container4.padLeft(15);
        container4.size(60f, 60f);
        Container<ImageButton> container5 = new Container<>(profile);
        container5.setFillParent(true);
        container5.bottom().right();
        container5.padBottom(-15f);
        container5.padRight(15);
        container5.size(102f, 102f);
        Container<ImageButton> leaderboardContainer =
                new Container<>(leaderboardButton);
        leaderboardContainer.setFillParent(true);
        leaderboardContainer.bottom().right();
        leaderboardContainer.padBottom(15f);
        leaderboardContainer.padRight(95f);
        leaderboardContainer.size(
                72f,
                72f
        );
        TextureRegion normal = game.getTextureBank().region(GAME_ICON);
        Image i = new Image(normal) ;
        Container<Image> container6 = new Container<>(i);
        container6.setFillParent(true);
        container6.center().center();
        container6.padBottom(300);
        root.add(backgroundImage);
        root.add(content);
        root.add(container4);
        root.add(container5);
        root.add(leaderboardContainer);
        root.add(container6);


        stage.addActor(root);
        buildDots();
        refreshCarousel();

        root.add(carouselGroup);
        stage.addActor(dotsTable);
    }

    private void toggleLeaderboard() {
        if (leaderBoardPopup != null && leaderBoardPopup.hasParent()) {
            leaderBoardPopup.remove();
            return;
        }

        leaderBoardPopup = new LeaderBoardPopup(game);
        stage.addActor(leaderBoardPopup);
        leaderBoardPopup.toFront();
    }

    private void toggleProfile() {
        if (profilePopup != null && profilePopup.hasParent()) {
            profilePopup.remove();
            return;
        }
        profilePopup = new ProfilePopup(game);
        profilePopup.pack();
        profilePopup.setPosition(
                (stage.getWidth() - profilePopup.getWidth()) / 2f,
                (stage.getHeight() - profilePopup.getHeight()) / 2f
        );
        stage.addActor(profilePopup);
    }

    private ImageButton createExitButton() {
        TextureRegion normalRegion = game.getTextureBank().region(EXIT_NORMAL_ID);
        TextureRegion pressedRegion = game.getTextureBank().region(EXIT_PRESSED_ID);
        ImageButton.ImageButtonStyle style = new ImageButton.ImageButtonStyle();
        style.imageUp = new TextureRegionDrawable(normalRegion);
        style.imageDown = new TextureRegionDrawable(pressedRegion);
        style.imageOver = new TextureRegionDrawable(pressedRegion);
        ImageButton imageButton = new ImageButton(style);
        return imageButton;
    }


    private ImageButton createLeaderboardButton() {
        TextureRegion normalRegion = game.getTextureBank().region(leaderBoard);
        TextureRegionDrawable normal = new TextureRegionDrawable(normalRegion);
        Drawable faded = normal.tint(new Color(1f, 1f, 1f, 0.65f));

        ImageButton.ImageButtonStyle style = new ImageButton.ImageButtonStyle();
        style.imageUp = normal;
        style.imageDown = faded;
        style.imageOver = faded;

        return new ImageButton(style);
    }

    private ImageButton createProfileButton(){
        TextureRegion normalRegion = game.getTextureBank().region(PROFILE);
        TextureRegionDrawable normal = new TextureRegionDrawable(normalRegion);
        Drawable faded = normal.tint(new Color(1f, 1f, 1f, 0.65f));
        ImageButton.ImageButtonStyle style = new ImageButton.ImageButtonStyle();
        style.imageUp = normal;
        style.imageDown = faded;
        style.imageOver = faded;

        return new ImageButton(style);
    }
    private ImageButton createCards(String path){
        Texture texture = new Texture(Gdx.files.internal(path));
        TextureRegion normalRegion =new TextureRegion(texture);;
        TextureRegionDrawable normal = new TextureRegionDrawable(normalRegion);
        Drawable faded = normal.tint(new Color(1f, 1f, 1f, 0.65f));
        ImageButton.ImageButtonStyle style = new ImageButton.ImageButtonStyle();
        style.imageUp = normal;
        style.imageDown = faded;
        style.imageOver = faded;

        return new ImageButton(style);
    }
    private void buildDots() {
        navDots.clear();
        dotsTable.clearChildren();

        for (int i = 0; i < 7; i++) {
            final int targetIndex = i;
            TextureRegion region = game.getTextureBank().region(Dokme);
            Image dot = new Image(region);

        dot.setTouchable(Touchable.enabled);

        dot.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event,float x,float y) {
                currentCardIndex = targetIndex;
                refreshCarousel();
            }
        });

            navDots.add(dot);
            dotsTable.add(dot).pad(6f).size(18f, 18f);
        }
        dotsTable.pack();
        dotsTable.setPosition((stage.getWidth() - dotsTable.getWidth()) / 2f, 250f);
        refreshDots();
    }
    private void refreshDots() {
        for (int i = 0; i < navDots.size(); i++) {
            TextureRegion region = game.getTextureBank().region(i == currentCardIndex ? Dokme_FILL : Dokme);
            navDots.get(i).setDrawable(new TextureRegionDrawable(region));
        }
    }
    private void refreshCarousel() {
        carouselGroup.clearChildren();

        float mainCardWidth = 520f;
        float mainCardHeight = 175f;

        float previewWidth = 250f;
        float previewHeight = 175f;

        float mainX = 380f;
        float mainY = 280f;

        float previousX = 100f;
        float previousY = 280f;

        float nextX = 930f;
        float nextY = 280f;

        ImageButton currentCard = cards.get(currentCardIndex);
        currentCard.setSize(mainCardWidth, mainCardHeight);
        currentCard.getImageCell().expand().fill();
        currentCard.getImage().setScaling(Scaling.stretch);
        currentCard.setPosition(mainX, mainY);
        carouselGroup.addActor(currentCard);

        int previousIndex = (currentCardIndex - 1 + cards.size()) % cards.size();

        Drawable previousDrawable = cards.get(previousIndex).getStyle().imageUp;
        Image previousCard = new Image(previousDrawable);
        previousCard.setScaling(Scaling.stretch);
        previousCard.setBounds(previousX, previousY, previewWidth, previewHeight);
        previousCard.setTouchable(Touchable.enabled);

        previousCard.addListener(
                new ClickListener() {
                    @Override
                    public void clicked(InputEvent event, float x, float y) {
                        showPreviousCard();
                    }
                }
        );

        carouselGroup.addActor(previousCard);

        int nextIndex = (currentCardIndex + 1) % cards.size();
        Drawable previewDrawable = cards.get(nextIndex).getStyle().imageUp;

        Image previewCard = new Image(previewDrawable);
        previewCard.setScaling(Scaling.stretch);
        previewCard.setBounds(nextX, nextY, previewWidth, previewHeight);

        previewCard.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                showNextCard();
            }
        });

        carouselGroup.addActor(previewCard);

        refreshDots();
    }
    private void showNextCard() {
        currentCardIndex++;
        if (currentCardIndex >= cards.size()) {
            currentCardIndex = 0;
        }
        refreshCarousel();
    }
    private void showPreviousCard() {
        currentCardIndex--;

        if (currentCardIndex < 0) {
            currentCardIndex = cards.size() - 1;
        }

        refreshCarousel();
    }
    @Override
    public void show() {
        super.show();
        game.showHud(
                0,
                0,
                true,
                this::handleLogout
        );
        AudioManager.getInstance().playMusic("assets/sounds/MainMenu.mp3");
    }
    private void handleLogout() {
        if (logoutInFlight) {
            return;
        }

        if (!game.getNetworkManager().isConnected()) {
            finishLocalLogout();
            return;
        }

        logoutInFlight = true;

        try {
            game.getNetworkManager()
                    .getAccountClientService()
                    .logout()
                    .whenComplete(
                            (response, throwable) ->
                                    Gdx.app.postRunnable(
                                            () -> finishServerLogout(
                                                    response,
                                                    throwable
                                            )
                                    )
                    );
        } catch (IOException | RuntimeException exception) {
            finishServerLogout(
                    null,
                    exception
            );
        }
    }
    private void finishServerLogout(
            LogoutResponse response,
            Throwable throwable
    ) {
        logoutInFlight = false;

        if (throwable != null) {
            game.notifyError(
                    "Connection was lost while logging out."
            );
        } else if (response != null
                && !response.isSuccess()) {
            game.notifyError(
                    response.getMessage()
            );
        }

        finishLocalLogout();
    }
    private void finishLocalLogout() {
        ClientAuthState.clear();

        game.showScreen(
                new FirstScreen(game)
        );
    }
}
