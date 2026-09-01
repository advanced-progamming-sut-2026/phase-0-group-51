package network.protocol.match;

public class ProjectileNetState {

    private int entityId;
    private String type;
    private double x;
    private double y;
    private int sourcePlantEntityId;
    private String sourcePlantName;
    private int sourceRow;
    private int sourceColumn;
    private int visualReleaseId;
    private boolean launched;
    private double visualArcOffset;
    private Double targetX;
    private Double targetY;

    public ProjectileNetState() {
    }

    public ProjectileNetState(int entityId, String type, double x, double y) {
        this(entityId, type, x, y, 0, null, -1, -1,
            0, true, 0.0, null, null);
    }

    public ProjectileNetState(
            int entityId,
            String type,
            double x,
            double y,
            int sourcePlantEntityId,
            String sourcePlantName,
            int sourceRow,
            int sourceColumn,
            int visualReleaseId,
            boolean launched,
            double visualArcOffset,
            Double targetX,
            Double targetY
    ) {
        this.entityId = entityId;
        this.type = type;
        this.x = x;
        this.y = y;
        this.sourcePlantEntityId = sourcePlantEntityId;
        this.sourcePlantName = sourcePlantName;
        this.sourceRow = sourceRow;
        this.sourceColumn = sourceColumn;
        this.visualReleaseId = visualReleaseId;
        this.launched = launched;
        this.visualArcOffset = visualArcOffset;
        this.targetX = targetX;
        this.targetY = targetY;
    }

    public int getEntityId() { return entityId; }
    public void setEntityId(int entityId) { this.entityId = entityId; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public double getX() { return x; }
    public void setX(double x) { this.x = x; }

    public double getY() { return y; }
    public void setY(double y) { this.y = y; }

    public int getSourcePlantEntityId() { return sourcePlantEntityId; }
    public void setSourcePlantEntityId(int sourcePlantEntityId) { this.sourcePlantEntityId = sourcePlantEntityId; }

    public String getSourcePlantName() { return sourcePlantName; }
    public void setSourcePlantName(String sourcePlantName) { this.sourcePlantName = sourcePlantName; }

    public int getSourceRow() { return sourceRow; }
    public void setSourceRow(int sourceRow) { this.sourceRow = sourceRow; }

    public int getSourceColumn() { return sourceColumn; }
    public void setSourceColumn(int sourceColumn) { this.sourceColumn = sourceColumn; }

    public int getVisualReleaseId() { return visualReleaseId; }
    public void setVisualReleaseId(int visualReleaseId) { this.visualReleaseId = visualReleaseId; }

    public boolean isLaunched() { return launched; }
    public void setLaunched(boolean launched) { this.launched = launched; }

    public double getVisualArcOffset() { return visualArcOffset; }
    public void setVisualArcOffset(double visualArcOffset) { this.visualArcOffset = visualArcOffset; }

    public Double getTargetX() { return targetX; }
    public void setTargetX(Double targetX) { this.targetX = targetX; }

    public Double getTargetY() { return targetY; }
    public void setTargetY(Double targetY) { this.targetY = targetY; }
}
