package LMS.LeaveManagement.security;

import java.util.List;
import java.util.Map;
import java.util.Set;

public final class RbacConstants {

    public static final String ROLE_ADMIN = "ADMIN";
    public static final String ROLE_MANAGER = "MANAGER";
    public static final String ROLE_EMPLOYEE = "EMPLOYEE";
    public static final String ROLE_USER = "USER";

    public static final Set<String> VALID_ROLES = Set.of(
        ROLE_ADMIN, ROLE_MANAGER, ROLE_EMPLOYEE, ROLE_USER
    );

    public static final Map<String, String> PERMISSION_DESCRIPTIONS = Map.ofEntries(
        Map.entry("EMPLOYEE_READ_ALL", "View all employee and user records"),
        Map.entry("EMPLOYEE_CREATE", "Create new employee, manager, or user records"),
        Map.entry("EMPLOYEE_UPDATE", "Update employee records and assign roles or managers"),
        Map.entry("EMPLOYEE_DELETE", "Delete employee or user records"),
        Map.entry("USER_MANAGE", "Manage user accounts and role assignments"),
        Map.entry("LEAVE_APPROVE_ALL", "Approve or reject leave applications"),
        Map.entry("EMPLOYEE_READ_ASSIGNED", "View employees assigned to this manager"),
        Map.entry("EMPLOYEE_UPDATE_ASSIGNED", "Edit permitted info for assigned employees"),
        Map.entry("LEAVE_MANAGE_ASSIGNED", "Approve or reject assigned leave requests"),
        Map.entry("LEAVE_APPLY", "Submit new leave applications"),
        Map.entry("LEAVE_READ_OWN", "Check personal leave status and history"),
        Map.entry("REPORT_READ_OWN", "View personal leave reports and summary"),
        Map.entry("PROFILE_READ_OWN", "View personal profile information"),
        Map.entry("PROFILE_UPDATE_OWN", "Update personal profile information")
    );

    public static final Map<String, List<String>> ROLE_PERMISSIONS = Map.of(
        ROLE_ADMIN, List.of(
            "EMPLOYEE_READ_ALL", "EMPLOYEE_CREATE", "EMPLOYEE_UPDATE", "EMPLOYEE_DELETE",
            "USER_MANAGE", "LEAVE_APPROVE_ALL",
            "EMPLOYEE_READ_ASSIGNED", "EMPLOYEE_UPDATE_ASSIGNED", "LEAVE_MANAGE_ASSIGNED",
            "LEAVE_APPLY", "LEAVE_READ_OWN", "REPORT_READ_OWN",
            "PROFILE_READ_OWN", "PROFILE_UPDATE_OWN"
        ),
        ROLE_MANAGER, List.of(
            "EMPLOYEE_READ_ASSIGNED", "EMPLOYEE_UPDATE_ASSIGNED", "LEAVE_MANAGE_ASSIGNED",
            "LEAVE_APPLY", "LEAVE_READ_OWN", "REPORT_READ_OWN",
            "PROFILE_READ_OWN", "PROFILE_UPDATE_OWN"
        ),
        ROLE_EMPLOYEE, List.of(
            "LEAVE_APPLY", "LEAVE_READ_OWN", "REPORT_READ_OWN",
            "PROFILE_READ_OWN", "PROFILE_UPDATE_OWN"
        ),
        ROLE_USER, List.of(
            "PROFILE_READ_OWN", "PROFILE_UPDATE_OWN"
        )
    );

    private RbacConstants() {
    }

    public static String normalizeRole(String role) {
        if (role == null || role.isBlank()) {
            return ROLE_EMPLOYEE;
        }
        String clean = role.trim().toUpperCase();
        if (clean.startsWith("ROLE_")) {
            clean = clean.substring(5);
        }
        return VALID_ROLES.contains(clean) ? clean : ROLE_EMPLOYEE;
    }

    public static List<String> getPermissionsForRole(String role) {
        return ROLE_PERMISSIONS.getOrDefault(normalizeRole(role), ROLE_PERMISSIONS.get(ROLE_USER));
    }
}
