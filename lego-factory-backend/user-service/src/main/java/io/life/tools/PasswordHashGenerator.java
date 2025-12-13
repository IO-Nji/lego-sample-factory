package io.life.tools;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class PasswordHashGenerator {
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(12);
        
        System.out.println("=== PASSWORD HASH GENERATOR ===");
        
        // Generate hash for lego_Pass
        String legoPass = encoder.encode("lego_Pass");
        System.out.println("lego_admin password (lego_Pass): " + legoPass);
        
        // Generate hash for testPass123
        String testPass = encoder.encode("testPass123");
        System.out.println("Other users password (testPass123): " + testPass);
        
        // Verify hashes work
        System.out.println("\n=== VERIFICATION ===");
        System.out.println("lego_Pass matches: " + encoder.matches("lego_Pass", legoPass));
        System.out.println("testPass123 matches: " + encoder.matches("testPass123", testPass));
    }
}