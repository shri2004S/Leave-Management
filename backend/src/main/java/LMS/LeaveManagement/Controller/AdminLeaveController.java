package LMS.LeaveManagement.Controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import LMS.LeaveManagement.service.AdminService;

@RestController
@RequestMapping("/api/admin/leaves")
@PreAuthorize("hasRole('ADMIN')")
public class AdminLeaveController {

    private final AdminService adminService;

    public AdminLeaveController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getAllLeaves(Authentication authentication) {
        return ResponseEntity.ok(adminService.getAllLeaves(authentication));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<Map<String, Object>> updateLeaveStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> request,
            Authentication authentication) {

        String status = request.get("status");
        return ResponseEntity.ok(adminService.updateLeaveStatus(id, status, authentication));
    }
}

