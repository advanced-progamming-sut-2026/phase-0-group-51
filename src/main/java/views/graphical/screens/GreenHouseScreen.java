package views.graphical.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Scaling;
import com.badlogic.gdx.utils.ScreenUtils;
import controllers.GreenHouseMenuController;
import graphics.PvzGame;
import models.Result;
import views.graphical.ui.CollectionMenuTable;
import views.graphical.ui.ForgotPassPopup;
import views.graphical.ui.NotificationOverlay;

public class GreenHouseScreen extends BaseScreen{
     private static final String POT_COUNT = "IMAGE_UI_HUD_INGAME_SPROUT_ICON";
     private static final String POT_COUNT_CLICKED = "IMAGE_UI_HUD_INGAME_SPROUT_ICON_DOWN";
     private static final String SHOVEL_CLICKED = "IMAGE_UI_HUD_INGAME_SHOVEL_BUTTON_DOWN";;
     private static final String SHOVEL = "IMAGE_UI_HUD_INGAME_SHOVEL_BUTTON";
     private static final String VASE_CLICKED = "IMAGE_UI_GENERIC_BUTTON_HUD_MINIGAMES_SELECTED";
     private static final String VASE ="IMAGE_UI_GENERIC_BUTTON_HUD_MINIGAMES_NORMAL" ;
     private static final String COIN = "IMAGE_UI_GENERIC_BUTTONS_COIN_BUY_NORMAL";
     private static final String COIN_CLICKED = "IMAGE_UI_GENERIC_BUTTONS_COIN_BUY_SELECTED";
     private static final String GEMS = "IMAGE_UI_GENERIC_BUTTONS_PREMIUM_NORMAL";
     private static final String GEMS_CLICKED = "IMAGE_UI_GENERIC_BUTTONS_PREMIUM_SELECTED";
     private static final String EARN_COIN = "IMAGE_UI_HUD_WORLDMAP_FREE_COINS_BUTTON_GOLDEN_FREE_COINS_BUTTON_GOLDEN_300X130";
     private static final String EARN_COIN_CLICKED = "IMAGE_UI_HUD_WORLDMAP_FREE_COINS_BUTTON_GOLDEN_FREE_COINS_BUTTON_GOLDEN_300X130_2";
     private static final String EARN_GEM = "IMAGE_UI_HUD_WORLDMAP_FREE_COINS_BUTTON_FREE_COINS_BUTTON_300X130";
     private static final String EARN_GEM_CLICKED = "IMAGE_UI_HUD_WORLDMAP_FREE_COINS_BUTTON_FREE_COINS_BUTTON_300X130_2";
     private static final String SALE = "IMAGE_UI_HUD_WORLDMAP_HUD_STORE_SALE_BANNER";
     private static final String SHOP = "IMAGE_UI_HUD_WORLDMAP_BUTTONS_HUD_STORE_NORMAL";
     private static final String SHOP_CLICKED = "IMAGE_UI_HUD_WORLDMAP_BUTTONS_HUD_STORE_SELECTED";
     private static final String BOOK = "IMAGE_UI_HUD_WORLDMAP_BUTTONS_HUD_ALMANAC_NORMAL";
     private static final String BOOK_CLICKED = "IMAGE_UI_HUD_WORLDMAP_BUTTONS_HUD_ALMANAC_SELECTED";
     private static final String POT = "IMAGE_ZEN_GARDEN_GROWING_PLANT_SLOT_GROWING_PLANT_SLOT_184X161";
     private static final String POT_GROWING = "IMAGE_ZEN_GARDEN_GROWING_PLANT_SLOT_GROWING_PLANT_SLOT_184X161_2";
     private static final String GEM_READY = "IMAGE_ZEN_GARDEN_BUTTON_UNLOCK_INACTIVE";
     private static final String GEM_READY_CLICKED = "IMAGE_ZEN_GARDEN_BUTTON_UNLOCK_ACTIVE";
     private static final String WATERING_POT = "IMAGE_ZEN_GARDEN_ZENGARDEN_WATER_POURING_ZENGARDEN_WATER_POURING_317X281";
     private static final float TOP_BUTTON_Y = 635f;
     private static final String LOCK=
             "768/INITIAL/UI/CHOOSER/SLOT_LOCK_SMALL/SLOT_LOCK_SMALL.PAM";
     private Stack root;
     private Texture backgroundTexture;
     private static final String BACK = "IMAGE_UI_ALMANAC_BUTTONS_HUD_BACK_NORMAL";
     private static final String BACK_PRESSED = "IMAGE_UI_ALMANAC_BUTTONS_HUD_BACK_SELECTED";
     private final GreenHouseMenuController controller = new GreenHouseMenuController();
     private NotificationOverlay notificationOverlay;
     public GreenHouseScreen(PvzGame game) {
          super(game);
          buildUi();
     }

     private void buildUi() {
          root = new Stack();
          root.setFillParent(true);
          backgroundTexture = new Texture(
                  Gdx.files.internal("assets/backgrounds/GreenHouseBG.png")
          );

          backgroundTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);

          Image backgroundImage = new Image(backgroundTexture);
          backgroundImage.setScaleX(1.2f);
          backgroundImage.setFillParent(true);
          backgroundImage.setTouchable(Touchable.disabled);
          Group uiLayer = new Group();

          ImageButton backButton = createImageButton(BACK, BACK_PRESSED);
          ImageButton collectionButton = createImageButton(BOOK, BOOK_CLICKED);
          ImageButton vaseButton = createImageButton(VASE, VASE_CLICKED);
          ImageButton potCountButton = createImageButton(POT_COUNT, POT_COUNT_CLICKED);
          ImageButton gemsButton = createImageButton(GEMS, GEMS_CLICKED);
          ImageButton coinButton = createImageButton(COIN, COIN_CLICKED);
          ImageButton earnGemButton = createImageButton(EARN_GEM, EARN_GEM_CLICKED);
          ImageButton earnCoinButton = createImageButton(EARN_COIN, EARN_COIN_CLICKED);
          ImageButton shopButton = createImageButton(SHOP, SHOP_CLICKED);
          ImageButton shovelButton = createImageButton(SHOVEL, SHOVEL_CLICKED);
          Image saleBanner = createImage(SALE);
          saleBanner.setTouchable(Touchable.disabled);
          place(uiLayer, backButton,
                  14f, TOP_BUTTON_Y);

          place(uiLayer, collectionButton,
                  103f, TOP_BUTTON_Y);

          place(uiLayer, vaseButton,
                  192f, TOP_BUTTON_Y);


          place(uiLayer, potCountButton,
                  490f, 650f);

          place(uiLayer, gemsButton,
                  680f, 650f);

          place(uiLayer, coinButton,
                  870f, 650f);

          place(uiLayer, earnGemButton,
                  675f, 600f);

          place(uiLayer, earnCoinButton,
                  900f, 600f);

          place(uiLayer, shopButton,
                  1140f, 615f);

          place(uiLayer, saleBanner,
                  1118f, 600f);

          place(uiLayer, shovelButton,
                  1144f, 16f);



          backButton.addListener(new ChangeListener() {
               @Override
               public void changed(ChangeEvent event, Actor actor) {
                    game.showScreen(new MainMenuScreen(game));
               }
          });
          collectionButton.addListener(new ChangeListener() {
               @Override
               public void changed(ChangeEvent event, Actor actor) {
                stage.addActor(new CollectionMenuTable(game));
               }
          });

          root.add(backgroundImage);
          root.add(uiLayer);

          notificationOverlay = new NotificationOverlay(game.getSkin());
          root.add(notificationOverlay);

          stage.addActor(root);
          game.getPamPlayer().loadSync(LOCK);
          Actor lockActor = game.createPamActor(LOCK, "idle", 1100f, 300f, true);
          Group lockGroup = new Group();
          lockGroup.setScale(0.5f);
          lockGroup.setTransform(true);
          lockGroup.addActor(lockActor);
          uiLayer.addActor(lockGroup);
     }

     @Override
     public void show() {
          game.hideHud();

     }
     private ImageButton createImageButton(String normalAsset, String pressedAsset) {
          TextureRegion normalRegion = game.getTextureBank().region(normalAsset);
          TextureRegion pressedRegion = game.getTextureBank().region(pressedAsset);
          ImageButton.ImageButtonStyle style = new ImageButton.ImageButtonStyle();
          style.imageUp = new TextureRegionDrawable(normalRegion);
          style.imageDown = new TextureRegionDrawable(pressedRegion);
          style.imageOver = new TextureRegionDrawable(pressedRegion);

          ImageButton button = new ImageButton(style);
          button.getImageCell().expand().fill();
          button.getImage().setScaling(Scaling.stretch);
          return button;
     }

     private Image createImage(String assetId) {
          TextureRegion region = game.getTextureBank().region(assetId);
          Image image = new Image(new TextureRegionDrawable(region));
          image.setScaling(Scaling.stretch);
          return image;
     }
     private void place(Group layer, Actor actor, float x, float y) {
          actor.setPosition(x, y);
          layer.addActor(actor);
     }
     @Override
     public void render(float delta) {
          ScreenUtils.clear(0.05f, 0.05f, 0.05f, 1f);
          super.render(delta);
     }

     @Override
     public void hide() {

     }


}
