package LMS.LeaveManagement.service;

import LMS.LeaveManagement.entity.LeaveRequest;
import LMS.LeaveManagement.entity.User;
import LMS.LeaveManagement.repository.LeaveRequestRepository;
import LMS.LeaveManagement.repository.UserRepository;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class LeaveService {

    private final LeaveRequestRepository leaveRepository;
    private final UserRepository userRepository;

    public LeaveService(
            LeaveRequestRepository leaveRepository,
            UserRepository userRepository) {
        this.leaveRepository = leaveRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public LeaveRequest applyForLeave(
            LeaveRequest request, Authentication authentication) {

        if (request.getFromDate() == null || request.getToDate() == null) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST, "Leave dates are required");
        }

        if (request.getFromDate().isBefore(LocalDate.now())) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Start date cannot be in the past");
        }

        if (request.getToDate().isBefore(request.getFromDate())) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "End date cannot be before start date");
        }

        if (request.getLeaveType() == null || request.getLeaveType().isBlank()) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST, "Leave type is required");
        }

        if (request.getReason() == null ||
            request.getReason().isBlank() ||
            request.getReason().length() > 500) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Reason is required and must be at most 500 characters");
        }

        String email = authentication.getName();

        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.UNAUTHORIZED, "User not found"));

        request.setId(null);
        request.setUser(user);
        request.setStatus("PENDING");
        if (request.getAssignedAdmin() == null || request.getAssignedAdmin().isBlank()) {
            request.setAssignedAdmin("Manideep Sir (manideep@gmail.com)");
        }
        if (request.getSharedWith() == null || request.getSharedWith().isBlank()) {
            request.setSharedWith(request.getAssignedAdmin());
        }

        return leaveRepository.save(request);
    }

    public List<LeaveRequest> getMyLeaves(Authentication authentication) {
        String email = authentication.getName();

        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.UNAUTHORIZED, "User not found"));

        return leaveRepository.findByUserId(user.getId());
    }

    @Transactional
    public LeaveRequest updateLeave(Long id, LeaveRequest updatedRequest, Authentication authentication) {
        if (id == null) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST, "Leave ID is required");
        }
        String email = authentication.getName();

        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.UNAUTHORIZED, "User not found"));

        LeaveRequest existing = leaveRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Leave request with ID " + id + " not found"));

        // Ownership rule: Employee can only update their own request
        if (!existing.getUser().getId().equals(user.getId())) {
            throw new ResponseStatusException(
                HttpStatus.FORBIDDEN, "You are not authorized to update another employee's leave request");
        }

        // Workflow rule: Only PENDING requests can be edited
        if (!"PENDING".equalsIgnoreCase(existing.getStatus())) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST, "Cannot edit leave request with status '" + existing.getStatus() + "'. Only PENDING requests can be edited.");
        }

        // Validate updated fields
        if (updatedRequest.getFromDate() == null || updatedRequest.getToDate() == null) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST, "Leave dates are required");
        }

        if (updatedRequest.getFromDate().isBefore(LocalDate.now())) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST, "Start date cannot be in the past");
        }

        if (updatedRequest.getToDate().isBefore(updatedRequest.getFromDate())) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST, "End date cannot be before start date");
        }

        if (updatedRequest.getLeaveType() == null || updatedRequest.getLeaveType().isBlank()) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST, "Leave type is required");
        }

        if (updatedRequest.getReason() == null ||
            updatedRequest.getReason().isBlank() ||
            updatedRequest.getReason().length() > 500) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST, "Reason is required and must be at most 500 characters");
        }

        existing.setLeaveType(updatedRequest.getLeaveType());
        existing.setFromDate(updatedRequest.getFromDate());
        existing.setToDate(updatedRequest.getToDate());
        existing.setReason(updatedRequest.getReason().trim());
        if (updatedRequest.getAssignedAdmin() != null) {
            existing.setAssignedAdmin(updatedRequest.getAssignedAdmin());
        }
        if (updatedRequest.getSharedWith() != null) {
            existing.setSharedWith(updatedRequest.getSharedWith());
        }

        return leaveRepository.save(existing);
    }

    @Transactional(readOnly = true)
    public List<java.util.Map<String, Object>> getAvailableAdmins() {
        java.util.Set<String> legacy = java.util.Set.of(
            "manideepsir@gmail.com", "sushilsir@gmail.com", "gauravsir@gmail.com",
            "admin@settribe.com", "admin@gmail.com", "newadmin@gmail.com",
            "shri@gmail.com", "shri1@gmail.com", "superadmin@gmail.com"
        );
        List<User> approvers = new java.util.ArrayList<>(userRepository.findByRoleIgnoreCase("ADMIN"));
        List<User> managers = userRepository.findByRoleIgnoreCase("MANAGER");
        if (managers != null && !managers.isEmpty()) {
            approvers.addAll(managers);
        }
        return approvers.stream()
            .filter(u -> u.getEmail() != null && !legacy.contains(u.getEmail().toLowerCase()))
            .map(u -> {
                java.util.Map<String, Object> map = new java.util.HashMap<>();
                map.put("id", u.getId());
                map.put("name", u.getName());
                map.put("email", u.getEmail());
                map.put("role", u.getRole());
                return map;
            })
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getMyLeaveReport(Authentication authentication) {
        String email = authentication.getName();
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.UNAUTHORIZED, "User not found"));

        List<LeaveRequest> leaves = leaveRepository.findByUserId(user.getId());
        long total = leaves.size();
        long pending = leaves.stream().filter(l -> "PENDING".equalsIgnoreCase(l.getStatus())).count();
        long approved = leaves.stream().filter(l -> "APPROVED".equalsIgnoreCase(l.getStatus())).count();
        long rejected = leaves.stream().filter(l -> "REJECTED".equalsIgnoreCase(l.getStatus())).count();

        Map<String, Long> workingDaysByCategory = new LinkedHashMap<>();
        workingDaysByCategory.put("CASUAL", 0L);
        workingDaysByCategory.put("SICK", 0L);
        workingDaysByCategory.put("EARNED", 0L);
        workingDaysByCategory.put("OTHER", 0L);

        long totalApprovedWorkingDays = 0;
        for (LeaveRequest l : leaves) {
            if ("APPROVED".equalsIgnoreCase(l.getStatus())) {
                long days = countWorkingDays(l.getFromDate(), l.getToDate());
                totalApprovedWorkingDays += days;
                String cat = l.getLeaveType() != null ? l.getLeaveType().trim().toUpperCase() : "OTHER";
                if (!workingDaysByCategory.containsKey(cat)) {
                    cat = "OTHER";
                }
                workingDaysByCategory.put(cat, workingDaysByCategory.get(cat) + days);
            }
        }

        List<Map<String, Object>> history = leaves.stream()
            .sorted((a, b) -> b.getId().compareTo(a.getId()))
            .map(l -> {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("id", l.getId());
                item.put("leaveType", l.getLeaveType());
                item.put("fromDate", l.getFromDate());
                item.put("toDate", l.getToDate());
                item.put("workingDays", countWorkingDays(l.getFromDate(), l.getToDate()));
                item.put("reason", l.getReason());
                item.put("status", l.getStatus());
                item.put("assignedAdmin", l.getAssignedAdmin() != null ? l.getAssignedAdmin() : "");
                item.put("rejectionReason", l.getRejectionReason() != null ? l.getRejectionReason() : "");
                return item;
            })
            .collect(Collectors.toList());

        Map<String, Object> report = new LinkedHashMap<>();
        report.put("employeeName", user.getName());
        report.put("employeeEmail", user.getEmail());
        report.put("department", user.getDepartment() != null ? user.getDepartment() : "");
        report.put("designation", user.getDesignation() != null ? user.getDesignation() : "");
        report.put("totalRequests", total);
        report.put("pendingRequests", pending);
        report.put("approvedRequests", approved);
        report.put("rejectedRequests", rejected);
        report.put("totalApprovedWorkingDays", totalApprovedWorkingDays);
        report.put("workingDaysByCategory", workingDaysByCategory);
        report.put("history", history);
        return report;
    }

    private long countWorkingDays(LocalDate from, LocalDate to) {
        if (from == null || to == null || to.isBefore(from)) {
            return 0L;
        }
        long working = 0L;
        LocalDate cursor = from;
        while (!cursor.isAfter(to)) {
            DayOfWeek dow = cursor.getDayOfWeek();
            if (dow != DayOfWeek.SATURDAY && dow != DayOfWeek.SUNDAY) {
                working++;
            }
            cursor = cursor.plusDays(1);
        }
        return working;
    }

    @Transactional(readOnly = true)
    public List<java.util.Map<String, Object>> getAllUsers() {
        return userRepository.findAll().stream()
            .map(u -> {
                java.util.Map<String, Object> map = new java.util.HashMap<>();
                map.put("id", u.getId());
                map.put("name", u.getName());
                map.put("email", u.getEmail());
                map.put("role", u.getRole());
                return map;
            })
            .collect(java.util.stream.Collectors.toList());
    }

    @Transactional
    public void deleteLeave(Long id, Authentication authentication) {
        if (id == null) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST, "Leave ID is required");
        }
        String email = authentication.getName();

        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.UNAUTHORIZED, "User not found"));

        LeaveRequest existing = leaveRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Leave request with ID " + id + " not found"));

        // Ownership rule: Employee can only cancel their own request
        if (!existing.getUser().getId().equals(user.getId())) {
            throw new ResponseStatusException(
                HttpStatus.FORBIDDEN, "You are not authorized to cancel another employee's leave request");
        }

        // Workflow rule: Only PENDING requests can be cancelled
        if (!"PENDING".equalsIgnoreCase(existing.getStatus())) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST, "Cannot cancel leave request with status '" + existing.getStatus() + "'. Only PENDING requests can be cancelled.");
        }

        leaveRepository.delete(existing);
    }
}
