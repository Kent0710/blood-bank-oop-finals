import java.util.NoSuchElementException;
import java.util.Scanner;

import Role.User;

/**
 * Application Entry Point for Blood Bank Management System.
 * Coordinates system startup and main menu navigation loop.
 */
public class Main {

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        System.out.println("=================================================================");
        System.out.println("   CENTRAL BLOOD BANK MANAGEMENT & LOGISTICS SYSTEM (OOP/GoF)    ");
        System.out.println("=================================================================");

        Runnable printMainMenu = () -> {
            System.out.println("\n+----------------------- MAIN SYSTEM MENU -----------------------+");
            System.out.println("| [1] Sign in (Account ID & Password)                            |");
            System.out.println("| [2] Create account (Public Registration)                       |");
            System.out.println("| [3] Exit                                                       |");
            System.out.println("+----------------------------------------------------------------+");
        };

        boolean isRunning = true;
        while (isRunning) {
            try {
                printMainMenu.run();
                int choice = InputValidator.readInt(scanner, "Enter choice (1-3): ", 1, 3, printMainMenu);

                switch (choice) {
                    case 1:
                        System.out.println(">> Sign In selected (Pending AuthController in Sprint 3)");
                        break;
                    case 2:
                        System.out.println(">> Create Account selected (Pending AuthController in Sprint 3)");
                        break;
                    case 3:
                        System.out.println("\nThank you for using the Blood Bank System. System terminated safely.");
                        isRunning = false;
                        break;
                    default:
                        break;
                }
            } catch (NoSuchElementException e) {
                // Input stream closed (EOF or test execution)
                break;
            } catch (Exception e) {
                System.out.println("\n  [Application Notice] An unexpected error occurred: " + e.getMessage());
                System.out.println("  Returning to main menu...");
            }
        }

        scanner.close();
    }

    //Print User Profile Badge
    private static void printUserProfile(User user) {
        String border = "+" + "-".repeat(15) + "+" + "-".repeat(67) + "+";

        System.out.println(border);
        System.out.printf("| %-80s |%n", "USER PROFILE BADGE");
        System.out.println(border);

        printRow("Account ID", user.getId());
        printRow("Full Name", user.getFullName());
        printRow("Role", user.getRole().toString());
        printRow("Affiliation", user.getAffiliation());
        printRow("Account Status", user.getAccountStatus().toString());

        System.out.println(border);
    }

    private static void printRow(String label, String value) {
        System.out.printf("| %-13s | %-65s |%n", label, value);
    }

}
