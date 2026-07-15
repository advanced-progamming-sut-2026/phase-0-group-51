package models.Plant;

import models.games.GameState;

public enum Modifier implements PlantType {
    TORCHWOOD(52),
    HYPNO_SHROOM(54),
    IMITATER(56),
    LILY_PAD(58),
    ENCHANT_MINT(67);

    private final int id;

    Modifier(int id) {
        this.id = id;
    }

    public Plant create() {
        return PlantEnumSupport.create(id, this);
    }

    @Override
    public void onTick(Plant plant, GameState gameState) {
        // Modifier mechanics are implemented as their board interactions are added.
    }
}
