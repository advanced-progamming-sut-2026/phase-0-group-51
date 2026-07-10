package models.Minigames;

import models.Plant.Plant;
import models.Plant.PlantType;
import models.Zombie.Zombie;

public class RollingWallnut  { //extend plant
    private WallnutType wallnutType;

    public RollingWallnut() {
      //  super(5,"", PlantType);
    }

    public void move(){}
    private int damage; //به هر زامبی ای بخوره با له یا انفجار چه اسیبی میرسونه بهش
    private int directionX;
    private int directionY;//الان در چه جهت هایی داره حرکت میکنه
    public void onHit(Zombie targetZombie) {}//به هر زامبی ای رسید رفتار خاص رو اجرا کنه
}
