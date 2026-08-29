package network.protocol.greenhouse;

import java.util.ArrayList;
import java.util.List;

public class GreenHouseResponse {
    private boolean success;
    private String message;
    private List<GreenHousePotDto> pots = new ArrayList<>();
    private int coins;
    private int gems;

    public GreenHouseResponse() {
    }

    public GreenHouseResponse(
            boolean success,
            String message,
            List<GreenHousePotDto> pots,
            int coins,
            int gems
    ) {
        this.success = success;
        this.message = message;
        this.pots = pots == null
                ? new ArrayList<>()
                : new ArrayList<>(pots);
        this.coins = coins;
        this.gems = gems;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public List<GreenHousePotDto> getPots() {
        return pots;
    }

    public void setPots(List<GreenHousePotDto> pots) {
        this.pots = pots == null
                ? new ArrayList<>()
                : new ArrayList<>(pots);
    }

    public int getCoins() {
        return coins;
    }

    public void setCoins(int coins) {
        this.coins = coins;
    }

    public int getGems() {
        return gems;
    }

    public void setGems(int gems) {
        this.gems = gems;
    }
}
