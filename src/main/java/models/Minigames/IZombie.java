package models.Minigames;
import models.App;
import models.Board.Board;
import models.Zombie.Zombie;
import models.Zombie.ZombieType;
import models.games.ChapterTheme;
import models.games.GameState;

import java.util.List;
public class IZombie extends GameState {
    //int playerSun=App.getInstance().loggedInUser.getSunAmount();
    Brain[] brains= new Brain[5]; // یک مغز برای هر ردیف
    int redLineColumn; // ستون خط قرمز
    List<ZombieType> availableZombies; // 5 زامبی انتخابی این مرحله
    List<Zombie> placedZombies; // زامبی‌های روی صفحه
    public IZombie(Board board, ChapterTheme chapterTheme) {
        super(board, chapterTheme);
    }

}
