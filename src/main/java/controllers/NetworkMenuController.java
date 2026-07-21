package controllers;

import models.App;
import models.Result;
import models.enums.Menu;

public class NetworkMenuController {

    public Result showCurrentMenu() {
        return new Result(true, "You are now in the network menu.\n", null);
    }

    public Result exitMenu() {
        App.getInstance().setCurrentMenu(Menu.MAIN_MENU);
        return new Result(true, "", null);
    }
}

