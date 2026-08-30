package network.protocol.match;

public class ProjectileNetState {

    private int entityId;
    private String type;
    private double x;
    private double y;

    public ProjectileNetState() {
    }

    public ProjectileNetState(int entityId, String type, double x, double y) {
        this.entityId = entityId;
        this.type = type;
        this.x = x;
        this.y = y;
    }

    public int getEntityId() { return entityId; }
    public void setEntityId(int entityId) { this.entityId = entityId; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public double getX() { return x; }
    public void setX(double x) { this.x = x; }

    public double getY() { return y; }
    public void setY(double y) { this.y = y; }
}
