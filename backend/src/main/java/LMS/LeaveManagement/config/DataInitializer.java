package LMS.LeaveManagement.config;

import LMS.LeaveManagement.entity.LeaveRequest;
import LMS.LeaveManagement.entity.User;
import LMS.LeaveManagement.repository.LeaveRequestRepository;
import LMS.LeaveManagement.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final UserRepository userRepository;
    private final LeaveRequestRepository leaveRequestRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(
            UserRepository userRepository,
            LeaveRequestRepository leaveRequestRepository,
            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.leaveRequestRepository = leaveRequestRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        try {
            log.info("Checking initial application data in database...");

            // 1. Seed Official Reviewer Admins & Super Admin
            createOrUpdateAdmin("Super Admin", "superadmin@gmail.com", "Password@123");
            createOrUpdateAdmin("Manideep Sir", "manideep@gmail.com", "Password@123");
            createOrUpdateAdmin("Sushil Sir", "sushil@gmail.com", "Password@123");
            createOrUpdateAdmin("Gaurav Sir", "gaurav@gmail.com", "Password@123");

            // 2. Ensure non-admin accounts strictly retain EMPLOYEE role
            ensureEmployeeRole("shri@gmail.com", "Shri");
            ensureEmployeeRole("shri1@gmail.com", "Shri");

            // 3. Seed Default Test Employee (John Doe)
            User john = createOrGetEmployee("John Doe", "john@settribe.com", "Password@123");

            // 4. Seed Sample Leave Requests for initial testing if empty
            if (leaveRequestRepository.count() == 0 && john != null) {
                seedSampleLeaveRequests(john);
            }

            log.info("Initial application data check completed successfully.");
        } catch (Exception ex) {
            log.warn("Notice: DataInitializer ran with notice: {}", ex.getMessage());
        }
    }

    private void createOrUpdateAdmin(String name, String email, String password) {
        String cleanEmail = email.trim().toLowerCase();
        userRepository.findByEmail(cleanEmail).ifPresentOrElse(
                user -> {
                    boolean changed = false;
                    if (name != null && !name.equalsIgnoreCase(user.getName())) {
                        user.setName(name);
                        changed = true;
                    }
                    if (!"ADMIN".equalsIgnoreCase(user.getRole())) {
                        user.setRole("ADMIN");
                        changed = true;
                    }
                    if (!passwordEncoder.matches(password, user.getPassword())
                            && !passwordEncoder.matches("Shri1234@", user.getPassword())) {
                        user.setPassword(passwordEncoder.encode(password));
                        changed = true;
                        log.info("Synchronized official admin credentials for: {}", cleanEmail);
                    }
                    if (changed) {
                        userRepository.save(user);
                    }
                },
                () -> {
                    User admin = new User();
                    admin.setName(name);
                    admin.setEmail(cleanEmail);
                    admin.setPassword(passwordEncoder.encode(password));
                    admin.setRole("ADMIN");
                    userRepository.save(admin);
                    log.info("Created Admin account: {}", cleanEmail);
                });
    }

    private void ensureEmployeeRole(String email, String defaultName) {
        String cleanEmail = email.trim().toLowerCase();
        userRepository.findByEmail(cleanEmail).ifPresent(user -> {
            boolean changed = false;
            if (!"EMPLOYEE".equalsIgnoreCase(user.getRole())) {
                user.setRole("EMPLOYEE");
                changed = true;
            }
            if (user.getName() != null && user.getName().contains("(Admin)")) {
                user.setName(user.getName().replace("(Admin)", "").trim());
                if (user.getName().isBlank()) {
                    user.setName(defaultName);
                }
                changed = true;
            }
            if (changed) {
                userRepository.save(user);
                log.info("Ensured {} is set to EMPLOYEE role", cleanEmail);
            }
        });
    }

    private User createOrGetEmployee(String name, String email, String password) {
        String cleanEmail = email.trim().toLowerCase();
        return userRepository.findByEmail(cleanEmail).map(user -> {
            if (!"EMPLOYEE".equalsIgnoreCase(user.getRole())) {
                user.setRole("EMPLOYEE");
                return userRepository.save(user);
            }
            return user;
        }).orElseGet(() -> {
            User emp = new User();
            emp.setName(name);
            emp.setEmail(cleanEmail);
            emp.setPassword(passwordEncoder.encode(password));
            emp.setRole("EMPLOYEE");
            User saved = userRepository.save(emp);
            log.info("Created test employee account: {}", cleanEmail);
            return saved;
        });
    }

    private void seedSampleLeaveRequests(User employee) {
        log.info("Seeding initial sample leave requests for employee {}...", employee.getEmail());

        LeaveRequest r1 = new LeaveRequest();
        r1.setUser(employee);
        r1.setLeaveType("CASUAL");
        r1.setFromDate(LocalDate.now().plusDays(2));
        r1.setToDate(LocalDate.now().plusDays(4));
        r1.setReason("Attending family wedding ceremony");
        r1.setStatus("PENDING");
        r1.setAssignedAdmin("Manideep Sir (manideep@gmail.com)");
        r1.setSharedWith("Manideep Sir (manideep@gmail.com)");
        leaveRequestRepository.save(r1);

        LeaveRequest r2 = new LeaveRequest();
        r2.setUser(employee);
        r2.setLeaveType("SICK");
        r2.setFromDate(LocalDate.now().minusDays(10));
        r2.setToDate(LocalDate.now().minusDays(9));
        r2.setReason("Severe fever and doctor consultation");
        r2.setStatus("APPROVED");
        r2.setAssignedAdmin("Sushil Sir (sushil@gmail.com)");
        r2.setSharedWith("Sushil Sir (sushil@gmail.com)");
        leaveRequestRepository.save(r2);
    }
}
