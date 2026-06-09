package models.Minigames;

import models.Plant.Plant;
import models.Zombie.Zombie;

public class RollingWallnut extends Plant {
    private WallnutType wallnutType;
    public void move(){}
    private int damage; //به هر زامبی ای بخوره با له یا انفجار چه اسیبی میرسونه بهش
    private int directionX;
    private int directionY;//الان در چه جهت هایی داره حرکت میکنه
    public void onHit(Zombie targetZombie) {}//به هر زامبی ای رسید رفتار خاص رو اجرا کنه
}
