package network.protocol.match;

public class PlantNetState {

    private int entityId;
    private String name;
    private int row;
    private int column;
    private int hp;
    private int maxHp;
    private int level;

    public PlantNetState() {
    }

    public PlantNetState(int entityId, String name, int row, int column,
                         int hp, int maxHp, int level) {
        this.entityId = entityId;
        this.name = name;
        this.row = row;
        this.column = column;
        this.hp = hp;
        this.maxHp = maxHp;
        this.level = level;
    }

    public int getEntityId() { return entityId; }
    public void setEntityId(int entityId) { this.entityId = entityId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public int getRow() { return row; }
    public void setRow(int row) { this.row = row; }

    public int getColumn() { return column; }
    public void setColumn(int column) { this.column = column; }

    public int getHp() { return hp; }
    public void setHp(int hp) { this.hp = hp; }

    public int getMaxHp() { return maxHp; }
    public void setMaxHp(int maxHp) { this.maxHp = maxHp; }

    public int getLevel() { return level; }
    public void setLevel(int level) { this.level = level; }
}
