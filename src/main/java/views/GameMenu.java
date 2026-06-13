package views;

import java.util.Scanner;

public class GameMenu implements AppMenu{
    @Override
    public void check(Scanner scanner) {
        //دستور ورودی چک شود و توابع زیر از کنترلر گیم منیو صدا زده بشه
        //menuEnter,enterMenuGreenHouse,enterMenuTravelLog,
        // enterMenuLeaderBoard,coinWallet,gemWallet
        //دستور ورودی چک شود و توابع زیر از کنترلر گیمینگ صدا زده بشه
        //winTheGame,handleWave,handleMower,zombiesAttack,feedPlants
         //zombiesInfo,showPlantsStatus,showMap,zombiesDrop
    }
    public void handleEnterChapter(String input){}//menu enter chapter -c <chaptername> دستور
    public void handleCheatAdd(String input){}//menu cheat add <n> <coin/diamond>
    public void handleAdvanceTime(String input){}
    public void handlePlantPlant(String input){}
    public void handlePluckPlant(String input){}
    public void handleShowTileStatus(String input){}
}
