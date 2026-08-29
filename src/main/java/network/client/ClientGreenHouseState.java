package network.client;

import models.App;
import models.User;
import models.greenHouse.FlowerPot;
import models.greenHouse.GreenHouse;
import network.protocol.greenhouse.GreenHousePotDto;
import network.protocol.greenhouse.GreenHouseResponse;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;

public final class ClientGreenHouseState {
    private static boolean loaded;
    private static GreenHouse greenHouse;

    private ClientGreenHouseState() {
    }

    public static synchronized void apply(
            GreenHouseResponse response
    ) {
        if (response == null
                || response.getPots() == null
                || response.getPots().isEmpty()) {
            return;
        }

        replaceWith(response.getPots());

        User user = App.getInstance().getLoggedInUser();
        if (user != null) {
            user.setCoins(response.getCoins());
            user.setGems(response.getGems());
            user.setGreenHouse(greenHouse);
        }
    }

    public static synchronized void replaceWith(
            List<GreenHousePotDto> pots
    ) {
        GreenHouse replacement = new GreenHouse();

        for (int row = 1; row <= GreenHouse.ROWS; row++) {
            for (int column = 1; column <= GreenHouse.COLUMNS; column++) {
                FlowerPot pot = replacement.getPot(row, column);
                pot.setUnlocked(false);
                pot.setPlantId(null);
                pot.setPlantedAt(null);
            }
        }

        if (pots != null) {
            for (GreenHousePotDto dto : pots) {
                if (!valid(dto)) {
                    continue;
                }

                FlowerPot pot = replacement.getPot(
                        dto.getRow(),
                        dto.getColumn()
                );
                pot.setUnlocked(dto.isUnlocked());
                pot.setPlantId(dto.getPlantId());
                pot.setPlantedAt(parseDateTime(dto.getPlantedAt()));
            }
        }

        greenHouse = replacement;
        loaded = true;

        User user = App.getInstance().getLoggedInUser();
        if (user != null) {
            user.setGreenHouse(greenHouse);
        }
    }

    public static synchronized void unlockPot(
            int row,
            int column
    ) {
        if (!loaded || greenHouse == null
                || row < 1 || row > GreenHouse.ROWS
                || column < 1 || column > GreenHouse.COLUMNS) {
            return;
        }

        greenHouse.getPot(row, column).setUnlocked(true);
    }

    public static synchronized FlowerPot getPot(
            int row,
            int column
    ) {
        if (!loaded || greenHouse == null
                || row < 1 || row > GreenHouse.ROWS
                || column < 1 || column > GreenHouse.COLUMNS) {
            return null;
        }
        return greenHouse.getPot(row, column);
    }

    public static synchronized GreenHouse getGreenHouse() {
        return loaded ? greenHouse : null;
    }

    public static synchronized boolean isLoaded() {
        return loaded;
    }

    public static synchronized void clear() {
        loaded = false;
        greenHouse = null;
    }

    private static boolean valid(GreenHousePotDto dto) {
        return dto != null
                && dto.getRow() >= 1
                && dto.getRow() <= GreenHouse.ROWS
                && dto.getColumn() >= 1
                && dto.getColumn() <= GreenHouse.COLUMNS;
    }

    private static LocalDateTime parseDateTime(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            return LocalDateTime.parse(value);
        } catch (DateTimeParseException ignored) {
            return null;
        }
    }
}
