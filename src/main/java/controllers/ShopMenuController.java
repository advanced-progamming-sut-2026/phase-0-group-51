package controllers;

import Data.database.DailyOfferRepository;
import Data.database.GreenHouseRepository;
import Data.database.PlantRepository;
import Data.database.UserRepository;
import Data.loader.PlantData;
import Data.loader.PlantRegistry;
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
  private final ShopMenuValidation validation;
  private final Shop shop;
  private final DailyOfferRepository dailyOfferRepository;
    private final ShopPurchaseRepository purchaseRepository;
    private final Random random = new Random();

  public ShopMenuController(Shop shop) {
    this.validation = new ShopMenuValidation();
    this.dailyOfferRepository = new DailyOfferRepository();
        this.purchaseRepository = new ShopPurchaseRepository();
    this.shop = shop;
  }

    private User currentUser() {
        return App.getInstance().getLoggedInUser();
    }

    private Result loginRequired() {
        return new Result(
                false,
                "You must log in before using the shop.\n",
                null
        );
    }

  public Result showShopList() {
        StringBuilder output = new StringBuilder();
        output.append("=========== Permanent Shop ===========\n");
    int id = 1;
    for (ShopItem item : shop.getCatalogue()) {
            output.append('\n');
            output.append('[').append(id++).append("] ")
                    .append(item.getName()).append('\n');
            output.append("   Price    : ")
                    .append(item.getBasePrice()).append(' ')
                    .append(item.getCurrency()).append('\n');
            output.append("   Quantity : ")
                    .append(item.getAmountPerPurchase()).append('\n');
      if (item.getMaxStack() != null) {
                output.append("   Max Stack: ")
                        .append(item.getMaxStack()).append('\n');
      }
      if (item.isRequiresPlantType()) {
                output.append("   Note     : Requires plant type\n");
      }
    }
        return new Result(true, output.toString(), null);
  }

  public Result showShopDaily() {
    User user = currentUser();
        if (user == null) {
            return loginRequired();
        }

    DailyOffer offer = dailyOfferRepository.getOrCreateDailyOffer(user.getId());
        if (offer == null) {
            return new Result(false, "No daily offer available.\n", null);
        }

    PlantData plant = PlantRegistry.get(offer.getPlantId());
        if (plant == null) {
            return new Result(
                    false,
                    "The plant assigned to today's offer does not exist.\n",
                    null
            );
        }

        StringBuilder output = new StringBuilder();
        output.append("========== Daily Offer ==========\n\n");
        output.append("Plant           : ").append(plant.name()).append('\n');
        output.append("Seed Packets    : 10\n");
        output.append("Base Price      : 2000 Coins\n");
        output.append("Discount        : 20%\n");
        output.append("Final Price     : ")
                .append(offer.getFinalPrice()).append(" Coins\n");
        output.append("Purchased       : ")
                .append(offer.isPurchased() ? "Yes" : "No")
                .append('\n');
        return new Result(true, output.toString(), null);
  }

    public Result shopBuy(String itemId, String count, String plantType) {
    User user = currentUser();
        if (user == null) {
            return loginRequired();
        }
        if (!validation.isCountValid(count)) {
            return new Result(false, "Please enter a valid positive number.\n", null);
        }
        if (!validation.isIdValid(itemId)) {
            return new Result(false, "Please enter a valid item id.\n", null);
        }

    ShopItem item = shop.getItemById(validation.getId());
        if (item == null) {
            return new Result(false, "Item not found.\n", null);
        }

        Integer selectedPlantId = null;
    if (item.getType() == ShopItemType.SEED_PACKET_SELECTED) {
            if (plantType == null || plantType.isBlank()) {
                return new Result(false, "Please enter plant type.\n", null);
            }
            if (!validation.isPlantTypeValid(plantType)) {
                return new Result(false, "Plant not found.\n", null);
            }
            selectedPlantId = validation.getData().id();
            Set<Integer> unlocked = PlantRepository.loadUnlockedPlants(user.getId());
            if (!unlocked.contains(selectedPlantId)) {
                return new Result(
                        false,
                        "The selected plant is not unlocked.\n",
                        null
                );
            }
        }

        List<Integer> randomPlantIds = null;
        if (item.getType() == ShopItemType.SEED_PACKET_RANDOM) {
      Set<Integer> unlocked = PlantRepository.loadUnlockedPlants(user.getId());
            if (unlocked.isEmpty()) {
                return new Result(
                        false,
                        "You do not have any unlocked plants for random Seed Packets.\n",
                        null
                );
            }
            List<Integer> pool = new ArrayList<>(unlocked);
            randomPlantIds = new ArrayList<>();
            for (int index = 0; index < validation.getCount(); index++) {
                randomPlantIds.add(pool.get(random.nextInt(pool.size())));
            }
        }

        int totalPrice;
        if (item.getCurrency() == Currency.COIN) {
            validation.isCoinEnough(user, item);
            totalPrice = validation.getTotalCoinsNeeded();
    } else {
            validation.isGemEnough(user, item);
            totalPrice = validation.getTotalGemsNeeded();
      }

        ShopPurchaseRepository.PurchaseResult result =
                purchaseRepository.purchasePermanentItem(
                        user.getId(),
                        item.getType(),
                        item.getCurrency(),
                        totalPrice,
                        validation.getCount(),
                        selectedPlantId,
                        randomPlantIds
                );

        if (result.status() != ShopPurchaseRepository.PurchaseStatus.SUCCESS) {
            return purchaseFailure(result.status());
  }

        applyBalances(user, result);
        if (item.getType() == ShopItemType.POT) {
            user.setGreenHouse(GreenHouseRepository.load(user.getId()));
  }

        return new Result(true, "Purchase completed successfully.\n", null);
    }

  public Result buyDailyOffer() {
    User user = currentUser();
        if (user == null) {
            return loginRequired();
        }

    DailyOffer offer = dailyOfferRepository.getOrCreateDailyOffer(user.getId());
        if (offer == null) {
            return new Result(false, "No daily offer available.\n", null);
        }
        if (PlantRegistry.get(offer.getPlantId()) == null) {
            return new Result(false, "The daily offer plant no longer exists.\n", null);
        }

        ShopPurchaseRepository.PurchaseResult result =
                purchaseRepository.purchaseDailyOffer(
                        user.getId(),
                        offer.getPlantId(),
                        offer.getFinalPrice()
    );

        if (result.status() != ShopPurchaseRepository.PurchaseStatus.SUCCESS) {
            return purchaseFailure(result.status());
        }

        applyBalances(user, result);
    offer.setPurchased(true);
        return new Result(true, "Daily offer purchased successfully.\n", null);
    }


  public Result showCurrentMenu(){
        if (currentUser() == null) {
            return loginRequired();
        }
    return new Result(true,"You are now in the shop menu.\n",null);
  }
  public Result exitMenu(){
    App.getInstance().setCurrentMenu(Menu.GREENHOUSE_MENU);
    return new Result(true,"Going back to the greenhouse...\n",null);
  }

    private void applyBalances(
            User user,
            ShopPurchaseRepository.PurchaseResult result
    ) {
        user.setCoins(result.coins());
        user.setGems(result.gems());
        user.setPlantFoodNum(result.plantFood());
    }

    private Result purchaseFailure(ShopPurchaseRepository.PurchaseStatus status) {
        return switch (status) {
            case USER_NOT_FOUND -> new Result(
                    false, "The logged-in user no longer exists.\n", null
            );
            case NOT_ENOUGH_COINS -> new Result(
                    false, "Not enough coins.\n", null
            );
            case NOT_ENOUGH_GEMS -> new Result(
                    false, "Not enough gems.\n", null
            );
            case MAXIMUM_POTS_REACHED -> new Result(
                    false, "Maximum greenhouse slots reached.\n", null
            );
            case MAXIMUM_PLANT_FOOD_REACHED -> new Result(
                    false, "Maximum Plant Food reached.\n", null
            );
            case NO_UNLOCKED_PLANTS -> new Result(
                    false, "No unlocked plant is available for this purchase.\n", null
            );
            case OFFER_NOT_FOUND -> new Result(
                    false, "No daily offer is currently available.\n", null
            );
            case OFFER_ALREADY_PURCHASED -> new Result(
                    false, "Today's offer has already been purchased.\n", null
            );
            case DATABASE_ERROR -> new Result(
                    false, "The purchase could not be saved.\n", null
            );
            case SUCCESS -> new Result(true, "", null);
        };
    }
}
