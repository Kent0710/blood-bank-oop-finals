import java.time.LocalDateTime;
import java.time.Month;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.Locale;
import java.util.Scanner;
import java.util.regex.Pattern;

/**
 * Robust terminal input validation engine for Central Blood Bank Management System.
 * Ensures user input adheres to required formats and boundaries without crashing.
 */
public class InputValidator {
    private static final Pattern NAME_PATTERN = Pattern.compile("^[A-Za-z]+([A-Za-z\\s\\.\\-']*[A-Za-z\\.]+)?$");
    private static final Pattern DIN_PATTERN = Pattern.compile("^[A-Za-z0-9][A-Za-z0-9\\-_]{1,24}[A-Za-z0-9]$");
    private static final Pattern ACCOUNT_ID_PATTERN = Pattern.compile("^[A-Za-z0-9\\-_]{3,20}$");

    /**
     * Reads a non-blank string, executing onReprompt callback on failure.
     */
    public static String readNonBlankString(Scanner scanner, String prompt, Runnable onReprompt) {
        boolean isValid = false;
        String result = "";

        while (!isValid) {
            System.out.print(prompt);
            String input = scanner.nextLine();
            if (input == null) continue;
            input = input.trim();

            if (!input.isEmpty()) {
                result = input;
                isValid = true;
            } else {
                System.out.println("  [Validation Error] Input cannot be empty or whitespace only.");
                if (onReprompt != null) {
                    System.out.println();
                    onReprompt.run();
                }
            }
        }
        return result;
    }

    /**
     * Reads a non-blank string without reprompt callback.
     */
    public static String readNonBlankString(Scanner scanner, String prompt) {
        return readNonBlankString(scanner, prompt, null);
    }

    /**
     * Reads a clean non-empty string with leading/trailing whitespace trimmed.
     */
    public static String readNonEmptyString(Scanner scanner, String prompt) {
        return readNonBlankString(scanner, prompt, null);
    }

    /**
     * Validates human or healthcare facility names (letters, spaces, dots, hyphens, apostrophes).
     */
    public static String readValidName(Scanner scanner, String prompt) {
        boolean isValid = false;
        String result = "";

        while (!isValid) {
            System.out.print(prompt);
            String input = scanner.nextLine();
            if (input == null) continue;
            input = input.trim();

            if (input.isEmpty()) {
                System.out.println("  [Validation Error] Name cannot be empty.");
            } else if (input.length() < 2 || input.length() > 60) {
                System.out.println("  [Validation Error] Name must be between 2 and 60 characters long.");
            } else if (NAME_PATTERN.matcher(input).matches()) {
                result = input;
                isValid = true;
            } else {
                System.out.println("  [Validation Error] Invalid characters detected. Names must only contain letters, spaces, dots, or hyphens (no numbers or symbols).");
            }
        }
        return result;
    }

    /**
     * Validates Donation Identification Number (DIN).
     */
    public static String readValidDIN(Scanner scanner, String prompt) {
        boolean isValid = false;
        String result = "";

        while (!isValid) {
            System.out.print(prompt);
            String input = scanner.nextLine();
            if (input == null) continue;
            input = input.trim();

            if (input.isEmpty()) {
                System.out.println("  [Validation Error] DIN cannot be empty.");
            } else if (input.length() < 3 || input.length() > 25) {
                System.out.println("  [Validation Error] DIN must be between 3 and 25 characters long.");
            } else if (DIN_PATTERN.matcher(input).matches()) {
                result = input.toUpperCase();
                isValid = true;
            } else {
                System.out.println("  [Validation Error] DIN format invalid. Use alphanumeric characters and hyphens (e.g., DIN-1001, DIN-9002).");
            }
        }
        return result;
    }

    /**
     * Validates Account ID format (e.g. BB-1001, HA-2001, PAT-4001).
     */
    public static String readValidAccountId(Scanner scanner, String prompt) {
        boolean isValid = false;
        String result = "";

        while (!isValid) {
            System.out.print(prompt);
            String input = scanner.nextLine();
            if (input == null) continue;
            input = input.trim().toUpperCase();

            if (input.isEmpty()) {
                System.out.println("  [Validation Error] Account ID cannot be empty.");
            } else if (input.length() < 3 || input.length() > 20) {
                System.out.println("  [Validation Error] Account ID must be between 3 and 20 characters long.");
            } else if (ACCOUNT_ID_PATTERN.matcher(input).matches()) {
                result = input;
                isValid = true;
            } else {
                System.out.println("  [Validation Error] Account ID format invalid (e.g. BB-1001, HA-2001, PAT-4001).");
            }
        }
        return result;
    }

    /**
     * Validates Password (minimum 3 characters, maximum 50 characters).
     */
    public static String readValidPassword(Scanner scanner, String prompt) {
        boolean isValid = false;
        String result = "";

        while (!isValid) {
            System.out.print(prompt);
            String input = scanner.nextLine();
            if (input == null) continue;
            input = input.trim();

            if (input.isEmpty()) {
                System.out.println("  [Validation Error] Password cannot be empty.");
            } else if (input.length() < 3 || input.length() > 50) {
                System.out.println("  [Validation Error] Password must be between 3 and 50 characters long.");
            } else {
                result = input;
                isValid = true;
            }
        }
        return result;
    }

    /**
     * Reads an integer within a mandatory [min, max] range, re-executing onReprompt if invalid.
     */
    public static int readInt(Scanner scanner, String prompt, int min, int max, Runnable onReprompt) {
        boolean isValid = false;
        int result = min;

        while (!isValid) {
            System.out.print(prompt);
            String input = scanner.nextLine();
            if (input == null) continue;
            input = input.trim();

            if (input.isEmpty()) {
                System.out.println("  [Validation Error] Input cannot be empty. Please enter a valid number.");
                if (onReprompt != null) {
                    System.out.println();
                    onReprompt.run();
                }
            } else {
                try {
                    int value = Integer.parseInt(input);
                    if (value >= min && value <= max) {
                        result = value;
                        isValid = true;
                    } else {
                        System.out.printf("  [Validation Error] Value must be between %d and %d.\n", min, max);
                        if (onReprompt != null) {
                            System.out.println();
                            onReprompt.run();
                        }
                    }
                } catch (NumberFormatException e) {
                    System.out.println("  [Validation Error] Please enter a valid integer number (no letters or symbols).");
                    if (onReprompt != null) {
                        System.out.println();
                        onReprompt.run();
                    }
                }
            }
        }
        return result;
    }

    /**
     * Reads an integer within a mandatory [min, max] range without reprompt callback.
     */
    public static int readInt(Scanner scanner, String prompt, int min, int max) {
        return readInt(scanner, prompt, min, max, null);
    }

    /**
     * Alias for readInt to ensure backward compatibility across controllers.
     */
    public static int readInteger(Scanner scanner, String prompt, int min, int max, Runnable onReprompt) {
        return readInt(scanner, prompt, min, max, onReprompt);
    }

    /**
     * Alias for readInt without reprompt callback.
     */
    public static int readInteger(Scanner scanner, String prompt, int min, int max) {
        return readInt(scanner, prompt, min, max, null);
    }

    /**
     * Reads a floating point numeric value within a mandatory [min, max] range.
     */
    public static double readDouble(Scanner scanner, String prompt, double min, double max) {
        boolean isValid = false;
        double result = min;

        while (!isValid) {
            System.out.print(prompt);
            String input = scanner.nextLine();
            if (input == null) continue;
            input = input.trim();

            if (input.isEmpty()) {
                System.out.println("  [Validation Error] Input cannot be empty. Please enter a numeric value.");
            } else {
                try {
                    double value = Double.parseDouble(input);
                    if (value >= min && value <= max) {
                        result = value;
                        isValid = true;
                    } else {
                        System.out.printf("  [Validation Error] Value must be between %.1f and %.1f.\n", min, max);
                    }
                } catch (NumberFormatException e) {
                    System.out.println("  [Validation Error] Please enter a valid numeric value.");
                }
            }
        }
        return result;
    }

    /**
     * Reads an explicit Yes/No boolean response ('y' or 'n').
     */
    public static boolean readBoolean(Scanner scanner, String prompt) {
        boolean isValid = false;
        boolean result = false;

        while (!isValid) {
            System.out.print(prompt);
            String input = scanner.nextLine();
            if (input == null) continue;
            input = input.trim().toLowerCase();

            if (input.isEmpty()) {
                System.out.println("  [Validation Error] Please enter 'y' for Yes or 'n' for No.");
            } else if (input.equals("y") || input.equals("yes") || input.equals("true") || input.equals("1")) {
                result = true;
                isValid = true;
            } else if (input.equals("n") || input.equals("no") || input.equals("false") || input.equals("0")) {
                result = false;
                isValid = true;
            } else {
                System.out.println("  [Validation Error] Invalid input. Please enter 'y' for Yes or 'n' for No.");
            }
        }
        return result;
    }

    /**
     * Helper to parse month from number (1-12) or text name/abbreviation.
     * Returns 1-12, or -1 if invalid.
     */
    public static int parseMonthInput(String input) {
        if (input == null) return -1;
        String trimmed = input.trim().toLowerCase();
        try {
            int m = Integer.parseInt(trimmed);
            if (m >= 1 && m <= 12) return m;
            return -1;
        } catch (NumberFormatException ignored) {}

        switch (trimmed) {
            case "jan": case "january": return 1;
            case "feb": case "february": return 2;
            case "mar": case "march": return 3;
            case "apr": case "april": return 4;
            case "may": return 5;
            case "jun": case "june": return 6;
            case "jul": case "july": return 7;
            case "aug": case "august": return 8;
            case "sep": case "sept": case "september": return 9;
            case "oct": case "october": return 10;
            case "nov": case "november": return 11;
            case "dec": case "december": return 12;
            default: return -1;
        }
    }

    /**
     * Gets the full English month name for a given month number (1-12).
     */
    public static String getMonthName(int month) {
        return Month.of(month).getDisplayName(TextStyle.FULL, Locale.ENGLISH);
    }

    /**
     * Reads a valid month (by number or text name), dynamically constrained by minMonth.
     */
    public static int readMonth(Scanner scanner, int year, int minMonth) {
        while (true) {
            String prompt;
            if (minMonth > 1) {
                prompt = String.format("  Enter Month (%d [%s] to 12 [December] / enter name or number): ",
                        minMonth, getMonthName(minMonth));
            } else {
                prompt = "  Enter Month (1 to 12 / e.g., 'January' or '1'): ";
            }
            System.out.print(prompt);
            String input = scanner.nextLine();
            if (input == null) continue;
            input = input.trim();
            if (input.isEmpty()) {
                System.out.println("  [Validation Error] Month cannot be empty. Please enter a month name or number.");
                continue;
            }

            int parsed = parseMonthInput(input);
            if (parsed == -1) {
                System.out.println("  [Validation Error] Invalid month '" + input + "'. Enter a number (1-12) or month name (e.g., 'September' or 'Sep').");
                continue;
            }

            if (parsed < minMonth || parsed > 12) {
                if (parsed < minMonth) {
                    System.out.printf("  [Validation Error] %s is in the past for year %d. Minimum allowed month is %s (%d).\n",
                            getMonthName(parsed), year, getMonthName(minMonth), minMonth);
                } else {
                    System.out.println("  [Validation Error] Month must be between 1 and 12.");
                }
                continue;
            }
            return parsed;
        }
    }

    /**
     * Prompts for and validates a complete Year, Month, Day, Hour, and Minute,
     * auto-calculating dynamic valid ranges to guarantee a future timestamp.
     */
    public static LocalDateTime readFutureDateTime(Scanner scanner) {
        return readFutureDateTime(scanner, "--- Scheduled Delivery Date & Time Input ---", null);
    }

    /**
     * Overload for readFutureDateTime supporting optional header prompt and reprompt callback.
     */
    public static LocalDateTime readFutureDateTime(Scanner scanner, String prompt, Runnable onReprompt) {
        LocalDateTime now = LocalDateTime.now();
        int currentYear = now.getYear();
        int maxYear = currentYear + 30;

        if (prompt != null && !prompt.isEmpty()) {
            System.out.println("\n" + prompt);
        }
        System.out.println("  Current Date & Time: " + now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));

        // 1. Year
        int year = readInt(scanner, "  Enter Year (" + currentYear + " to " + maxYear + "): ", currentYear, maxYear, onReprompt);

        // 2. Month (auto-constrains min month if selected year is current year)
        int minMonth = (year == currentYear) ? now.getMonthValue() : 1;
        int month = readMonth(scanner, year, minMonth);

        // 3. Day (auto-constrains min day if current year and current month)
        int maxDays = YearMonth.of(year, month).lengthOfMonth();
        int minDay = (year == currentYear && month == now.getMonthValue()) ? now.getDayOfMonth() : 1;
        int day = readInt(scanner, "  Enter Day (" + minDay + " to " + maxDays + "): ", minDay, maxDays, onReprompt);

        // 4. Hour (auto-constrains min hour if today)
        int minHour = 0;
        if (year == currentYear && month == now.getMonthValue() && day == now.getDayOfMonth()) {
            if (now.getMinute() >= 59) {
                minHour = Math.min(23, now.getHour() + 1);
            } else {
                minHour = now.getHour();
            }
        }
        int hour = readInt(scanner, "  Enter Hour (" + minHour + " to 23, 24-hr format): ", minHour, 23, onReprompt);

        // 5. Minute (auto-constrains min minute if current hour today)
        int minMinute = 0;
        if (year == currentYear && month == now.getMonthValue() && day == now.getDayOfMonth() && hour == now.getHour()) {
            minMinute = Math.min(59, now.getMinute() + 1);
        }
        int minute = readInt(scanner, "  Enter Minute (" + minMinute + " to 59): ", minMinute, 59, onReprompt);

        LocalDateTime candidate = LocalDateTime.of(year, month, day, hour, minute);
        if (!candidate.isAfter(now)) {
            candidate = now.plusMinutes(1);
        }
        return candidate;
    }
}
