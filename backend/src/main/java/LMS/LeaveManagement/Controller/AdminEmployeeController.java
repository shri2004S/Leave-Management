package LMS.LeaveManagement.Controller;

import LMS.LeaveManagement.dto.EmployeeRequest;
import LMS.LeaveManagement.service.RbacUserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/employees")
@PreAuthorize("hasRole('ADMIN')")
public class AdminEmployeeController {

    private final RbacUserService rbacUserService;

    public AdminEmployeeController(RbacUserService rbacUserService) {
        this.rbacUserService = rbacUserService;
    }

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getAllEmployees() {
        return ResponseEntity.ok(rbacUserService.listAllUsersForAdmin());
    }

    @GetMapping("/managers")
    public ResponseEntity<List<Map<String, Object>>> getAvailableManagers() {
        return ResponseEntity.ok(rbacUserService.listManagersAndAdmins());
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> createEmployee(
            @Valid @RequestBody EmployeeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(rbacUserService.createEmployeeRecord(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updateEmployee(
            @PathVariable Long id,
            @Valid @RequestBody EmployeeRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(rbacUserService.updateEmployeeRecord(id, request, authentication));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteEmployee(
            @PathVariable Long id,
            Authentication authentication) {
        return ResponseEntity.ok(rbacUserService.deleteEmployeeRecord(id, authentication));
    }
}
