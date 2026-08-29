package network.protocol.match;

public class BrainNetState {

    private int lane;
    private boolean eaten;

    public BrainNetState() {
    }

    public BrainNetState(int lane, boolean eaten) {
        this.lane = lane;
        this.eaten = eaten;
    }

    public int getLane() { return lane; }
    public void setLane(int lane) { this.lane = lane; }

    public boolean isEaten() { return eaten; }
    public void setEaten(boolean eaten) { this.eaten = eaten; }
}
