package network.server.match;

public interface PlayerChannel {

    String playerId();

    void send(Object message);
}
