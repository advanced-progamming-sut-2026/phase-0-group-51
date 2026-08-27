package network.client;

import network.protocol.NetworkMessage;

@FunctionalInterface
public interface ServerEventListener {
    void onServerEvent(NetworkMessage message);
}
