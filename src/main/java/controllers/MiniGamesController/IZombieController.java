package controllers.MiniGamesController;

import controllers.GamingController;
import models.Minigames.IZombie;
import models.Zombie.Zombie;
import models.Zombie.ZombieType;

public class IZombieController extends GamingController {
    IZombie iZombieGame;
    ZombieType selectedZombieType;
    public boolean checkWin(){return false;}// آیا همه مغز خورده شدن؟
    public boolean checkLoss(){return false;} // آیا sun کافی نیست و هیچ زامبی‌ای روی صفحه نیست؟
    public boolean canPlaceZombie(ZombieType zombieType) {return false;}
    public void placeZombie(ZombieType zombieType,int x,int y){}
    public boolean isRightOfRedLine(int x){return false;}
    public void addSun(int amount){}
    void onSunProduced(Zombie SunProducerZombie){}
    public void initLevel(){}// باید صفحه رو reset کنه، خورشید اولیه رو ست کنه و مغزها رو بذاره
}
