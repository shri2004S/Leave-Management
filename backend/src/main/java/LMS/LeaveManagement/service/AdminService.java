package LMS.LeaveManagement.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import LMS.LeaveManagement.entity.LeaveRequest;
import LMS.LeaveManagement.entity.User;
import LMS.LeaveManagement.repository.LeaveRequestRepository;
import LMS.LeaveManagement.repository.UserRepository;
import org.springframework.security.core.Authentication;

@Service
@Transactional
public class AdminService {

    private final LeaveRequestRepository leaveRepository;
    private final UserRepository userRepository;

    public AdminService(LeaveRequestRepository leaveRepository) {
        this(leaveRepository, null);
    }

    public AdminService(LeaveRequestRepository leaveRepository, UserRepository userRepository) {
        this.leaveRepository = leaveRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getAllLeaves() {
        return getAllLeaves(null);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getAllLeaves(Authentication authentication) {
        String email = authentication != null && authentication.getName() != null
                ? authentication.getName().trim().toLowerCase() : "";
        boolean isSuperAdmin = "superadmin@gmail.com".equalsIgnoreCase(email) || email.isBlank();

        String adminName = "";
        if (userRepository != null && !email.isBlank()) {
            User currentAdmin = userRepository.findByEmail(email).orElse(null);
            if (currentAdmin != null && currentAdmin.getName() != null) {
                adminName = currentAdmin.getName().trim().toLowerCase();
            }
        }

        final String finalAdminName = adminName;

        return leaveRepository.findAll().stream()
            .filter(leave -> {
                if (isSuperAdmin) {
                    return true; // Super Admin sees all leave applications
                }
                String assigned = leave.getAssignedAdmin() != null ? leave.getAssignedAdmin().toLowerCase() : "";
                String shared = leave.getSharedWith() != null ? leave.getSharedWith().toLowerCase() : "";

                boolean matchesEmail = !email.isBlank() && (assigned.contains(email) || shared.contains(email));
                boolean matchesName = !finalAdminName.isBlank() && (assigned.contains(finalAdminName) || shared.contains(finalAdminName));
                boolean isAssignedToAll = assigned.contains("all admins") || assigned.isBlank();

                return matchesEmail || matchesName || isAssignedToAll;
            })
            .sorted((a, b) -> b.getId().compareTo(a.getId()))
            .map(this::mapLeaveToResponse)
            .collect(Collectors.toList());
    }

    private Map<String, Object> mapLeaveToResponse(LeaveRequest leave) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", leave.getId());
        map.put("employeeId", leave.getUser() != null ? leave.getUser().getId() : null);
        map.put("employeeName", leave.getUser() != null ? leave.getUser().getName() : "Unknown");
        map.put("employeeEmail", leave.getUser() != null ? leave.getUser().getEmail() : "Unknown");
        map.put("leaveType", leave.getLeaveType());
        map.put("fromDate", leave.getFromDate());
        map.put("toDate", leave.getToDate());
        map.put("reason", leave.getReason());
        map.put("status", leave.getStatus());
        map.put("assignedAdmin", leave.getAssignedAdmin() != null ? leave.getAssignedAdmin() : "All Admins");
        map.put("sharedWith", leave.getSharedWith() != null ? leave.getSharedWith() : "");
        map.put("createdAt", leave.getCreatedAt() != null ? leave.getCreatedAt().toString() : "");
        return map;
    }

    public Map<String, Object> updateLeaveStatus(Long id, String newStatus) {
        if (id == null) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST, "Leave ID is required");
        }
        if (newStatus == null || 
            (!newStatus.equalsIgnoreCase("APPROVED") && !newStatus.equalsIgnoreCase("REJECTED"))) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST, "Status must be either APPROVED or REJECTED");
        }

        LeaveRequest leave = leaveRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Leave request not found with id: " + id));

        leave.setStatus(newStatus.toUpperCase());
        leaveRepository.save(leave);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Leave request #" + id + " has been " + newStatus.toUpperCase());
        response.put("id", leave.getId());
        response.put("status", leave.getStatus());
        return response;
    }
}
