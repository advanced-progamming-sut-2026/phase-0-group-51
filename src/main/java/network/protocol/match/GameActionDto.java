package network.protocol.match;
public class GameActionDto {

    private GameActionType type;
    private String entityName;
    private int row;
    private int column;
    private String clientActionId;

    public GameActionDto() {
    }

    public GameActionDto(
        GameActionType type,
        String entityName,
        int row,
        int column,
        String clientActionId
    ) {
        this.type = type;
        this.entityName = entityName;
        this.row = row;
        this.column = column;
        this.clientActionId = clientActionId;
    }

    public GameActionType getType() {
        return type;
    }

    public void setType(GameActionType type) {
        this.type = type;
    }

    public String getEntityName() {
        return entityName;
    }

    public void setEntityName(String entityName) {
        this.entityName = entityName;
    }

    public int getRow() {
        return row;
    }

    public void setRow(int row) {
        this.row = row;
    }

    public int getColumn() {
        return column;
    }

    public void setColumn(int column) {
        this.column = column;
    }

    public String getClientActionId() {
        return clientActionId;
    }

    public void setClientActionId(String clientActionId) {
        this.clientActionId = clientActionId;
    }
}
