package com.xhulife.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PasswordEncoderTest {
    @Test void encodedPasswordMatchesOriginal() {
        String encoded = PasswordEncoder.encode("admin123");
        assertNotEquals("admin123", encoded);
        assertTrue(PasswordEncoder.matches(encoded, "admin123"));
        assertFalse(PasswordEncoder.matches(encoded, "wrong-password"));
    }
    @Test void nullValuesNeverMatch() {
        assertFalse(PasswordEncoder.matches(null, "password"));
        assertFalse(PasswordEncoder.matches("value", null));
    }
}
