import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service managing user accounts, authentication, and Role-Based Access Control (RBAC).
 * Implemented using the GoF Singleton Pattern.
 */
public class UserService {
    private static UserService instance;
    private final Map<String, User> users;
    private int superAdminCounter = 1000;
    private int hospitalAdminCounter = 2000;
    private int logisticsCounter = 3000;
    private int patientCounter = 4000;

    private UserService() {
        this.users = new HashMap<>();
    }

    public static synchronized UserService getInstance() {
        if (instance == null) {
            instance = new UserService();
        }
        return instance;
    }

    /**
     * Generates a unique role-prefixed account ID (BB-xxxx, HA-xxxx, LG-xxxx, PA-xxxx).
     */
    public synchronized String generateId(Roles role) {
        if (role == null) {
            return "USR-" + System.currentTimeMillis();
        }
        String id;
        switch (role) {
            case SUPER_ADMIN:
                do {
                    id = "BB-" + (++superAdminCounter);
                } while (users.containsKey(id));
                return id;
            case HOSPITAL_ADMIN:
                do {
                    id = "HA-" + (++hospitalAdminCounter);
                } while (users.containsKey(id));
                return id;
            case LOGISTICS:
                do {
                    id = "LG-" + (++logisticsCounter);
                } while (users.containsKey(id));
                return id;
            case PATIENT:
                do {
                    id = "PA-" + (++patientCounter);
                } while (users.containsKey(id));
                return id;
            default:
                return "USR-" + System.currentTimeMillis();
        }
    }

    public synchronized String generateUniqueAccountId(Roles role) {
        return generateId(role);
    }

    public synchronized void registerUser(User user) {
        if (user != null && user.getId() != null) {
            users.put(user.getId(), user);
        }
    }

    public synchronized User authenticate(String id, String password) throws AuthenticationException, UserNotFoundException {
        if (id == null || !users.containsKey(id)) {
            throw new UserNotFoundException("No account found matching Account ID: '" + id + "'. Please check your ID or create an account.");
        }

        User user = users.get(id);
        if (user.isArchived()) {
            throw new AuthenticationException("Account '" + id + "' is disabled/archived. Access denied.");
        }

        if (!user.checkPassword(password)) {
            throw new AuthenticationException("Incorrect password for Account ID '" + id + "'. Access denied.");
        }

        return user;
    }

    public synchronized User getUserById(String id) {
        if (id == null) return null;
        return users.get(id);
    }

    public synchronized Optional<User> findUserById(String id) {
        if (id == null) return Optional.empty();
        return Optional.ofNullable(users.get(id));
    }

    public synchronized List<User> getAllUsers() {
        return new ArrayList<>(users.values());
    }

    public synchronized void deleteUser(String id) {
        if (id != null) {
            users.remove(id);
        }
    }

    public synchronized boolean deleteUser(User user) {
        if (user != null && user.getId() != null) {
            return users.remove(user.getId()) != null;
        }
        return false;
    }

    public synchronized boolean isEmpty() {
        return users.isEmpty();
    }

    public synchronized List<Patient> getActivePatients() {
        return users.values().stream()
                .filter(u -> u instanceof Patient && !u.isArchived())
                .map(u -> (Patient) u)
                .collect(Collectors.toList());
    }

    public synchronized List<Patient> getAllPatients() {
        return users.values().stream()
                .filter(u -> u instanceof Patient)
                .map(u -> (Patient) u)
                .collect(Collectors.toList());
    }
}
