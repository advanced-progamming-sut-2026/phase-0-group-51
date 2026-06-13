package models.Minigames;
import models.Board.Board;
import models.Zombie.Zombie;

import java.util.Set;
public class VaseBreaker {
    private Vase vase;
    private  Board board;
    private Set<Vase> remainVases; //کوزه های هنوز نشکسته
    private Set<Zombie> zombiesInTheGame;
    private int difficultyLevel; // هر مرحله سخت تر میشه
    private int levelNum; // مرحله 1 2 3
    private boolean isFinished;

}
