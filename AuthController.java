import java.lang.reflect.Method;
import java.util.NoSuchElementException;
import java.util.Scanner;

/**
 * AuthController coordinates system entry, user authentication,
 * self-service public registration for patients, and role dashboard routing.
 */
public class AuthController {

    private final UserService userService;

    public AuthController() {
        this.userService = UserService.getInstance();
    }

    /**
     * Displays Main System Menu and manages the authentication loop.
     * Options: [1] Sign in, [2] Create account, [3] Exit.
     */
    public static void startAuthFlow(Scanner sc) {
        if (sc == null) {
            sc = new Scanner(System.in);
        }

        Runnable printMainMenu = () -> {
            System.out.println("+----------------------- MAIN SYSTEM MENU -----------------------+");
            System.out.println("| [1] Sign in (Account ID & Password)                            |");
            System.out.println("| [2] Create account (Public Registration)                       |");
            System.out.println("| [3] Exit                                                       |");
            System.out.println("+----------------------------------------------------------------+");
        };

        boolean isRunning = true;
        while (isRunning) {
            try {
                System.out.println();
                printMainMenu.run();
                int choice = InputValidator.readInt(sc, "Enter choice (1-3): ", 1, 3, printMainMenu);

                switch (choice) {
                    case 1:
                        handleLogin(sc);
                        break;
                    case 2:
                        handlePublicRegistration(sc);
                        break;
                    case 3:
                        System.out.println("\nThank you for using the Central Blood Bank System. Goodbye!");
                        isRunning = false;
                        break;
                }
            } catch (NoSuchElementException e) {
                // Input stream terminated (EOF / non-interactive test)
                break;
            } catch (Exception e) {
                System.out.println("\n  [Application Notice] An unexpected error occurred: " + e.getMessage());
                System.out.println("  Returning to main menu...");
            }
        }
    }

    /**
     * Prompts for Account ID and Password, validates credentials via UserService,
     * rejects archived/disabled accounts, and routes to the user's role dashboard.
     */
    public static void handleLogin(Scanner sc) {
        System.out.println("\n>>> SIGN IN");
        UserService userService = UserService.getInstance();

        if (userService.isEmpty()) {
            System.out.println("  [Notice] No registered accounts exist in the system yet.");
            System.out.println("  Please select [2] Create account from the Main Menu to register a Patient account.");
            return;
        }

        String accountId = InputValidator.readValidAccountId(sc, "Account ID: ");
        String password = InputValidator.readValidPassword(sc, "Password: ");

        try {
            User user = userService.authenticate(accountId, password);

            if (user.isArchived() || !user.isActive()) {
                System.out.println("  [Access Denied] Account '" + accountId + "' is disabled/archived. Access denied.");
                return;
            }

            System.out.println("[SUCCESS] Welcome back, " + user.getName() + " (" + user.getRole() + ")!");
            routeToRoleDashboard(sc, user);
        } catch (UserNotFoundException | AuthenticationException e) {
            System.out.println("  [Authentication Error] " + e.getMessage());
        }
    }

    /**
     * Self-service public registration for walk-in patients.
     * Collects patient name, blood type, and password, generates a PA-xxxx ID,
     * and registers the new Patient account.
     */
    public static void handlePublicRegistration(Scanner sc) {
        System.out.println("\n>>> CREATE ACCOUNT (Public Registration)");
        System.out.println("Self-service walk-in patient registration.");

        String name = InputValidator.readValidName(sc, "Enter full name: ");
        BloodType bloodType = promptBloodType(sc);
        String password = InputValidator.readValidPassword(sc, "Set password: ");

        Patient patient = registerPatient(name, bloodType, password);
        printAccountCreationReceipt(patient.getId(), patient.getName(), bloodType);

        System.out.println("\n>> Successfully signed in as " + patient.getName() + " [" + patient.getId() + "]!");
        routeToRoleDashboard(sc, patient);
    }

    /**
     * Helper to programmatically register a patient account without console prompting.
     */
    public static Patient registerPatient(String name, BloodType bloodType, String password) {
        UserService userService = UserService.getInstance();
        String generatedId = userService.generateId(Roles.PATIENT);
        Patient patient = new Patient(generatedId, name, "Walk-in", password, bloodType);
        userService.registerUser(patient);
        return patient;
    }

    /**
     * Programmatic credential authentication helper.
     */
    public static User login(String accountId, String password) throws UserNotFoundException, AuthenticationException {
        UserService userService = UserService.getInstance();
        User user = userService.authenticate(accountId, password);
        if (user.isArchived() || !user.isActive()) {
            throw new AuthenticationException("Account '" + accountId + "' is disabled/archived. Access denied.");
        }
        return user;
    }

    /**
     * Dispatches authenticated users to their corresponding role dashboard.
     * Uses reflection where modular role controllers are being developed in parallel.
     */
    public static void routeToRoleDashboard(Scanner sc, User user) {
        if (user == null || user.getRole() == null) return;

        switch (user.getRole()) {
            case SUPER_ADMIN:
                invokeControllerMethod("SuperAdminController", "handleBloodBankSuperAdminMenu", sc, user, BloodBank.class);
                break;
            case HOSPITAL_ADMIN:
                invokeControllerMethod("HospitalAdminController", "handleHospitalAdminMenu", sc, user, HospitalAdmin.class);
                break;
            case LOGISTICS:
                invokeLogisticsController(sc, user);
                break;
            case PATIENT:
                invokeControllerMethod("PatientController", "handlePatientMenu", sc, user, Patient.class);
                break;
        }
    }

    /**
     * Prompts and validates an ABO/Rh blood type from console input.
     */
    public static BloodType promptBloodType(Scanner sc) {
        BloodType bloodType = null;
        while (bloodType == null) {
            System.out.print("Enter ABO/Rh Blood Type (e.g. O+, A-, B+, AB-): ");
            String input = sc.nextLine();
            if (input == null) continue;
            input = input.trim();

            if (input.isEmpty()) {
                System.out.println("  [Validation Error] Blood type cannot be empty.");
                continue;
            }

            try {
                bloodType = BloodType.fromString(input);
            } catch (IllegalArgumentException e) {
                System.out.println("  [Validation Error] Invalid blood type: '" + input + "'. (Allowed: O+, O-, A+, A-, B+, B-, AB+, AB-)");
            }
        }
        return bloodType;
    }

    private static void printAccountCreationReceipt(String accountId, String name, BloodType bloodType) {
        System.out.println("\n+-----------------------------------------------------------------------------------+");
        System.out.println("|                            ACCOUNT CREATED SUCCESSFULLY                           |");
        System.out.println("+----------------------+------------------------------------------------------------+");
        System.out.printf("| %-20s | %-58s |\n", "Generated Account ID", accountId);
        System.out.printf("| %-20s | %-58s |\n", "Account Holder", name);
        System.out.printf("| %-20s | %-58s |\n", "Blood Type", (bloodType != null ? bloodType.getCode() : "N/A"));
        System.out.printf("| %-20s | %-58s |\n", "Role Classification", "Patient");
        System.out.println("+----------------------+------------------------------------------------------------+");
        System.out.printf("| %-81s |\n", ">> IMPORTANT: Remember your Account ID (" + accountId + ") to sign in later!");
        System.out.println("+-----------------------------------------------------------------------------------+");
    }

    private static void invokeControllerMethod(String className, String methodName, Scanner sc, User user, Class<?> userClass) {
        try {
            Class<?> clazz = Class.forName(className);
            Object controller = clazz.getDeclaredConstructor().newInstance();
            Method method = clazz.getMethod(methodName, Scanner.class, userClass);
            method.invoke(controller, sc, user);
        } catch (ClassNotFoundException e) {
            printDashboardPlaceholder(user);
        } catch (Exception e) {
            System.out.println("  [Controller Notice] " + e.getMessage());
        }
    }

    private static void invokeLogisticsController(Scanner sc, User user) {
        try {
            Class<?> clazz = Class.forName("LogisticsController");
            Object controller = clazz.getDeclaredConstructor().newInstance();
            try {
                Method method = clazz.getMethod("handleLogisticsMenu", Scanner.class, LogisticsManager.class, InspectionService.class);
                method.invoke(controller, sc, (LogisticsManager) user, InspectionService.getInstance());
            } catch (NoSuchMethodException ex) {
                Method method = clazz.getMethod("handleLogisticsMenu", Scanner.class, LogisticsManager.class);
                method.invoke(controller, sc, (LogisticsManager) user);
            }
        } catch (ClassNotFoundException e) {
            printDashboardPlaceholder(user);
        } catch (Exception e) {
            System.out.println("  [Controller Notice] " + e.getMessage());
        }
    }

    private static void printDashboardPlaceholder(User user) {
        System.out.println("\n================================================================================");
        System.out.println(">>> ACTIVE SESSION: " + user.getName() + " [" + user.getId() + "]");
        System.out.println("Role        : " + user.getRole());
        System.out.println("Description : " + user.getRoleDescription());
        System.out.println("Status      : Authenticated successfully.");
        System.out.println("================================================================================");
    }

    // Instance method aliases for polymorphic / instance-based controllers
    public void handleSignIn(Scanner scanner) {
        handleLogin(scanner);
    }

    public void handleCreateAccount(Scanner scanner) {
        handlePublicRegistration(scanner);
    }

    public void navigateRoleDashboard(Scanner scanner, User user) {
        routeToRoleDashboard(scanner, user);
    }
}
