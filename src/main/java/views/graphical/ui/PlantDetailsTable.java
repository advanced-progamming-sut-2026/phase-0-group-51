package views.graphical.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Scaling;
import Data.loader.PlantRegistry;
import graphics.PvzGame;
import models.Result;
import network.client.ClientShopState;
import network.protocol.shop.ShopResponse;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

public final class PlantDetailsTable extends Table {

    private static final String PLANT_BACKGROUND = "IMAGE_UI_CARDS_BACKGROUNDS_CARD_PLANT_BG_MODERN";
    private static final String BACK = "IMAGE_UI_ALMANAC_BUTTONS_HUD_BACK_NORMAL";
    private static final String BACK_PRESSED = "IMAGE_UI_ALMANAC_BUTTONS_HUD_BACK_SELECTED";
    private static final String SUN_COST_ICON = "IMAGE_UI_ALMANAC_ALMANAC_STAT_ICON_SUNCOST";
    private static final String RECHARGE_ICON = "IMAGE_UI_ALMANAC_PLANTS_RECHARGE_ICON";
    private static final String TOUGHNESS_ICON = "IMAGE_UI_ALMANAC_PLANTS_TOUGHNESS_ICON";
    private static final String DAMAGE_ICON = "IMAGE_UI_ALMANAC_PLANTS_DAMAGE_ICON";
    private static final String RANGE_ICON = "IMAGE_UI_ALMANAC_PLANTS_RANGE_ICON";
    private static final String SPECIAL_ICON = "IMAGE_UI_ALMANAC_ALMANAC_STAT_ICON_SPECIAL";
    private static final String FAMILY_ICON = "IMAGE_UI_PACKETS_MINTFAM_PEASHOOTER";
    private static final String PLANT_FOOD_ICON = "IMAGE_UI_ALMANAC_PLANT_FOOD_STAT_ICON";

    private final PvzGame game;
    private final PlantCard.ViewData data;

    public PlantDetailsTable(
            PvzGame game,
            PlantCard.ViewData data,
            Runnable onBack
    ) {
        if (game == null) {
            throw new IllegalArgumentException(
                    "game cannot be null"
            );
        }

        if (data == null) {
            throw new IllegalArgumentException(
                    "data cannot be null"
            );
        }

        if (onBack == null) {
            throw new IllegalArgumentException(
                    "onBack cannot be null"
            );
        }

        this.game = game;
        this.data = data;

        setFillParent(true);
        setTouchable(Touchable.enabled);

        setBackground(
                game.getSkin().newDrawable(
                        "white_pixel",
                        Color.valueOf("183A78")
                )
        );

        ImageButton backButton =
                createBackButton();

        backButton.addListener(
                new ChangeListener() {
                    @Override
                    public void changed(
                            ChangeEvent event,
                            Actor actor
                    ) {
                        PlantDetailsTable.this.remove();
                        onBack.run();
                    }
                }
        );

        Table header = new Table();

        Label plantName =
                new Label(
                        data.plant().name(),
                        game.getSkin(),
                        "big"
                );

        header.add(backButton)
                .left()
                .padLeft(20f)
                .padTop(15f);

        header.add(plantName)
                .expandX()
                .center()
                .padTop(15f);

        header.add()
                .width(backButton.getPrefWidth())
                .padRight(20f);

        add(header)
                .growX()
                .row();

        Table body = new Table();

        Table leftSide =
                createLeftSide(onBack);

        Table rightSide = createRightSide();
        ScrollPane cardsScroll = new ScrollPane(
                rightSide,
                game.getSkin()
        );

        cardsScroll.setFadeScrollBars(false);
        cardsScroll.setOverscroll(false, false);
        cardsScroll.setScrollingDisabled(
                true,
                false
        );

        body.add(leftSide)
                .width(470f)
                .growY()
                .top()
                .padLeft(40f)
                .padTop(25f);

        body.add(cardsScroll)
                .grow();

        add(body)
                .grow();
    }

    private Table createRightSide() {
        Table right = new Table();
        right.top().left();

        Table stats = new Table();
        stats.top().left();

        stats.add(createStat(SUN_COST_ICON, "SUN COST", String.valueOf(data.plant().cost())))
                .width(245f)
                .left();

        stats.add(createStat(RECHARGE_ICON,
                                "RECHARGE",
                                formatNumber(
                                        data.plant().recharge()
                                )
                        )
                )
                .width(245f)
                .left()
                .row();


        stats.add(
                        createStat(
                                TOUGHNESS_ICON,
                                "TOUGHNESS",
                                String.valueOf(
                                        data.plant().baseHp()
                                )
                        )
                )
                .width(245f)
                .left()
                .padTop(15f);

        stats.add(
                        createStat(
                                DAMAGE_ICON,
                                "DAMAGE",
                                data.plant().damageExpression()
                        )
                )
                .width(245f)
                .left()
                .padTop(15f).row();

        stats.add(
                        createStat(
                                SPECIAL_ICON,
                                "SPECIAL",
                                data.plant().tags().toString()
                        )
                )
                .width(245f)
                .left()
                .padTop(15f)
                .row();


        right.add(stats)
                .growX()
                .left()
                .row();


        right.add(
                        createFamilyRow()
                )
                .width(245f)
                .left()
                .padTop(25f)
                .row();


        right.add(
                        createDescriptionRow(
                                PLANT_FOOD_ICON,
                                "Plant Food:",
                                data.plant().onPlantFoodDescription()
                        )
                )
                .width(500f)
                .left()
                .padTop(20f)
                .row();


        right.add(
                        createDescriptionRow(
                                null,
                                "",
                                data.plant().overallDescription()
                        )
                )
                .width(500f)
                .left()
                .padTop(15f)
                .row();


        Label funDescription =
                new Label(
                        data.plant()
                                .funDescription(),
                        game.getSkin().get("big", Label.LabelStyle.class)
                );
        funDescription.setColor(Color.YELLOW);

        funDescription.setWrap(true);

        funDescription.setColor(
                Color.valueOf("FFD75A")
        );

        right.add(funDescription)
                .width(500f)
                .left()
                .padTop(25f)
                .padBottom(15f);

        return right;
    }
    private Table createStat(
            String iconAsset,
            String title,
            String value
    ) {
        Table stat = new Table();
        stat.left();

        Actor icon =createIconOrPlaceholder(iconAsset);

        Table text = new Table();
        text.left();

        Label titleLabel = new Label(title, game.getSkin());

        titleLabel.setColor(Color.LIGHT_GRAY);

        Label valueLabel = new Label(value == null || value.isBlank() ? "—" : value, game.getSkin(), "big");

        valueLabel.setColor(Color.WHITE);

        text.add(titleLabel)
                .left()
                .row();

        text.add(valueLabel)
                .left();

        stat.add(icon)
                .size(48f)
                .padRight(8f);

        stat.add(text)
                .left();

        return stat;
    }
    private Table createFamilyRow() {
        Table row = new Table();
        row.left();

        Actor icon =
                createIconOrPlaceholder(
                        FAMILY_ICON
                );

        Label family =
                new Label(
                        familyName(
                                data.plant().category()
                        ),
                        game.getSkin(),
                        "big"
                );

        row.add(icon)
                .size(48f)
                .padRight(10f);

        row.add(family)
                .left();

        return row;
    }
    private Table createDescriptionRow(
            String iconAsset,
            String title,
            String description
    ) {
        Table row = new Table();
        row.left().top();

        if (iconAsset != null) {
            Actor icon =
                    createIconOrPlaceholder(
                            iconAsset
                    );

            row.add(icon)
                    .size(42f)
                    .top()
                    .padRight(8f);
        }

        Label text =
                new Label(
                        title + " " + description,
                        game.getSkin().get("big", Label.LabelStyle.class)
                );

        text.setWrap(true);
        text.setColor(Color.WHITE);

        row.add(text)
                .growX()
                .left()
                .top();

        return row;
    }
    private String formatNumber(
            double number
    ) {
        if (number == Math.rint(number)) {
            return String.valueOf(
                    (int) number
            );
        }

        return String.valueOf(number);
    }
    private String familyName(
            String category
    ) {
        return switch (category) {
            case "SunProducer" ->
                    "Enlighten-mint";

            case "Shooter" ->
                    "Appease-mint";

            case "Lobber" ->
                    "Arma-mint";

            case "Explosive" ->
                    "Bombard-mint";

            case "Melee" ->
                    "Enforce-mint";

            case "Wall-nut" ->
                    "Reinforce-mint";

            case "Modifier" ->
                    "Enchant-mint";

            case "Strike-through" ->
                    "Pierce-mint";

            case "Homing" ->
                    "catTail-mint";

            default ->
                    category;
        };
    }
    private Actor createIconOrPlaceholder(
            String assetId
    ) {
        if (assetId != null
                && !assetId.isBlank()) {

            Image image =
                    new Image(
                            drawable(assetId)
                    );

            image.setScaling(
                    Scaling.fit
            );

            image.setTouchable(
                    Touchable.disabled
            );

            return image;
        }

        Table placeholder =
                new Table();

        placeholder.setBackground(
                game.getSkin().newDrawable(
                        "white_pixel",
                        Color.valueOf("08782E")
                )
        );

        placeholder.setTouchable(
                Touchable.disabled
        );

        return placeholder;
    }

    private Table createLeftSide(
            Runnable onBack
    ) {
        Table left = new Table();
        left.top();

        Drawable backgroundDrawable =
                drawable(PLANT_BACKGROUND);

        float previewWidth =
                backgroundDrawable.getMinWidth();

        float previewHeight =
                backgroundDrawable.getMinHeight();

        Stack previewStack =
                new Stack();

        Image background =
                new Image(backgroundDrawable);

        background.setScaling(Scaling.none);
        background.setTouchable(
                Touchable.disabled
        );

        Actor idleActor =
                game.createPamActor(
                        data.plant().idlePamPath(),
                        data.plant().idleClip(),
                        0f,
                        0f,
                        true
                );

        WidgetGroup animationLayer =
                new WidgetGroup() {
                    @Override
                    public void layout() {
                        idleActor.setPosition(
                                getWidth() / 2f,
                                getHeight() / 2f
                        );
                    }
                };

        animationLayer.setTouchable(
                Touchable.disabled
        );

        animationLayer.addActor(idleActor);

        previewStack.add(background);
        previewStack.add(animationLayer);

        ProgressBar progressBar =
                createProgressBar();

        TextButton findMoreButton =
                new TextButton(
                        "FIND MORE",
                        game.getSkin(),
                        "green"
                );

        left.add(previewStack)
                .size(
                        previewWidth,
                        previewHeight
                )
                .row();

        left.add(progressBar)
                .width(previewWidth)
                .height(17f)
                .padTop(8f)
                .row();

        left.add(findMoreButton)
                .width(220f)
                .padTop(15f)
                .row();

        left.add(
                        createCollectionActionButton(
                                onBack
                        )
                )
                .width(300f)
                .padTop(10f);

        return left;
    }

    private Actor createCollectionActionButton(
            Runnable onBack
    ) {
        Table actionArea = new Table();

        if (!data.unlocked()) {
            PlantRegistry.UnlockRule unlockRule =
                    PlantRegistry.getUnlockRule(
                            data.plant().id()
                    );

            if (!unlockRule.isPurchasable()) {
                Label unlockInfo =
                        new Label(
                                unlockRule.description(),
                                game.getSkin()
                        );
                unlockInfo.setWrap(true);

                actionArea.add(unlockInfo)
                        .width(280f)
                        .row();

                if (GameSettings.debugMode) {
                    TextButton testUnlockButton =
                            new TextButton(
                                    "TEST UNLOCK",
                                    game.getSkin(),
                                    "green"
                            );

                    testUnlockButton.addListener(
                            new ChangeListener() {
                                @Override
                                public void changed(
                                        ChangeEvent event,
                                        Actor actor
                                ) {
                                    requestDebugPlantUnlock(onBack);
                                }
                            }
                    );

                    actionArea.add(testUnlockButton)
                            .width(130f)
                            .height(34f)
                            .padTop(6f);
                }

                return actionArea;
            }

            TextButton buyButton =
                    new TextButton(
                            "BUY",
                            game.getSkin(),
                            "green"
                    );

            buyButton.addListener(
                    new ChangeListener() {
                        @Override
                        public void changed(
                                ChangeEvent event,
                                Actor actor
                        ) {
                            requestCollectionPlantPurchase(onBack);
                        }
                    }
            );

            Label priceLabel =
                    new Label(
                            unlockRule.purchaseCost()
                                    + " COINS",
                            game.getSkin()
                    );

            actionArea.add(buyButton)
                    .width(160f)
                    .row();
            actionArea.add(priceLabel)
                    .padTop(4f);

            return actionArea;
        }

        int maximumLevel =
                data.plant().upgrades() == null
                        ? 1
                        : data.plant().upgrades().size() + 1;

        if (data.level() >= maximumLevel) {
            Label maxLevelLabel =
                    new Label(
                            "MAX LEVEL",
                            game.getSkin()
                    );

            actionArea.add(maxLevelLabel);
            return actionArea;
        }

        int targetLevel =
                data.level() + 1;

        int coinCost =
                coinCostForLevel(
                        targetLevel
                );

        int packetCost =
                requiredSeedPacketsForLevel(
                        targetLevel
                );

        TextButton upgradeButton =
                new TextButton(
                        "UPGRADE",
                        game.getSkin(),
                        "green"
                );

        upgradeButton.addListener(
                new ChangeListener() {
                    @Override
                    public void changed(
                            ChangeEvent event,
                            Actor actor
                    ) {
                        requestPlantUpgrade(onBack);
                    }
                }
        );

        Label upgradeInfo =
                new Label(
                        "TO LV." + targetLevel
                                + " | " + coinCost + " COINS"
                                + " | " + packetCost + " PACKETS",
                        game.getSkin()
                );

        actionArea.add(upgradeButton)
                .width(160f)
                .row();
        actionArea.add(upgradeInfo)
                .padTop(4f);

        return actionArea;
    }

    private void requestDebugPlantUnlock(
            Runnable onBack
    ) {
        game.getNetworkManager()
                .ensureConnectedAsync()
                .thenCompose(ignored -> {
                    try {
                        return game.getNetworkManager()
                                .getShopClientService()
                                .debugUnlockPlant(
                                        data.plant().id()
                                );
                    } catch (IOException | RuntimeException exception) {
                        return failedFuture(exception);
                    }
                })
                .whenComplete(
                        (response, throwable) ->
                                Gdx.app.postRunnable(
                                        () -> finishServerPlantAction(
                                                response,
                                                throwable,
                                                onBack
                                        )
                                )
                );
    }

    private void requestCollectionPlantPurchase(
            Runnable onBack
    ) {
        game.getNetworkManager()
                .ensureConnectedAsync()
                .thenCompose(ignored -> {
                    try {
                        return game.getNetworkManager()
                                .getShopClientService()
                                .purchaseCollectionPlant(
                                        data.plant().id()
                                );
                    } catch (IOException | RuntimeException exception) {
                        return failedFuture(exception);
                    }
                })
                .whenComplete(
                        (response, throwable) ->
                                Gdx.app.postRunnable(
                                        () -> finishServerPlantAction(
                                                response,
                                                throwable,
                                                onBack
                                        )
                                )
                );
    }

    private void requestPlantUpgrade(
            Runnable onBack
    ) {
        game.getNetworkManager()
                .ensureConnectedAsync()
                .thenCompose(ignored -> {
                    try {
                        return game.getNetworkManager()
                                .getShopClientService()
                                .upgradePlant(
                                        data.plant().id()
                                );
                    } catch (IOException | RuntimeException exception) {
                        return failedFuture(exception);
                    }
                })
                .whenComplete(
                        (response, throwable) ->
                                Gdx.app.postRunnable(
                                        () -> finishServerPlantAction(
                                                response,
                                                throwable,
                                                onBack
                                        )
                                )
                );
    }

    private void finishServerPlantAction(
            ShopResponse response,
            Throwable throwable,
            Runnable onBack
    ) {
        if (throwable != null) {
            game.notifyError(
                    "Plant update failed: "
                            + rootMessage(throwable)
            );
            return;
        }

        if (response == null || !response.isSuccess()) {
            game.notifyError(
                    response == null
                            ? "Plant update failed."
                            : response.getMessage()
            );
            return;
        }

        ClientShopState.apply(response);
        game.notifyInfo(response.getMessage());
        remove();
        onBack.run();
    }

    private static <T> CompletableFuture<T> failedFuture(
            Throwable throwable
    ) {
        CompletableFuture<T> future = new CompletableFuture<>();
        future.completeExceptionally(throwable);
        return future;
    }

    private static String rootMessage(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null) {
            current = current.getCause();
        }
        String message = current.getMessage();
        return message == null || message.isBlank()
                ? current.getClass().getSimpleName()
                : message;
    }

    private void handleCollectionAction(
            Result result,
            Runnable onBack
    ) {
        if (result == null) {
            return;
        }

        if (!result.success()) {
            game.notifyError(
                    result.message()
            );
            return;
        }

        game.notifyInfo(
                result.message()
        );

        remove();
        onBack.run();
    }

    private int coinCostForLevel(
            int targetLevel
    ) {
        return switch (targetLevel) {
            case 2 -> 1000;
            case 3 -> 2000;
            case 4 -> 4000;
            default ->
                    4000 * Math.max(
                            1,
                            targetLevel - 3
                    );
        };
    }

    private int requiredSeedPacketsForLevel(
            int targetLevel
    ) {
        return switch (targetLevel) {
            case 2 -> 5;
            case 3 -> 10;
            case 4 -> 20;
            default ->
                    20 * Math.max(
                            1,
                            targetLevel - 3
                    );
        };
    }

    private ProgressBar createProgressBar() {
        float maximum =
                Math.max(
                        1f,
                        data.requiredSeedPackets()
                );

        ProgressBar progressBar =
                new ProgressBar(
                        0f,
                        maximum,
                        1f,
                        false,
                        game.getSkin(),
                        "xp_yellow"
                );

        progressBar.setValue(
                Math.min(
                        data.seedPackets(),
                        maximum
                )
        );

        progressBar.setAnimateDuration(0f);
        progressBar.setTouchable(
                Touchable.disabled
        );

        return progressBar;
    }

    private ImageButton createBackButton() {
        ImageButton.ImageButtonStyle style =
                new ImageButton.ImageButtonStyle();

        style.imageUp =
                drawable(BACK);

        style.imageDown =
                drawable(BACK_PRESSED);

        style.imageOver =
                drawable(BACK_PRESSED);

        return new ImageButton(style);
    }

    private Drawable drawable(
            String assetId
    ) {
        TextureRegion region =
                game.getTextureBank()
                        .region(assetId);

        if (region == null) {
            throw new IllegalStateException(
                    "TextureBank region was not found: "
                            + assetId
            );
        }

        return new TextureRegionDrawable(
                region
        );
    }
}