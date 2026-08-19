package views.graphical.screens.minigamesScreen.beghouled;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.scenes.scene2d.*;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Scaling;
import controllers.miniGamesController.BeghouledController;
import graphics.PvzGame;
import models.App;
import models.Board.Board;
import models.Board.Tile;
import models.Result;
import models.games.ChapterTheme;
import models.games.GameState;
import models.minigames.MinigameType;
import models.minigames.beghouled.Beghouled;
import views.graphical.gameplay.board.BoardArea;
import views.graphical.gameplay.board.BoardTransform;
import views.graphical.gameplay.board.BoardView;
import views.graphical.gameplay.manager.PlantViewManager;
import views.graphical.gameplay.manager.ProjectileViewManager;
import views.graphical.gameplay.zombie.ZombieAnimationSystem;
import views.graphical.screens.minigamesScreen.BaseMinigameScreen;
import views.graphical.screens.minigamesScreen.minigames;
import views.graphical.ui.BorderedPanel;
import views.graphical.ui.GameOverPopup;
import views.graphical.ui.GameWinPopup;
import views.graphical.ui.StartGameMenuPopup;

import java.util.ArrayList;
import java.util.List;

public class BeghouledScreen extends BaseMinigameScreen {
    private static final String BG_LEFT = "IMAGE_BACKGROUNDS_FRONTLAWN_PADDYS_TEXTURE_LEFT";
    private static final String BG_MID = "IMAGE_BACKGROUNDS_FRONTLAWN_PADDYS_TEXTURE";
    private static final String BG_RIGHT = "IMAGE_BACKGROUNDS_FRONTLAWN_PADDYS_TEXTURE_RIGHT";
    private static final String MATCHES_LEFT="IMAGE_UI_GENERIC_BUTTONS_TICKETS_NORMAL";
    private static final String MATCHES_LEFT_SELECTED="IMAGE_UI_GENERIC_BUTTONS_TICKETS_SELECTED";
    private static final float DRAG_THRESHOLD = 22f;
    private final int stageNumber;
    private final BeghouledController controller;
    private final Beghouled beghouled;
    private final BoardArea boardArea;
    private final BoardTransform boardTransform;
    private BoardView boardView;
    private PlantViewManager plantViewManager;
    private ProjectileViewManager projectileViewManager;
    private ZombieAnimationSystem zombieAnimationSystem;
    private final Texture craterTexture;

    private Label matchesLabel;
    private Group matchesDisplay;
    private TextButton upgradeButton;
    private Image firstHighlight;
    private Image secondHighlight;
    private Tile dragStartTile;
    private Tile dragTargetTile;

    private float dragStartX;
    private float dragStartY;
    private float renderDelta;
    private boolean resultShown;

    public BeghouledScreen(PvzGame game, int stageNumber) {
        super(game, BG_LEFT, BG_MID, BG_RIGHT);
        this.stageNumber = stageNumber;
        controller = new BeghouledController();
        Result result = controller.startStage(stageNumber);
        if (!result.success()) {
            throw new IllegalStateException(result.message());
        }
        if (!(App.getInstance().getCurrentGame() instanceof Beghouled current)) {
            throw new IllegalStateException(
                    "Beghouled game was not created."
            );
        }
        beghouled = current;
        boardArea = new BoardArea(533f, 62f, 737f, 380f);
        boardTransform = new BoardTransform(boardArea);
        craterTexture = new Texture(Gdx.files.internal("assets/UIs/tile_khaki.png"));

        craterTexture.setFilter(
                Texture.TextureFilter.Linear,
                Texture.TextureFilter.Linear
        );

        buildBoard();

        buildHighlights();
        buildBeghouledHudControls();
    }

    private void buildBoard() {
        Board board = beghouled.getGameState().getBoard();
        boardView = new BoardView(board, boardTransform);
        worldStage.addActor(boardView);
        plantViewManager = new PlantViewManager(game, boardTransform);
        worldStage.addActor(plantViewManager);
        projectileViewManager = new ProjectileViewManager(game, boardTransform);
        worldStage.addActor(projectileViewManager);
        zombieAnimationSystem =
                new ZombieAnimationSystem(
                        game.getPamPlayer(),
                        worldStage,
                        boardTransform,
                        ChapterTheme.MINIGAME);

        installDragListeners();
        plantViewManager.sync(board);
    }

    private void installDragListeners() {
        Board board = beghouled.getGameState().getBoard();
        for (int lane = 0; lane < board.getLaneCount(); lane++) {
            for (int column = 0; column < board.getColumnCount(); column++) {
                final Tile tile = board.getTile(lane, column);
                Actor tileView = boardView.getTileView(lane, column);
                tileView.addListener(
                        new InputListener() {

                            @Override
                            public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                                if (!isPlaying() || isPaused() || tile.isCrater() || !tile.hasTopPlant()) {
                                    return false;
                                }

                                dragStartTile = tile;
                                dragTargetTile = null;
                                dragStartX = x;
                                dragStartY = y;
                                showFirstHighlight(tile);
                                return true;
                            }

                            @Override
                            public void touchDragged(InputEvent event, float x, float y, int pointer) {
                                if (dragStartTile == null) {
                                    return;
                                }

                                float dx = x - dragStartX;
                                float dy = y - dragStartY;
                                Tile target = determineTargetTile(dragStartTile, dx, dy);

                                if (target !=
                                        dragTargetTile) {

                                    dragTargetTile =
                                            target;

                                    showSecondHighlight(
                                            target
                                    );
                                }
                            }

                            @Override
                            public void touchUp(
                                    InputEvent event,
                                    float x,
                                    float y,
                                    int pointer,
                                    int button
                            ) {

                                Tile first =
                                        dragStartTile;

                                Tile second =
                                        dragTargetTile;

                                clearDragState();

                                if (first == null
                                        || second == null) {
                                    return;
                                }

                                performSwap(
                                        first,
                                        second
                                );
                            }
                        }
                );
            }
        }
    }

    private Tile determineTargetTile(
            Tile start,
            float dx,
            float dy
    ) {

        if (Math.abs(dx) < DRAG_THRESHOLD
                && Math.abs(dy) < DRAG_THRESHOLD) {

            return null;
        }

        int lane =
                start.getLane();

        int column =
                start.getColumn();

        if (Math.abs(dx) > Math.abs(dy)) {

            column +=
                    dx > 0
                            ? 1
                            : -1;

        } else {

            lane +=
                    dy > 0
                            ? -1
                            : 1;
        }

        Board board =
                beghouled
                        .getGameState()
                        .getBoard();

        if (lane < 0
                || lane >= board.getLaneCount()
                || column < 0
                || column >= board.getColumnCount()) {

            return null;
        }

        Tile target =
                board.getTile(
                        lane,
                        column
                );

        if (target.isCrater()
                || !target.hasTopPlant()) {

            return null;
        }

        return target;
    }

    private void performSwap(
            Tile first,
            Tile second
    ) {

        if (!isPlaying()
                || isPaused()) {
            return;
        }

        Result result =
                controller.swapPlantsGraphical(
                        first.getColumn() + 1,
                        first.getLane() + 1,
                        second.getColumn() + 1,
                        second.getLane() + 1
                );

        if (!result.success()) {

            game.notifyError(
                    result.message()
            );

            flashInvalidSwap(
                    first,
                    second
            );

            return;
        }

        flashSuccessfulSwap(
                first,
                second
        );

        syncBoard();
        refreshMatchesCounter();
    }

    private void buildBeghouledHudControls() {
        matchesDisplay = createMatchesDisplay();

        upgradeButton =
                new TextButton(
                        "UPGRADES",
                game.getSkin()
                );

        upgradeButton.addListener(
                new InputListener() {
                    @Override
                    public boolean touchDown(
                            InputEvent event,
                            float x,
                            float y,
                            int pointer,
                            int button
                    ) {
                        if (!isPlaying() || isPaused()) {
                            return false;
                        }

                        showUpgradeMenu();
                        return true;
                    }
                }
        );

        Table hudTopRight = gameHud.getTopRight();

        Actor existingControls =
                hudTopRight.getChildren().size > 0
                        ? hudTopRight.getChildren().first()
                        : null;

        if (existingControls != null) {
            existingControls.remove();
        }

        hudTopRight.clearChildren();

        hudTopRight.add(upgradeButton)
                .height(48f)
                .padRight(8f);

        hudTopRight.add(matchesDisplay)
                .size(
                        matchesDisplay.getWidth(),
                        matchesDisplay.getHeight()
                                )
                .padRight(8f);

        if (existingControls != null) {
            hudTopRight.add(existingControls);
        }

        refreshMatchesCounter();
    }

    private Group createMatchesDisplay() {
        TextureRegionDrawable normal =
                new TextureRegionDrawable(
                        game.getTextureBank().region(
                                MATCHES_LEFT
                        )
                );

        TextureRegionDrawable selected =
                new TextureRegionDrawable(
                        game.getTextureBank().region(
                                MATCHES_LEFT_SELECTED
                        )
        );

        ImageButton.ImageButtonStyle style =
                new ImageButton.ImageButtonStyle();

        style.imageUp = normal;
        style.imageOver = selected;
        style.imageDown = selected;

        ImageButton background =
                new ImageButton(style);

        background.getImageCell()
                .expand()
                .fill();

        background.getImage()
                .setScaling(
                        Scaling.stretch
                );

        float width =
                background.getPrefWidth();

        float height =
                background.getPrefHeight();

        Group group =
                new Group();

        group.setSize(
                width,
                height
        );

        background.setBounds(
                0f,
                0f,
                width,
                height
                );

        matchesLabel =
                new Label(
                        "0",
                        game.getSkin()
                );

        matchesLabel.setAlignment(
                Align.center
        );

        matchesLabel.setFontScale(
                1.1f
        );

        matchesLabel.setTouchable(
                Touchable.disabled
        );

        matchesLabel.setBounds(
                width * 0.38f,
                0f,
                width * 0.58f,
                height
        );

        group.addActor(background);
        group.addActor(matchesLabel);

        return group;
    }

    private void refreshMatchesCounter() {
        if (matchesLabel == null) {
            return;
        }

        int remaining =
                Math.max(
                        0,
                        beghouled
                                .getTargetMatches()
                                - beghouled
                                .getCompletedMatches()

        );

        matchesLabel.setText(
                String.valueOf(remaining)
        );
    }

    private void showUpgradeMenu() {
        overlayMode = OverlayMode.PAUSE;
        gameTickAccumulator = 0f;

        BorderedPanel popup =
                new BorderedPanel(
                        game,
                        Color.valueOf("7A4A24")
        );

        Table content = popup.getContent();
        content.pad(24f, 28f, 26f, 28f);

        Label title =
                new Label(
                        "PLANT UPGRADES",
                        game.getSkin()
                );

        title.setColor(
                Color.valueOf("FFD88A")
        );

        title.setFontScale(1.25f);

        content.add(title)
                .colspan(2)
                .padBottom(8f)
                .row();

        Image separator =
                new Image(
                        game.getSkin().newDrawable(
                                "white_pixel",
                                Color.valueOf("C88A52")
                        )
                );

        content.add(separator)
                .colspan(2)
                .growX()
                .height(2f)
                .padBottom(14f)
                .row();

        int currentSun =
                (int) beghouled
                        .getGameState()
                        .getSun();

        Label sunInfo =
                new Label(
                        "Available Sun: " + currentSun,
                        game.getSkin()
                );

        sunInfo.setColor(Color.WHITE);

        content.add(sunInfo)
                .colspan(2)
                .left()
                .padBottom(14f)
                .row();

        var plantCounts =
                beghouled.getPlantCounts();

        for (Beghouled.UpgradeRule rule :
                beghouled.getAvailableUpgrades()) {

            int count =
                    plantCounts.getOrDefault(
                            rule.fromPlant(),
                            0
                    );

            TextButton button =
                    new TextButton(
                            rule.fromPlant()
                                    + " -> "
                                    + rule.toPlant()
                                    + "\n"
                                    + rule.cost()
                                    + " SUN",
                            game.getSkin()
                    );

            boolean available =
                    count > 0
                            && currentSun >= rule.cost();

            button.setDisabled(
                    !available
            );

            button.addListener(
                    new InputListener() {

                        @Override
                        public boolean touchDown(
                                InputEvent event,
                                float x,
                                float y,
                                int pointer,
                                int buttonCode
                        ) {
                            if (button.isDisabled()) {
                                return true;
                            }

                            performUpgrade(
                                    rule
                            );

                            return true;
                        }
                    }
            );

            Label countLabel =
                    new Label(
                            count + " on board",
                            game.getSkin()
                    );

            countLabel.setColor(
                    available
                            ? Color.WHITE
                            : Color.LIGHT_GRAY
            );

            content.add(button)
                    .width(245f)
                    .height(62f)
                    .padBottom(8f)
                    .padRight(12f);

            content.add(countLabel)
                    .left()
                    .padBottom(8f)
                    .row();
        }

        TextButton close =
                new TextButton(
                        "CLOSE",
                        game.getSkin()
                );

        close.addListener(
                new InputListener() {
                    @Override
                    public boolean touchDown(
                            InputEvent event,
                            float x,
                            float y,
                            int pointer,
                            int button
                    ) {
                        closeUpgradeMenu();
                        return true;
                    }
                }
        );

        content.add(close)
                .colspan(2)
                .width(170f)
                .height(48f)
                .padTop(12f);

        popup.pack();
        showModal(popup);
    }

    private void closeUpgradeMenu() {
        removeModal();
        overlayMode = OverlayMode.NONE;
        gameTickAccumulator = 0f;
    }

    private void performUpgrade(
            Beghouled.UpgradeRule rule
    ) {
        if (!isPlaying()) {
            return;
        }

        Result result =
                controller.upgradePlantsGraphical(
                        rule.fromPlant(),
                        rule.toPlant()
                );

        if (!result.success()) {

            game.notifyError(
                    result.message()
            );

            return;
        }

        game.notifyInfo(
                result.message()
        );

        closeUpgradeMenu();
        syncBoard();
        refreshMatchesCounter();
        }



    private void buildHighlights() {

        Drawable drawable =
                game.getSkin()
                        .newDrawable(
                                "white_pixel",
                                new Color(
                                        1f,
                                        1f,
                                        1f,
                                        0.35f
                                )
                        );

        firstHighlight =
                new Image(
                        drawable
                );

        secondHighlight =
                new Image(
                        drawable
                );

        firstHighlight.setTouchable(
                Touchable.disabled
        );

        secondHighlight.setTouchable(
                Touchable.disabled
        );

        firstHighlight.setVisible(false);
        secondHighlight.setVisible(false);

        worldStage.addActor(
                firstHighlight
        );

        worldStage.addActor(
                secondHighlight
        );
    }

    private void showFirstHighlight(
            Tile tile
    ) {
        positionHighlight(
                firstHighlight,
                tile
        );

        firstHighlight.setVisible(
                true
        );
    }

    private void showSecondHighlight(
            Tile tile
    ) {

        if (tile == null) {
            secondHighlight.setVisible(
                    false
            );
            return;
        }

        positionHighlight(
                secondHighlight,
                tile
        );

        secondHighlight.setVisible(
                true
        );
    }

    private void positionHighlight(
            Image highlight,
            Tile tile
    ) {

        highlight.setBounds(
                boardTransform.tileX(
                        tile.getColumn()
                ),
                boardTransform.tileY(
                        tile.getLane()
                ),
                boardTransform.tileWidth(),
                boardTransform.tileHeight()
        );
    }

    private void clearDragState() {

        dragStartTile = null;
        dragTargetTile = null;

        firstHighlight.setVisible(
                false
        );

        secondHighlight.setVisible(
                false
        );
    }

    private void flashSuccessfulSwap(
            Tile first,
            Tile second
    ) {

        positionHighlight(
                firstHighlight,
                first
        );

        positionHighlight(
                secondHighlight,
                second
        );

        firstHighlight.setVisible(true);
        secondHighlight.setVisible(true);

        firstHighlight.getColor().a = 0.65f;
        secondHighlight.getColor().a = 0.65f;

        firstHighlight.addAction(
                Actions.sequence(
                        Actions.fadeOut(0.22f),
                        Actions.visible(false)
                )
        );

        secondHighlight.addAction(
                Actions.sequence(
                        Actions.fadeOut(0.22f),
                        Actions.visible(false)
                )
        );
    }

    private void flashInvalidSwap(
            Tile first,
            Tile second
    ) {

        flashSuccessfulSwap(
                first,
                second
        );
    }

    private void syncBoard() {

        if (plantViewManager != null) {
            plantViewManager.sync(
                    beghouled
                            .getGameState()
                            .getBoard()
            );
        }
    }

    @Override
    protected void onGameTick() {

        syncBoard();
        refreshMatchesCounter();
    }

    @Override
    public void render(
            float delta
    ) {

        renderDelta =
                Math.min(
                        delta,
                        0.25f
                );

        super.render(
                delta
        );

        checkForGameEnd();
    }

    private float getRenderTickAlpha() {

        GameState state =
                beghouled
                        .getGameState();

        int ticksPerSecond =
                Math.max(
                        1,
                        state.getTicksPerSecond()
                );

        float duration =
                1f / ticksPerSecond;

        return Math.max(
                0f,
                Math.min(
                        1f,
                        gameTickAccumulator
                                / duration
                )
        );
    }

    @Override
    protected void renderWorldUnderlay() {

        if (!isPlaying()
                || isPaused()) {
            return;
        }

        GameState state =
                beghouled
                        .getGameState();

        float partialTick =
                getRenderTickAlpha();

        projectileViewManager.sync(
                state.getBoard()
                        .getProjectiles(),
                partialTick
        );

        zombieAnimationSystem.update(
                renderDelta,
                partialTick,
                state.getTickCounter(),
                state.getZombiesInTheGame()
        );

        drawCraters();
    }

    private void drawCraters() {

        Board board =
                beghouled
                        .getGameState()
                        .getBoard();

        game.getBatch()
                .setProjectionMatrix(
                        camera.combined
                );

        game.getBatch().begin();

        for (int lane = 0;
             lane < board.getLaneCount();
             lane++) {

            for (int column = 0;
                 column < board.getColumnCount();
                 column++) {

                Tile tile =
                        board.getTile(
                                lane,
                                column
                        );

                if (!tile.isCrater()) {
                    continue;
                }

                float tileWidth =
                        boardTransform.tileWidth();

                float tileHeight =
                        boardTransform.tileHeight();

                float size =
                        Math.min(
                                tileWidth,
                                tileHeight
                        );

                float x =
                        boardTransform.tileX(column)
                                + (tileWidth - size) / 2f;

                float y =
                        boardTransform.tileY(lane)
                                + (tileHeight - size) / 2f;

                game.getBatch().draw(
                        craterTexture,
                        x,
                        y,
                        size,
                        size
                );
            }
        }

        game.getBatch().end();
    }

    private void checkForGameEnd() {

        if (resultShown
                || beghouled.getGameState() == null
                || !beghouled
                .getGameState()
                .isFinished()) {

            return;
        }

        resultShown = true;

        controller.recordGraphicalResult();

        showResultPopup(
                beghouled
                        .getGameState()
                        .isWon()
        );
    }

    private void showResultPopup(
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
                                                    new BeghouledScreen(
                                                            game,
                                                            stageNumber + 1
                                )
                        )
                            )
                            : this::exitMinigame;

            String message =
                        "Matches: "
                                + beghouled.getCompletedMatches()
                                + " / "
                                + beghouled.getTargetMatches()
                                + "\nSun: "
                            + (int) beghouled
                                .getGameState()
                            .getSun();

            GameWinPopup popup =
                    new GameWinPopup(
                            game,
                            "BEGHOULDED COMPLETE!",
                            message,
                            "EXIT",
                            this::exitMinigame,
                            hasNextStage
                                ? "NEXT STAGE"
                                    : "MINIGAMES",
                            nextAction
                );

            uiStage.addActor(popup);
            popup.toFront();
            return;
                    }

        GameOverPopup popup =
                new GameOverPopup(
                                                            game,
                        "THE ZOMBIES\nATE YOUR\nBRAINS!",
                        "EXIT",
                        this::exitMinigame,
                        "RETRY",
                        this::restartMinigame
        );

        uiStage.addActor(popup);
        popup.toFront();
    }

    private String[] buildStartObjectives() {
        List<String> objectives = new ArrayList<>();
        objectives.add(
                "Drag a plant toward an adjacent plant."
        );

        objectives.add(
                "Only swaps that create a match are allowed."
        );

        objectives.add(
                "A match of 3 gives 50 sun; larger matches give more."
        );

        objectives.add(
                "Cascade matches give one extra 50-sun reward."
        );

        objectives.add(
                "Available upgrades in this stage:"
        );

        for (Beghouled.UpgradeRule rule :
                beghouled.getAvailableUpgrades()) {

            objectives.add(
                    rule.fromPlant()
                            + " -> "
                            + rule.toPlant()
                            + " : "
                            + rule.cost()
                            + " sun"
            );
        }

        objectives.add(
                "Create "
                        + beghouled.getTargetMatches()
                        + " matches to win."
        );

        return objectives.toArray(
                String[]::new
        );
    }

    @Override
    protected Actor createStartPopup(
            Runnable onContinue
    ) {

        return new StartGameMenuPopup(
                game,
                onContinue,
                "Beghouled",
                stageNumber,
                "Create matches while defending the lawn.",
                buildStartObjectives()
        );
    }

    @Override
    protected void onGameplayStarted() {

        gameTickAccumulator = 0f;

        syncBoard();
        refreshMatchesCounter();
    }

    @Override
    protected void restartMinigame() {

        removeModal();

        Gdx.app.postRunnable(
                () ->
                        game.showScreen(
                                new BeghouledScreen(
                                        game,
                                        stageNumber
                                )
                        )
        );
    }

    @Override
    protected void exitMinigame() {
        removeModal();
        controller.exitMenu();
        Gdx.app.postRunnable(
                () -> game.showScreen(new minigames(game, MinigameType.BEGHOULDED)));
    }

    @Override
    public void dispose() {
        if (zombieAnimationSystem != null) {
            zombieAnimationSystem.clear();
        }
        craterTexture.dispose();
        super.dispose();
    }
}
