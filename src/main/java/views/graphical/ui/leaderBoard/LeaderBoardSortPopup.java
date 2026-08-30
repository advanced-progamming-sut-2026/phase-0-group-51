package views.graphical.ui.leaderBoard;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import graphics.PvzGame;
import views.graphical.ui.BorderedPanel;

import java.util.function.Consumer;

public class LeaderBoardSortPopup extends BorderedPanel {
    public LeaderBoardSortPopup(
            PvzGame game, Consumer<Integer> onSelected) {
        super(game, Color.valueOf("A0522D"));
        Table content = getContent();
        content.defaults()
                .pad(8);

        content.add(label(game, "SORT BY - SELECT AGAIN TO REVERSE"))
                .center()
                .row();


        addOption(
                game,
                content,
                "MAX MEOW POINT",
                0,
                onSelected
        );

        addOption(
                game,
                content,
                "MINIGAMES",
                1,
                onSelected
        );

        addOption(
                game,
                content,
                "DAILY QUESTS",
                2,
                onSelected
        );

        addOption(
                game,
                content,
                "NON-DAILY QUESTS",
                3,
                onSelected
        );

        addOption(
                game,
                content,
                "LAST PROGRESS",
                4,
                onSelected
        );


        TextButton cancel = new TextButton("CANCEL", game.getSkin(), "purple");

        cancel.addListener(
                new com.badlogic.gdx.scenes.scene2d.utils.ClickListener(){
                    @Override
                    public void clicked(
                            com.badlogic.gdx.scenes.scene2d.InputEvent event,
                            float x,
                            float y){
                        remove();
                    }
                }
        );

        content.add(cancel)
                .width(180f)
                .height(45f)
                .row();


        pack();
    }


    private void addOption(
            PvzGame game,
            Table table,
            String text,
            int mode,
            Consumer<Integer> callback
    ){

        TextButton button =
                new TextButton(
                        text,
                        game.getSkin(),
                        "green"
                );


        button.addListener(
                new com.badlogic.gdx.scenes.scene2d.utils.ClickListener(){

                    @Override
                    public void clicked(
                            com.badlogic.gdx.scenes.scene2d.InputEvent event,
                            float x,
                            float y
                    ){

                        callback.accept(mode);
                        remove();
                    }
                }
        );


        table.add(button)
                .width(180f)
                .height(45f)
                .row();
    }


    private com.badlogic.gdx.scenes.scene2d.ui.Label label(
            PvzGame game,
            String text
    ){

        com.badlogic.gdx.scenes.scene2d.ui.Label label =
                new com.badlogic.gdx.scenes.scene2d.ui.Label(
                        text,
                        game.getSkin()
                                .get(
                                        "big_outline",
                                        com.badlogic.gdx.scenes.scene2d.ui.Label.LabelStyle.class
                                )
                );

        label.setColor(Color.WHITE);

        return label;
    }
}
