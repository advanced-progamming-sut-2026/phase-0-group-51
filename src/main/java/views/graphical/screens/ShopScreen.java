package views.graphical.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.*;
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

import java.time.format.DateTimeFormatter;
import java.util.List;

public class ShopScreen extends BaseScreen {

    private final ShopMenuController shopController;
    private Table itemsTable;
    private Label coinsLabel;
    private Label gemsLabel;
    private Texture backgroundTexture;

    public ShopScreen(PvzGame game) {
        super(game);
        // مقداردهی کنترلر فروشگاه و ارسال دیتابیس کالاها به آن
        this.shopController = new ShopMenuController(new Shop());
        buildUi();
    }

    private void buildUi() {
        // تنظیم بک‌گراند فروشگاه
        backgroundTexture = new Texture(Gdx.files.internal("assets/backgrounds/adventure.jpeg"));
        backgroundTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        Image bgImage = new Image(backgroundTexture);
        bgImage.setFillParent(true);
        bgImage.setScaling(Scaling.stretch);
        stage.addActor(bgImage);

        Table mainTable = new Table();
        mainTable.setFillParent(true);
        mainTable.top();
        stage.addActor(mainTable);

        // ۱. ساخت نوار بالای صفحه (موجودی و دکمه خروج)
        mainTable.add(buildTopBar()).growX().pad(10).row();

        // ۲. ساخت لیست افقی کالاها
        itemsTable = new Table();
        itemsTable.left();

        ScrollPane scrollPane = new ScrollPane(itemsTable, game.getSkin());
        scrollPane.setScrollingDisabled(false, true); // فقط اسکرول افقی
        scrollPane.setFadeScrollBars(false);

        mainTable.add(scrollPane).grow().pad(20);

        // بارگذاری کالاها در لیست
        populateShopItems();
    }

    private Table buildTopBar() {
        Table topBar = new Table();

        // دکمه بازگشت
        TextButton backBtn = new TextButton("Back", game.getSkin(), "default");
        backBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                game.showScreen(new ChapterSelectScreen(game)); // یا هر منوی قبلی
            }
        });
        topBar.add(backBtn).left().size(100, 50);

        topBar.add().expandX();

        // نمایش موجودی کاربر
        User user = App.getInstance().getLoggedInUser();
        int coins = user != null ? user.getCoins() : 0;
        int gems = user != null ? user.getGems() : 0;

        coinsLabel = new Label("Coins: " + coins, game.getSkin(), "medium_outline");
        coinsLabel.setColor(Color.YELLOW);
        gemsLabel = new Label("Gems: " + gems, game.getSkin(), "medium_outline");
        gemsLabel.setColor(Color.CYAN);

        topBar.add(coinsLabel).padRight(20);
        topBar.add(gemsLabel);

        return topBar;
    }

    private void populateShopItems() {
        itemsTable.clearChildren();
        User user = App.getInstance().getLoggedInUser();
        if (user == null) return;

        // ۱. اضافه کردن کالای روزانه (Daily Offer) به ابتدای لیست
        DailyOffer dailyOffer = shopController.getDailyOfferRepository().getOrCreateDailyOffer(user.getId());
        if (dailyOffer != null) {
            String dailyTitle = "Daily Offer (Plant #" + dailyOffer.getPlantId() + ")";
            String priceStr = dailyOffer.getFinalPrice() + " Coins";
            String remainingTime = "Ends: " + dailyOffer.getDate().format(DateTimeFormatter.ISO_LOCAL_DATE);

            Table dailyCard = buildItemCard(
                dailyTitle,
                priceStr,
                "PLACEHOLDER_DAILY_ICON",
                remainingTime,
                dailyOffer.isPurchased(),
                () -> handleDailyOfferPurchase()
            );
            itemsTable.add(dailyCard).pad(10).width(250).height(350);
        }

        // ۲. اضافه کردن کالاهای همیشگی (Permanent Items)
        List<ShopItem> catalogue = shopController.getShop().getCatalogue();
        for (int i = 0; i < catalogue.size(); i++) {
            ShopItem item = catalogue.get(i);
            int itemId = i + 1;
            String priceStr = item.getBasePrice() + " " + item.getCurrency().name();

            Table itemCard = buildItemCard(
                item.getName(),
                priceStr,
                "PLACEHOLDER_SHOP_ITEM_" + item.getType().name(),
                "Qty: " + item.getAmountPerPurchase(),
                false, // کالاهای همیشگی پیش‌فرض Sold Out نیستند
                () -> handlePermanentItemPurchase(item, itemId)
            );
            itemsTable.add(itemCard).pad(10).width(250).height(350);
        }
    }

    private Table buildItemCard(String title, String price, String imageId, String infoText, boolean isSoldOut, Runnable onBuyClicked) {
        Table card = new Table();
        card.setBackground(game.getSkin().newDrawable("white_pixel", new Color(0.2f, 0.2f, 0.2f, 0.8f)));

        // عنوان
        Label titleLabel = new Label(title, game.getSkin(), "medium");
        titleLabel.setWrap(true);
        titleLabel.setAlignment(Align.center);
        card.add(titleLabel).growX().pad(10).row();

        // تصویر کالا
        Table imagePlaceHolder = new Table();
        imagePlaceHolder.setBackground(game.getSkin().newDrawable("white_pixel", Color.DARK_GRAY));
        Label imgLabel = new Label("Image", game.getSkin(), "default");
        imagePlaceHolder.add(imgLabel).center();
        card.add(imagePlaceHolder).size(120, 120).pad(10).row();

        // اطلاعات اضافه (زمان باقی‌مانده یا تعداد)
        Label infoLabel = new Label(infoText, game.getSkin(), "default");
        infoLabel.setColor(Color.LIGHT_GRAY);
        card.add(infoLabel).padBottom(10).row();

        card.add().expandY().row();

        // دکمه خرید یا برچسب فروخته شده
        if (isSoldOut) {
            Label soldOutLbl = new Label("SOLD OUT", game.getSkin(), "medium_outline");
            soldOutLbl.setColor(Color.RED);
            card.add(soldOutLbl).padBottom(20).row();
        } else {
            TextButton buyBtn = new TextButton(price, game.getSkin(), "default");
            buyBtn.getLabel().setColor(Color.GREEN);
            buyBtn.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    onBuyClicked.run();
                }
            });
            card.add(buyBtn).size(150, 50).padBottom(20).row();
        }

        return card;
    }

    private void handleDailyOfferPurchase() {
        showConfirmationDialog("Purchase Confirmation", "Would you like to purchase today's Daily Offer?", () -> {
            Result result = shopController.buyDailyOffer();
            if (result.isSuccess()) {
                showPopup("Success", "Daily offer purchased successfully!");
                updateTopBar();
                populateShopItems(); // رفرش کردن لیست برای اعمال حالت Sold Out
            } else {
                showPopup("Error", result.getMessage());
            }
        });
    }

    private void handlePermanentItemPurchase(ShopItem item, int itemId) {
        // بررسی اینکه آیا کالا نیاز به انتخاب نوع گیاه دارد (مانند بسته بذر انتخابی)
        if (item.isRequiresPlantType()) {
            showPlantSelectionDialog(item, itemId);
        } else {
            showConfirmationDialog("Purchase Confirmation", "Would you like to purchase " + item.getName() + "?", () -> {
                Result result = shopController.shopBuy(String.valueOf(itemId), "1", null);
                if (result.isSuccess()) {
                    showPopup("Success", "Item purchased successfully!");
                    updateTopBar();
                } else {
                    showPopup("Error", result.getMessage());
                }
            });
        }
    }

    private void showPlantSelectionDialog(ShopItem item, int itemId) {
        Dialog dialog = new Dialog("Select Plant", game.getSkin(), "dialog") {
            @Override
            protected void result(Object object) {
                if ((Boolean) object) {
                    // گرفتن متن وارد شده توسط کاربر به عنوان نام گیاه
                    TextField plantInput = this.findActor("plantInputField");
                    String plantName = plantInput.getText();

                    if (plantName != null && !plantName.trim().isEmpty()) {
                        // ادامه فرآیند خرید با باز کردن دیالوگ تأییدیه
                        showConfirmationDialog("Purchase Confirmation", "Would you like to purchase " + item.getName() + " for " + plantName + "?", () -> {
                            Result result = shopController.shopBuy(String.valueOf(itemId), "1", plantName);
                            if (result.isSuccess()) {
                                showPopup("Success", "Item purchased successfully!");
                                updateTopBar();
                            } else {
                                showPopup("Error", result.getMessage());
                            }
                        });
                    } else {
                        showPopup("Error", "Plant name cannot be empty.");
                    }
                }
            }
        };

        dialog.text("Please enter the plant name for this Seed Packet:");

        TextField plantInput = new TextField("", game.getSkin());
        plantInput.setName("plantInputField");
        dialog.getContentTable().row();
        dialog.getContentTable().add(plantInput).width(200).padTop(10);

        dialog.button("Continue", true);
        dialog.button("Cancel", false);
        dialog.show(stage);
    }

    private void showConfirmationDialog(String title, String message, Runnable onConfirm) {
        Dialog dialog = new Dialog(title, game.getSkin(), "dialog") {
            @Override
            protected void result(Object object) {
                if ((Boolean) object) {
                    onConfirm.run();
                }
            }
        };
        dialog.text(message);
        dialog.button("Yes", true);
        dialog.button("No", false);
        dialog.show(stage);
    }

    private void showPopup(String title, String message) {
        Dialog dialog = new Dialog(title, game.getSkin(), "dialog");
        dialog.text(message);
        dialog.button("OK", true);
        dialog.show(stage);
    }

    private void updateTopBar() {
        User user = App.getInstance().getLoggedInUser();
        if (user != null) {
            coinsLabel.setText("Coins: " + user.getCoins());
            gemsLabel.setText("Gems: " + user.getGems());
        }
    }

    @Override
    public void render(float delta) {
        super.render(delta);
    }

    @Override
    public void dispose() {
        super.dispose();
        if (backgroundTexture != null) {
            backgroundTexture.dispose();
        }
    }
}
