package org.example.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;

public class SessionIdGenerator {
    public static String generate(String userId, String pageUrl, Instant timestamp) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            String base = userId + "|" + pageUrl + "|" + (timestamp.getEpochSecond() / (1000 * 60));
            byte[] h = md.digest(base.getBytes(StandardCharsets.UTF_8));
            return userId + "-" + HexFormat.of().formatHex(h).substring(0, 12);
        } catch (Exception e) {
            return userId + "-" + timestamp;
        }
    }
}
