package views.graphical.screens.minigamesScreen.vaseBreaker;

import com.badlogic.gdx.scenes.scene2d.Group;
import graphics.PvzGame;
import lombok.Getter;
import lombok.Setter;
import models.minigames.vaseBreaker.Vase;
import models.minigames.vaseBreaker.VaseBreaker;
import views.graphical.gameplay.board.BoardTransform;

import java.util.function.Consumer;
@Getter
@Setter
public class VaseBoardView extends Group {
    private static final float VASE_ASPECT = 115f / 150f;
    private static final float VASE_HEIGHT_SCALE = 1.10f;


    private final PvzGame game;
    private final VaseBreaker vaseBreaker;
    private final BoardTransform transform;
    private Consumer<Vase> onVaseClicked;

    public VaseBoardView(PvzGame game, VaseBreaker vaseBreaker, BoardTransform transform) {
        if (game == null) {
            throw new IllegalArgumentException(
                    "game cannot be null"
            );
        }
        if (vaseBreaker == null) {
            throw new IllegalArgumentException(
                    "vaseBreaker cannot be null"
            );
        }
        if (transform == null) {
            throw new IllegalArgumentException(
                    "transform cannot be null"
            );
        }

        this.game = game;
        this.vaseBreaker = vaseBreaker;
        this.transform = transform;
        rebuild();
    }

    public void refresh() {
        rebuild();
    }


    private void rebuild() {
        clearChildren();
        for (Vase vase : vaseBreaker.getVases()) {
            if (vase.isBroken()) {
                continue;
            }
            VaseView vaseView = new VaseView(game, vase, this::handleVaseClicked);
            placeVase(vaseView, vase);
            addActor(vaseView);
        }
    }

    private void handleVaseClicked(Vase vase) {
        if (onVaseClicked == null) {
            return;
        }
        onVaseClicked.accept(vase);
    }

    private void placeVase(VaseView vaseView, Vase vase) {
        int column = vase.getX() - 1;
        int lane = vase.getY() - 1;
        float tileX = transform.tileX(column);
        float tileY = transform.tileY(lane);

        float tileWidth = transform.tileWidth();
        float tileHeight = transform.tileHeight();


        float vaseHeight = tileHeight * VASE_HEIGHT_SCALE;
        float vaseWidth = vaseHeight * VASE_ASPECT;

        float vaseX = tileX + (tileWidth - vaseWidth) / 2f;
        float vaseY = tileY + (tileHeight - vaseHeight) / 2f;


        vaseView.setBounds(
                vaseX,
                vaseY,
                vaseWidth,
                vaseHeight
        );
    }
}
