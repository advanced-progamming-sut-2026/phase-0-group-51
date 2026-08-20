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
import models.Zombie.Zombie;
import models.games.GameState;
import models.minigames.MinigameType;
import models.minigames.zombotany.Zombotany;
import views.graphical.gameplay.board.BoardArea;
import views.graphical.gameplay.board.BoardTransform;
import views.graphical.gameplay.board.BoardView;
import views.graphical.gameplay.manager.PlantViewManager;
import views.graphical.gameplay.manager.ProjectileViewManager;
import views.graphical.gameplay.manager.SunViewManager;
import views.graphical.screens.MainMenuScreen;
import views.graphical.screens.minigamesScreen.BaseMinigameScreen;
import views.graphical.screens.minigamesScreen.minigames;
import views.graphical.ui.*;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class ZombotanyScreen extends BaseMinigameScreen {
    private static final String BG_LEFT =
            "IMAGE_BACKGROUNDS_JOUST_TEXTURE_LEFT";

    private static final String BG_MID =
            "IMAGE_BACKGROUNDS_JOUST_TEXTURE";

    private static final String BG_RIGHT =
            "IMAGE_BACKGROUNDS_JOUST_TEXTURE_RIGHT";

    private static final String PEASHOOTER_ZOMBIE_TEXTURE =
            "assets/zombieP.png";

    private final ZombotanyController controller;

    private final int stageNumber;

    private Zombotany gameModel;

    private final BoardTransform boardTransform;

    private BoardView boardView;

    private PlantViewManager plantViewManager;

    private ProjectileViewManager projectileViewManager;

    private SunViewManager sunViewManager;

    private PlantSelectionMenuTable plantSelectionMenu;

    private PlantSlotsBar selectionSlotsBar;

    private boolean gameplayViewsBuilt;

    private float renderDelta;

    private Texture peashooterZombieTexture;
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
                new ZombotanyController();

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

        peashooterZombieTexture =
                new Texture(
                        Gdx.files.internal(
                                PEASHOOTER_ZOMBIE_TEXTURE
                        )
                );

        peashooterZombieTexture.setFilter(
                Texture.TextureFilter.Nearest,
                Texture.TextureFilter.Nearest
        );

        peashooterZombieTexture.setWrap(
                Texture.TextureWrap.ClampToEdge,
                Texture.TextureWrap.ClampToEdge
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

        syncCustomZombies();
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

        String alias =
                zombie.getAlias();


        if (
                "ZombotanyPeashooter"
                        .equals(alias)
        ) {

            return new ZombotanyPeashooterActor(
                    zombie,
                    peashooterZombieTexture,
                    boardTransform
            );
        }


        /*
         * بعداً:
         *
         * else if (
         *     "ZombotanyWallNut".equals(alias)
         * ) {
         *
         *     return new ZombotanyWallNutActor(...);
         * }
         *
         * else if (
         *     "ZombotanyJalapeno".equals(alias)
         * ) {
         *
         *     return new ZombotanyJalapenoActor(...);
         * }
         *
         * else if (
         *     "ZombotanySquash".equals(alias)
         * ) {
         *
         *     return new ZombotanySquashActor(...);
         * }
         */


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


        /*
         * Texture
         */
        if (
                peashooterZombieTexture != null
        ) {

            peashooterZombieTexture.dispose();

            peashooterZombieTexture = null;
        }


        super.dispose();
    }

}

