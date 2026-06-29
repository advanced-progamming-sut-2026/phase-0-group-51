package models.Shop;

import lombok.Getter;
import lombok.Setter;
import models.Plant.PlantType;

import java.time.LocalDate;

@Getter
@Setter
public class DailyOffer {
    private static final int BASE_PRICE      = 2000;
    private static final double DISCOUNT     = 0.20;
    private final int plantId;
    private final LocalDate date;
    private  boolean   purchased;


    public DailyOffer(int plantId, LocalDate date, boolean purchased) {
        this.plantId = plantId;
        this.date = date;
        this.purchased = purchased;
    }
    public DailyOffer(int plantId) {
        this.plantId = plantId;
        this.date = LocalDate.now();
        this.purchased = false;
    }

    public int getFinalPrice() {
        return (int)(BASE_PRICE*(1 - DISCOUNT));
    }
    public boolean isFinished() {
        return date.isBefore(LocalDate.now());
    }
}
