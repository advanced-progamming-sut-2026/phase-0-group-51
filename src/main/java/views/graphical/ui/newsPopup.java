package views.graphical.ui;

import graphics.PvzGame;

public class newsPopup extends BorderedPanel{
    PvzGame game;
    public newsPopup(PvzGame game){
        super(game, com.badlogic.gdx.graphics.Color.valueOf("A0522D"));
        this.game = game;
    }
}
