package models.items;

import lombok.Getter;
import models.Board.Board;
import models.Zombie.Zombie;

@Getter
public class Mower {
    private int rowNumber;
    private boolean activated;
    private boolean destroyed;
    private float posX;

    private final float speed = 4f;
    private final float maxBoundary = 9 * 80f;

    public Mower(int rowNumber) {
        this.rowNumber = rowNumber;
        this.activated = false;
        this.destroyed = false;
        this.posX = 0f;
    }

    public void update(Board board) {
        if(destroyed) {
            return;
        }
        Zombie firstZombie = board.getFirstZombieInLane(rowNumber);
        if (!activated && firstZombie != null && firstZombie.getX() <= this.posX) {
            this.activated = true;
            System.out.println("The lawn mower in the row " + rowNumber + " is triggered!");
        }
        if(activated) {
            posX += speed;
            for(Zombie zombie : board.getZombiesInLane(rowNumber)) {
                if (!zombie.isDead() && zombie.getX() <= this.posX) {
                    zombie.setDead(true);
                    System.out.println("The lawn mower in the row " + rowNumber + " killed zombie: " + zombie.getAlias());
                }
            }
            if (posX >= maxBoundary) {
                this.destroyed = true;
            }
        }
    }
}
