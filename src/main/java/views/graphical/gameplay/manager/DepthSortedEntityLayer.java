package views.graphical.gameplay.manager;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;


public final class DepthSortedEntityLayer extends Group {

    public static final int GRAVE_PRIORITY = 0;
    public static final int PLANT_BASE_PRIORITY = 10;
    public static final int ZOMBIE_PRIORITY = 20;

    public DepthSortedEntityLayer() {
        setTransform(false);
    }

    public static void setDepthPriority(Actor actor, int priority) {
        if (actor != null) {
            actor.setUserObject(Integer.valueOf(priority));
        }
    }

    private static int depthPriority(Actor actor) {
        if (actor == null) {
            return 0;
        }

        Object value = actor.getUserObject();
        if (value instanceof Number number) {
            return number.intValue();
        }

        return 0;
    }

    public void sortNow() {
        getChildren().sort((first, second) -> {
            int yOrder = Float.compare(second.getY(), first.getY());
            if (yOrder != 0) {
                return yOrder;
            }

            return Integer.compare(
                depthPriority(first),
                depthPriority(second)
            );
        });
    }

    @Override
    public void act(float delta) {
        super.act(delta);
        sortNow();
    }
}
