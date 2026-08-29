package network.client;

import java.util.prefs.Preferences;

public final class ClientSessionTokenStore {

    private static final String TOKEN_KEY =
            "pvz-server-session-token";

    private static final Preferences PREFERENCES =
            Preferences.userNodeForPackage(
                    ClientSessionTokenStore.class
            );

    private ClientSessionTokenStore() {
    }

    public static void save(String token) {
        if (token == null || token.isBlank()) {
            clear();
            return;
        }

        PREFERENCES.put(
                TOKEN_KEY,
                token
        );
    }

    public static String load() {
        String token =
                PREFERENCES.get(
                        TOKEN_KEY,
                        null
                );

        if (token == null || token.isBlank()) {
            return null;
        }

        return token;
    }

    public static void clear() {
        PREFERENCES.remove(TOKEN_KEY);
    }

    public static boolean hasToken() {
        return load() != null;
    }
}