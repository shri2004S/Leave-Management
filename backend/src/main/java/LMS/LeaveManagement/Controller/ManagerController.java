package LMS.LeaveManagement.Controller;

import LMS.LeaveManagement.dto.ManagerUpdateEmployeeRequest;
import LMS.LeaveManagement.service.RbacUserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/manager")
@PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
public class ManagerController {

    private final RbacUserService rbacUserService;

    public ManagerController(RbacUserService rbacUserService) {
        this.rbacUserService = rbacUserService;
    }

    @GetMapping("/employees")
    public ResponseEntity<List<Map<String, Object>>> getAssignedEmployees(Authentication authentication) {
        return ResponseEntity.ok(rbacUserService.getAssignedEmployees(authentication));
    }

    @PutMapping("/employees/{id}")
    public ResponseEntity<Map<String, Object>> updateAssignedEmployee(
            @PathVariable Long id,
            @Valid @RequestBody ManagerUpdateEmployeeRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(rbacUserService.updateAssignedEmployee(id, request, authentication));
    }

    @GetMapping("/leaves")
    public ResponseEntity<List<Map<String, Object>>> getAssignedLeaves(Authentication authentication) {
        return ResponseEntity.ok(rbacUserService.getAssignedLeaves(authentication));
    }

    @PutMapping("/leaves/{id}/status")
    public ResponseEntity<Map<String, Object>> updateAssignedLeaveStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> request,
            Authentication authentication) {
        String status = request.get("status");
        String rejectionReason = request.get("rejectionReason");
        return ResponseEntity.ok(
                rbacUserService.updateAssignedLeaveStatus(id, status, rejectionReason, authentication)
        );
    }
}
