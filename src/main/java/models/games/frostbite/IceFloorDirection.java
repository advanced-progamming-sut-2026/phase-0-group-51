package models.games.frostbite;

public enum IceFloorDirection {
    UP(-1),
    DOWN(1);

    private final int laneDelta;

    IceFloorDirection(int laneDelta) {
        this.laneDelta = laneDelta;
    }

    public int targetLane(int currentLane, int laneCount) {
        int target = currentLane + laneDelta;
        if (target < 0 || target >= laneCount) {
            return currentLane;
        }
        return target;
    }
}
