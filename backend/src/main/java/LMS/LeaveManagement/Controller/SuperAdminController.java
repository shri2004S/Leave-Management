package LMS.LeaveManagement.Controller;

import LMS.LeaveManagement.dto.CreateAdminRequest;
import LMS.LeaveManagement.entity.LeaveRequest;
import LMS.LeaveManagement.entity.User;
import LMS.LeaveManagement.repository.LeaveRequestRepository;
import LMS.LeaveManagement.repository.UserRepository;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/users")
public class SuperAdminController {

    private static final String SUPER_ADMIN_EMAIL = "superadmin@gmail.com";
    private static final String ROLE_ADMIN = "ADMIN";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final LeaveRequestRepository leaveRequestRepository;

    public SuperAdminController(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this(userRepository, passwordEncoder, null);
    }

    @Autowired
    public SuperAdminController(UserRepository userRepository, PasswordEncoder passwordEncoder, LeaveRequestRepository leaveRequestRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.leaveRequestRepository = leaveRequestRepository;
    }

    private void verifySuperAdmin(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }
        String email = authentication.getName().trim().toLowerCase();
        if (!SUPER_ADMIN_EMAIL.equals(email)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access Denied: Only Super Admin can access this resource");
        }
    }

    @GetMapping("/admins")
    public ResponseEntity<List<Map<String, Object>>> getAdmins(Authentication authentication) {
        verifySuperAdmin(authentication);

        List<Map<String, Object>> admins = userRepository.findAll().stream()
                .filter(u -> ROLE_ADMIN.equalsIgnoreCase(u.getRole()))
                .sorted((a, b) -> a.getId().compareTo(b.getId()))
                .map(u -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", u.getId());
                    map.put("name", SUPER_ADMIN_EMAIL.equalsIgnoreCase(u.getEmail()) ? "Super Admin" : u.getName());
                    map.put("email", u.getEmail());
                    map.put("role", u.getRole());
                    map.put("createdAt", u.getCreatedAt() != null ? u.getCreatedAt().toString() : "");
                    return map;
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(admins);
    }

    @GetMapping("/employees")
    public ResponseEntity<List<Map<String, Object>>> getEmployees(Authentication authentication) {
        verifySuperAdmin(authentication);

        List<LeaveRequest> allLeaves = leaveRequestRepository != null ? leaveRequestRepository.findAll() : List.of();

        List<Map<String, Object>> employees = userRepository.findAll().stream()
                .filter(u -> "EMPLOYEE".equalsIgnoreCase(u.getRole()))
                .sorted((a, b) -> a.getId().compareTo(b.getId()))
                .map(u -> {
                    long totalRequests = allLeaves.stream()
                            .filter(l -> l.getUser() != null && u.getId().equals(l.getUser().getId()))
                            .count();
                    long pendingRequests = allLeaves.stream()
                            .filter(l -> l.getUser() != null && u.getId().equals(l.getUser().getId()) && "PENDING".equalsIgnoreCase(l.getStatus()))
                            .count();
                    long approvedRequests = allLeaves.stream()
                            .filter(l -> l.getUser() != null && u.getId().equals(l.getUser().getId()) && "APPROVED".equalsIgnoreCase(l.getStatus()))
                            .count();
                    long rejectedRequests = allLeaves.stream()
                            .filter(l -> l.getUser() != null && u.getId().equals(l.getUser().getId()) && "REJECTED".equalsIgnoreCase(l.getStatus()))
                            .count();

                    Map<String, Object> map = new HashMap<>();
                    map.put("id", u.getId());
                    map.put("name", u.getName());
                    map.put("email", u.getEmail());
                    map.put("role", u.getRole());
                    map.put("createdAt", u.getCreatedAt() != null ? u.getCreatedAt().toString() : "");
                    map.put("totalRequests", totalRequests);
                    map.put("pendingRequests", pendingRequests);
                    map.put("approvedRequests", approvedRequests);
                    map.put("rejectedRequests", rejectedRequests);
                    return map;
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(employees);
    }

    private String generateAdminName(String email) {
        if (email == null || !email.contains("@")) return "Admin";
        String prefix = email.split("@")[0].replaceAll("[._-]", " ").trim();
        if (prefix.isBlank()) return "Admin";
        String[] words = prefix.split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (String w : words) {
            if (!w.isBlank()) {
                sb.append(Character.toUpperCase(w.charAt(0)))
                  .append(w.substring(1).toLowerCase())
                  .append(" ");
            }
        }
        return sb.toString().trim() + " (Admin)";
    }

    @PostMapping("/create-admin")
    public ResponseEntity<Map<String, Object>> createAdmin(
            @Valid @RequestBody CreateAdminRequest request,
            Authentication authentication) {
        verifySuperAdmin(authentication);

        String cleanEmail = request.email().trim().toLowerCase();
        String cleanName;
        if (request.name() != null && !request.name().trim().isBlank()) {
            cleanName = request.name().trim();
            // Validate name does not contain numbers (O(n) character inspection without backtracking)
            if (cleanName.chars().anyMatch(Character::isDigit)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Numbers are not allowed in name. Please use only letters.");
            }
        } else {
            cleanName = generateAdminName(cleanEmail);
        }

        // Validate password complexity (O(n) character checks, eliminating regex super-linear runtime)
        String pwd = request.password();
        boolean hasLower = pwd != null && pwd.chars().anyMatch(Character::isLowerCase);
        boolean hasUpper = pwd != null && pwd.chars().anyMatch(Character::isUpperCase);
        boolean hasDigit = pwd != null && pwd.chars().anyMatch(Character::isDigit);

        if (pwd == null || pwd.length() < 6 || !hasLower || !hasUpper || !hasDigit) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password must be at least 6 characters and contain uppercase, lowercase, and a number.");
        }

        if (userRepository.findByEmail(cleanEmail).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "An account with this email address already exists.");
        }

        User newAdmin = new User();
        newAdmin.setName(cleanName);
        newAdmin.setEmail(cleanEmail);
        newAdmin.setPassword(passwordEncoder.encode(pwd));
        newAdmin.setRole(ROLE_ADMIN);

        User saved = userRepository.save(newAdmin);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Admin account created successfully");
        response.put("id", saved.getId());
        response.put("name", saved.getName());
        response.put("email", saved.getEmail());
        response.put("role", saved.getRole());

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
