package models.minigames.vaseBreaker;
import lombok.Getter;
import lombok.Setter;

import java.util.Objects;

@Getter
@Setter
public class Vase {
    private final VaseType vaseType;
    private final VaseContentType contentType;
    private final int x;
    private final int y;
    private final String plantName;
    private final String zombieAlias;
    private boolean broken;

    private Vase(VaseType vaseType,
                 VaseContentType contentType,
                 int x,
                 int y,
                 String plantName,
                 String zombieAlias) {
        if (x < 1 || x > 9 || y < 1 || y > 5) {
            throw new IllegalArgumentException("Vase coordinates are outside the board.\n");
        }
        this.vaseType = Objects.requireNonNull(vaseType);
        this.contentType = Objects.requireNonNull(contentType);
        this.x = x;
        this.y = y;
        this.plantName = plantName;
        this.zombieAlias = zombieAlias;
        validateContent();
    }

    public static Vase empty(int x, int y) {
        return new Vase(VaseType.SIMPLE, VaseContentType.EMPTY, x, y, null, null);
    }

    public static Vase simpleSeedPacket(int x, int y, String plantName) {
        return new Vase(VaseType.SIMPLE, VaseContentType.SEED_PACKET,
                x, y, Objects.requireNonNull(plantName), null);
    }

    public static Vase simpleZombie(int x, int y, String zombieAlias) {
        return new Vase(VaseType.SIMPLE, VaseContentType.ZOMBIE,
                x, y, null, Objects.requireNonNull(zombieAlias));
    }

    public static Vase plantVase(int x, int y, String plantName) {
        return new Vase(VaseType.PLANT, VaseContentType.SEED_PACKET,
                x, y, Objects.requireNonNull(plantName), null);
    }

    public static Vase gargantuarVase(int x, int y, String gargantuarAlias) {
        return new Vase(VaseType.GARGANTUAR, VaseContentType.GARGANTUAR,
                x, y, null, Objects.requireNonNull(gargantuarAlias));
    }

    private void validateContent() {
        if (vaseType == VaseType.PLANT && contentType != VaseContentType.SEED_PACKET) {
            throw new IllegalArgumentException("A plant vase must contain a seed packet.\n");
        }
        if (vaseType == VaseType.GARGANTUAR && contentType != VaseContentType.GARGANTUAR) {
            throw new IllegalArgumentException("A Gargantuar vase must contain a Gargantuar.\n");
        }
        if (contentType == VaseContentType.SEED_PACKET && (plantName == null || plantName.isBlank())) {
            throw new IllegalArgumentException("A seed-packet vase needs a plant name.\n");
        }
        if ((contentType == VaseContentType.ZOMBIE || contentType == VaseContentType.GARGANTUAR) &&
                (zombieAlias == null || zombieAlias.isBlank())) {
            throw new IllegalArgumentException("A zombie vase needs a zombie alias.\n");
        }
    }
    public boolean breakVase() {
        if (broken) {
            return false;
        }
        broken = true;
        return true;
    }

}
