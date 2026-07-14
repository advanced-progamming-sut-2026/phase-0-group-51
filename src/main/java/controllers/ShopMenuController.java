package controllers;

import data.database.DailyOfferRepository;
import data.database.GreenHouseRepository;
import data.database.PlantRepository;
import data.database.UserRepository;
import data.loader.PlantData;
import data.loader.PlantRegistry;
import controllers.validation.ShopMenuValidation;
import lombok.Getter;
import lombok.Setter;
import models.App;
import models.greenHouse.FlowerPot;
import models.greenHouse.GreenHouse;
import models.Result;
import models.shop.*;
import models.User;
import models.enums.Menu;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;

@Getter
@Setter
public class ShopMenuController {
  private User user;
  private final ShopMenuValidation validation;
  private final Shop shop;
  private final UserRepository userRepository;
  private final DailyOfferRepository dailyOfferRepository;
  public ShopMenuController(Shop shop) {
    this.user = App.getInstance().getLoggedInUser();
    this.validation = new ShopMenuValidation();
    this.userRepository = new UserRepository();
    this.dailyOfferRepository = new DailyOfferRepository();
    this.shop = shop;
  }

  public Result showShopList() {
    StringBuilder sb = new StringBuilder();
    sb.append("=========== Permanent Shop ===========\n");
    int id = 1;
    for (ShopItem item : shop.getCatalogue()) {
      sb.append("\n");
      sb.append("[").append(id++).append("] ")
              .append(item.getName()).append("\n");
      sb.append("   Price    : ")
              .append(item.getBasePrice())
              .append(" ")
              .append(item.getCurrency()).append("\n");
      sb.append("   Quantity : ")
              .append(item.getAmountPerPurchase()).append("\n");
      if (item.getMaxStack() != null) {
        sb.append("   Max Stack: ")
                .append(item.getMaxStack()).append("\n");
      }
      if (item.isRequiresPlantType()) {
        sb.append("   Note     : Requires plant type\n");
      }
    }
    return new Result(true, sb.toString(), null);
  }

  public Result showShopDaily() {
    DailyOffer offer = dailyOfferRepository.getOrCreateDailyOffer(user.getId());
    if (offer == null)
      return new Result(false,
              "No daily offer available.",
              null);

    PlantData plant = PlantRegistry.get(offer.getPlantId());
    StringBuilder sb = new StringBuilder();
    sb.append("========== Daily Offer ==========\n\n");
    sb.append("Plant           : ")
            .append(plant.name())
            .append("\n");
    sb.append("Seed Packets    : 10\n");
    sb.append("Base Price      : 2000 Coins\n");
    sb.append("Discount        : 20%\n");
    sb.append("Final Price     : ")
            .append(offer.getFinalPrice())
            .append(" Coins\n");
    sb.append("Purchased       : ")
            .append(offer.isPurchased() ? "Yes" : "No");
    return new Result(true, sb.toString(), null);

  }

  public Result shopBuy(String itemID, String count, String plantType) {
    if (!validation.isCountValid(count)) return new Result(false,
            "Please enter a valid number.\n", null);
    if (!validation.isIdValid(itemID)) return new Result(false,
            "Please enter a valid item id.\n", null);
    ShopItem item = shop.getItemById(validation.id);
    if (item==null) return new Result(false,"Item not found\n",null);
    if (item.getType() == ShopItemType.SEED_PACKET_SELECTED) {
      if (plantType == null) return new Result(false,
              "Please enter plant type.\n", null);
      if (!validation.isPlantTypeValid(plantType)) return new Result(false,
              "Plant not found.\n", null);
      Set<Integer> unlocked = PlantRepository.loadUnlockedPlants(user.getId());
      if (!unlocked.contains(validation.data.id()))
        return new Result(false,
                "This plant you selected is not unlocked.\n", null);}
    if (item.getCurrency() == Currency.GEM) {
      if (!validation.isGemEnough(item)) return new Result(false,
              "Not enough gems!\n", null);
    } else {
      if (!validation.isCoinEnough(item)) return new Result(false,
              "Not enough coins!\n", null);}
    switch (item.getType()) {
      case POT -> {
        if (validation.doesExceedsMaxStackPot())
          return new Result(false,
                  "Maximum greenhouse slots reached.\n", null);
      }
      case PLANT_FOOD -> {
        if (validation.doesExceedsMaxStackFood())
          return new Result(false,
                  "Maximum plant food reached.\n", null);
      }}
    if (item.getCurrency() == Currency.COIN)
      user.setCoins(user.getCoins() - validation.totalCoinsNeeded);
    else
      user.setGems(user.getGems() - validation.totalGemsNeeded);
    int number = validation.count;
   switch (item.getType()) {
        case POT -> buyPot(number);
        case PLANT_FOOD -> buyPlantFood(number);
        case SEED_PACKET_RANDOM -> buyRandomSeed(number);
        case SEED_PACKET_SELECTED -> buySelectedSeed(number, plantType);
        case COIN_CONVERSION -> buyCoinConversion(number);
  }
    userRepository.updateStats(user);
    return new Result(true,
            "Purchase completed successfully.",
            null);
  }
  public Result buyDailyOffer() {
    DailyOffer offer = dailyOfferRepository.getOrCreateDailyOffer(user.getId());
    if (offer == null)
      return new Result(false,
              "No daily offer.", null);
    if (offer.isPurchased())
      return new Result(false,
              "Today's offer has already been purchased.\n", null);
    if (user.getCoins() < offer.getFinalPrice())
      return new Result(false,
              "Not enough coins.", null);
    user.setCoins(user.getCoins() - offer.getFinalPrice());
    PlantRepository.addSeedPackets(user.getId(), offer.getPlantId(), 10
    );
    offer.setPurchased(true);
    dailyOfferRepository.updatePurchased(user.getId(), true);
    userRepository.updateStats(user);
    return new Result(true,
            "Daily offer purchased successfully.\n", null);
  }
  public void buyPot(int num){
    GreenHouse greenHouse = user.getGreenHouse();
    for (int i = 0; i < num; i++) {
      boolean found = false;
      for (int row = 1; row <= GreenHouse.ROWS && !found; row++) {
        for (int col = 1; col <= GreenHouse.COLUMNS; col++) {
          FlowerPot pot = greenHouse.getPot(row, col);
          if (!pot.isUnlocked()) {
            pot.setUnlocked(true);
            GreenHouseRepository.updatePot(user.getId(), pot);
            found = true;
            break;
          }
        }
      }
      if (!found)
        break;
    }
  }
  public void buyPlantFood(int num){
    user.setPlantFoodNum(user.getPlantFoodNum() + num);
  }
  public void buyRandomSeed(int num){
    Set<Integer> unlocked = PlantRepository.loadUnlockedPlants(user.getId());
    if(unlocked.isEmpty()) return;
    List<Integer> ids = new ArrayList<>(unlocked);
    Random random = new Random();
    for(int i=0;i<num;i++){
      int plantId = ids.get(random.nextInt(ids.size()));
      PlantRepository.addSeedPackets(user.getId(), plantId, 5);
    }

  }
  public void buySelectedSeed(int num, String plantType){
    PlantData data = PlantRegistry.getByName(plantType);
    PlantRepository.addSeedPackets(user.getId(), data.id(), num*10);
  }
  public void buyCoinConversion(int num){
    user.setCoins(user.getCoins() + num * 500);
  }
  public Result showCurrentMenu(){
    return new Result(true,"You are now in the shop menu.\n",null);
  }
  public Result exitMenu(){
    App.getInstance().setCurrentMenu(Menu.GREENHOUSE_MENU);
    return new Result(true,"Going back to the greenhouse...\n",null);
  }
}
