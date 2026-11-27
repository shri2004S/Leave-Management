package LMS.LeaveManagement.Controller;

import LMS.LeaveManagement.dto.ProfileUpdateRequest;
import LMS.LeaveManagement.service.RbacUserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/profile")
public class ProfileController {

    private final RbacUserService rbacUserService;

    public ProfileController(RbacUserService rbacUserService) {
        this.rbacUserService = rbacUserService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('PROFILE_READ_OWN')")
    public ResponseEntity<Map<String, Object>> getOwnProfile(Authentication authentication) {
        return ResponseEntity.ok(rbacUserService.getOwnProfile(authentication));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('PROFILE_READ_OWN')")
    public ResponseEntity<Map<String, Object>> getProfileById(
            @PathVariable Long id,
            Authentication authentication) {
        return ResponseEntity.ok(rbacUserService.getProfileById(id, authentication));
    }

    @PutMapping
    @PreAuthorize("hasAuthority('PROFILE_UPDATE_OWN')")
    public ResponseEntity<Map<String, Object>> updateOwnProfile(
            @Valid @RequestBody ProfileUpdateRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(rbacUserService.updateOwnProfile(request, authentication));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('PROFILE_UPDATE_OWN')")
    public ResponseEntity<Map<String, Object>> updateProfileById(
            @PathVariable Long id,
            @Valid @RequestBody ProfileUpdateRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(rbacUserService.updateProfileById(id, request, authentication));
    }
}
