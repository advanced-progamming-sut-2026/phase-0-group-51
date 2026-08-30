package network.server.service;

import network.server.presence.ConnectionRegistry;

import java.util.Objects;

public class ReactionService {
    private final ConnectionRegistry connectionRegistry;

    public ReactionService(ConnectionRegistry connectionRegistry) {
        this.connectionRegistry =
                Objects.requireNonNull(
                        connectionRegistry,
                        "connectionRegistry cannot be null"
                );
    }

    /*
     * Logic اصلی Reaction را در مرحله P3.9
     * بعد از Active Match integration کامل می‌کنیم.
     */

    public void handleDisconnect(String username) {
        // فعلاً چیزی برای cleanup نداریم.
        // بعداً rate-limit state این user اینجا پاک می‌شود.
    }
}
