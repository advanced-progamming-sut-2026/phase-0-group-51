package network.protocol.gameplay;

import models.enums.LootType;

public class LootCollectRequest {
    private LootType type;

    public LootCollectRequest() {
    }

    public LootCollectRequest(
            LootType type
    ) {
        this.type = type;
    }

    public LootType getType() {
        return type;
    }

    public void setType(
            LootType type
    ) {
        this.type = type;
    }
}
