package views.graphical.screens;

import Data.loader.PlantData;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.*;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.ButtonGroup;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import controllers.GamingController;
import graphics.PvzGame;
import models.App;
import models.Board.Board;
import models.Board.Tile;
import models.Result;
import models.effects.GameplayNoticeEvent;
import models.effects.VisualEffectEvent;
import models.Zombie.Zombie;
import models.Zombie.ZombieType;
import models.games.ChapterTheme;
import models.games.Game;
import models.games.GameState;
import models.games.Level;
import models.enums.LootType;
import models.items.DroppedLoot;
import models.games.ZombieWaveManager;
import models.sun.Sun;


import views.graphical.gameplay.actors.PlantActor;
import views.graphical.gameplay.board.BoardArea;
import views.graphical.gameplay.board.BoardTransform;
import views.graphical.gameplay.board.BoardView;

import views.graphical.gameplay.hud.GameHud;
import views.graphical.gameplay.grave.GraveAnimationSystem;
import views.graphical.gameplay.frostbite.IceFloorAnimationSystem;
import views.graphical.gameplay.frostbite.FrozenZombieIceAnimationSystem;
import views.graphical.gameplay.effects.SandstormAnimationSystem;
import views.graphical.gameplay.effects.FrostbiteSnowstormAnimationSystem;
import views.graphical.gameplay.manager.PlantViewManager;
import views.graphical.gameplay.manager.DepthSortedEntityLayer;
import views.graphical.gameplay.manager.ProtectedPlantOverlayManager;
import views.graphical.gameplay.manager.DeadlineOverlayManager;
import views.graphical.gameplay.manager.ProjectileViewManager;
import views.graphical.gameplay.manager.SunViewManager;
import views.graphical.gameplay.manager.WorldEffectManager;
import views.graphical.gameplay.mower.MowerAnimationSystem;
import views.graphical.gameplay.loot.LootAnimationSystem;
import views.graphical.gameplay.zombie.ZombieAnimationSystem;
import views.graphical.gameplay.zombie.ZombieLevelPreview;
import views.graphical.dialogue.LevelDialogueRegistry;
import views.graphical.dialogue.NpcDialogueSequence;
import views.graphical.ui.*;
import views.graphical.ui.conveyorBelt.ConveyorBeltActor;
import views.graphical.ui.conveyorBelt.ConveyorSpecialLevel;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.List;

public class GameScreen extends BaseScreen {

    private final ChapterTheme theme;
    private final Level currentLevel;

    private final OrthographicCamera camera;
    private final Viewport viewport;
    private final Stage uiStage;
    private final Stage worldStage;
    private final DepthSortedEntityLayer entityDepthLayer;
    private final InputMultiplexer inputMultiplexer;
    private final PlantSlotsBar plantSlotsBar;

    private TextureRegion bgLeft;
    private TextureRegion bgMid;
    private TextureRegion bgRight;

    private final float viewWidth = 1066f;
    private final float worldHeight = 600f;
    private static final float FROSTBITE_MIDDLE_BACKGROUND_Y_OFFSET = -10f;
    private ConveyorBeltActor specialConveyorBelt;
    private List<PlantData> lastConveyorSnapshot = List.of();
    private int selectedConveyorIndex = -1;
    private final ButtonGroup<PlantCard> conveyorButtonGroup = new ButtonGroup<>();
    private PlantData specialConveyorSelectedPlant;
    private enum IntroState {
        WAIT_AT_MAIN,
        PAN_TO_ZOMBIES,
        WAIT_AT_ZOMBIES,
        PAN_TO_SELECTION,
        SHOW_PLANT_SELECT,
        WAITING_FOR_SELECTION,
        PAN_BACK_TO_MAIN,
        START_COUNTDOWN,
        PLAYING
    }

    private enum OverlayMode {
        NONE, NPC_DIALOGUE, START_OBJECTIVES, PAUSE, GAME_END
    }

    private enum ToolMode {
        NONE, SHOVEL, PLANT_FOOD
    }

    private OverlayMode overlayMode = OverlayMode.NONE;
    private ToolMode toolMode = ToolMode.NONE;

    private Table modalOverlay;
    private final Texture modalDimTexture;

    private IntroState introState = IntroState.WAIT_AT_MAIN;
    private float stateTime = 0f;

    private static final float INITIAL_ZOMBIE_SPAWN_DELAY_SECONDS = 5f;

    private float cameraMainX;
    private float cameraRightX;
    private float cameraSelectionX;
    private float cameraGameplayX;

    private GameHud gameHud;
    private CenterGameNotice startCountdownNotice;
    private CenterGameNotice waveNotice;
    private final Deque<List<String>> gameplayNoticeQueue =
        new ArrayDeque<>();
    private int lastWaveNoticeNumber;
    private boolean firstWaveCoveredByCountdown = true;

    private static final float NOTICE_DURATION = 1f;
    private static final List<String> START_COUNTDOWN_MESSAGES =
        List.of("ready?", "1", "2", "3");
    private static final String HUGE_WAVE_NOTICE =
        "A HUGE WAVE OF ZOMBIES IS APPROACHING!";
    private static final String FINAL_WAVE_NOTICE = "FINAL WAVE";
    private static final String NECROMANCY_NOTICE = "NECROMANCY!";
    private static final String LOW_TIDE_NOTICE =
        "ZOMBIES ARE RISING FROM THE TIDE!";

    private final ShapeRenderer shapeRenderer;

    private BoardView boardView;
    private BoardArea boardArea;
    private final BoardTransform boardTransform;
    private final ZombieAnimationSystem zombieAnimationSystem;
    private final LootAnimationSystem lootAnimationSystem;
    private final SandstormAnimationSystem sandstormAnimationSystem;
    private final FrostbiteSnowstormAnimationSystem frostbiteSnowstormAnimationSystem;
    private final IceFloorAnimationSystem iceFloorAnimationSystem;
    private final FrozenZombieIceAnimationSystem frozenZombieIceAnimationSystem;
    private final MowerAnimationSystem mowerAnimationSystem;
    private final GraveAnimationSystem graveAnimationSystem;
    private final ZombieLevelPreview zombieLevelPreview;


    private final GamingController gamingController = new GamingController();

    private float gameTickAccumulator;
    private int renderInterpolationModelTick = Integer.MIN_VALUE;
    private float renderInterpolationElapsed;
    private float renderInterpolationDuration = 0.1f;
    private boolean gameEndShown;

    private PlantActor placementPreview;
    private PlantViewManager plantViewManager;
    private ProtectedPlantOverlayManager protectedPlantOverlayManager;
    private DeadlineOverlayManager deadlineOverlayManager;
    private SunViewManager sunViewManager;
    private ProjectileViewManager projectileViewManager;
    private WorldEffectManager worldEffectManager;

    private static final float EXPLOSION_SHAKE_DURATION = 0.28f;
    private static final float EXPLOSION_SHAKE_MAGNITUDE = 8f;

    private float screenShakeRemaining;
    private float screenShakeDuration;
    private float screenShakeMagnitude;
    private float screenShakeBaseX;
    private float screenShakeBaseY;
    private boolean screenShakeApplied;

    private Image rowHighlight;
    private Image columnHighlight;

    private static final String SHOVEL_CURSOR =
        "IMAGE_UI_HUD_INGAME_SHOVEL_ICON";
    private static final String PLANT_FOOD_CURSOR =
        "IMAGE_UI_HUD_INGAME_PLANTFOOD_BUTTON";
    private static final float TOOL_CURSOR_ALPHA = 0.58f;
    private static final float TOOL_CURSOR_SCALE = 0.65f;

    private Image toolCursorPreview;
    private final Vector2 toolCursorPosition = new Vector2();


    public GameScreen(PvzGame game, ChapterTheme theme, int levelNumber) {
        super(game);
        this.theme = theme;
        this.currentLevel = theme.getLevels().stream()
            .filter(l -> l.levelNumber() == levelNumber)
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Level " + levelNumber + " not found!"));

        Game currentGame = new Game();
        int chapterIndex = currentGame.getChapters().indexOf(theme);
        int levelIndex = theme.getLevels().indexOf(currentLevel);
        if (chapterIndex < 0 || levelIndex < 0) {
            throw new IllegalStateException(
                "Could not resolve selected chapter/level."
            );
        }

        currentGame.setCurrentChapterIndex(chapterIndex);
        currentGame.setCurrentLevelIndex(levelIndex);
        currentGame.loadLevelForPreview();

        App.getInstance().setCurrentGame(currentGame);

        loadBackgroundAssets();

        float totalWorldWidth = bgLeft.getRegionWidth() + bgMid.getRegionWidth() + bgRight.getRegionWidth();

        camera = new OrthographicCamera();
        viewport = new ExtendViewport(viewWidth, worldHeight, camera);
        worldStage = new Stage(viewport, game.getBatch());
        uiStage = new Stage(new ExtendViewport(viewWidth, worldHeight));
        inputMultiplexer = new InputMultiplexer(uiStage, worldStage);

        plantSlotsBar = new PlantSlotsBar(game);
        plantSlotsBar.setMode(PlantSlotsBar.Mode.SELECTION);

        cameraMainX = viewWidth / 2f;
        cameraGameplayX = bgLeft.getRegionWidth() + bgMid.getRegionWidth() / 2f;
        cameraRightX = totalWorldWidth - (viewWidth / 2f);
        cameraSelectionX = cameraRightX - (viewWidth / 2.5f);

        camera.position.set(cameraMainX, worldHeight / 2f, 0);
        camera.update();

        shapeRenderer = new ShapeRenderer();

        Pixmap modalDimPixmap =
            new Pixmap(
                1,
                1,
                Pixmap.Format.RGBA8888
            );

        modalDimPixmap.setColor(Color.WHITE);
        modalDimPixmap.fill();

        modalDimTexture =
            new Texture(modalDimPixmap);

        modalDimPixmap.dispose();

        boardArea = new BoardArea(
            533f,
            62f,
            737f,
            380f);

        boardTransform = new BoardTransform(boardArea);

        iceFloorAnimationSystem =
            new IceFloorAnimationSystem(
                game.getPamPlayer(),
                boardTransform,
                theme
            );

        worldStage.addActor(
            iceFloorAnimationSystem
        );

        entityDepthLayer = new DepthSortedEntityLayer();
        worldStage.addActor(entityDepthLayer);

        zombieAnimationSystem = new ZombieAnimationSystem(
            game.getPamPlayer(),
            worldStage,
            boardTransform,
            theme,
            0.61f,
            currentGame.getGameState(),
            entityDepthLayer
        );

        lootAnimationSystem =
            new LootAnimationSystem(
                game.getPamPlayer(),
                worldStage,
                boardTransform,
                zombieAnimationSystem,
                game
            );

        frozenZombieIceAnimationSystem =
            new FrozenZombieIceAnimationSystem(
                game.getPamPlayer(),
                zombieAnimationSystem,
                theme
            );

        sandstormAnimationSystem =
            new SandstormAnimationSystem(
                game.getPamPlayer(),
                worldStage,
                zombieAnimationSystem,
                theme
            );

        frostbiteSnowstormAnimationSystem =
            new FrostbiteSnowstormAnimationSystem(
                game.getPamPlayer(),
                worldStage,
                zombieAnimationSystem,
                theme
            );

        mowerAnimationSystem =
            new MowerAnimationSystem(
                game.getPamPlayer(),
                worldStage,
                boardTransform,
                theme
            );

        graveAnimationSystem = new GraveAnimationSystem(
            game.getPamPlayer(),
            boardTransform,
            theme,
            GraveAnimationSystem.DEFAULT_SCALE,
            entityDepthLayer
        );

        worldStage.addActor(
            graveAnimationSystem
        );

        if (currentGame.getGameState() != null
            && currentGame.getGameState().getBoard() != null) {
            graveAnimationSystem.sync(
                currentGame
                    .getGameState()
                    .getBoard()
            );

            iceFloorAnimationSystem.sync(
                currentGame
                    .getGameState()
                    .getBoard()
            );

            protectedPlantOverlayManager =
                new ProtectedPlantOverlayManager(
                    game,
                    boardTransform
                );
            worldStage.addActor(
                protectedPlantOverlayManager
            );

            // Protected Save Our Seeds tiles must render behind the actual
            // plant actors. Plant actors live inside entityDepthLayer.
            int entityZ = entityDepthLayer.getZIndex();
            int protectedZ = protectedPlantOverlayManager.getZIndex();

            if (protectedZ > entityZ) {
                protectedPlantOverlayManager.setZIndex(entityZ);
            }

            deadlineOverlayManager =
                new DeadlineOverlayManager(
                    game,
                    boardTransform
                );
            worldStage.addActor(
                deadlineOverlayManager
            );

            plantViewManager =
                new PlantViewManager(
                    game,
                    boardTransform,
                    entityDepthLayer
                );
            worldStage.addActor(
                plantViewManager
            );
            protectedPlantOverlayManager.sync(
                currentGame.getGameState()
            );
            deadlineOverlayManager.sync(
                currentGame.getGameState(),
                0f
            );
            plantViewManager.sync(
                currentGame
                    .getGameState()
                    .getBoard()
            );
        }

        zombieLevelPreview = new ZombieLevelPreview(
            game.getPamPlayer(),
            worldStage,
            theme,
            cameraRightX,
            entityDepthLayer
        );

        zombieLevelPreview.show(
            currentLevel.resolveAllowedZombies(
                theme.getAllowedZombies()
            )
        );
        conveyorButtonGroup.setMinCheckCount(0);
        conveyorButtonGroup.setMaxCheckCount(1);
        conveyorButtonGroup.setUncheckLast(true);
    }
    private float getMiddleBackgroundYOffset() {
        return theme == ChapterTheme.FROSTBITE_CAVES
            ? FROSTBITE_MIDDLE_BACKGROUND_Y_OFFSET
            : 0f;
    }
    private void loadBackgroundAssets() {
        switch (theme) {
            case ANCIENT_EGYPT:
                bgLeft = game.getTextureBank().region("IMAGE_BACKGROUNDS_EGYPT_TEXTURE_LEFT");
                bgMid = game.getTextureBank().region("IMAGE_BACKGROUNDS_EGYPT_TEXTURE");
                bgRight = game.getTextureBank().region("IMAGE_BACKGROUNDS_EGYPT_TEXTURE_RIGHT");
                break;
            case FROSTBITE_CAVES:
                bgLeft = game.getTextureBank().region("IMAGE_BACKGROUNDS_ICEAGE_TEXTURE_LEFT");
                bgMid = game.getTextureBank().region("IMAGE_BACKGROUNDS_ICEAGE_TEXTURE");
                bgRight = game.getTextureBank().region("IMAGE_BACKGROUNDS_ICEAGE_TEXTURE_RIGHT");
                break;
            case BIG_WAVE_BEACH:
                bgLeft = game.getTextureBank().region("IMAGE_BACKGROUNDS_BEACH_TEXTURE_LEFT");
                bgMid = game.getTextureBank().region("IMAGE_BACKGROUNDS_BEACH_TEXTURE");
                bgRight = game.getTextureBank().region("IMAGE_BACKGROUNDS_BEACH_TEXTURE_RIGHT");
                break;
            case DARK_AGES:
                bgLeft = game.getTextureBank().region("IMAGE_BACKGROUNDS_DARK_TEXTURE_LEFT");
                bgMid = game.getTextureBank().region("IMAGE_BACKGROUNDS_DARK_TEXTURE");
                bgRight = game.getTextureBank().region("IMAGE_BACKGROUNDS_DARK_TEXTURE_RIGHT");
                break;
            default:
                bgLeft = game.getTextureBank().region("IMAGE_BACKGROUNDS_EGYPT_TEXTURE_LEFT");
                bgMid = game.getTextureBank().region("IMAGE_BACKGROUNDS_EGYPT_TEXTURE");
                bgRight = game.getTextureBank().region("IMAGE_BACKGROUNDS_EGYPT_TEXTURE_RIGHT");
                break;
        }
    }

    @Override
    public InputMultiplexer getInputProcessor() {
        return inputMultiplexer;
    }

    @Override
    public void show() {
        game.hideHud();
        showLevelIntro();
    }

    private void showLevelIntro() {
        NpcDialogueSequence dialogue = LevelDialogueRegistry.find(
            theme,
            currentLevel.levelNumber()
        );

        if (dialogue == null) {
            showStartObjectives();
            return;
        }

        showNpcDialogue(dialogue);
    }

    private void showNpcDialogue(NpcDialogueSequence dialogue) {
        if (overlayMode != OverlayMode.NONE) {
            return;
        }

        overlayMode = OverlayMode.NPC_DIALOGUE;
        gameTickAccumulator = 0f;
        resetRenderTickInterpolation();

        NpcDialogueOverlay npcDialogueOverlay = new NpcDialogueOverlay(
            game,
            dialogue,
            this::finishNpcDialogue
        );
        uiStage.addActor(npcDialogueOverlay);
        uiStage.setKeyboardFocus(npcDialogueOverlay);
        npcDialogueOverlay.toFront();
    }

    private void finishNpcDialogue() {
        if (overlayMode != OverlayMode.NPC_DIALOGUE) {
            return;
        }

        uiStage.setKeyboardFocus(null);
        overlayMode = OverlayMode.NONE;
        gameTickAccumulator = 0f;
        resetRenderTickInterpolation();
        showStartObjectives();
    }

    @Override
    public void hide() {
        if (gameHud != null) {
            gameHud.hideGameHud();
        }
        removeModal();
    }

    private void updateCutscene(float delta) {
        if (overlayMode != OverlayMode.NONE) {
            return;
        }
        if (introState == IntroState.PLAYING
            || introState == IntroState.WAITING_FOR_SELECTION
            || introState == IntroState.START_COUNTDOWN) {
            return;
        }

        stateTime += delta;

        float waitDuration = 1.0f;
        float panDuration = 1.5f;
        float shortPanDuration = 0.8f;
        switch (introState) {
            case WAIT_AT_MAIN:
                if (stateTime >= waitDuration) {
                    introState = IntroState.PAN_TO_ZOMBIES;
                    stateTime = 0f;
                }
                break;

            case PAN_TO_ZOMBIES:
                float progressRight = Math.min(1f, stateTime / panDuration);
                camera.position.x = Interpolation.smooth.apply(cameraMainX, cameraRightX, progressRight);
                if (progressRight >= 1f) {
                    introState = IntroState.WAIT_AT_ZOMBIES;
                    stateTime = 0f;
                }
                break;

            case WAIT_AT_ZOMBIES:
                if (stateTime >= waitDuration) {
                    introState = IntroState.PAN_TO_SELECTION;
                    stateTime = 0f;
                }
                break;

            case PAN_TO_SELECTION:
                float progressSelection = Math.min(1f, stateTime / shortPanDuration);
                camera.position.x = Interpolation.smooth.apply(cameraRightX, cameraSelectionX, progressSelection);
                if (progressSelection >= 1f) {
                    introState = IntroState.SHOW_PLANT_SELECT;
                    stateTime = 0f;
                }
                break;

            case SHOW_PLANT_SELECT:
                Game currentGame = App.getInstance().getCurrentGame();
                if (currentGame != null && currentGame.isConveyorBeltLevel()) {
                    startGameAfterSelection();
                    break;
                }

                PlantSelectionMenuTable plantSelection =
                    new PlantSelectionMenuTable(
                        game,
                        plantSlotsBar,
                        this::startGameAfterSelection
                    );
                uiStage.addActor(plantSelection);
                introState = IntroState.WAITING_FOR_SELECTION;
                break;

            case PAN_BACK_TO_MAIN:
                float progressLeft = Math.min(1f, stateTime / panDuration);
                camera.position.x = Interpolation.smooth.apply(cameraSelectionX, cameraGameplayX, progressLeft);
                if (progressLeft >= 1f) {
                    camera.position.x = cameraGameplayX;
                    introState = IntroState.START_COUNTDOWN;
                    stateTime = 0f;
                    if (gameHud != null) {
                        gameHud.showGameHud();
                    }
                    showStartCountdown();
                }
                break;
        }

        camera.update();
    }

    private void showStartCountdown() {
        if (startCountdownNotice != null) {
            startCountdownNotice.remove();
        }

        startCountdownNotice =
            new CenterGameNotice(
                game.getSkin(),
                true
            );

        uiStage.addActor(startCountdownNotice);
        startCountdownNotice.showSequence(
            START_COUNTDOWN_MESSAGES,
            NOTICE_DURATION,
            this::finishStartCountdown
        );
    }

    private void finishStartCountdown() {
        if (introState != IntroState.START_COUNTDOWN) {
            return;
        }

        startCountdownNotice = null;

        gameTickAccumulator = 0f;
        resetRenderTickInterpolation();

        lastWaveNoticeNumber = 0;
        firstWaveCoveredByCountdown = isFirstWaveReadyToAutoStart();

        armInitialZombieSpawnDelay();

        introState = IntroState.PLAYING;
        stateTime = 0f;
    }

    private void armInitialZombieSpawnDelay() {
        Game currentGame = App.getInstance().getCurrentGame();
        if (currentGame == null || currentGame.getGameState() == null) {
            return;
        }

        ZombieWaveManager waveManager =
            currentGame.getGameState().getZombieWaveManager();

        if (waveManager == null
            || waveManager.getCurrentWaveNumber() > 0) {
            return;
        }

        int ticksPerSecond =
            Math.max(
                1,
                currentGame.getGameState().getTicksPerSecond()
            );

        int delayTicks =
            Math.max(
                0,
                Math.round(
                    INITIAL_ZOMBIE_SPAWN_DELAY_SECONDS
                        * ticksPerSecond
                )
            );

        waveManager.setFirstWaveDelayTicks(delayTicks);
    }

    private boolean isFirstWaveReadyToAutoStart() {
        Game currentGame = App.getInstance().getCurrentGame();
        if (currentGame == null || currentGame.getGameState() == null) {
            return true;
        }

        ZombieWaveManager waveManager =
            currentGame.getGameState().getZombieWaveManager();
        return waveManager == null
            || (waveManager.isStarted()
            && waveManager.getFirstWaveDelayTicks() <= 0);
    }

    private void processGameplayNotices() {
        Game currentGame =
            App.getInstance().getCurrentGame();

        if (currentGame == null
            || currentGame.getGameState() == null) {
            return;
        }

        for (
            GameplayNoticeEvent event :
            currentGame
                .getGameState()
                .consumeGameplayNotices()
        ) {
            switch (event.type()) {
                case NECROMANCY ->
                    showWaveNotice(
                        List.of(NECROMANCY_NOTICE)
                    );
                case LOW_TIDE_ZOMBIES ->
                    showWaveNotice(
                        List.of(LOW_TIDE_NOTICE)
                    );
            }
        }
    }

    private void updateWaveNotice() {
        if (introState != IntroState.PLAYING) {
            return;
        }

        Game currentGame = App.getInstance().getCurrentGame();
        if (currentGame == null || currentGame.getGameState() == null) {
            return;
        }

        ZombieWaveManager waveManager =
            currentGame.getGameState().getZombieWaveManager();
        if (waveManager == null) {
            return;
        }

        int waveNumber = waveManager.getCurrentWaveNumber();
        if (waveNumber <= 0 || waveNumber <= lastWaveNoticeNumber) {
            return;
        }

        lastWaveNoticeNumber = waveNumber;

        if (waveNumber == 1 && firstWaveCoveredByCountdown) {
            return;
        }

        if (!waveManager.isEndless()
            && waveNumber == waveManager.getTotalWaves()) {
            showWaveNotice(
                List.of(
                    HUGE_WAVE_NOTICE,
                    FINAL_WAVE_NOTICE
                )
            );
            return;
        }

        showWaveNotice(
            List.of("Wave " + waveNumber)
        );
    }

    private void showWaveNotice(List<String> messages) {
        if (messages == null || messages.isEmpty()) {
            return;
        }

        gameplayNoticeQueue.addLast(
            List.copyOf(messages)
        );

        showNextGameplayNotice();
    }

    private void showNextGameplayNotice() {
        if (waveNotice != null
            || gameplayNoticeQueue.isEmpty()) {
            return;
        }

        CenterGameNotice notice =
            new CenterGameNotice(
                game.getSkin(),
                false
            );

        waveNotice = notice;
        uiStage.addActor(notice);

        notice.showSequence(
            gameplayNoticeQueue.removeFirst(),
            NOTICE_DURATION,
            () -> {
                if (waveNotice == notice) {
                    waveNotice = null;
                    showNextGameplayNotice();
                }
            }
        );
    }

    public void startGameAfterSelection() {
        zombieLevelPreview.clear();

        plantSlotsBar.remove();
        plantSlotsBar.setOnRemoveRequested(null);

        uiStage.clear();
        plantSlotsBar.setMode(
            PlantSlotsBar.Mode.GAMEPLAY
        );
        plantSlotsBar.setOnPlantSelected(
            this::handlePlantSelectionChanged
        );

        gameHud = new GameHud(
            game,
            plantSlotsBar,
            this::showPauseMenu
        );
        uiStage.addActor(gameHud);
        gameHud.setOnShovelRequested(
            this::toggleShovelMode
        );
        gameHud.setOnPlantFoodRequested(
            this::togglePlantFoodMode
        );

        Game currentGame =
            App.getInstance()
                .getCurrentGame();

        if (currentGame == null
            || currentGame.getGameState() == null) {
            throw new IllegalStateException(
                "Game state was not created."
            );
        }
        if (currentGame.isConveyorBeltLevel()) {

            plantSlotsBar.setVisible(
                    false
            );


            specialConveyorBelt = new ConveyorBeltActor("assets/UIs/Belt.png");

            if (currentGame.isConveyorBeltLevel()) {
                plantSlotsBar.setVisible(false);
                specialConveyorBelt = new ConveyorBeltActor("assets/UIs/Belt.png");
                specialConveyorBelt.setPosition(0f, 82f);
                uiStage.addActor(specialConveyorBelt);
                specialConveyorBelt.toFront();
                refreshConveyorBar(true);
            }
        }
        Board board =
            currentGame
                .getGameState()
                .getBoard();

        boardView =
            new BoardView(
                board,
                boardTransform
            );

        boardView.setOnTileClicked(
            this::handleTileClick
        );
        boardView.setOnTileHovered(
            this::handleTileHover
        );

        createPlacementHighlights();
        worldStage.addActor(rowHighlight);
        worldStage.addActor(columnHighlight);
        worldStage.addActor(boardView);

        if (deadlineOverlayManager != null) {
            deadlineOverlayManager.toFront();
            deadlineOverlayManager.sync(
                currentGame.getGameState(),
                0f
            );
        }

        graveAnimationSystem.toFront();
        graveAnimationSystem.sync(board);

        if (protectedPlantOverlayManager != null) {
            protectedPlantOverlayManager.sync(
                currentGame.getGameState()
            );
        }
        if (plantViewManager != null) {
            plantViewManager.sync(
                board
            );
        }

        projectileViewManager = new ProjectileViewManager(game, boardTransform);
        worldStage.addActor(projectileViewManager);
        worldEffectManager = new WorldEffectManager(game, boardTransform);
        worldStage.addActor(worldEffectManager);
        sunViewManager = new SunViewManager(game, boardTransform);
        sunViewManager.setOnSunClicked(this::handleSunClicked);
        worldStage.addActor(sunViewManager);

        mowerAnimationSystem.update(
            0f,
            0f,
            currentGame.getGameState().getTickCounter(),
            currentGame.getGameState()
        );

        placementPreview =
            new PlantActor(game);
        placementPreview.setPreviewMode(true);
        worldStage.addActor(placementPreview);

        gameTickAccumulator = 0f;
        resetRenderTickInterpolation();
        introState = IntroState.PAN_BACK_TO_MAIN;
        stateTime = 0f;
    }
    private void refreshConveyorBar(boolean force) {
        Game currentGame = App.getInstance().getCurrentGame();
        if (currentGame == null || !currentGame.isConveyorBeltLevel()) return;

        List<PlantData> current = currentGame.getConveyorBeltPlants();
        if (!force && current.equals(lastConveyorSnapshot)) return;

        int diff = current.size() - lastConveyorSnapshot.size();

        if (force || current.isEmpty()) {
            specialConveyorBelt.clearPlants();
            conveyorButtonGroup.clear();
            for (PlantData data : current) {
                addCardToBelt(data);
            }
        } else if (diff > 0) {
            for (int i = lastConveyorSnapshot.size(); i < current.size(); i++) {
                addCardToBelt(current.get(i));
            }
        }
        lastConveyorSnapshot = List.copyOf(current);
    }

    private void addCardToBelt(PlantData data) {
        if (data == null) return;

        PlantCard card = new PlantCard(game, new PlantCard.ViewData(data, true, false, 1, 0, 1, false, false), 0.72f);
        Stack stack = new Stack();
        stack.add(card);

        stack.setSize(card.getPrefWidth(), card.getPrefHeight());
        stack.layout();

        conveyorButtonGroup.add(card);

        card.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                if (card.isChecked()) {
                    selectedConveyorIndex = specialConveyorBelt.getItems().indexOf(stack, true);
                    handleSpecialConveyorSelection(data);
                } else {
                    int currentIndex = specialConveyorBelt.getItems().indexOf(stack, true);
                    if (selectedConveyorIndex == currentIndex) {
                        selectedConveyorIndex = -1;
                        handleSpecialConveyorSelection(null);
                    }
                }
            }
        });
        specialConveyorBelt.addPlant(stack);
    }

    private void rebuildButtonGroup() {
        conveyorButtonGroup.clear();
        for (Actor actor : specialConveyorBelt.getItems()) {
            if (actor instanceof Stack stack && stack.getChildren().size > 0 && stack.getChild(0) instanceof PlantCard card) {
                conveyorButtonGroup.add(card);
            }
        }
    }
    private void handlePlantSelectionChanged(
        PlantData plant
    ) {
        if (placementPreview == null) {
            return;
        }

        if (plant == null) {
            placementPreview.clearPlant();
            if (toolMode == ToolMode.NONE) {
                hidePlacementHighlights();
            }
            return;
        }

        setToolMode(ToolMode.NONE);
        placementPreview.setPreviewMode(true);
        placementPreview.setPlant(plant);
    }

    private void toggleShovelMode() {
        if (toolMode == ToolMode.SHOVEL) {
            setToolMode(ToolMode.NONE);
            return;
        }

        clearCurrentPlantSelection();
        setToolMode(ToolMode.SHOVEL);
    }

    private void togglePlantFoodMode() {
        if (toolMode == ToolMode.PLANT_FOOD) {
            setToolMode(ToolMode.NONE);
            return;
        }

        Game currentGame = App.getInstance().getCurrentGame();
        if (currentGame == null
            || currentGame.getGameState() == null) {
            return;
        }

        if (currentGame.getGameState().getPlantFoodCount() <= 0) {
            game.notifyError("You do not have any Plant Food.");
            return;
        }

        clearCurrentPlantSelection();
        setToolMode(ToolMode.PLANT_FOOD);
    }

    private void setToolMode(ToolMode mode) {
        toolMode = mode;

        if (gameHud != null) {
            gameHud.setShovelSelected(
                mode == ToolMode.SHOVEL
            );
        }

        switch (mode) {
            case SHOVEL -> showToolCursor(SHOVEL_CURSOR);
            case PLANT_FOOD -> showToolCursor(PLANT_FOOD_CURSOR);
            case NONE -> hideToolCursor();
        }

        if (mode != ToolMode.NONE && placementPreview != null) {
            placementPreview.clearPlant();
        }

        if (mode == ToolMode.NONE
            && getCurrentSelectedPlant() == null) {
            hidePlacementHighlights();
        }
    }

    private void showToolCursor(String assetId) {
        TextureRegion region =
            game.getTextureBank().region(assetId);

        if (region == null) {
            throw new IllegalStateException(
                "TextureBank region was not found: " + assetId
            );
        }

        if (toolCursorPreview == null) {
            toolCursorPreview = new Image();
            toolCursorPreview.setTouchable(Touchable.disabled);
            uiStage.addActor(toolCursorPreview);
        }

        toolCursorPreview.setDrawable(
            new TextureRegionDrawable(region)
        );
        toolCursorPreview.setSize(
            region.getRegionWidth() * TOOL_CURSOR_SCALE,
            region.getRegionHeight() * TOOL_CURSOR_SCALE
        );
        toolCursorPreview.setColor(
            1f,
            1f,
            1f,
            TOOL_CURSOR_ALPHA
        );
        toolCursorPreview.setVisible(true);
        toolCursorPreview.toFront();
    }

    private void hideToolCursor() {
        if (toolCursorPreview != null) {
            toolCursorPreview.setVisible(false);
        }
    }

    private void updateToolCursorPreview() {
        if (toolCursorPreview == null) {
            return;
        }

        boolean shouldShow =
            toolMode != ToolMode.NONE
                && introState == IntroState.PLAYING
                && overlayMode == OverlayMode.NONE;

        toolCursorPreview.setVisible(shouldShow);
        if (!shouldShow) {
            return;
        }

        toolCursorPosition.set(
            Gdx.input.getX(),
            Gdx.input.getY()
        );
        uiStage.screenToStageCoordinates(toolCursorPosition);

        toolCursorPreview.setPosition(
            toolCursorPosition.x
                - toolCursorPreview.getWidth() / 2f,
            toolCursorPosition.y
                - toolCursorPreview.getHeight() / 2f
        );
        toolCursorPreview.toFront();
    }
    private void handleSunClicked(Sun sun) {
        if (sun == null || introState != IntroState.PLAYING || overlayMode != OverlayMode.NONE) {

            return;
        }

        Game currentGame = App.getInstance().getCurrentGame();

        if (currentGame == null || currentGame.getGameState() == null) {

            return;
        }

        boolean collected = currentGame.getGameState().getBoard().collectSun(sun, currentGame.getGameState());

        if (!collected) {
            game.notifyError("Sun has expired or was already collected.");
        }
    }

    private void showStartObjectives() {
        if (overlayMode != OverlayMode.NONE) {
            return;
        }

        overlayMode = OverlayMode.START_OBJECTIVES;

        gameTickAccumulator = 0f;
        resetRenderTickInterpolation();

        ChapterTheme levelTheme =
            currentLevel.chapterTheme();

        String chapterName =
            levelTheme == null
                ? theme.getName()
                : levelTheme.getName();

        StartGameMenuPopup popup = new StartGameMenuPopup(
            game,
            this::continueAfterObjectives,
            chapterName,
            currentLevel.levelNumber(),
            currentLevel.description(),
            currentLevel.objectives().toArray(String[]::new));

        showModal(popup);
        animateStartPopup(popup);
    }
    private void animateStartPopup(Actor popup) {
        modalOverlay.validate();
        float targetX = popup.getX();
        float targetY = popup.getY();
        popup.setPosition(targetX, -popup.getHeight());
        popup.addAction(Actions.moveTo(targetX, targetY, 0.55f, Interpolation.pow3Out));
    }
    private void continueAfterObjectives() {
        if (overlayMode != OverlayMode.START_OBJECTIVES) {
            return;
        }

        removeModal();

        overlayMode = OverlayMode.NONE;
        gameTickAccumulator = 0f;
        resetRenderTickInterpolation();
        stateTime = 0f;
    }
    private void showModal(Actor popup) {
        removeModal();

        Table overlay = new Table();
        overlay.setFillParent(true);
        overlay.setTouchable(Touchable.enabled);

        TextureRegionDrawable dimBackground =
            new TextureRegionDrawable(
                new TextureRegion(
                    modalDimTexture
                )
            );

        overlay.setBackground(
            dimBackground.tint(
                new Color(
                    0f,
                    0f,
                    0f,
                    0.62f
                )
            )
        );

        overlay.addListener(
            new InputListener() {
                @Override
                public boolean touchDown(
                    InputEvent event,
                    float x,
                    float y,
                    int pointer,
                    int button
                ) {
                    return true;
                }
            }
        );

        overlay.add(popup).center();

        modalOverlay = overlay;
        uiStage.addActor(modalOverlay);
        modalOverlay.toFront();
    }
    private void removeModal() {
        if (modalOverlay == null) {
            return;
        }
        modalOverlay.remove();
        modalOverlay = null;
    }

    private void handlePauseShortcut() {
        if (!Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            return;
        }

        if (overlayMode == OverlayMode.PAUSE) {
            resumeGame();
            return;
        }

        if (introState == IntroState.PLAYING
            && overlayMode == OverlayMode.NONE) {
            showPauseMenu();
        }
    }

    private void showPauseMenu() {
        if (introState != IntroState.PLAYING
            || overlayMode != OverlayMode.NONE) {
            return;
        }

        overlayMode = OverlayMode.PAUSE;
        gameTickAccumulator = 0f;
        resetRenderTickInterpolation();

        PauseMenuPopup popup =
            new PauseMenuPopup(
                game,
                this::saveAndExit,
                this::restartLevel,
                this::resumeGame
            );

        showModal(popup);
    }

    private void resumeGame() {
        if (overlayMode != OverlayMode.PAUSE) {
            return;
        }

        removeModal();
        overlayMode = OverlayMode.NONE;
        gameTickAccumulator = 0f;
        resetRenderTickInterpolation();
    }

    private void restartLevel() {
        removeModal();

        Gdx.app.postRunnable(
            () -> game.showScreen(
                new GameScreen(
                    game,
                    theme,
                    currentLevel.levelNumber()
                )
            )
        );
    }

    private void saveAndExit() {
        removeModal();
        if (gameHud != null) {
            gameHud.hideGameHud();
            gameHud.remove();
        }

        Gdx.app.postRunnable(
            () -> game.showScreen(
                new MainMenuScreen(game)
            )
        );
    }
    private void createPlacementHighlights() {
        Drawable highlightDrawable = game.getSkin().newDrawable(
            "white_pixel",
            new Color(1f, 1f, 1f, 0.70f)
        );
        rowHighlight = new Image(highlightDrawable);
        columnHighlight = new Image(highlightDrawable);
        rowHighlight.setTouchable(Touchable.disabled);
        columnHighlight.setTouchable(Touchable.disabled);
        rowHighlight.setVisible(false);
        columnHighlight.setVisible(false);
    }
    private void handleTileHover(Tile tile) {
        boolean hasPlantSelection =
                getCurrentSelectedPlant() != null;
        boolean hasToolSelection =
            toolMode != ToolMode.NONE;

        if (tile == null
            || (!hasPlantSelection && !hasToolSelection)) {
            hidePlacementHighlights();
            return;
        }
        BoardArea area = boardTransform.getArea();
        rowHighlight.setBounds(area.x(),
            boardTransform.tileY(tile.getLane()),
            area.width(),
            boardTransform.tileHeight()
        );

        columnHighlight.setBounds(
            boardTransform.tileX(tile.getColumn()),
            area.y(),
            boardTransform.tileWidth(),
            area.height()
        );

        rowHighlight.setVisible(true);
        columnHighlight.setVisible(true);
    }
    private void hidePlacementHighlights() {
        if (rowHighlight != null) {
            rowHighlight.setVisible(false);
        }

        if (columnHighlight != null) {
            columnHighlight.setVisible(false);
        }
    }

    private void processVisualEffects(GameState state) {
        if (state == null) {
            return;
        }

        for (VisualEffectEvent event : state.consumeVisualEffects()) {
            if (worldEffectManager != null) {
                worldEffectManager.play(event);
            }

            if (event.type() == VisualEffectEvent.Type.PLANT_EXPLOSION) {
                startScreenShake(
                    EXPLOSION_SHAKE_DURATION,
                    EXPLOSION_SHAKE_MAGNITUDE
                );
            }
        }
    }

    private void startScreenShake(
        float duration,
        float magnitude
    ) {
        screenShakeDuration = Math.max(
            screenShakeDuration,
            Math.max(0.01f, duration)
        );
        screenShakeRemaining = Math.max(
            screenShakeRemaining,
            Math.max(0.01f, duration)
        );
        screenShakeMagnitude = Math.max(
            screenShakeMagnitude,
            Math.max(0f, magnitude)
        );
    }

    private void applyScreenShake(float delta) {
        screenShakeApplied = false;

        if (introState != IntroState.PLAYING
            || overlayMode != OverlayMode.NONE
            || screenShakeRemaining <= 0f
            || screenShakeDuration <= 0f) {
            return;
        }

        screenShakeRemaining = Math.max(
            0f,
            screenShakeRemaining - Math.max(0f, delta)
        );

        float strength =
            screenShakeRemaining / screenShakeDuration;

        screenShakeBaseX = camera.position.x;
        screenShakeBaseY = camera.position.y;

        camera.position.x =
            screenShakeBaseX
                + MathUtils.random(
                -screenShakeMagnitude,
                screenShakeMagnitude
            ) * strength;

        camera.position.y =
            screenShakeBaseY
                + MathUtils.random(
                -screenShakeMagnitude,
                screenShakeMagnitude
            ) * strength;

        camera.update();
        screenShakeApplied = true;

        if (screenShakeRemaining <= 0f) {
            screenShakeMagnitude = 0f;
            screenShakeDuration = 0f;
        }
    }

    private void restoreScreenShake() {
        if (!screenShakeApplied) {
            return;
        }

        camera.position.set(
            screenShakeBaseX,
            screenShakeBaseY,
            camera.position.z
        );
        camera.update();
        screenShakeApplied = false;
    }

    @Override
    public void render(float delta) {
        handlePauseShortcut();
        updateCutscene(delta);
        updateGameplayTicks(delta);
        updateSpecialConveyor(delta);
        processGameplayNotices();
        updateRenderTickInterpolation(delta);
        updateWaveNotice();
        checkGameEnd();

        float gameplayDelta = scaledGameplayDelta(delta);

        Game renderGame = App.getInstance().getCurrentGame();
        if (introState == IntroState.PLAYING
            && overlayMode == OverlayMode.NONE
            && renderGame != null) {
            processVisualEffects(renderGame.getGameState());
        }

        if (introState == IntroState.PLAYING
            && overlayMode == OverlayMode.NONE) {
            Game currentGame = App.getInstance().getCurrentGame();
            if (currentGame != null && currentGame.getGameState() != null) {
                zombieAnimationSystem.update(
                    gameplayDelta,
                    getRenderTickAlpha(),
                    currentGame.getGameState().getTickCounter(),
                    currentGame.getGameState().getZombiesInTheGame()
                );

                mowerAnimationSystem.update(
                    gameplayDelta,
                    getRenderTickAlpha(),
                    currentGame.getGameState().getTickCounter(),
                    currentGame.getGameState()
                );

                sandstormAnimationSystem.update(
                    gameplayDelta,
                    currentGame
                        .getGameState()
                        .getZombiesInTheGame()
                );

                frostbiteSnowstormAnimationSystem.update(
                    gameplayDelta,
                    currentGame
                        .getGameState()
                        .getZombiesInTheGame()
                );
            }
        }

        updateToolCursorPreview();
        uiStage.act(delta);
        applyScreenShake(delta);

        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        viewport.apply();
        game.getBatch().setProjectionMatrix(camera.combined);
        game.getBatch().begin();
        game.getBatch().setColor(Color.WHITE);

        float currentX = 0f;

        game.getBatch().draw(bgLeft, currentX, 0f, bgLeft.getRegionWidth(), worldHeight);
        currentX += bgLeft.getRegionWidth();

        game.getBatch().draw(
            bgMid,
            currentX,
            getMiddleBackgroundYOffset(),
            bgMid.getRegionWidth(),
            worldHeight
        );
        currentX += bgMid.getRegionWidth();

        game.getBatch().draw(bgRight, currentX, 0f, bgRight.getRegionWidth(), worldHeight);

        game.getBatch().end();
        boolean animateZombiePreview =
            introState == IntroState.PAN_TO_ZOMBIES
                || introState == IntroState.WAIT_AT_ZOMBIES
                || introState == IntroState.PAN_TO_SELECTION
                || introState == IntroState.WAITING_FOR_SELECTION;

        Game currentGameForVisuals = App.getInstance().getCurrentGame();
        if (currentGameForVisuals != null
            && currentGameForVisuals.getGameState() != null) {
            Board board =
                currentGameForVisuals
                    .getGameState()
                    .getBoard();

            iceFloorAnimationSystem.sync(
                board
            );

            graveAnimationSystem.sync(
                board
            );

            Collection<Zombie> visualZombies =
                currentGameForVisuals
                    .getGameState()
                    .getZombiesInTheGame();


            if (introState != IntroState.PLAYING) {
                zombieAnimationSystem.update(
                    0f,
                    0f,
                    currentGameForVisuals
                        .getGameState()
                        .getTickCounter(),
                    visualZombies
                );
            }

            syncFrozenZombiePreviewVisibility(
                visualZombies
            );

            frozenZombieIceAnimationSystem.sync(
                visualZombies,
                introState == IntroState.PLAYING
                    && overlayMode == OverlayMode.NONE
                    ? gameplayDelta
                    : 0f
            );

            lootAnimationSystem.sync(
                visualZombies,
                board.getActiveLoots()
            );

            if (protectedPlantOverlayManager != null) {
                protectedPlantOverlayManager.sync(
                    currentGameForVisuals.getGameState()
                );
            }

            if (deadlineOverlayManager != null) {
                deadlineOverlayManager.sync(
                    currentGameForVisuals.getGameState(),
                    introState == IntroState.PLAYING
                        && overlayMode == OverlayMode.NONE
                        ? gameplayDelta
                        : 0f
                );
            }

            if (plantViewManager != null) {
                plantViewManager.sync(
                    board
                );
            }

            if (introState == IntroState.PLAYING
                && overlayMode == OverlayMode.NONE) {
                if (projectileViewManager != null) {
                    projectileViewManager.sync(
                        board.getProjectiles(),
                        getRenderTickAlpha()
                    );
                }
                if (sunViewManager != null) {
                    sunViewManager.sync(
                        board.getActiveSuns(),
                        getRenderTickAlpha()
                    );
                }

                if (worldEffectManager != null) {
                    worldEffectManager.toFront();
                }
                if (sunViewManager != null) {
                    sunViewManager.toFront();
                }
                if (placementPreview != null) {
                    placementPreview.toFront();
                }
            }
        }

        if (overlayMode == OverlayMode.NONE
            && (introState == IntroState.PLAYING
            || animateZombiePreview)) {
            worldStage.act(
                introState == IntroState.PLAYING
                    ? gameplayDelta
                    : delta
            );
        }

        entityDepthLayer.sortNow();
        worldStage.draw();

        drawDebugGrid();
        restoreScreenShake();
        game.getBatch().setColor(Color.WHITE);
        uiStage.draw();
    }

    private void syncFrozenZombiePreviewVisibility(
        Collection<Zombie> zombies
    ) {
        if (zombies == null) {
            return;
        }

        boolean showZombie =
            introState == IntroState.PLAYING;

        for (Zombie zombie : zombies) {
            if (zombie == null
                || !zombie.hasIceShell()) {
                continue;
            }

            views.graphical.animation.PamAnimationActor actor =
                zombieAnimationSystem.getActor(
                    zombie
                );

            if (actor != null) {
                actor.setVisible(
                    showZombie
                );
            }
        }
    }

    private void checkGameEnd() {
        if (gameEndShown
            || introState != IntroState.PLAYING) {
            return;
        }

        Game currentGame =
            App.getInstance()
                .getCurrentGame();

        if (currentGame == null
            || currentGame.getGameState() == null
            || !currentGame.getGameState().isFinished()) {
            return;
        }

        gameEndShown = true;
        overlayMode = OverlayMode.GAME_END;
        gameTickAccumulator = 0f;
        resetRenderTickInterpolation();

        hidePlacementHighlights();

        if (placementPreview != null) {
            placementPreview.clearPlant();
            placementPreview.setVisible(false);
        }

        clearCurrentPlantSelection();

        if (gameHud != null) {
            gameHud.hideGameHud();
        }

        Actor popup =
            currentGame.getGameState().isWon()
                ? new GameWinPopup(
                game,
                theme,
                currentLevel.levelNumber()
            )
                : new GameOverPopup(
                game,
                theme,
                currentLevel.levelNumber()
            );

        showGameEndModal(popup);
    }

    private void showGameEndModal(Actor popup) {
        removeModal();

        Table overlay = new Table();
        overlay.setFillParent(true);
        overlay.setTouchable(Touchable.enabled);

        overlay.addListener(
            new InputListener() {
                @Override
                public boolean touchDown(
                    InputEvent event,
                    float x,
                    float y,
                    int pointer,
                    int button
                ) {
                    return true;
                }
            }
        );

        overlay.add(popup).grow();

        modalOverlay = overlay;
        uiStage.addActor(modalOverlay);
        modalOverlay.toFront();
    }

    private float scaledGameplayDelta(float delta) {
        float safeDelta = Math.max(0f, Math.min(delta, 0.25f));
        int speed = Math.max(1, Math.min(3, GameSettings.gameSpeed));
        return safeDelta * speed;
    }

    private void updateGameplayTicks(float delta) {
        if (introState != IntroState.PLAYING || overlayMode != OverlayMode.NONE) {
            return;
        }

        Game currentGame = App.getInstance().getCurrentGame();

        if (currentGame == null
            || currentGame.getGameState() == null
            || currentGame.getGameState().isFinished()) {
            return;
        }

        int ticksPerSecond = Math.max(1, currentGame.getGameState().getTicksPerSecond());

        float tickDuration = 1f / ticksPerSecond;

        gameTickAccumulator += scaledGameplayDelta(delta);

        while (gameTickAccumulator >= tickDuration) {
            if (currentGame.getGameState().isFinished()) {
                gameTickAccumulator = 0f;
                resetRenderTickInterpolation();
                break;
            }
            currentGame.forward(1);
            gameTickAccumulator -= tickDuration;
        }
    }

    private void updateRenderTickInterpolation(
        float delta
    ) {
        if (introState != IntroState.PLAYING
            || overlayMode != OverlayMode.NONE) {
            return;
        }

        Game currentGame =
            App.getInstance()
                .getCurrentGame();

        if (currentGame == null
            || currentGame.getGameState() == null
            || currentGame.getGameState().isFinished()) {
            resetRenderTickInterpolation();
            return;
        }

        int modelTick =
            currentGame
                .getGameState()
                .getTickCounter();

        float safeDelta = scaledGameplayDelta(delta);

        if (renderInterpolationModelTick
            == Integer.MIN_VALUE) {
            renderInterpolationModelTick =
                modelTick;

            renderInterpolationElapsed =
                Math.max(
                    0f,
                    gameTickAccumulator
                );

            renderInterpolationDuration =
                secondsUntilNextModelTick(
                    currentGame
                );

            return;
        }

        if (modelTick
            != renderInterpolationModelTick) {
            renderInterpolationModelTick =
                modelTick;

            renderInterpolationElapsed =
                Math.max(
                    0f,
                    gameTickAccumulator
                );

            renderInterpolationDuration =
                secondsUntilNextModelTick(
                    currentGame
                );

            return;
        }

        renderInterpolationElapsed =
            Math.min(
                renderInterpolationDuration,
                renderInterpolationElapsed
                    + safeDelta
            );
    }

    private float secondsUntilNextModelTick(
        Game currentGame
    ) {
        int ticksPerSecond =
            Math.max(
                1,
                currentGame
                    .getGameState()
                    .getTicksPerSecond()
            );

        int difficultyLevel =
            App.getInstance().getLoggedInUser() == null
                ? 3
                : App.getInstance()
                  .getLoggedInUser()
                  .getDifficultyLevel();

        difficultyLevel =
            Math.max(
                1,
                Math.min(
                    3,
                    difficultyLevel
                )
            );

        long pendingScaledTicks =
            Math.floorMod(
                currentGame.getPendingScaledTicks(),
                3L
            );

        long scaledTicksNeeded =
            3L
                - pendingScaledTicks;

        int requestedTicksUntilNextModelTick =
            (int) (
                (
                    scaledTicksNeeded
                        + difficultyLevel
                        - 1L
                )
                    / difficultyLevel
            );

        float requestedTickDuration =
            1f / ticksPerSecond;

        return Math.max(
            requestedTickDuration,
            requestedTicksUntilNextModelTick
                * requestedTickDuration
        );
    }

    private float getRenderTickAlpha() {
        if (renderInterpolationDuration <= 0f) {
            return 0f;
        }

        return Math.max(
            0f,
            Math.min(
                1f,
                renderInterpolationElapsed
                    / renderInterpolationDuration
            )
        );
    }

    private void resetRenderTickInterpolation() {
        renderInterpolationModelTick =
            Integer.MIN_VALUE;

        renderInterpolationElapsed = 0f;
        renderInterpolationDuration = 0.1f;
    }

    private void handleTileClick(
        Tile tile
    ) {
        if (introState != IntroState.PLAYING
            || overlayMode != OverlayMode.NONE
            || tile == null) {
            return;
        }

        int x = tile.getColumn() + 1;
        int y = tile.getLane() + 1;

        Game currentGame =
            App.getInstance()
                .getCurrentGame();

        if (currentGame != null
            && currentGame.getGameState() != null) {
            DroppedLoot loot =
                findLootAt(
                    currentGame
                        .getGameState()
                        .getBoard(),
                    tile.getLane(),
                    tile.getColumn()
                );

            if (loot != null) {

                System.out.println(
                    "CLICKED LOOT = "
                        + loot.getType()
                        + " x="
                        + loot.getX()
                        + " column="
                        + loot.getColumn()
                        + " lane="
                        + loot.getLane()
                );

                Result result =
                    gamingController.collectLoot(
                        loot
                    );

                if (!result.success()) {
                    game.notifyError(result.message());
                }

                return;
            }
        }

        if (toolMode == ToolMode.SHOVEL) {
            Result result = gamingController.pluckPlant(x, y);
            if (!result.success()) {
                game.notifyError(result.message());
                return;
            }

            setToolMode(ToolMode.NONE);
            return;
        }

        if (toolMode == ToolMode.PLANT_FOOD) {
            Result result = gamingController.feedPlant(x, y);
            if (!result.success()) {
                game.notifyError(result.message());
                return;
            }

            setToolMode(ToolMode.NONE);
            return;
        }

        PlantData selectedPlant = getCurrentSelectedPlant();
        if (selectedPlant == null) {
            return;
        }

        Result result =
            gamingController.plantPlant(
                selectedPlant.name(),
                x,
                y
            );

        if (!result.success()) {
            game.notifyError(result.message());
            return;
        }

        if (isSpecialConveyorLevel() && selectedConveyorIndex >= 0) {
            specialConveyorBelt.removePlant(selectedConveyorIndex);
            conveyorButtonGroup.uncheckAll();
            selectedConveyorIndex = -1;
            specialConveyorSelectedPlant = null;
            lastConveyorSnapshot = List.copyOf(App.getInstance().getCurrentGame().getConveyorBeltPlants());
            rebuildButtonGroup();
        }

        clearCurrentPlantSelection();
    }

    private DroppedLoot findLootAt(
        Board board,
        int lane,
        int column
    ) {
        if (board == null) {
            return null;
        }

        for (DroppedLoot loot : board.getActiveLoots()) {

            if (loot == null) {
                continue;
            }

            if (loot.getType() == LootType.POT) {

                if (Math.abs(loot.getColumn() - column) <= 3
                    && Math.abs(loot.getLane() - lane) <= 2) {

                    return loot;
                }

            } else {

                if (loot.getLane() == lane
                    && loot.getColumn() == column) {

                    return loot;
                }
            }
        }

        return null;
    }

    private void drawDebugGrid() {
        if (!GameSettings.showGrid) {
            return;
        }

        BoardArea area = boardTransform.getArea();

        float tileWidth = boardTransform.tileWidth();

        float tileHeight = boardTransform.tileHeight();

        shapeRenderer.setProjectionMatrix(camera.combined);

        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);

        shapeRenderer.setColor(Color.RED);

        for (int column = 0; column <= BoardTransform.COLUMNS; column++) {
            float x = area.x() + column * tileWidth;
            shapeRenderer.line(x, area.y(), x, area.y() + area.height());
        }

        for (int row = 0; row <= BoardTransform.ROWS; row++) {
            float y = area.y() + row * tileHeight;
            shapeRenderer.line(area.x(), y, area.x() + area.width(), y);
        }
        shapeRenderer.end();
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height);
        uiStage.getViewport().update(width, height, true);
    }

    @Override
    public void dispose() {
        if (gameHud != null) {
            gameHud.dispose();
        }
        zombieLevelPreview.clear();
        sandstormAnimationSystem.clear();
        frostbiteSnowstormAnimationSystem.clear();
        iceFloorAnimationSystem.clearVisuals();
        frozenZombieIceAnimationSystem.clear();
        lootAnimationSystem.clear();
        mowerAnimationSystem.clear();
        zombieAnimationSystem.clear();
        graveAnimationSystem.clearVisuals();
        if (protectedPlantOverlayManager != null) {
            protectedPlantOverlayManager.clearVisuals();
        }
        if (deadlineOverlayManager != null) {
            deadlineOverlayManager.clearVisuals();
        }
//        if (specialConveyorBelt != null) {
//            specialConveyorBelt.dispose();
//            specialConveyorBelt = null;
//        }
        uiStage.dispose();
        worldStage.dispose();
        shapeRenderer.dispose();
        modalDimTexture.dispose();

    }
    private boolean isSpecialConveyorLevel() {
        Game currentGame = App.getInstance().getCurrentGame();
        return currentGame != null && currentGame.isConveyorBeltLevel();
    }


    private PlantData getCurrentSelectedPlant() {
        if (isSpecialConveyorLevel()) {
            return specialConveyorSelectedPlant;
        }
        return plantSlotsBar.getSelectedPlant();
    }


    private void handleSpecialConveyorSelection(PlantData plant) {
        specialConveyorSelectedPlant = plant;
        handlePlantSelectionChanged(plant);
    }


    private void clearCurrentPlantSelection() {
        if (isSpecialConveyorLevel()) {
            specialConveyorSelectedPlant = null;
            conveyorButtonGroup.uncheckAll();
            selectedConveyorIndex = -1;
            handlePlantSelectionChanged(null);
            return;
        }
        plantSlotsBar.clearPlantSelection();
    }
    private void updateSpecialConveyor(float delta) {
        if (specialConveyorBelt == null) return;
        specialConveyorBelt.update(delta);
        refreshConveyorBar(false);
    }
}

