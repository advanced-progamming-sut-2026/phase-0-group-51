package views.graphical.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Array;
import graphics.PvzGame;
import lombok.Getter;

import java.util.Iterator;

@Getter
public class ConveyorBeltActor extends Group {
    private final Image background1;
    private final Image background2;
    private final Group itemLayer = new Group();
    private final Array<Actor> items = new Array<>();

    private float offset = 0f;
    private float scrollSpeed = 25f;
    private float cardMoveSpeed = 250f;
    private float itemSpacing = 55f;

    public ConveyorBeltActor(String texturePath) {
        Texture texture = new Texture(Gdx.files.internal(texturePath));
        background1 = new Image(texture);
        background2 = new Image(texture);
        addActor(background1);
        addActor(background2);
        addActor(itemLayer);

        setSize(texture.getWidth(), texture.getHeight());
        background1.setPosition(0, 0);
        background2.setPosition(0, -getHeight());
    }

    public Array<Actor> getItems() {
        return items;
    }

    public void addPlant(Actor plant) {
        if (items.contains(plant, true)) return;

        float x = getWidth() / 2f - plant.getWidth() / 2f;


        float startY = -plant.getHeight() - 20f;

        if (items.size > 0) {
            Actor lastItem = items.get(items.size - 1);
            float expectedSpacing = lastItem.getY() - itemSpacing;
            if (startY > expectedSpacing) {
                startY = expectedSpacing;
            }
        }

        plant.setPosition(x, startY);

        items.add(plant);
        itemLayer.addActor(plant);
    }

    public void removePlant(int index) {
        if (index >= 0 && index < items.size) {
            Actor removed = items.removeIndex(index);

            removed.addAction(Actions.sequence(
                    Actions.fadeOut(0.15f),
                    Actions.removeActor()
            ));
        }
    }

    public void clearPlants() {
        itemLayer.clearChildren();
        items.clear();
    }

    public void update(float delta) {

        offset += scrollSpeed * delta;
        if (offset >= getHeight()) {
            offset %= getHeight();
        }
        background1.setY(offset);
        background2.setY(offset - getHeight());


        for (int i = 0; i < items.size; i++) {
            Actor item = items.get(i);

            float targetY = getHeight() - 350f - (i * itemSpacing);

            if (item.getY() < targetY) {
                float newY = item.getY() + (cardMoveSpeed * delta);
                if (newY > targetY) {
                    newY = targetY;
                }
                item.setY(newY);
            }
        }
    }
}
