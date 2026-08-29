package network.protocol.gameplay;

import models.enums.LootType;

public class LootCollectResponse {
    private boolean success;
    private String message;
    private LootType type;
    private int total;
    private int unlockedRow;
    private int unlockedColumn;

    public LootCollectResponse() {
    }

    public LootCollectResponse(
            boolean success,
            String message,
            LootType type,
            int total,
            int unlockedRow,
            int unlockedColumn
    ) {
        this.success = success;
        this.message = message;
        this.type = type;
        this.total = total;
        this.unlockedRow = unlockedRow;
        this.unlockedColumn = unlockedColumn;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(
            boolean success
    ) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(
            String message
    ) {
        this.message = message;
    }

    public LootType getType() {
        return type;
    }

    public void setType(
            LootType type
    ) {
        this.type = type;
    }

    public int getTotal() {
        return total;
    }

    public void setTotal(
            int total
    ) {
        this.total = total;
    }

    public int getUnlockedRow() {
        return unlockedRow;
    }

    public void setUnlockedRow(
            int unlockedRow
    ) {
        this.unlockedRow = unlockedRow;
    }

    public int getUnlockedColumn() {
        return unlockedColumn;
    }

    public void setUnlockedColumn(
            int unlockedColumn
    ) {
        this.unlockedColumn = unlockedColumn;
    }
}
