package network.server.service;

import controllers.HashUtil;

import java.security.SecureRandom;
import java.util.Base64;

public final class AuthTokenUtil {

    private static final SecureRandom RANDOM =
            new SecureRandom();

    private AuthTokenUtil() {
    }

    public static String generateToken() {
        byte[] bytes = new byte[32];

        RANDOM.nextBytes(bytes);

        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(bytes);
    }

    public static String hashToken(String token) {
        return HashUtil.hashPassword(token);
    }
}