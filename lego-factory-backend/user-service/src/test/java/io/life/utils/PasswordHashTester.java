package io.life.utils;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class PasswordHashTester {
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(10); // Strength 10 to match existing
        
        // Test the existing hash vs lego_Pass
        String existingHash = "$2a$10$dXJ3SW6G7P2DeApd.6Q/zOxzfHCaoxRtLS9a5x5CgKkO3CkHjFaG.";
        String testPassword = "lego_Pass";
        
        System.out.println("Testing existing hash:");
        System.out.println("Password: " + testPassword);
        System.out.println("Hash: " + existingHash);
        System.out.println("Matches: " + encoder.matches(testPassword, existingHash));
        
        // Generate new hash for lego_Pass
        String newHash = encoder.encode(testPassword);
        System.out.println("\nGenerated new hash for '" + testPassword + "':");
        System.out.println("Hash: " + newHash);
        System.out.println("Matches: " + encoder.matches(testPassword, newHash));
        
        // Test hash for testPass123
        String testPass123Hash = "$2a$10$N9qo8uLOickgx2ZfVB00ue6WpKBdkJOIPY5uUskFBSC/SkLAJWYei";
        System.out.println("\nTesting testPass123 hash:");
        System.out.println("Password: testPass123");
        System.out.println("Hash: " + testPass123Hash);
        System.out.println("Matches: " + encoder.matches("testPass123", testPass123Hash));
    }
}