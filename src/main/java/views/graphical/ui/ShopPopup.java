package views.graphical.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Scaling;
import controllers.ShopMenuController;
import graphics.PvzGame;
import models.App;
import models.Result;
import models.User;
import models.shop.DailyOffer;
import models.shop.Shop;
import models.shop.ShopItem;
import models.shop.ShopItemType;
import models.shop.Currency;
import views.graphical.screens.GreenHouseScreen;
import Data.loader.PlantData;
import Data.loader.PlantRegistry;
import Data.database.PlantRepository;
import Data.database.PlantBoostRepository;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Group;

import java.time.LocalTime;
import java.util.Map;
import java.util.List;
import java.util.Set;

public class ShopPopup extends BorderedPanel {

    private final PvzGame game;
    private final ShopMenuController shopController;
    private Table itemsTable;
    private final Drawable shadowOverlay;
    private final Runnable onPurchaseChanged;

    private Table fakeHudOverlay;
    private Label fakeCoinLabel;
    private Label fakeGemLabel;

    public ShopPopup(PvzGame game) {
        this(game, null);
    }

    public ShopPopup(PvzGame game, Runnable onPurchaseChanged) {
        super(game, Color.valueOf("A0522D"));
        this.game = game;
        this.onPurchaseChanged = onPurchaseChanged;
        this.shopController = new ShopMenuController(new Shop());
        this.shadowOverlay = game.getSkin().newDrawable(
            "white_pixel",
            new Color(0, 0, 0, 0.75f)
        );

        buildFakeHud();
        buildUi();
    }

    private void buildUi() {
        TextureRegion topperRegion = game.getTextureBank().region("IMAGE_UI_PAUSEMENU_WINDOWTOPPER");
        TextureRegion storeIconRegion = game.getTextureBank().region("IMAGE_UI_ALMANAC_FINDMORE_STORE");

        Stack topDecoration = new Stack();

        Image topperImage = new Image(topperRegion);
        topperImage.setScaling(Scaling.none);

        Image storeIconImage = new Image(storeIconRegion);
        storeIconImage.setScaling(Scaling.none);
        Container<Image> storeIconContainer = new Container<>(storeIconImage);
        storeIconContainer.align(Align.top | Align.center);
        storeIconContainer.padTop(-25f);

        TextureRegion closeRegion = game.getTextureBank().region("IMAGE_UI_ALMANAC_TABS_CLOSE_TAB");
        ImageButton.ImageButtonStyle closeStyle = new ImageButton.ImageButtonStyle();
        if (closeRegion != null) {
            closeStyle.imageUp = new TextureRegionDrawable(closeRegion);
        }
        ImageButton closeBtn = new ImageButton(closeStyle);
        closeBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                remove();
            }
        });
        Container<ImageButton> closeContainer = new Container<>(closeBtn);
        closeContainer.align(Align.topLeft);
        closeContainer.padTop(-22f).padLeft(-15f);

        topDecoration.add(topperImage);
        topDecoration.add(storeIconContainer);
        topDecoration.add(closeContainer);

        itemsTable = new Table();
        itemsTable.top();

        ScrollPane scrollPane = new ScrollPane(itemsTable, game.getSkin());
        scrollPane.setScrollingDisabled(false, false);
        scrollPane.setFadeScrollBars(false);

        populateShopItems();

        this.getContent().add(topDecoration).align(Align.center).padTop(-50).row();
        this.getContent().add(scrollPane).width(720f).height(380f).padTop(10).padBottom(20).row();

        this.pack();
    }

    private void buildFakeHud() {
        fakeHudOverlay = new Table();
        fakeHudOverlay.pad(10f);
        fakeHudOverlay.top();
        fakeHudOverlay.setTouchable(Touchable.disabled);

        Table rightBar = new Table();

        fakeGemLabel = new Label("0", game.getSkin());
        fakeGemLabel.setFontScale(1.1f);
        Group gemDisplay = createCurrencyDisplay("IMAGE_UI_GENERIC_BUTTONS_PREMIUM_NORMAL", "IMAGE_UI_GENERIC_BUTTONS_PREMIUM_SELECTED", fakeGemLabel);

        fakeCoinLabel = new Label("0", game.getSkin());
        fakeCoinLabel.setFontScale(1.1f);
        Group coinDisplay = createCurrencyDisplay("IMAGE_UI_GENERIC_BUTTONS_COIN_BUY_NORMAL", "IMAGE_UI_GENERIC_BUTTONS_COIN_BUY_SELECTED", fakeCoinLabel);

        rightBar.add(gemDisplay).size(gemDisplay.getWidth(), gemDisplay.getHeight()).padRight(15f);
        rightBar.add(coinDisplay).size(coinDisplay.getWidth(), coinDisplay.getHeight());
        rightBar.add().size(60f).padLeft(10f);

        fakeHudOverlay.add().expandX();
        fakeHudOverlay.add(rightBar).right().top();

        updateTopBar();
    }

    private Group createCurrencyDisplay(String normalAsset, String pressedAsset, Label label) {
        TextureRegion normalRegion = game.getTextureBank().region(normalAsset);
        TextureRegion pressedRegion = game.getTextureBank().region(pressedAsset);
        ImageButton.ImageButtonStyle style = new ImageButton.ImageButtonStyle();
        style.imageUp = new TextureRegionDrawable(normalRegion);
        style.imageDown = new TextureRegionDrawable(pressedRegion);
        style.imageOver = new TextureRegionDrawable(pressedRegion);

        ImageButton button = new ImageButton(style);
        button.getImageCell().expand().fill();
        button.getImage().setScaling(Scaling.stretch);
        button.setTouchable(Touchable.disabled);

        float width = button.getPrefWidth();
        float height = button.getPrefHeight();

        Group group = new Group();
        group.setSize(width, height);
        button.setBounds(0f, 0f, width, height);
        label.setPosition(70f, 20f);

        group.addActor(button);
        group.addActor(label);

        return group;
    }

    private void updateTopBar() {
        User user = App.getInstance().getLoggedInUser();
        if (user != null && fakeCoinLabel != null && fakeGemLabel != null) {
            fakeCoinLabel.setText(String.format("%,d", user.getCoins()));
            fakeGemLabel.setText(String.format("%,d", user.getGems()));
        }
    }

    @Override
    public void draw(Batch batch, float parentAlpha) {
        Stage s = getStage();

        if (s != null && shadowOverlay != null) {
            shadowOverlay.draw(batch, 0, 0, s.getWidth(), s.getHeight());
        }

        super.draw(batch, parentAlpha);

        if (s != null && fakeHudOverlay != null) {
            fakeHudOverlay.setSize(s.getWidth(), s.getHeight());
            fakeHudOverlay.layout();
            fakeHudOverlay.draw(batch, parentAlpha);
        }
    }

    private TextButton createGreenButton(String text) {
        TextButton.TextButtonStyle style = new TextButton.TextButtonStyle();
        if (game.getSkin().has("medium_outline", Label.LabelStyle.class)) {
            style.font = game.getSkin().get("medium_outline", Label.LabelStyle.class).font;
        } else {
            style.font = game.getSkin().getFont("default-font");
        }
        style.fontColor = Color.WHITE;

        TextureRegion upRegion = game.getTextureBank().region("IMAGE_UI_GENERIC_GREENBUTTON");
        TextureRegion downRegion = game.getTextureBank().region("IMAGE_UI_GENERIC_GREENBUTTON_DOWN");

        if (upRegion != null) style.up = new TextureRegionDrawable(upRegion);
        if (downRegion != null) style.down = new TextureRegionDrawable(downRegion);

        TextButton btn = new TextButton(text, style);
        btn.getLabel().setFontScale(0.85f);
        return btn;
    }

    private Actor createQtyBadge(int amount, boolean isCoinConversion) {
        Table badge = new Table();
        TextureRegion bgReg = game.getTextureBank().region("IMAGE_UI_GENERIC_PURPLEBUTTON");
        if (bgReg != null) {
            badge.setBackground(new TextureRegionDrawable(bgReg));
        } else {
            badge.setBackground(game.getSkin().newDrawable("white_pixel", Color.PURPLE));
        }

        Label qtyLbl = createLabel("x" + amount, Color.WHITE);
        qtyLbl.setFontScale(0.6f);

        if (isCoinConversion) {
            badge.add(qtyLbl).padLeft(8).padRight(3);

            TextureRegion coinReg = game.getTextureBank().region("IMAGE_UI_HUD_INGAME_COIN");
            if (coinReg != null) {
                Image coin = new Image(coinReg);
                coin.setScaling(Scaling.fit);
                badge.add(coin).size(14, 14).padRight(6);
            }
        } else {
            badge.add(qtyLbl).pad(2, 6, 2, 6);
        }

        return badge;
    }

    private Actor createStruckLabel(String text, Color color, float fontScale) {
        Label label = createLabel(text, color);
        label.setFontScale(fontScale);

        Stack stack = new Stack();

        Table labelWrap = new Table();
        labelWrap.add(label);
        stack.add(labelWrap);

        Table lineWrap = new Table();
        lineWrap.center();
        Image line = new Image(game.getSkin().newDrawable("white_pixel", color));
        lineWrap.add(line).height(2f).growX();
        stack.add(lineWrap);

        return stack;
    }

    private Actor createPriceButton(int price, Integer originalPrice, Currency currency, Runnable onBuyClicked) {
        Table btnTable = new Table();

        TextureRegion bgRegion = game.getTextureBank().region("IMAGE_UI_HUD_INGAME_BACKGROUND_3SLICE");
        if (bgRegion != null) {
            btnTable.setBackground(new TextureRegionDrawable(bgRegion));
        } else {
            btnTable.setBackground(game.getSkin().newDrawable("white_pixel", Color.DARK_GRAY));
        }

        TextureRegion iconRegion = currency == Currency.GEM
            ? game.getTextureBank().region("IMAGE_UI_HUD_INGAME_GEM")
            : game.getTextureBank().region("IMAGE_UI_HUD_INGAME_COIN");

        if (iconRegion != null) {
            Image icon = new Image(iconRegion);
            icon.setScaling(Scaling.fit);
            btnTable.add(icon).size(26, 26).padLeft(6).padRight(4);
        }

        Table priceContainer = new Table();
        boolean hasDiscount = (originalPrice != null && originalPrice > price);

        if (hasDiscount) {
            Actor struckPrice = createStruckLabel(String.valueOf(originalPrice), new Color(0.8f, 0.8f, 0.8f, 1f), 0.55f);
            priceContainer.add(struckPrice).padRight(6);
        }

        Label priceLbl = createLabel(String.valueOf(price), hasDiscount ? Color.GOLD : Color.WHITE);
        priceLbl.setFontScale(0.75f);
        priceContainer.add(priceLbl).center();

        btnTable.add(priceContainer).expandX().left().padRight(10).padTop(2).padBottom(2);

        btnTable.setTouchable(Touchable.enabled);
        btnTable.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                onBuyClicked.run();
            }
        });

        return btnTable;
    }

    private void configureIconDisplay(Container<Actor> container, ShopItemType type) {
        float w = 80f;
        float h = 80f;
        float pTop = 0f;
        float pBottom = 0f;
        float pLeft = 0f;
        float pRight = 0f;

        if (type == ShopItemType.PLANT_FOOD) {
            w = 90f;
            h = 90f;
        } else if (type == ShopItemType.POT) {
            w = 95f;
            h = 95f;
        } else if (type == ShopItemType.COIN_CONVERSION) {
            w = 100f;
            h = 100f;
        } else {
            w = 90f;
            h = 90f;
        }

        container.size(w, h);
        container.pad(pTop, pLeft, pBottom, pRight);
    }

    private Actor createItemIcon(ShopItemType type, Integer plantId) {
        Actor innerActor = null;

        if (type == ShopItemType.PLANT_FOOD) {
            TextureRegion pfRegion = game.getTextureBank().region("IMAGE_DANGERROOM_CARD_PLANTFOOD");
            if (pfRegion != null) innerActor = new Image(pfRegion);
        } else if (type == ShopItemType.POT) {
            TextureRegion potRegion = game.getTextureBank().region("IMAGE_ZEN_GARDEN_GROWING_PLANT_SLOT_GROWING_PLANT_SLOT_184X161");
            if (potRegion != null) innerActor = new Image(potRegion);
        } else if (type == ShopItemType.COIN_CONVERSION) {
            TextureRegion coinRegion = game.getTextureBank().region("IMAGE_EFFECTS_PRIZE_COINS_LARGE_PRIZE_COINS_LARGE_581X453");
            if (coinRegion != null) innerActor = new Image(coinRegion);
        } else if (type == ShopItemType.SEED_PACKET_SELECTED && plantId == null) {
            Table brownTable = new Table();
            TextureRegion brownBtnRegion = game.getTextureBank().region("IMAGE_UI_GENERIC_BROWNBUTTON_DOWN");
            if (brownBtnRegion != null) {
                brownTable.setBackground(new TextureRegionDrawable(brownBtnRegion));
            } else {
                brownTable.setBackground(game.getSkin().newDrawable("white_pixel", Color.BROWN));
            }
            Label selectLbl = createLabel("Select\nYour Seed", Color.WHITE);
            selectLbl.setFontScale(0.55f);
            selectLbl.setAlignment(Align.center);
            brownTable.add(selectLbl).center().pad(5);
            innerActor = brownTable;
        } else {
            TextureRegion region = null;
            if (plantId != null) {
                PlantData pd = PlantRegistry.get(plantId);
                if (pd != null && pd.cardAssetId() != null) {
                    region = game.getTextureBank().region(pd.cardAssetId());
                }
            }
            if (region != null) {
                innerActor = new Image(region);
            } else {
                TextureRegion randomPacketRegion = game.getTextureBank().region("IMAGE_UI_STOREMULTI_SEEDPACKETICON");
                if (randomPacketRegion != null) innerActor = new Image(randomPacketRegion);
            }
        }

        if (innerActor == null) {
            Table pTable = new Table();
            pTable.setBackground(game.getSkin().newDrawable("white_pixel", new Color(0, 0, 0, 0.2f)));
            Label imgLabel = new Label("Icon", game.getSkin(), "default");
            imgLabel.setFontScale(0.7f);
            pTable.add(imgLabel).center();
            innerActor = pTable;
        }

        if (innerActor instanceof Image) {
            ((Image) innerActor).setScaling(Scaling.fit);
        }

        Container<Actor> container = new Container<>(innerActor);
        configureIconDisplay(container, type);

        return container;
    }

    private String calculateRemainingTime() {
        LocalTime now = LocalTime.now();
        int h = 23 - now.getHour();
        int m = 59 - now.getMinute();
        return String.format("%02dh %02dm", h, m);
    }

    private void populateShopItems() {
        itemsTable.clearChildren();
        User user = App.getInstance().getLoggedInUser();
        if (user == null) return;

        int itemCount = 0;

        DailyOffer dailyOffer = shopController.getDailyOfferRepository().getOrCreateDailyOffer(user.getId());
        if (dailyOffer != null) {
            String plantName = "Unknown Plant";
            PlantData pd = PlantRegistry.get(dailyOffer.getPlantId());
            if (pd != null) {
                plantName = pd.name();
            }

            String dailyTitle = "Daily Special\n(" + plantName + ")";
            String remainingTime = calculateRemainingTime();
            Actor itemIcon = createItemIcon(ShopItemType.DAILY_OFFER, dailyOffer.getPlantId());
            Actor qtyBadge = createQtyBadge(10, false);

            Table dailyCard = buildItemCard(
                dailyTitle,
                dailyOffer.getFinalPrice(),
                Currency.COIN,
                qtyBadge,
                dailyOffer.isPurchased(),
                this::handleDailyOfferPurchase,
                "IMAGE_UI_STORE_GACHA_PINATA_INNER_PLANT_BG",
                itemIcon,
                true,
                dailyOffer.getBasePrice(),
                remainingTime
            );
            itemsTable.add(dailyCard).pad(8).width(210).height(310);
            itemCount++;
        }

        List<ShopItem> catalogue = shopController.getShop().getCatalogue();
        for (int i = 0; i < catalogue.size(); i++) {
            if (itemCount > 0 && itemCount % 3 == 0) {
                itemsTable.row();
            }

            ShopItem item = catalogue.get(i);
            int itemId = i + 1;

            String displayName = item.getName();
            boolean isCoinConv = false;

            if (item.getType() == ShopItemType.POT) displayName = "Flower Pot";
            else if (item.getType() == ShopItemType.PLANT_FOOD) displayName = "Plant Food";
            else if (item.getType() == ShopItemType.SEED_PACKET_RANDOM) displayName = "Mystery Seeds";
            else if (item.getType() == ShopItemType.SEED_PACKET_SELECTED) displayName = "Specific Seeds";
            else if (item.getType() == ShopItemType.COIN_CONVERSION) {
                displayName = "Bag of Coins";
                isCoinConv = true;
            }

            Actor itemIcon = createItemIcon(item.getType(), null);
            Actor qtyBadge = createQtyBadge(item.getAmountPerPurchase(), isCoinConv);

            Table itemCard = buildItemCard(
                displayName,
                item.getBasePrice(),
                item.getCurrency(),
                qtyBadge,
                false,
                () -> handlePermanentItemPurchase(item, itemId),
                "IMAGE_UI_STORE_GACHA_PINATA_PLANT_CARD_EPIC_BG",
                itemIcon,
                false,
                null,
                null
            );
            itemsTable.add(itemCard).pad(8).width(210).height(310);
            itemCount++;
        }
    }

    private Table buildItemCard(String title, int price, Currency currency, Actor qtyBadge, boolean isSoldOut, Runnable onBuyClicked, String bgRegionName, Actor itemIcon, boolean isDailyOffer, Integer originalPrice, String timeStr) {
        Table card = new Table();

        TextureRegion cardBgRegion = game.getTextureBank().region(bgRegionName);
        if (cardBgRegion != null) {
            card.setBackground(new TextureRegionDrawable(cardBgRegion));
        } else {
            card.setBackground(game.getSkin().newDrawable("white_pixel", Color.valueOf("E3D0A3")));
        }

        Label titleLabel = createLabel(title, isDailyOffer ? Color.GOLD : Color.WHITE);
        titleLabel.setWrap(true);
        titleLabel.setAlignment(Align.center);
        titleLabel.setFontScale(0.85f);
        card.add(titleLabel).growX().padTop(15).padBottom(isDailyOffer ? 0 : 5).row();

        if (isDailyOffer && timeStr != null) {
            Table ribbonTable = new Table();
            TextureRegion ribbonRegion = game.getTextureBank().region("IMAGE_UI_GENERIC_TIMER_RIBBON_RED");
            if (ribbonRegion != null) {
                ribbonTable.setBackground(new TextureRegionDrawable(ribbonRegion));
            } else {
                ribbonTable.setBackground(game.getSkin().newDrawable("white_pixel", Color.RED));
            }
            Label timeLbl = createLabel(timeStr, Color.WHITE);
            timeLbl.setFontScale(0.55f);
            ribbonTable.add(timeLbl).center().padBottom(5);
            card.add(ribbonTable).size(110, 22).padTop(-2).padBottom(5).row();
        }

        Table iconArea = new Table();
        iconArea.add(itemIcon).center();

        iconArea.setTouchable(Touchable.enabled);
        iconArea.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                onBuyClicked.run();
            }
        });

        card.add(iconArea).size(140, 95).expandY().row();

        card.add(qtyBadge).size(70, 20).padBottom(4).row();

        if (isSoldOut) {
            Label soldOutLbl = createLabel("SOLD OUT", Color.RED);
            soldOutLbl.setFontScale(0.9f);
            card.add(soldOutLbl).padBottom(20).row();
        } else {
            Actor priceButton = createPriceButton(price, originalPrice, currency, onBuyClicked);
            card.add(priceButton).size((originalPrice != null && originalPrice > price) ? 135 : 100, 38).padBottom(15).row();
        }

        return card;
    }

    private Label createLabel(String text, Color color) {
        Label l = new Label(text, game.getSkin().get("medium_outline", Label.LabelStyle.class));
        l.setColor(color);
        return l;
    }

    private void handlePurchaseResult(Result result, Runnable onSuccess) {
        if (result.success()) {
            String message = result.message();
            if (message == null || message.isBlank()) {
                message = "Purchase completed successfully!";
            }
            showPopup("Success", message);
            onSuccess.run();
            if (onPurchaseChanged != null) {
                onPurchaseChanged.run();
            }
        } else {
            showPopup("Error", result.message());
        }
    }

    private void handleDailyOfferPurchase() {
        showConfirmationDialog("Purchase Confirmation", "Purchase today's Daily Offer?", () -> {
            Result result = shopController.buyDailyOffer();
            handlePurchaseResult(result, () -> {
                updateTopBar();
                populateShopItems();
            });
        });
    }

    private void handlePermanentItemPurchase(ShopItem item, int itemId) {
        if (item.isRequiresPlantType()) {
            showPlantSelectionDialog(item, itemId);
        } else {
            showConfirmationDialog("Purchase Confirmation", "Purchase " + item.getName() + "?", () -> {
                Result result = shopController.shopBuy(String.valueOf(itemId), "1", null);
                handlePurchaseResult(result, () -> {
                    updateTopBar();
                    if (item.getType() == ShopItemType.POT
                        && game.getScreen() instanceof GreenHouseScreen greenHouseScreen) {
                        greenHouseScreen.refreshGreenHouse();
                    }
                });
            });
        }
    }

    private void showCustomPopup(String title, Actor content, String yesText, String noText, Runnable onYes, Runnable onNo) {
        Table overlay = new Table();
        overlay.setFillParent(true);
        overlay.setTouchable(Touchable.enabled);
        overlay.setBackground(game.getSkin().newDrawable("white_pixel", new Color(0, 0, 0, 0.75f)));

        BorderedPanel panel = new BorderedPanel(game, Color.valueOf("A0522D"));
        Table inner = panel.getContent();

        inner.pad(25f);

        Label titleLbl = createLabel(title, Color.GOLD);
        titleLbl.setFontScale(0.9f);
        inner.add(titleLbl).padTop(10).padBottom(15).row();

        inner.add(content).pad(10).row();

        Table btnTable = new Table();
        if (yesText != null) {
            TextButton yesBtn = createGreenButton(yesText);
            yesBtn.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    overlay.remove();
                    if (onYes != null) onYes.run();
                }
            });
            btnTable.add(yesBtn).size(110, 40).pad(10);
        }

        if (noText != null) {
            TextButton noBtn = createGreenButton(noText);
            noBtn.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    overlay.remove();
                    if (onNo != null) onNo.run();
                }
            });
            btnTable.add(noBtn).size(110, 40).pad(10);
        }

        inner.add(btnTable).padTop(10).padBottom(10);
        panel.pack();

        overlay.add(panel).center();
        getStage().addActor(overlay);
    }

    private int requiredSeedPackets(PlantData plant, int currentLevel) {
        int maxLevel = plant.upgrades() == null ? 1 : plant.upgrades().size() + 1;
        if (currentLevel >= maxLevel) return 1;
        int targetLevel = currentLevel + 1;
        return switch (targetLevel) {
            case 2 -> 5;
            case 3 -> 10;
            case 4 -> 20;
            default -> 20 * Math.max(1, targetLevel - 3);
        };
    }

    private void showPlantSelectionDialog(ShopItem item, int itemId) {
        final String[] selectedPlantName = {null};
        Table contentTable = new Table();

        Label msg = createLabel("Select a plant for this Seed Packet:", Color.WHITE);
        msg.setFontScale(0.8f);
        contentTable.add(msg).padBottom(15).row();

        Table cardsGrid = new Table();
        cardsGrid.top().left();
        cardsGrid.defaults().expandX().top().pad(8f);

        User user = App.getInstance().getLoggedInUser();
        if (user != null) {
            Set<Integer> unlockedPlantIds = PlantRepository.loadUnlockedPlants(user.getId());
            Map<Integer, Integer> plantLevels = PlantRepository.loadPlantLevels(user.getId());
            Map<Integer, Integer> seedPackets = PlantRepository.loadSeedPackets(user.getId());

            ButtonGroup<PlantCard> plantGroup = new ButtonGroup<>();
            plantGroup.setMinCheckCount(0);
            plantGroup.setMaxCheckCount(1);
            plantGroup.setUncheckLast(true);

            int column = 0;
            int columnsPerRow = 4;

            for (Integer plantId : unlockedPlantIds) {
                PlantData pd = PlantRegistry.get(plantId);
                if (pd != null) {
                    boolean isBoosted = PlantBoostRepository.hasBoost(user.getId(), plantId);
                    int level = plantLevels.getOrDefault(plantId, 1);
                    int packets = seedPackets.getOrDefault(plantId, 0);
                    int required = requiredSeedPackets(pd, level);

                    PlantCard.ViewData viewData = new PlantCard.ViewData(
                        pd, true, isBoosted, level, packets, required, true
                    );

                    PlantCard card = new PlantCard(game, viewData);
                    card.hideProgressBar();

                    plantGroup.add(card);

                    card.addListener(new ChangeListener() {
                        @Override
                        public void changed(ChangeEvent event, Actor actor) {
                            if (card.isChecked()) {
                                selectedPlantName[0] = pd.name();
                            }
                        }
                    });

                    cardsGrid.add(card);

                    column++;
                    if (column >= columnsPerRow) {
                        cardsGrid.row();
                        column = 0;
                    }
                }
            }

            if (column != 0) {
                while (column < columnsPerRow) {
                    cardsGrid.add().expandX();
                    column++;
                }
            }
        }

        ScrollPane.ScrollPaneStyle spStyle = new ScrollPane.ScrollPaneStyle();
        if (game.getSkin().has("default", ScrollPane.ScrollPaneStyle.class)) {
            spStyle = new ScrollPane.ScrollPaneStyle(game.getSkin().get(ScrollPane.ScrollPaneStyle.class));
        }
        spStyle.background = null;

        ScrollPane scrollPane = new ScrollPane(cardsGrid, spStyle);
        scrollPane.setFadeScrollBars(false);
        scrollPane.setOverscroll(false, false);
        scrollPane.setScrollingDisabled(true, false);
        contentTable.add(scrollPane).width(560).height(260).padTop(5);

        showCustomPopup("Select Plant", contentTable, "Continue", "Cancel", () -> {
            if (selectedPlantName[0] != null && !selectedPlantName[0].trim().isEmpty()) {
                showConfirmationDialog("Purchase Confirmation", "Purchase " + item.getName() + " for " + selectedPlantName[0] + "?", () -> {
                    Result result = shopController.shopBuy(String.valueOf(itemId), "1", selectedPlantName[0]);
                    handlePurchaseResult(result, () -> {
                        updateTopBar();
                    });
                });
            } else {
                showPopup("Error", "Please select a plant from the board.");
            }
        }, null);
    }

    private void showConfirmationDialog(String title, String message, Runnable onConfirm) {
        Label msgLbl = createLabel(message, Color.WHITE);
        msgLbl.setFontScale(0.85f);

        showCustomPopup(title, msgLbl, "Yes", "No", onConfirm, null);
    }

    private void showPopup(String title, String message) {
        Label msgLbl = createLabel(message, Color.WHITE);
        msgLbl.setFontScale(0.85f);

        showCustomPopup(title, msgLbl, "OK", null, null, null);
    }
}
