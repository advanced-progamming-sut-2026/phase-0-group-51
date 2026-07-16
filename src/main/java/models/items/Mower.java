package models.items;

import lombok.Getter;
import models.Board.Board;
import models.Zombie.Zombie;
import models.games.GameState;
import models.quests.QuestKillSourceType;

import java.util.ArrayList;
import java.util.List;

@Getter
public class Mower {
    private final int rowNumber;
    private boolean activated;
    private boolean destroyed;
    private final GameState gameState;

    public Mower(int rowNumber, GameState gameState) {
        this.rowNumber = rowNumber;
        this.gameState = gameState;
    }

    public void update(Board board) {
        if (destroyed) {
            return;
        }

        Zombie firstZombie = board.getFirstZombieInLane(rowNumber);
        if (!activated && firstZombie != null && firstZombie.getX() <= 0) {
            activate(board);
        }
    }

    private void activate(Board board) {
        activated = true;
        List<String> killed = new ArrayList<>();
        for (Zombie zombie : new ArrayList<>(board.getZombiesInLane(rowNumber))) {
            if (!zombie.isDead()) {
                killed.add(zombie.getAlias());
                zombie.killInstantly(gameState, QuestKillSourceType.MOWER);
            }
        }

        StringBuilder message = new StringBuilder()
                .append("The lawn mower in the row ")
                .append(rowNumber + 1)
                .append(" is triggered and killed these zombies:\n");
        for (String alias : killed) {
            message.append(alias).append('\n');
        }
        gameState.logEvent(message.toString());
        destroyed = true;
    }
}
