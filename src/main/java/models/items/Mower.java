package models.items;

import lombok.Getter;
import models.Board.Board;
import models.Zombie.Zombie;
import models.games.GameState;
import models.quests.QuestKillSourceType;

import java.util.ArrayList;

@Getter
public class Mower {
    public static final float START_X = -0.85f;
    public static final float SPEED_COLUMNS_PER_SECOND = 7.5f;
    public static final float HIT_RADIUS_COLUMNS = 0.52f;
    public static final float EXIT_PADDING_COLUMNS = 0.85f;

    private final int rowNumber;
    private final GameState gameState;

    private boolean activated;
    private boolean destroyed;
    private float x = START_X;

    public Mower(int rowNumber, GameState gameState) {
        this.rowNumber = rowNumber;
        this.gameState = gameState;
    }

    public void update(Board board) {
        if (destroyed || board == null) {
            return;
        }

        if (!activated) {
            boolean zombieThreateningHouse = false;

            for (Zombie zombie : board.getZombiesInLane(rowNumber)) {
                if (zombie == null || zombie.isDead()) {
                    continue;
                }

                // direction > 0 means the zombie is moving left,
                // toward the house. Reversed zombies (Prospector
                // after its jump, hypnotized zombies, etc.) are
                // moving away from the house and must not trigger
                // the mower.
                if (zombie.getDirection() > 0
                    && zombie.getX() <= 0f) {
                    zombieThreateningHouse = true;
                    break;
                }
            }

            if (!zombieThreateningHouse) {
                return;
            }

            activate(board);
            return;
        }

        float previousX = x;
        float secondsPerTick = 1f / Math.max(1, gameState.getTicksPerSecond());

        x += SPEED_COLUMNS_PER_SECOND * secondsPerTick;

        killCrossedZombies(board, previousX, x);

        if (x >= board.getColumnCount() + EXIT_PADDING_COLUMNS) {
            destroyed = true;
            gameState.logEvent(
                "The lawn mower in row "
                    + (rowNumber + 1)
                    + " finished its run.\n"
            );
        }
    }

    private void activate(Board board) {
        activated = true;

        gameState.logEvent(
            "The lawn mower in row "
                + (rowNumber + 1)
                + " was triggered.\n"
        );

        killCrossedZombies(board, x, x);
    }

    private void killCrossedZombies(
        Board board,
        float fromX,
        float toX
    ) {
        float minX =
            Math.min(fromX, toX)
                - HIT_RADIUS_COLUMNS;

        float maxX =
            Math.max(fromX, toX)
                + HIT_RADIUS_COLUMNS;

        for (Zombie zombie : new ArrayList<>(board.getZombiesInLane(rowNumber))) {
            if (zombie == null || zombie.isDead()) {
                continue;
            }

            float zombieX = zombie.getX();

            if (zombieX < minX || zombieX > maxX) {
                continue;
            }

            zombie.killInstantly(
                gameState,
                QuestKillSourceType.MOWER
            );
        }
    }
}
