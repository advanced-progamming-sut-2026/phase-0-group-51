package controllers;

import models.App;
import models.Plant.PlantUpgrade;
import models.Result;
import models.enums.commands.GameCommands;
import models.games.GameState;
import models.sun.Sun;

public class GamingController {
    public void advanceTime(int tick){}
//    public String winTheGame(){}
//    public String handleWave(){}
//    public String handleMower(){}
//    public String zombiesAttack(){}
//    public String plantPlant(String plantType,int x, int y){}
//    public String pluckPlants(int x,int y){}
//    public void feedPlants(){}
//    public String zombiesInfo(){}
//     public String showPlantsStatus(){}
//    public String showMap(){}
//    public String showTileStatus(int x,int y){}
//    public String zombiesDrop(){}
    public Result showSunAmount(GameState gameState){
        return new Result(true,"You have "+gameState.getSun()+" suns.\n",null );
    }
    public Result cheatAddSun(GameState gameState,int n){
        gameState.addSun(n);
        return new Result(true,gameState.getSun()+" suns added.\n",null );
    }
    public Result collectSun(GameState gameState,int x,int y){
        Sun targetSun = null;
        for (Sun sun : gameState.getBoard().getActiveSuns()) {
            float sunGroundY = sun.getLane() * models.Board.Tile.TILEHEIGHT;
            if (Math.abs(sun.getX() - x) < 0.1 && Math.abs(sun.getY() - y) < 0.1) {
                targetSun = sun;
                break;
            }
        }

        if (targetSun != null) {
            boolean success = gameState.getBoard().collectSun(targetSun,gameState);
            if(!success){
                return new Result(false,"Sun has expired or collected before.\n",null);
            }
        } else {
            return new Result(false,"No sun found at given coordinates.\n",null);
        }
        return new Result(true,"Sun collected successfully.\n",null);
    }
}
