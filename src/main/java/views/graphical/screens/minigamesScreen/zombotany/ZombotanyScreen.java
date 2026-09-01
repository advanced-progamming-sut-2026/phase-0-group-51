package views.graphical.screens.minigamesScreen.zombotany;

import Data.loader.PlantData;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.Actor;
import controllers.miniGamesController.ZombotanyController;
import graphics.PvzGame;
import models.App;
import models.Board.Board;
import models.Board.Tile;
import models.Result;
import models.Zombie.Behavior.Zombotany.PeashooterZombieBehavior;
import models.Zombie.Zombie;
import models.games.ChapterTheme;
import models.games.GameState;
import models.minigames.MinigameType;
import models.minigames.zombotany.Zombotany;
import views.graphical.gameplay.board.BoardArea;
import views.graphical.gameplay.board.BoardTransform;
import views.graphical.gameplay.board.BoardView;
import views.graphical.gameplay.manager.PlantViewManager;
import views.graphical.gameplay.manager.ProjectileViewManager;
import views.graphical.gameplay.manager.SunViewManager;
import views.graphical.gameplay.mower.MowerAnimationSystem;
import views.graphical.screens.MainMenuScreen;
import views.graphical.screens.minigamesScreen.BaseMinigameScreen;
import views.graphical.screens.minigamesScreen.minigames;
import views.graphical.ui.*;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class ZombotanyScreen extends BaseMinigameScreen {
    private static final String BG_LEFT =
            "IMAGE_BACKGROUNDS_FRONTLAWN_PADDYS_TEXTURE_LEFT";

    private static final String BG_MID =
            "IMAGE_BACKGROUNDS_FRONTLAWN_PADDYS_TEXTURE";

    private static final String BG_RIGHT =
            "IMAGE_BACKGROUNDS_FRONTLAWN_PADDYS_TEXTURE_RIGHT";

    private static final String PEASHOOTER_ZOMBIE_WALK_TEXTURE =
            "assets/zombieP_walk.png";

    private static final String PEASHOOTER_ZOMBIE_SHOOT_TEXTURE =
            "assets/zombieP_shoot.png";

    private static final String WALLNUT_ZOMBIE_WALK_TEXTURE =
            "assets/zombieWN_walk.png";

    private static final int WALLNUT_FRAME_WIDTH = 339;

    private static final String SQUASH_ZOMBIE_WALK_TEXTURE =
            "assets/zombieSQ_walk.png";

    private static final int SQUASH_FRAME_WIDTH = 328;

    private static final String JALAPENO_ZOMBIE_WALK_TEXTURE =
            "assets/zombieJA_walk.png";

    private static final int JALAPENO_FRAME_WIDTH = 292;

    private static final float ZOMBOTANY_ZOMBIE_SCALE = 0.28f;

    private final ZombotanyController controller;

    private final int stageNumber;

    private Zombotany gameModel;

    private final BoardTransform boardTransform;

    private BoardView boardView;

    private PlantViewManager plantViewManager;

    private ProjectileViewManager projectileViewManager;

    private SunViewManager sunViewManager;

    private MowerAnimationSystem mowerAnimationSystem;

    private boolean shovelMode;

    private CenterGameNotice waveNotice;

    private int lastWaveShown;

    private PlantSelectionMenuTable plantSelectionMenu;

    private PlantSlotsBar selectionSlotsBar;

    private boolean gameplayViewsBuilt;

    private float renderDelta;

    private Texture peashooterZombieWalkTexture;
    private Texture peashooterZombieShootTexture;
    private Texture wallnutWalkTexture;
    private Texture squashWalkTexture;
    private Texture jalapenoWalkTexture;
    private final Map<Zombie, Actor> customZombieActors =
            new HashMap<>();


    public ZombotanyScreen(
            PvzGame game,
            int stageNumber
    ) {
        super(
                game,
                BG_LEFT,
                BG_MID,
                BG_RIGHT
        );

        this.stageNumber = stageNumber;

        this.controller =
                new ZombotanyController(game.getNetworkManager());

        BoardArea boardArea =
                new BoardArea(
                        533f,
                        62f,
                        737f,
                        380f
                );

        boardTransform =
                new BoardTransform(boardArea);
    }


    @Override
    public void show() {

        super.show();

        gameHud.hideGameHud();

        peashooterZombieWalkTexture =
                new Texture(
                        Gdx.files.internal(
                                PEASHOOTER_ZOMBIE_WALK_TEXTURE
                        )
                );

        peashooterZombieShootTexture =
                new Texture(
                        Gdx.files.internal(
                                PEASHOOTER_ZOMBIE_SHOOT_TEXTURE
                        )
                );

        configurePeashooterTexture(
                peashooterZombieWalkTexture
        );
        configurePeashooterTexture(
                peashooterZombieShootTexture
        );

        wallnutWalkTexture =
                new Texture(
                        Gdx.files.internal(
                                WALLNUT_ZOMBIE_WALK_TEXTURE
                        )
                );
        configurePeashooterTexture(
                wallnutWalkTexture
        );

        squashWalkTexture =
                new Texture(
                        Gdx.files.internal(
                                SQUASH_ZOMBIE_WALK_TEXTURE
                        )
                );
        configurePeashooterTexture(
                squashWalkTexture
        );

        jalapenoWalkTexture =
                new Texture(
                        Gdx.files.internal(
                                JALAPENO_ZOMBIE_WALK_TEXTURE
                        )
                );
        configurePeashooterTexture(
                jalapenoWalkTexture
        );


        Result startResult =
                controller.startStage(stageNumber);

        if (!startResult.success()) {

            game.notifyError(
                    startResult.message()
            );

            Gdx.app.postRunnable(
                    () -> game.showScreen(
                            new MainMenuScreen(game)
                    )
            );

            return;
        }


        this.gameModel =
                (Zombotany)
                        App.getInstance()
                                .getCurrentGame();
    }


    @Override
    protected Actor createStartPopup(
            Runnable onContinue
    ) {

        return new StartGameMenuPopup(
                game,
                onContinue,
                "Zombotany",
                stageNumber,
                "Watch out! The zombies have plant heads!",
                "They will shoot, explode, or crush your plants.",
                "Choose your seeds carefully and survive the waves."
        );
    }


    @Override
    protected void afterPreview() {

        if (
                plantSelectionMenu != null
                        || gameplayViewsBuilt
        ) {
            return;
        }


        selectionSlotsBar =
                new PlantSlotsBar(game);

        selectionSlotsBar.setMode(
                PlantSlotsBar.Mode.SELECTION
        );


        plantSelectionMenu =
                new PlantSelectionMenuTable(
                        game,
                        selectionSlotsBar,
                        this::finishPlantSelection
                );


        uiStage.addActor(
                plantSelectionMenu
        );

        plantSelectionMenu.toFront();
    }


    private void finishPlantSelection() {

        if (plantSelectionMenu != null) {

            plantSelectionMenu.remove();

            plantSelectionMenu = null;
        }


        PlantSlotsBar gameplayBar =
                gameHud.getPlantSlotsBar();


        gameplayBar.loadPlants(
                gameModel.getSelectedPlantsForThisGame()
        );

        gameplayBar.setMode(
                PlantSlotsBar.Mode.GAMEPLAY
        );


        buildGameplayViews();

        beginGameplayReturn();
    }


    private void buildGameplayViews() {

        if (gameplayViewsBuilt) {
            return;
        }


        GameState state =
                gameModel.getGameState();

        Board board =
                state.getBoard();


        boardView =
                new BoardView(
                        board,
                        boardTransform
                );

        boardView.setOnTileClicked(
                this::handleTileClicked
        );

        worldStage.addActor(
                boardView
        );


        plantViewManager =
                new PlantViewManager(
                        game,
                        boardTransform
                );


        projectileViewManager =
                new ProjectileViewManager(
                        game,
                        boardTransform
                );


        sunViewManager =
                new SunViewManager(
                        game,
                        boardTransform
                );


        sunViewManager.setOnSunClicked(
                sun ->
                        state.getBoard()
                                .collectSun(
                                        sun,
                                        state
                                )
        );


        worldStage.addActor(
                plantViewManager
        );

        worldStage.addActor(
                projectileViewManager
        );

        worldStage.addActor(
                sunViewManager
        );


        plantViewManager.sync(board);

        mowerAnimationSystem =
                new MowerAnimationSystem(
                        game.getPamPlayer(),
                        worldStage,
                        boardTransform,
                        ChapterTheme.MINIGAME
                );

        mowerAnimationSystem.update(
                0f,
                0f,
                state.getTickCounter(),
                state
        );

        gameHud.setOnShovelRequested(this::toggleShovelMode);

        gameplayViewsBuilt = true;
    }


    @Override
    protected void onGameplayStarted() {

        gameTickAccumulator = 0f;

        gameHud.showGameHud();
    }


    @Override
    public void render(float delta) {

        renderDelta =
                Math.min(delta, 0.25f);

        super.render(delta);
    }


    @Override
    protected void onGameTick() {

        if (
                gameModel != null
                        && !gameModel
                        .getGameState()
                        .isFinished()
        ) {

            controller.advanceTime(1);

            checkWinLossCondition();

            maybeShowWaveBanner();
        }
    }


    @Override
    protected void renderWorldUnderlay() {

        if (
                !gameplayViewsBuilt
                        || !isPlaying()
                        || isPaused()
                        || gameModel == null
        ) {
            return;
        }


        GameState state =
                gameModel.getGameState();

        Board board =
                state.getBoard();


        float ticksPerSecond =
                Math.max(
                        1,
                        state.getTicksPerSecond()
                );


        float partialTick =
                Math.max(
                        0f,
                        Math.min(
                                1f,
                                gameTickAccumulator
                                        / (1f / ticksPerSecond)
                        )
                );


        plantViewManager.sync(board);

        projectileViewManager.sync(
                board.getProjectiles(),
                partialTick
        );

        sunViewManager.sync(
                board.getActiveSuns(),
                partialTick
        );

        if (mowerAnimationSystem != null) {
            mowerAnimationSystem.update(
                    renderDelta,
                    partialTick,
                    state.getTickCounter(),
                    state
            );
        }

        syncCustomZombies();

        spawnPendingPeas();
    }


    private void configurePeashooterTexture(
            Texture texture
    ) {
        texture.setFilter(
                Texture.TextureFilter.Nearest,
                Texture.TextureFilter.Nearest
        );
        texture.setWrap(
                Texture.TextureWrap.ClampToEdge,
                Texture.TextureWrap.ClampToEdge
        );
    }

    private void spawnPendingPeas() {
        if (gameModel == null) {
            return;
        }

        GameState state = gameModel.getGameState();

        for (Zombie zombie : state.getZombiesInTheGame()) {
            if (zombie == null || zombie.isDead()) {
                continue;
            }

            if (!"ZombotanyPeashooter".equals(zombie.getAlias())) {
                continue;
            }

            PeashooterZombieBehavior behavior =
                    zombie.getBehavior(PeashooterZombieBehavior.class);

            if (behavior != null && behavior.consumeShot()) {
                worldStage.addActor(
                        new ZombotanyPeaActor(
                                game,
                                boardTransform,
                                zombie.getLane(),
                                zombie.getX()
                        )
                );
            }
        }
    }

    private void syncCustomZombies() {

        if (gameModel == null) {
            return;
        }


        GameState state =
                gameModel.getGameState();

        for (
                Zombie zombie :
                state.getZombiesInTheGame()
        ) {

            if (zombie == null) {
                continue;
            }

            if (zombie.isDead()) {

                removeCustomZombieActor(
                        zombie
                );

                continue;
            }


            Actor existing =
                    customZombieActors.get(zombie);


            if (existing == null) {

                Actor zombieActor =
                        createCustomZombieActor(
                                zombie
                        );


                if (zombieActor != null) {

                    customZombieActors.put(
                            zombie,
                            zombieActor
                    );

                    worldStage.addActor(
                            zombieActor
                    );
                }
            }
        }


        Iterator<
                        Map.Entry<Zombie, Actor>
                        > iterator =
                customZombieActors
                        .entrySet()
                        .iterator();


        while (iterator.hasNext()) {

            Map.Entry<Zombie, Actor> entry =
                    iterator.next();


            Zombie zombie =
                    entry.getKey();

            Actor actor =
                    entry.getValue();


            if (
                    !state
                            .getZombiesInTheGame()
                            .contains(zombie)
                            || zombie.isDead()
            ) {

                actor.remove();

                iterator.remove();
            }
        }
    }


    private Actor createCustomZombieActor(
            Zombie zombie
    ) {
        String alias = zombie.getAlias();
        if ("ZombotanyPeashooter".equals(alias)) {
            return new ZombotanyPeashooterActor(
                    zombie,
                    peashooterZombieWalkTexture,
                    peashooterZombieShootTexture,
                    boardTransform
            );
        }
        if ("ZombotanyWallnut".equals(alias)) {
            return new ZombotanyWalkActor(
                    zombie,
                    wallnutWalkTexture,
                    WALLNUT_FRAME_WIDTH,
                    8,
                    boardTransform,
                    ZOMBOTANY_ZOMBIE_SCALE
            );
        }
        if ("ZombotanySquash".equals(alias)) {
            return new ZombotanyWalkActor(
                    zombie,
                    squashWalkTexture,
                    SQUASH_FRAME_WIDTH,
                    8,
                    boardTransform,
                    ZOMBOTANY_ZOMBIE_SCALE
            );
        }
        if ("ZombotanyJalapeno".equals(alias)) {
            return new ZombotanyWalkActor(
                    zombie,
                    jalapenoWalkTexture,
                    JALAPENO_FRAME_WIDTH,
                    8,
                    boardTransform,
                    ZOMBOTANY_ZOMBIE_SCALE
            );
        }
        return null;
    }


    private void removeCustomZombieActor(
            Zombie zombie
    ) {

        Actor actor =
                customZombieActors.remove(zombie);


        if (actor != null) {
            actor.remove();
        }
    }


    private void handleTileClicked(
            Tile tile
    ) {

        if (
                !isPlaying()
                        || isPaused()
                        || tile == null
        ) {
            return;
        }


        if (shovelMode) {
            if (tile.hasPlant()) {
                tile.removePlant();
                plantViewManager.sync(
                        gameModel.getGameState().getBoard()
                );
            }
            setShovelMode(false);
            return;
        }


        PlantData selectedPlant =
                gameHud
                        .getPlantSlotsBar()
                        .getSelectedPlant();


        if (selectedPlant == null) {
            return;
        }


        int x =
                tile.getColumn() + 1;

        int y =
                tile.getLane() + 1;


        Result result =
                controller.placePlant(
                        selectedPlant.name(),
                        x,
                        y
                );


        if (!result.success()) {

            game.notifyError(
                    result.message()
            );

        } else {

            plantViewManager.sync(
                    gameModel
                            .getGameState()
                            .getBoard()
            );

            gameHud
                    .getPlantSlotsBar()
                    .clearPlantSelection();
        }
    }


    private void toggleShovelMode() {
        setShovelMode(!shovelMode);
    }

    private void setShovelMode(boolean active) {
        shovelMode = active;

        if (active) {
            gameHud.getPlantSlotsBar().clearPlantSelection();
        }

        gameHud.setShovelSelected(active);
    }

    private void maybeShowWaveBanner() {
        if (gameModel == null) {
            return;
        }

        int wavesSent = gameModel.getWavesSent();

        if (wavesSent <= lastWaveShown) {
            return;
        }

        lastWaveShown = wavesSent;

        if (wavesSent == gameModel.getTotalWaves()) {
            showWaveBanner("A HUGE WAVE OF ZOMBIES IS APPROACHING!");
        }
    }

    private void showWaveBanner(String message) {
        if (waveNotice != null) {
            waveNotice.remove();
            waveNotice = null;
        }

        CenterGameNotice notice =
                new CenterGameNotice(game.getSkin(), false);

        waveNotice = notice;

        uiStage.addActor(notice);

        notice.showSequence(
                java.util.List.of(message),
                1.6f,
                () -> {
                    if (waveNotice == notice) {
                        waveNotice = null;
                    }
                }
        );
    }

    private void checkWinLossCondition() {

        GameState state =
                gameModel.getGameState();


        if (
                state.isFinished()
                        && isPlaying()
        ) {

            onGameFinished(
                    state.isWon()
            );
        }
    }


    @Override
    protected void onGameFinished(
            boolean won
    ) {

        controller.recordGraphicalResult();
        gameHud.hideGameHud();


        if (won) {

            boolean hasNextStage =
                    stageNumber < 3;


            Runnable nextAction =
                    hasNextStage

                            ? () ->
                            Gdx.app.postRunnable(
                                    () ->
                                            game.showScreen(
                                                    new ZombotanyScreen(
                                                            game,
                                                            stageNumber + 1
                                                    )
                                            )
                            )

                            : this::exitMinigame;


            GameWinPopup winPopup =
                    new GameWinPopup(
                            game,
                            "ZOMBOTANY COMPLETE!",
                            "You survived the mutant zombies.",
                            "EXIT",
                            this::exitMinigame,
                            hasNextStage
                                    ? "NEXT STAGE"
                                    : "MINIGAMES",
                            nextAction
                    );


            uiStage.addActor(
                    winPopup
            );

        } else {

            GameOverPopup losePopup =
                    new GameOverPopup(
                            game,
                            "THE ZOMBIES\nREACHED YOUR HOUSE!",
                            "EXIT",
                            this::exitMinigame,
                            "RETRY",
                            this::restartMinigame
                    );


            uiStage.addActor(
                    losePopup
            );
        }
    }


    @Override
    protected void restartMinigame() {

        Gdx.app.postRunnable(
                () ->
                        game.showScreen(
                                new ZombotanyScreen(
                                        game,
                                        stageNumber
                                )
                        )
        );
    }


    @Override
    protected void exitMinigame() {

        controller.exitMenu();


        Gdx.app.postRunnable(
                () ->
                        game.showScreen(
                                new minigames(
                                        game,
                                        MinigameType.ZOMBOTANY
                                )
                        )
        );
    }


    @Override
    public void dispose() {

        /*
         * Actorهای سفارشی
         */
        for (
                Actor actor :
                customZombieActors.values()
        ) {
            actor.remove();
        }

        customZombieActors.clear();

        if (mowerAnimationSystem != null) {
            mowerAnimationSystem.clear();
            mowerAnimationSystem = null;
        }


        /*
         * Texture
         */
        if (
                peashooterZombieWalkTexture != null
        ) {
            peashooterZombieWalkTexture.dispose();
            peashooterZombieWalkTexture = null;
        }

        if (
                peashooterZombieShootTexture != null
        ) {
            peashooterZombieShootTexture.dispose();
            peashooterZombieShootTexture = null;
        }


        super.dispose();
    }

}

