package LMS.LeaveManagement.service;

import LMS.LeaveManagement.dto.EmployeeRequest;
import LMS.LeaveManagement.dto.ManagerUpdateEmployeeRequest;
import LMS.LeaveManagement.dto.ProfileUpdateRequest;
import LMS.LeaveManagement.entity.LeaveRequest;
import LMS.LeaveManagement.entity.Role;
import LMS.LeaveManagement.entity.User;
import LMS.LeaveManagement.repository.LeaveRequestRepository;
import LMS.LeaveManagement.repository.RoleRepository;
import LMS.LeaveManagement.repository.UserRepository;
import LMS.LeaveManagement.security.RbacConstants;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional
public class RbacUserService {

    private static final String SUPER_ADMIN_EMAIL = "superadmin@gmail.com";

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final LeaveRequestRepository leaveRequestRepository;
    private final PasswordEncoder passwordEncoder;

    public RbacUserService(
            UserRepository userRepository,
            RoleRepository roleRepository,
            LeaveRequestRepository leaveRequestRepository,
            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.leaveRequestRepository = leaveRequestRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // ==================== PROFILE (ALL ROLES - OWN ONLY) ====================

    @Transactional(readOnly = true)
    public Map<String, Object> getOwnProfile(Authentication authentication) {
        User currentUser = requireAuthenticatedUser(authentication);
        return sanitizeUser(currentUser, true);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getProfileById(Long targetId, Authentication authentication) {
        User currentUser = requireAuthenticatedUser(authentication);
        if (!currentUser.getId().equals(targetId) && !RbacConstants.ROLE_ADMIN.equalsIgnoreCase(currentUser.getRole())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "Access denied: you can only view your own profile");
        }
        User target = userRepository.findById(targetId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        return sanitizeUser(target, true);
    }

    public Map<String, Object> updateOwnProfile(ProfileUpdateRequest request, Authentication authentication) {
        User currentUser = requireAuthenticatedUser(authentication);
        String cleanName = sanitizeAlphabeticName(request.name());
        currentUser.setName(cleanName);
        currentUser.setPhone(request.phone() != null ? request.phone().trim() : null);
        User saved = userRepository.save(currentUser);
        return sanitizeUser(saved, true);
    }

    public Map<String, Object> updateProfileById(Long targetId, ProfileUpdateRequest request, Authentication authentication) {
        User currentUser = requireAuthenticatedUser(authentication);
        if (!currentUser.getId().equals(targetId)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "Access denied: you cannot modify another user's profile");
        }
        return updateOwnProfile(request, authentication);
    }

    // ==================== ADMIN (FULL EMPLOYEE & USER CRUD) ====================

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listAllUsersForAdmin() {
        List<LeaveRequest> allLeaves = leaveRequestRepository.findAll();
        return userRepository.findAll().stream()
                .sorted((a, b) -> a.getId().compareTo(b.getId()))
                .map(u -> attachLeaveCounts(sanitizeUser(u, false), u.getId(), allLeaves))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listManagersAndAdmins() {
        return userRepository.findAll().stream()
                .filter(u -> RbacConstants.ROLE_MANAGER.equalsIgnoreCase(u.getRole())
                        || RbacConstants.ROLE_ADMIN.equalsIgnoreCase(u.getRole()))
                .filter(u -> !SUPER_ADMIN_EMAIL.equalsIgnoreCase(u.getEmail()))
                .sorted((a, b) -> a.getId().compareTo(b.getId()))
                .map(u -> sanitizeUser(u, false))
                .collect(Collectors.toList());
    }

    public Map<String, Object> createEmployeeRecord(EmployeeRequest request) {
        if (request.email() == null || request.email().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email is required");
        }
        String cleanEmail = request.email().trim().toLowerCase();
        if (userRepository.existsByEmail(cleanEmail)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "An account with this email already exists");
        }
        validatePasswordComplexity(request.password());

        String targetRole = RbacConstants.normalizeRole(request.role());
        String cleanName = sanitizeAlphabeticName(request.name());

        User user = new User();
        user.setName(cleanName);
        user.setEmail(cleanEmail);
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setRole(targetRole);
        linkRoleEntity(user, targetRole);
        applyOptionalEmployeeMetadata(user, request.department(), request.designation(), request.phone());
        assignManagerIfProvided(user, request.managerId());

        User saved = userRepository.save(user);
        Map<String, Object> response = sanitizeUser(saved, false);
        response.put("message", "Record created successfully for " + saved.getEmail() + " (" + targetRole + ")");
        return response;
    }

    public Map<String, Object> updateEmployeeRecord(Long id, EmployeeRequest request, Authentication authentication) {
        User actor = requireAuthenticatedUser(authentication);
        User target = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found with id: " + id));

        String cleanName = sanitizeAlphabeticName(request.name());
        target.setName(cleanName);
        applyOptionalEmployeeMetadata(target, request.department(), request.designation(), request.phone());

        if (request.role() != null && !request.role().isBlank()) {
            String nextRole = RbacConstants.normalizeRole(request.role());
            if (SUPER_ADMIN_EMAIL.equalsIgnoreCase(target.getEmail()) && !RbacConstants.ROLE_ADMIN.equals(nextRole)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Super Admin role cannot be modified");
            }
            if (actor.getId().equals(target.getId()) && !RbacConstants.ROLE_ADMIN.equals(nextRole)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "You cannot downgrade your own ADMIN role");
            }
            target.setRole(nextRole);
            linkRoleEntity(target, nextRole);
        }

        if (request.managerId() != null) {
            if (request.managerId() <= 0) {
                target.setManager(null);
            } else {
                assignManagerIfProvided(target, request.managerId());
            }
        }

        User saved = userRepository.save(target);
        Map<String, Object> response = sanitizeUser(saved, false);
        response.put("message", "Employee record updated successfully");
        return response;
    }

    public Map<String, Object> deleteEmployeeRecord(Long id, Authentication authentication) {
        User actor = requireAuthenticatedUser(authentication);
        User target = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found with id: " + id));

        if (SUPER_ADMIN_EMAIL.equalsIgnoreCase(target.getEmail())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Super Admin account cannot be deleted");
        }
        if (actor.getId().equals(target.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "You cannot delete your own active account");
        }

        // Unassign subordinates if deleting a manager
        List<User> subordinates = userRepository.findByManagerId(target.getId());
        for (User sub : subordinates) {
            sub.setManager(null);
            userRepository.save(sub);
        }

        // Clean up dependent leave requests first to avoid FK constraint failure
        List<LeaveRequest> userLeaves = leaveRequestRepository.findByUserId(target.getId());
        if (!userLeaves.isEmpty()) {
            leaveRequestRepository.deleteAll(userLeaves);
        }

        userRepository.delete(target);
        return Map.of("message", "User record deleted successfully", "id", id);
    }

    // ==================== MANAGER (ASSIGNED EMPLOYEES & LEAVES ONLY) ====================

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getAssignedEmployees(Authentication authentication) {
        User manager = requireAuthenticatedUser(authentication);
        boolean isAdmin = RbacConstants.ROLE_ADMIN.equalsIgnoreCase(manager.getRole());

        List<LeaveRequest> allLeaves = leaveRequestRepository.findAll();
        List<User> employees = isAdmin
                ? userRepository.findAll().stream()
                    .filter(u -> RbacConstants.ROLE_EMPLOYEE.equalsIgnoreCase(u.getRole()))
                    .collect(Collectors.toList())
                : userRepository.findByManagerId(manager.getId());

        return employees.stream()
                .sorted((a, b) -> a.getId().compareTo(b.getId()))
                .map(u -> attachLeaveCounts(sanitizeUser(u, false), u.getId(), allLeaves))
                .collect(Collectors.toList());
    }

    public Map<String, Object> updateAssignedEmployee(
            Long employeeId, ManagerUpdateEmployeeRequest request, Authentication authentication) {
        User manager = requireAuthenticatedUser(authentication);
        User target = userRepository.findById(employeeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Employee not found"));

        boolean isAdmin = RbacConstants.ROLE_ADMIN.equalsIgnoreCase(manager.getRole());
        boolean isAssignedToManager = target.getManager() != null && target.getManager().getId().equals(manager.getId());

        if (!isAdmin && !isAssignedToManager) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "Access denied: employee is not assigned to you");
        }

        // Prevent privilege escalation by managers
        if (!isAdmin && request.role() != null && !request.role().isBlank()
                && !request.role().trim().equalsIgnoreCase(target.getRole())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "Managers are not permitted to change user roles");
        }

        target.setName(sanitizeAlphabeticName(request.name()));
        applyOptionalEmployeeMetadata(target, request.department(), request.designation(), request.phone());

        User saved = userRepository.save(target);
        Map<String, Object> response = sanitizeUser(saved, false);
        response.put("message", "Assigned employee details updated successfully");
        return response;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getAssignedLeaves(Authentication authentication) {
        User manager = requireAuthenticatedUser(authentication);
        boolean isSuperAdmin = SUPER_ADMIN_EMAIL.equalsIgnoreCase(manager.getEmail());
        boolean isAdmin = RbacConstants.ROLE_ADMIN.equalsIgnoreCase(manager.getRole());

        return leaveRequestRepository.findAll().stream()
                .filter(leave -> isSuperAdmin || isAdmin || isLeaveAssignedToManager(leave, manager))
                .sorted((a, b) -> b.getId().compareTo(a.getId()))
                .map(this::mapLeaveForManager)
                .collect(Collectors.toList());
    }

    public Map<String, Object> updateAssignedLeaveStatus(
            Long leaveId, String newStatus, String rejectionReason, Authentication authentication) {
        if (newStatus == null || (!newStatus.equalsIgnoreCase("APPROVED") && !newStatus.equalsIgnoreCase("REJECTED"))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Status must be either APPROVED or REJECTED");
        }
        User manager = requireAuthenticatedUser(authentication);
        LeaveRequest leave = leaveRequestRepository.findById(leaveId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Leave request not found"));

        boolean isAdmin = RbacConstants.ROLE_ADMIN.equalsIgnoreCase(manager.getRole());
        if (!isAdmin && !isLeaveAssignedToManager(leave, manager)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "Access denied: this leave request is not assigned to you");
        }

        leave.setStatus(newStatus.toUpperCase());
        if ("REJECTED".equalsIgnoreCase(newStatus) && rejectionReason != null && !rejectionReason.isBlank()) {
            leave.setRejectionReason(rejectionReason.trim());
        }
        leaveRequestRepository.save(leave);

        return Map.of(
                "message", "Leave request #" + leaveId + " has been " + leave.getStatus(),
                "id", leave.getId(),
                "status", leave.getStatus()
        );
    }

    // ==================== HELPERS ====================

    private boolean isLeaveAssignedToManager(LeaveRequest leave, User manager) {
        if (leave.getUser() != null && leave.getUser().getManager() != null
                && leave.getUser().getManager().getId().equals(manager.getId())) {
            return true;
        }
        String managerEmail = manager.getEmail() != null ? manager.getEmail().trim().toLowerCase() : "";
        String managerName = manager.getName() != null ? manager.getName().trim().toLowerCase() : "";
        String assigned = leave.getAssignedAdmin() != null ? leave.getAssignedAdmin().toLowerCase() : "";
        String shared = leave.getSharedWith() != null ? leave.getSharedWith().toLowerCase() : "";
        return (!managerEmail.isBlank() && (assigned.contains(managerEmail) || shared.contains(managerEmail)))
                || (!managerName.isBlank() && (assigned.contains(managerName) || shared.contains(managerName)));
    }

    private User requireAuthenticatedUser(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }
        String email = authentication.getName().trim().toLowerCase();
        return userRepository.findByEmail(email)
                .or(() -> userRepository.findByEmail(authentication.getName()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
    }

    private void linkRoleEntity(User user, String roleName) {
        if (roleRepository != null) {
            Role role = roleRepository.findByNameIgnoreCase(roleName).orElse(null);
            user.setAssignedRole(role);
        }
    }

    private void assignManagerIfProvided(User user, Long managerId) {
        if (managerId == null) return;
        if (user.getId() != null && user.getId().equals(managerId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User cannot be their own manager");
        }
        User mgr = userRepository.findById(managerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Assigned manager not found"));
        if (!RbacConstants.ROLE_MANAGER.equalsIgnoreCase(mgr.getRole())
                && !RbacConstants.ROLE_ADMIN.equalsIgnoreCase(mgr.getRole())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Selected supervisor must have MANAGER or ADMIN role");
        }
        user.setManager(mgr);
    }

    private void applyOptionalEmployeeMetadata(User user, String department, String designation, String phone) {
        if (department != null) user.setDepartment(department.trim());
        if (designation != null) user.setDesignation(designation.trim());
        if (phone != null) user.setPhone(phone.trim());
    }

    private String sanitizeAlphabeticName(String rawName) {
        if (rawName == null || rawName.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Name is required");
        }
        String cleaned = rawName.trim().replaceAll("\\s+", " ");
        if (cleaned.chars().anyMatch(Character::isDigit)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Numbers are not allowed in name. Please use only letters.");
        }
        if (cleaned.length() < 3 || cleaned.length() > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Name must be between 3 and 100 characters");
        }
        return cleaned;
    }

    private void validatePasswordComplexity(String pwd) {
        boolean hasLower = pwd != null && pwd.chars().anyMatch(Character::isLowerCase);
        boolean hasUpper = pwd != null && pwd.chars().anyMatch(Character::isUpperCase);
        boolean hasDigit = pwd != null && pwd.chars().anyMatch(Character::isDigit);
        if (pwd == null || pwd.length() < 6 || pwd.length() > 72 || !hasLower || !hasUpper || !hasDigit) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Password must be 6-72 characters and contain uppercase, lowercase, and a number");
        }
    }

    private Map<String, Object> sanitizeUser(User u, boolean includePermissions) {
        String role = RbacConstants.normalizeRole(u.getRole());
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", u.getId());
        map.put("userId", u.getId());
        map.put("name", SUPER_ADMIN_EMAIL.equalsIgnoreCase(u.getEmail()) ? "Super Admin" : u.getName());
        map.put("email", u.getEmail());
        map.put("role", role);
        map.put("department", u.getDepartment() != null ? u.getDepartment() : "");
        map.put("designation", u.getDesignation() != null ? u.getDesignation() : "");
        map.put("phone", u.getPhone() != null ? u.getPhone() : "");
        map.put("managerId", u.getManager() != null ? u.getManager().getId() : null);
        map.put("managerName", u.getManager() != null ? u.getManager().getName() : "");
        map.put("managerEmail", u.getManager() != null ? u.getManager().getEmail() : "");
        map.put("createdAt", u.getCreatedAt() != null ? u.getCreatedAt().toString() : "");
        if (includePermissions) {
            map.put("permissions", RbacConstants.getPermissionsForRole(role));
        }
        return map;
    }

    private Map<String, Object> attachLeaveCounts(Map<String, Object> map, Long userId, List<LeaveRequest> allLeaves) {
        long total = allLeaves.stream().filter(l -> l.getUser() != null && userId.equals(l.getUser().getId())).count();
        long pending = allLeaves.stream().filter(l -> l.getUser() != null && userId.equals(l.getUser().getId()) && "PENDING".equalsIgnoreCase(l.getStatus())).count();
        long approved = allLeaves.stream().filter(l -> l.getUser() != null && userId.equals(l.getUser().getId()) && "APPROVED".equalsIgnoreCase(l.getStatus())).count();
        long rejected = allLeaves.stream().filter(l -> l.getUser() != null && userId.equals(l.getUser().getId()) && "REJECTED".equalsIgnoreCase(l.getStatus())).count();
        map.put("totalRequests", total);
        map.put("pendingRequests", pending);
        map.put("approvedRequests", approved);
        map.put("rejectedRequests", rejected);
        return map;
    }

    private Map<String, Object> mapLeaveForManager(LeaveRequest leave) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", leave.getId());
        map.put("employeeId", leave.getUser() != null ? leave.getUser().getId() : null);
        map.put("employeeName", leave.getUser() != null ? leave.getUser().getName() : "Unknown");
        map.put("employeeEmail", leave.getUser() != null ? leave.getUser().getEmail() : "Unknown");
        map.put("department", leave.getUser() != null && leave.getUser().getDepartment() != null ? leave.getUser().getDepartment() : "");
        map.put("leaveType", leave.getLeaveType());
        map.put("fromDate", leave.getFromDate());
        map.put("toDate", leave.getToDate());
        map.put("reason", leave.getReason());
        map.put("status", leave.getStatus());
        map.put("rejectionReason", leave.getRejectionReason() != null ? leave.getRejectionReason() : "");
        map.put("assignedAdmin", leave.getAssignedAdmin() != null ? leave.getAssignedAdmin() : "");
        map.put("sharedWith", leave.getSharedWith() != null ? leave.getSharedWith() : "");
        map.put("createdAt", leave.getCreatedAt() != null ? leave.getCreatedAt().toString() : "");
        return map;
    }
}
