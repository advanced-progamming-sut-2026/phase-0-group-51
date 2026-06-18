package controllers;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
public class HashUtil {
        public static String hashPassword(String password) {
            try {
                MessageDigest digest =
                        MessageDigest.getInstance("SHA-256");

                byte[] hash =
                        digest.digest(password.getBytes(StandardCharsets.UTF_8));

                StringBuilder sb = new StringBuilder();

                for (byte b : hash) {
                    sb.append(String.format("%02x", b));
                }

                return sb.toString();

            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

