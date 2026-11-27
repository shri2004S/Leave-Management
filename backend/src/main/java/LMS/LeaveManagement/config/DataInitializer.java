package LMS.LeaveManagement.config;

import LMS.LeaveManagement.entity.LeaveRequest;
import LMS.LeaveManagement.entity.Permission;
import LMS.LeaveManagement.entity.Role;
import LMS.LeaveManagement.entity.User;
import LMS.LeaveManagement.repository.LeaveRequestRepository;
import LMS.LeaveManagement.repository.PermissionRepository;
import LMS.LeaveManagement.repository.RoleRepository;
import LMS.LeaveManagement.repository.UserRepository;
import LMS.LeaveManagement.security.RbacConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final UserRepository userRepository;
    private final LeaveRequestRepository leaveRequestRepository;
    private final PasswordEncoder passwordEncoder;
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;

    public DataInitializer(
            UserRepository userRepository,
            LeaveRequestRepository leaveRequestRepository,
            PasswordEncoder passwordEncoder) {
        this(userRepository, leaveRequestRepository, passwordEncoder, null, null);
    }

    @Autowired
    public DataInitializer(
            UserRepository userRepository,
            LeaveRequestRepository leaveRequestRepository,
            PasswordEncoder passwordEncoder,
            RoleRepository roleRepository,
            PermissionRepository permissionRepository) {
        this.userRepository = userRepository;
        this.leaveRequestRepository = leaveRequestRepository;
        this.passwordEncoder = passwordEncoder;
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
    }

    @Override
    public void run(String... args) {
        try {
            log.info("Checking initial RBAC roles, permissions, and application data...");

            Map<String, Role> roleMap = seedRolesAndPermissions();

            // 1. Seed Official Reviewer Admins & Super Admin
            createOrUpdateAdmin("Super Admin", "superadmin@gmail.com", "Password@123", roleMap.get("ADMIN"));
            createOrUpdateAdmin("Manideep Sir", "manideep@gmail.com", "Password@123", roleMap.get("ADMIN"));
            createOrUpdateAdmin("Sushil Sir", "sushil@gmail.com", "Password@123", roleMap.get("ADMIN"));
            createOrUpdateAdmin("Gaurav Sir", "gaurav@gmail.com", "Password@123", roleMap.get("ADMIN"));

            // 2. Ensure non-admin accounts strictly retain EMPLOYEE role
            ensureEmployeeRole("shri@gmail.com", "Shri", roleMap.get("EMPLOYEE"));
            ensureEmployeeRole("shri1@gmail.com", "Shri", roleMap.get("EMPLOYEE"));

            // 3. Seed RBAC Demo Accounts for MANAGER, EMPLOYEE, and USER roles
            User sarahManager = createOrGetAccount(
                    "Sarah Manager", "manager@settribe.com", "Password@123",
                    "MANAGER", "Engineering", "Engineering Manager", null, roleMap.get("MANAGER"));

            User john = createOrGetAccount(
                    "John Doe", "john@settribe.com", "Password@123",
                    "EMPLOYEE", "Engineering", "Software Engineer", sarahManager, roleMap.get("EMPLOYEE"));

            createOrGetAccount(
                    "Alex Viewer", "user@settribe.com", "Password@123",
                    "USER", "General", "Profile Viewer", null, roleMap.get("USER"));

            // Backfill missing Role FK references on existing user rows
            syncMissingRoleLinks(roleMap);

            // 4. Seed Sample Leave Requests for initial testing if empty
            if (leaveRequestRepository.count() == 0 && john != null) {
                seedSampleLeaveRequests(john);
            }

            log.info("Initial RBAC and application data check completed successfully.");
        } catch (Exception ex) {
            log.warn("Notice: DataInitializer ran with notice: {}", ex.getMessage());
        }
    }

    private Map<String, Role> seedRolesAndPermissions() {
        Map<String, Role> roleMap = new HashMap<>();
        if (roleRepository == null || permissionRepository == null) {
            return roleMap;
        }

        Map<String, Permission> permMap = new HashMap<>();
        for (Map.Entry<String, String> entry : RbacConstants.PERMISSION_DESCRIPTIONS.entrySet()) {
            Permission perm = permissionRepository.findByNameIgnoreCase(entry.getKey())
                    .orElseGet(() -> permissionRepository.save(new Permission(entry.getKey(), entry.getValue())));
            permMap.put(entry.getKey(), perm);
        }

        for (String roleName : RbacConstants.VALID_ROLES) {
            Role role = roleRepository.findByNameIgnoreCase(roleName)
                    .orElseGet(() -> new Role(roleName, "System " + roleName + " role"));
            Set<Permission> assignedPerms = new HashSet<>();
            for (String permName : RbacConstants.getPermissionsForRole(roleName)) {
                Permission p = permMap.get(permName);
                if (p != null) {
                    assignedPerms.add(p);
                }
            }
            role.setPermissions(assignedPerms);
            roleMap.put(roleName, roleRepository.save(role));
        }

        return roleMap;
    }

    private void syncMissingRoleLinks(Map<String, Role> roleMap) {
        if (roleMap.isEmpty()) return;
        for (User u : userRepository.findAll()) {
            String normRole = RbacConstants.normalizeRole(u.getRole());
            Role expectedRole = roleMap.get(normRole);
            if (expectedRole != null && (u.getAssignedRole() == null || !expectedRole.getId().equals(u.getAssignedRole().getId()))) {
                u.setAssignedRole(expectedRole);
                userRepository.save(u);
            }
        }
    }

    private void createOrUpdateAdmin(String name, String email, String password, Role adminRole) {
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
                    if (adminRole != null && user.getAssignedRole() == null) {
                        user.setAssignedRole(adminRole);
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
                    admin.setAssignedRole(adminRole);
                    admin.setDepartment("Administration");
                    admin.setDesignation("Administrator");
                    userRepository.save(admin);
                    log.info("Created Admin account: {}", cleanEmail);
                });
    }

    private void ensureEmployeeRole(String email, String defaultName, Role empRole) {
        String cleanEmail = email.trim().toLowerCase();
        userRepository.findByEmail(cleanEmail).ifPresent(user -> {
            boolean changed = false;
            if (!"EMPLOYEE".equalsIgnoreCase(user.getRole())) {
                user.setRole("EMPLOYEE");
                changed = true;
            }
            if (empRole != null && user.getAssignedRole() == null) {
                user.setAssignedRole(empRole);
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

    private User createOrGetAccount(
            String name, String email, String password, String role,
            String department, String designation, User manager, Role roleEntity) {
        String cleanEmail = email.trim().toLowerCase();
        return userRepository.findByEmail(cleanEmail).map(existing -> {
            boolean changed = false;
            if (existing.getRole() == null || existing.getRole().isBlank()) {
                existing.setRole(role);
                changed = true;
            }
            if (roleEntity != null && existing.getAssignedRole() == null) {
                existing.setAssignedRole(roleEntity);
                changed = true;
            }
            if (manager != null && existing.getManager() == null && "EMPLOYEE".equalsIgnoreCase(existing.getRole())) {
                existing.setManager(manager);
                changed = true;
            }
            if ((existing.getDepartment() == null || existing.getDepartment().isBlank()) && department != null) {
                existing.setDepartment(department);
                changed = true;
            }
            if ((existing.getDesignation() == null || existing.getDesignation().isBlank()) && designation != null) {
                existing.setDesignation(designation);
                changed = true;
            }
            return changed ? userRepository.save(existing) : existing;
        }).orElseGet(() -> {
            User user = new User();
            user.setName(name);
            user.setEmail(cleanEmail);
            user.setPassword(passwordEncoder.encode(password));
            user.setRole(role);
            user.setAssignedRole(roleEntity);
            user.setDepartment(department);
            user.setDesignation(designation);
            user.setManager(manager);
            User saved = userRepository.save(user);
            log.info("Created default {} account: {}", role, cleanEmail);
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
