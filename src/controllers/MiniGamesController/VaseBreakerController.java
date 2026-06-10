package controllers.MiniGamesController;

import controllers.GamingController;
import models.Board.Board;
import models.Minigames.Vase;

import java.util.ArrayList;
import java.util.List;

public class VaseBreakerController extends GamingController {
    public List <Vase> vases= new ArrayList<>();
    Board board;
    public void pickUpSeedPacket(int Vase.x,int Vase.y){}
    public void breakVase(int Vase.x,int Vase.y){}
    public void plantPlant(){}
    public void spawnFromVase(Vase vase){}
    public boolean checkWin(){return false;}// ﺗﻤﺎﻣﯽ ﮐﻮزه ﻫﺎی ﭼﯿﺪه ﺷﺪه روی ﺣﯿﺎط ﺧﻮدش را ﺑﺸﮑﻨﺪ و ﻫﻢ زﻣﺎن ﻣﺮاﻗﺐ ﺑﺎﺷﺪ
    //ﮐﻪ زاﻣﺒﯽ ﻫﺎﯾﯽ ﮐﻪ ﻣﻤﮑﻦ اﺳﺖ درون ﮐﻮزه ﺑﻮده ﺑﺎﺷﻨﺪ، ﻣﻐﺰ او را ﻧﺨﻮرﻧﺪ
}
