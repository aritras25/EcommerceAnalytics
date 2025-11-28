package org.example.util;

import org.junit.jupiter.api.Test;
import java.time.Instant;
import static org.junit.jupiter.api.Assertions.*;

class SessionIdGeneratorTest {
    @Test
    void generate_shouldReturnConsistentId() {
        String id1 = SessionIdGenerator.generate("user1", "/home", Instant.ofEpochSecond(1000000));
        String id2 = SessionIdGenerator.generate("user1", "/home", Instant.ofEpochSecond(1000000));
        assertEquals(id1, id2);
    }

    @Test
    void generate_shouldHandleException() {
        String id = SessionIdGenerator.generate("user1", null, Instant.now());
        assertTrue(id.startsWith("user1-"));
    }
}

