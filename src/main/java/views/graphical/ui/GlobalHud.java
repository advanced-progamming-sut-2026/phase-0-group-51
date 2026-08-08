package views.graphical.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Scaling;
import graphics.PvzGame;

public final class GlobalHud extends Table {

    private final PvzGame game;
    private final Skin skin;

    private Label coinLabel;
    private Label gemLabel;

    private static final String GEM_ICON  = "IMAGE_UI_GENERIC_BUTTONS_PREMIUM_NORMAL";
    private static final String COIN_ICON = "IMAGE_UI_GENERIC_BUTTONS_COIN_BUY_NORMAL";

    public GlobalHud(PvzGame game, Skin skin) {
        this.game = game;
        this.skin = skin;

        setFillParent(true);
        top().right();
        pad(10);

        buildUi();
        setVisible(false);
    }

    private void buildUi() {
        Table bar = new Table();

        //Coins
        Drawable coinDrawable = safeRegion(COIN_ICON);
        Stack coinStack = new Stack();
        if (coinDrawable != null) {
            Image coinImg = new Image(coinDrawable);
            coinImg.setScaling(Scaling.fit);
            coinStack.add(coinImg);
        }

        coinLabel = new Label("0", labelStyle("medium_outline"));
        coinLabel.setColor(Color.WHITE);
        coinLabel.setAlignment(Align.center);

        Table coinTextTable = new Table();
        coinTextTable.add(coinLabel).padLeft(20f);
        coinStack.add(coinTextTable);

        bar.add(coinStack).size(100, 40).padRight(15);

        //Gems
        Drawable gemDrawable = safeRegion(GEM_ICON);
        Stack gemStack = new Stack();
        if (gemDrawable != null) {
            Image gemImg = new Image(gemDrawable);
            gemImg.setScaling(Scaling.fit);
            gemStack.add(gemImg);
        }

        gemLabel = new Label("0", labelStyle("medium_outline"));
        gemLabel.setColor(Color.WHITE);
        gemLabel.setAlignment(Align.center);

        Table gemTextTable = new Table();
        gemTextTable.add(gemLabel).padLeft(20f);
        gemStack.add(gemTextTable);

        bar.add(gemStack).size(100, 40).padRight(15);


        add(bar).align(Align.right);
    }

    public void configure(
        int coins,
        int gems,
        boolean showBackButton,
        Runnable backAction
    ) {
        updateCurrencies(coins, gems);
        setVisible(true);
    }

    public void updateCurrencies(int coins, int gems) {
        if (coinLabel != null) coinLabel.setText(String.valueOf(coins));
        if (gemLabel != null) gemLabel.setText(String.valueOf(gems));
    }

    public void hideHud() {
        setVisible(false);
    }

    private Drawable safeRegion(String id) {
        try {
            TextureRegion r = game.getTextureBank().region(id);
            return (r == null) ? null : new TextureRegionDrawable(r);
        } catch (Exception e) {
            return null;
        }
    }

    private Label.LabelStyle labelStyle(String name) {
        try {
            return skin.get(name, Label.LabelStyle.class);
        } catch (Exception e) {
            return skin.get("default", Label.LabelStyle.class);
        }
    }

    private Actor gameIcon(String normalRegion, String selectedRegion, String fallbackLabel) {
        Drawable normal = safeRegion(normalRegion);
        Drawable selected = safeRegion(selectedRegion);
        if (normal != null) {
            Drawable sel = (selected != null) ? selected : normal;
            ImageButton.ImageButtonStyle st = new ImageButton.ImageButtonStyle();
            st.imageUp = normal;
            st.imageOver = sel;
            st.imageDown = sel;
            return new ImageButton(st);
        }
        Table ph = new Table();
        ph.setBackground(skin.newDrawable("white_pixel", new Color(1, 1, 1, 0.18f)));
        Label l = new Label(fallbackLabel, labelStyle("medium"));
        l.setColor(Color.WHITE);
        ph.add(l);
        return ph;
    }
}
