/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package utility;

/**
 *
 * @author Whrl
 */
import java.util.Scanner;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class InputUtil {
    public static int getIntInput(Scanner scanner, String prompt) {
        int value;
        while (true) {
            System.out.print(prompt);
            try {
                value = scanner.nextInt();
                scanner.nextLine(); // consume newline
                break;
            } catch (java.util.InputMismatchException e) {
                System.out.println("Invalid input. Please enter a number.");
                scanner.nextLine(); // consume invalid input
            }
        }
        return value;
    }

    public static String getInput(Scanner scanner, String prompt) {
        System.out.print(prompt);
        return scanner.nextLine().trim();
    }

    public static String getNonEmptyInput(Scanner scanner, String prompt) {
        while (true) {
            String v = getInput(scanner, prompt);
            if (!v.isEmpty()) return v;
            System.out.println("Value cannot be empty. Try again.");
        }
    }

    // Generic M/F choice returning mapped full word
    public static String getMFChoice(Scanner scanner, String prompt, String mMeaning, String fMeaning) {
        while (true) {
            String v = getInput(scanner, prompt + " (M/F): ").toUpperCase();
            if (v.equals("M")) return mMeaning;
            if (v.equals("F")) return fMeaning;
            System.out.println("Please enter only M or F.");
        }
    }

    // Email validation (simple RFC-lite pattern)
    public static String getValidatedEmail(Scanner scanner, String prompt) {
        while (true) {
            String v = getInput(scanner, prompt);
            if (v.matches("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) return v;
            System.out.println("Invalid email format. Try again.");
        }
    }

    // Phone: digits only 7-15 length
    public static String getValidatedPhone(Scanner scanner, String prompt) {
        while (true) {
            String v = getInput(scanner, prompt);
            if (v.matches("^[0-9]{7,15}$")) return v;
            System.out.println("Invalid phone (digits 7-15). Try again.");
        }
    }

    // Date: yyyy-MM-dd
    public static String getValidatedDate(Scanner scanner, String prompt) {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        while (true) {
            String v = getInput(scanner, prompt);
            try { LocalDate.parse(v, fmt); return v; } catch (DateTimeParseException e) { System.out.println("Invalid date (use yyyy-MM-dd). Try again."); }
        }
    }
    
    /**
     * Clears the screen for better UI experience
     * Works on both Windows and Unix-based systems
     */
    public static void clearScreen() {
        try {
            // Check if running on Windows
            String os = System.getProperty("os.name").toLowerCase();
            if (os.contains("windows")) {
                // For Windows
                new ProcessBuilder("cmd", "/c", "cls").inheritIO().start().waitFor();
            } else {
                // For Unix/Linux/Mac
                new ProcessBuilder("clear").inheritIO().start().waitFor();
            }
        } catch (Exception e) {
            // Fallback: print multiple newlines
            for (int i = 0; i < 50; i++) {
                System.out.println();
            }
        }
    }
    
    /**
     * Pauses execution and waits for user to press Enter
     * @param message Custom message to display (optional)
     */
    public static void pauseScreen(String message) {
        System.out.println(message != null ? message : "Press Enter to continue...");
        try {
            System.in.read();
        } catch (Exception e) {
            // Ignore exception
        }
    }
    
    /**
     * Pauses execution with default message
     */
    public static void pauseScreen() {
        pauseScreen(null);
    }
} 
