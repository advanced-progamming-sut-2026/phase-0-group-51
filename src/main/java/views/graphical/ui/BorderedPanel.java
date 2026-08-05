package views.graphical.ui;

import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import graphics.PvzGame;

public class BorderedPanel extends Table {
    private final Table contentLayer;
    private final Table colorBox;

    public BorderedPanel(PvzGame game, com.badlogic.gdx.graphics.Color bgColor) {
        Stack stack = new Stack();
        Table colorLayer = new Table();
        colorBox = new Table();
        colorBox.setBackground(game.getSkin().newDrawable("white_pixel", bgColor));

        colorLayer.add(colorBox).expand().fill().pad(15, 20, 20, 20);
        stack.add(colorLayer);
        contentLayer = new Table();
        contentLayer.setBackground(game.getSkin().getDrawable("image_ui_dialog_asset_dialogborder_10"));
        contentLayer.pad(17, 22, 24, 22);

        stack.add(contentLayer);
        this.add(stack).expand().fill();
    }

    public Table getContent() {
        return contentLayer;
    }

    public void setFillColor(PvzGame game, com.badlogic.gdx.graphics.Color bgColor) {
        colorBox.setBackground(game.getSkin().newDrawable("white_pixel", bgColor));
    }
}
