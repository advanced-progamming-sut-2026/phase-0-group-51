package controllers.MiniGamesController;

import controllers.GamingController;
import models.minigames.IZombie;
import models.zombie.Zombie;
import models.zombie.ZombieType;

public class IZombieController extends GamingController {
    IZombie IZombieGame;
    ZombieType selectedZombieType;
    public boolean checkWin(){return false;}// آیا همه مغز خورده شدن؟
    public boolean checkLoss(){return false;} // آیا sun کافی نیست و هیچ زامبی‌ای روی صفحه نیست؟
    public boolean canPlaceZombie(ZombieType zombieType){return false;}//شرطی که گفته شده توی داک برای اینکه میشه یه زامبی گذاشت یا نه
    public void placeZombie(ZombieType zombieType,int x,int y){}
    public boolean isRightOfRedLine(int x){return false;}
    public void addSun(int amount){}
    void onSunProduced(Zombie SunProducerZombie){} //خورشید گرفته شده از زامبی تولید کننده خورشید رو اضافه میکنه به خورشیدا
    public void initLevel(){}// باید صفحه رو reset کنه، خورشید اولیه رو ست کنه و مغزها رو بذاره
}
