package network.protocol.news;

public class ZombieDiscoverRequest {
    private String zombieAlias;

    public ZombieDiscoverRequest() {
    }

    public ZombieDiscoverRequest(String zombieAlias) {
        this.zombieAlias = zombieAlias;
    }

    public String getZombieAlias() {
        return zombieAlias;
    }

    public void setZombieAlias(String zombieAlias) {
        this.zombieAlias = zombieAlias;
    }
}
