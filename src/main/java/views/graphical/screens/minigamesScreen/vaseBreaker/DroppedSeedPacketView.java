package views.graphical.screens.minigamesScreen.vaseBreaker;

import Data.loader.PlantData;
import Data.loader.PlantRegistry;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import graphics.PvzGame;
import models.minigames.vaseBreaker.DroppedSeedPacket;
import views.graphical.ui.PlantCard;

import java.util.function.Consumer;

public class DroppedSeedPacketView extends Stack {
    private final DroppedSeedPacket packet;
    private final Label timerLabel;


    public DroppedSeedPacketView(PvzGame game, DroppedSeedPacket packet, Consumer<DroppedSeedPacket> onClicked) {
        if (game == null) {
            throw new IllegalArgumentException("game cannot be null");
        }

        if (packet == null) {
            throw new IllegalArgumentException("packet cannot be null");
        }

        this.packet = packet;
        PlantData plant = PlantRegistry.getByName(packet.getPlantName());

        if (plant == null) {
            throw new IllegalStateException(
                    "Plant was not found: "
                            + packet.getPlantName()
            );
        }


        PlantCard card = new PlantCard(game, new PlantCard.ViewData(plant, true, false, 1, 0,
                        1, false, false), 0.62f);

        card.setTouchable(
                Touchable.disabled
        );
        add(card);

        timerLabel = new Label("", game.getSkin());
        timerLabel.setTouchable(Touchable.disabled);

        Table timerLayer = new Table();
        timerLayer.bottom().right();
        timerLayer.setTouchable(Touchable.disabled);
        timerLayer.add(timerLabel).padRight(3f).padBottom(2f);
        add(timerLayer);

        setTouchable(Touchable.enabled);

        addListener(
                new ClickListener() {
                    @Override
                    public void clicked(InputEvent event, float x, float y) {
                        if (onClicked != null) {
                            onClicked.accept(packet);
                        }
                    }
                }
        );
    }


    public void refreshTimer(int currentTick, int ticksPerSecond) {

        int remainingTicks = packet.ticksRemaining(currentTick);
        int seconds = MathUtils.ceil(remainingTicks / (float) Math.max(1, ticksPerSecond));
        timerLabel.setText(seconds + "s");
    }
}
