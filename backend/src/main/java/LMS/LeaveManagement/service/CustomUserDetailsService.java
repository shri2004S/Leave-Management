package LMS.LeaveManagement.service;

import LMS.LeaveManagement.entity.Permission;
import LMS.LeaveManagement.entity.User;
import LMS.LeaveManagement.repository.UserRepository;
import LMS.LeaveManagement.security.RbacConstants;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    private static final java.util.Set<String> OFFICIAL_ADMIN_EMAILS = java.util.Set.of(
            "superadmin@gmail.com",
            "manideep@gmail.com",
            "sushil@gmail.com",
            "gaurav@gmail.com");

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    private boolean isOfficialAdmin(String email) {
        if (email == null)
            return false;
        return OFFICIAL_ADMIN_EMAILS.contains(email.trim().toLowerCase());
    }

    @Override
    public UserDetails loadUserByUsername(String email)
            throws UsernameNotFoundException {

        String cleanEmail = email != null ? email.trim().toLowerCase() : "";
        User user = userRepository.findByEmail(cleanEmail)
                .or(() -> userRepository.findByEmail(email))
                .orElse(null);

        // Auto-provision official admin if missing in database
        if (user == null && isOfficialAdmin(cleanEmail)) {
            user = new User();
            user.setEmail(cleanEmail);
            user.setName(cleanEmail.contains("superadmin") ? "Super Admin"
                    : cleanEmail.contains("manideep") ? "Manideep Sir"
                            : cleanEmail.contains("sushil") ? "Sushil Sir"
                                    : cleanEmail.contains("gaurav") ? "Gaurav Sir" : "Admin");
            user.setRole("ADMIN");
            user.setPassword("$2a$10$baN/1DaM95ALzvOHcy6FPeXLvk.sZXXHwciSnSNAaeOfi666iaGI6");
            try {
                user = userRepository.saveAndFlush(user);
            } catch (Exception ignored) {
            }
        }

        if (user == null) {
            throw new UsernameNotFoundException("User not found: " + email);
        }

        boolean adminUser = isOfficialAdmin(cleanEmail) || isOfficialAdmin(user.getEmail());

        if (adminUser && !"ADMIN".equalsIgnoreCase(user.getRole())) {
            user.setRole("ADMIN");
            try {
                userRepository.saveAndFlush(user);
            } catch (Exception ignored) {
            }
        }

        String rawRole = adminUser ? "ADMIN"
                : (user.getRole() != null ? user.getRole().trim().toUpperCase() : "EMPLOYEE");
        String cleanRole = RbacConstants.normalizeRole(rawRole);

        Set<SimpleGrantedAuthority> authoritySet = new LinkedHashSet<>();
        authoritySet.add(new SimpleGrantedAuthority("ROLE_" + cleanRole));
        authoritySet.add(new SimpleGrantedAuthority(cleanRole));

        if (user.getAssignedRole() != null && user.getAssignedRole().getPermissions() != null
                && !user.getAssignedRole().getPermissions().isEmpty()) {
            for (Permission permission : user.getAssignedRole().getPermissions()) {
                if (permission != null && permission.getName() != null && !permission.getName().isBlank()) {
                    authoritySet.add(new SimpleGrantedAuthority(permission.getName().trim().toUpperCase()));
                }
            }
        } else {
            for (String perm : RbacConstants.getPermissionsForRole(cleanRole)) {
                authoritySet.add(new SimpleGrantedAuthority(perm));
            }
        }

        List<SimpleGrantedAuthority> authorities = new ArrayList<>(authoritySet);

        return org.springframework.security.core.userdetails.User
                .withUsername(user.getEmail())
                .password(user.getPassword())
                .authorities(authorities)
                .build();
    }
}
