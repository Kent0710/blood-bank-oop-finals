import java.util.NoSuchElementException;
import java.util.Scanner;

/**
 * Controller managing Logistics operations, dispatches, and cold-chain intake transport.
 */
public class LogisticsController {
    private final LogisticsService logisticsService;
    private final CentralInventory centralInventory;

    public LogisticsController() {
        this.logisticsService = LogisticsService.getInstance();
        this.centralInventory = CentralInventory.getInstance();
    }

    public static void handleLogisticsMenu(Scanner scanner, LogisticsManager logisticsUser) {
        new LogisticsController().handleLogisticsMenu(scanner, logisticsUser, InspectionService.getInstance());
    }

    public static void showDashboard(Scanner scanner, LogisticsManager logisticsUser) {
        new LogisticsController().handleLogisticsMenu(scanner, logisticsUser, InspectionService.getInstance());
    }

    public static void showAdminDashboard(Scanner scanner) {
        LogisticsService.getInstance().displayQueues();
    }

    public void handleLogisticsMenu(Scanner scanner, LogisticsManager logisticsUser, InspectionService inspectionService) {
        if (scanner == null) scanner = new Scanner(System.in);
        String name = (logisticsUser != null && logisticsUser.getName() != null) ? logisticsUser.getName() : "Logistics Dispatcher";
        String id = (logisticsUser != null && logisticsUser.getAccountId() != null) ? logisticsUser.getAccountId() : "LG-3001";

        Runnable printLogisticsMenu = () -> {
            System.out.println("+-----------------------------------------------------------------------------------+");
            System.out.printf("| %-81s |\n", "LOGISTICS DISPATCH HUB: " + truncate(name + " [ID: " + id + "]", 56));
            System.out.println("+-----------------------------------------------------------------------------------+");
            System.out.println("| [1] Process Scheduled Distribution (Blood Bank -> Hospital)                       |");
            System.out.println("| [2] Process Urgent Distribution (Blood Bank -> Hospital - Priority Queue)         |");
            System.out.println("| [3] Collection / Intake Transport (Hospital or Org -> Blood Bank)                 |");
            System.out.println("| [4] View Logistics Queues & Transport Records (Read)                              |");
            System.out.println("| [5] Log out                                                                       |");
            System.out.println("+-----------------------------------------------------------------------------------+");
        };

        boolean inLogisticsMenu = true;
        while (inLogisticsMenu) {
            try {
                System.out.println();
                printLogisticsMenu.run();

                int choice = InputValidator.readInteger(scanner, "Enter choice (1-5): ", 1, 5, printLogisticsMenu);

                switch (choice) {
                    case 1:
                        logisticsService.processScheduledDistribution(centralInventory);
                        break;
                    case 2:
                        logisticsService.processUrgentDistribution(centralInventory);
                        break;
                    case 3:
                        PipelineHelper.executeFullProcessingPipeline(scanner, "External Intake", centralInventory, inspectionService, logisticsService);
                        break;
                    case 4:
                        logisticsService.displayQueues();
                        break;
                    case 5:
                        System.out.println("Logging out from Logistics...");
                        inLogisticsMenu = false;
                        break;
                }
            } catch (NoSuchElementException e) {
                break;
            } catch (Exception e) {
                System.out.println("\n  [Logistics Notice] An unexpected error occurred: " + e.getMessage());
                System.out.println("  Returning to logistics hub menu...");
            }
        }
    }

    private static String truncate(String text, int maxLen) {
        if (text == null) return "";
        if (text.length() <= maxLen) return text;
        return text.substring(0, maxLen - 3) + "...";
    }
}
