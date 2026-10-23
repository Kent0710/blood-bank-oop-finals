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
                        handleCreateAccount(sc);
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
     * Public account creation flow.
     * Selects account type: [1] Blood Bank (Super Admin) or [2] Patient.
     */
    public static void handleCreateAccount(Scanner sc) {
        UserService userService = UserService.getInstance();
        Runnable printCreateMenu = () -> {
            System.out.println("--- Create Account ---");
            System.out.println("Select account type to register:");
            System.out.println("[1] Blood Bank (Super Admin)");
            System.out.println("[2] Patient");
            System.out.println("Note: Hospital Admin and Logistics Manager accounts are employee roles");
            System.out.println("      and must be provisioned by an authorized Blood Bank Super Admin.");
        };

        System.out.println();
        printCreateMenu.run();
        int typeChoice = InputValidator.readInteger(sc, "Enter choice (1-2): ", 1, 2, printCreateMenu);

        String name = InputValidator.readValidName(sc, "Enter full name: ");

        User newUser = null;
        switch (typeChoice) {
            case 1:
                String bbPass = InputValidator.readValidPassword(sc, "Set Account Password: ");
                String generatedBbId = userService.generateUniqueAccountId(Roles.SUPER_ADMIN);
                BloodBank bb = new BloodBank(generatedBbId, name, bbPass);
                userService.registerUser(bb);
                newUser = bb;

                printAccountCreationReceipt(bb.getAccountId(), bb.getName(), "Blood Bank Super Admin");
                break;
            case 2:
                BloodType bt = promptBloodType(sc);
                String patPass = InputValidator.readValidPassword(sc, "Set Account Password: ");
                String generatedPatId = userService.generateUniqueAccountId(Roles.PATIENT);
                Patient patient = new Patient(generatedPatId, name, patPass, bt);
                userService.registerUser(patient);
                newUser = patient;

                printAccountCreationReceipt(patient.getAccountId(), patient.getName(), "Patient (" + bt.getLabel() + ")");
                break;
        }

        if (newUser != null) {
            System.out.println("\n>> Successfully signed in as " + newUser.getName() + " [" + newUser.getAccountId() + "]!");
            System.out.println("Privileges: " + newUser.getRoleDescription());
            routeToRoleDashboard(sc, newUser);
        }
    }

    /**
     * Self-service public registration for walk-in patients (delegates to handleCreateAccount).
     */
    public static void handlePublicRegistration(Scanner sc) {
        handleCreateAccount(sc);
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
        return PipelineHelper.promptBloodType(sc);
    }

    private static void printAccountCreationReceipt(String accountId, String name, String roleTitle) {
        System.out.println("\n+-----------------------------------------------------------------------------------+");
        System.out.println("|                            ACCOUNT CREATED SUCCESSFULLY                           |");
        System.out.println("+----------------------+------------------------------------------------------------+");
        System.out.printf("| %-20s | %-58s |\n", "Generated Account ID", accountId);
        System.out.printf("| %-20s | %-58s |\n", "Account Holder", name);
        System.out.printf("| %-20s | %-58s |\n", "Role Classification", roleTitle);
        System.out.println("+----------------------+------------------------------------------------------------+");
        System.out.printf("| %-81s |\n", ">> IMPORTANT: Remember your Account ID (" + accountId + ") to sign in later!");
        System.out.println("+-----------------------------------------------------------------------------------+");
    }

    private static void printAccountCreationReceipt(String accountId, String name, BloodType bloodType) {
        printAccountCreationReceipt(accountId, name, "Patient (" + (bloodType != null ? bloodType.getLabel() : "N/A") + ")");
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

    public void navigateRoleDashboard(Scanner scanner, User user) {
        routeToRoleDashboard(scanner, user);
    }
}
