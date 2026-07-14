package controllers.miniGamesController;

import controllers.GamingController;
import models.plant.WallNut;
import models.zombie.Zombie;

public class WallnutBowlingController extends GamingController {
    //Queue<PlantType> conveyorBelt=new Queue<PlantType>() {
        // این داخل  بعدا باید توابع اضافه کردن و برداشتن والنات به نوار رو هندل کنیم
    //};//  نوبت‌بندی والنات‌هایی که کاربر می‌تونه بیندازه مثل یک صف والنات ها میان
    int redLineColumn; //	ستون خط قرمز . زامبی‌ها از این ستون رد نشن، وگرنه میبازه
    public void rollWallnut(int yRow, int xCol){}//	یه والنات جدید رو توی ردیف row از ستون col به حرکت در میاره
    public void handleCollision(WallNut wallnut, Zombie targetZombie){}//	وقتی والنات به زامبی برخورد کرد آسیب بزنه، منفجر بشه، یا explode راه بیندازه
    public void checkBounds(WallNut wallNut){}//بررسی می‌کنه والنات از لبه‌ی صفحه خارج شده یا نه — اگه شد حذفش کن
    public void rotate(int degree){}//در اثر برخورد میچرخه و directionx , directiony اش تغییر میکنه
}
