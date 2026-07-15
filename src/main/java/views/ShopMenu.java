package views;

import controllers.ShopMenuController;
import models.Result;
import models.shop.Shop;
import models.enums.commands.ShopMenuCommands;

import java.util.Scanner;
import java.util.regex.Matcher;

public class ShopMenu implements AppMenu{
    private final ShopMenuController controller;
    private final Shop shop = new Shop();
    public ShopMenu(){this.controller = new ShopMenuController(shop);}
    @Override
    public void check(Scanner scanner) {
        String line = scanner.nextLine().trim();
      if (ShopMenuCommands.SHOP_DAILY_REGEX.matches(line)){
          Result result = controller.showShopDaily();
          System.out.println(result.message());
      }
      else if(ShopMenuCommands.SHOP_LIST_REGEX.matches(line)){
          Result result = controller.showShopList();
          System.out.println(result.message());
      }
      else if(ShopMenuCommands.SHOP_BUY_REGEX.matches(line)){
          handleShopBuy(line);
      }
      else if(ShopMenuCommands.CURRENT_MENU_REGEX.matches(line)){
          Result result = controller.showCurrentMenu();
          System.out.println(result.message());
      } else if (ShopMenuCommands.EXIT_MENU_REGEX.matches(line)) {
          Result result = controller.exitMenu();
          System.out.println(result.message());
      } else if (ShopMenuCommands.SHOP_BUY_DAILY.matches(line)) {
          Result result = controller.buyDailyOffer();
          System.out.println(result.message());
      } else invalidCommand();
    }
    public void handleShopBuy(String input){
        Matcher matcher = ShopMenuCommands.SHOP_BUY_REGEX.getMatcher(input);
        String itemId = matcher.group("item_id");
        String count = matcher.group("count");
        String plantType = matcher.group("plant_type");
        if (plantType == null) plantType = "";
        Result result = controller.shopBuy(itemId,count,plantType);
        System.out.println(result.message());

    }
}
