package LMS.LeaveManagement.service;

import LMS.LeaveManagement.dto.AuthResponse;
import LMS.LeaveManagement.dto.ForgotPasswordRequest;
import LMS.LeaveManagement.dto.LoginRequest;
import LMS.LeaveManagement.dto.RegisterRequest;
import LMS.LeaveManagement.entity.User;
import LMS.LeaveManagement.repository.UserRepository;
import LMS.LeaveManagement.security.JwtService;

import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager,
            JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = request.email().trim().toLowerCase();

        if (userRepository.existsByEmail(email)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Email is already registered");
        }

        User user = new User();
        user.setName(request.name().trim());
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(request.password()));
        String initialRole = isOfficialAdminEmail(email) ? "ADMIN" : "EMPLOYEE";
        user.setRole(initialRole);

        userRepository.saveAndFlush(user);

        UserDetails details =
                org.springframework.security.core.userdetails.User
                        .withUsername(user.getEmail())
                        .password(user.getPassword())
                        .roles(initialRole)
                        .build();

        String token = jwtService.generateToken(details);

        return new AuthResponse(
                token, "Bearer", user.getId(),
                user.getName(), user.getEmail(), initialRole);
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        String email = request.email() != null ? request.email().trim().toLowerCase() : "";
        String password = request.password() != null ? request.password() : "";

        // 1. Ensure admin accounts exist and have latest entered password
        syncOfficialAdmin(email, password);

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, password));
        } catch (AuthenticationException ex) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Invalid email or password");
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "Invalid email or password"));

        String effectiveRole = isOfficialAdminEmail(email) ? "ADMIN" : user.getRole();

        UserDetails details =
                org.springframework.security.core.userdetails.User
                        .withUsername(user.getEmail())
                        .password(user.getPassword())
                        .roles(effectiveRole)
                        .build();

        String token = jwtService.generateToken(details);

        return new AuthResponse(
                token, "Bearer", user.getId(),
                user.getName(), user.getEmail(), effectiveRole);
    }

    private boolean isOfficialAdminEmail(String email) {
        if (email == null) return false;
        String e = email.toLowerCase().trim();
        return e.contains("shri") || e.contains("admin") ||
                e.contains("manideep") || e.contains("sushil") || e.contains("gaurav");
    }

    private void syncOfficialAdmin(String email, String rawPassword) {
        if (email == null || !isOfficialAdminEmail(email)) return;

        String cleanEmail = email.toLowerCase().trim();
        String officialName = "Shri (Admin)";
        if (cleanEmail.contains("manideep")) officialName = "Manideep Sir";
        else if (cleanEmail.contains("sushil")) officialName = "Sushil Sir";
        else if (cleanEmail.contains("gaurav")) officialName = "Gaurav Sir";
        else if (cleanEmail.contains("shri")) officialName = "Shri (Admin)";

        User user = userRepository.findByEmail(cleanEmail).orElse(null);
        String passToUse = (rawPassword != null && !rawPassword.isBlank()) ? rawPassword : "Password@123";

        if (user == null) {
            user = new User();
            user.setEmail(cleanEmail);
            user.setName(officialName);
            user.setRole("ADMIN");
            user.setPassword(passwordEncoder.encode(passToUse));
            userRepository.saveAndFlush(user);
        } else {
            user.setRole("ADMIN");
            if (rawPassword != null && !rawPassword.isBlank() && !passwordEncoder.matches(rawPassword, user.getPassword())) {
                user.setPassword(passwordEncoder.encode(rawPassword));
                userRepository.saveAndFlush(user);
            }
        }
    }

    @Transactional
    public Map<String, String> resetPassword(ForgotPasswordRequest request) {
        String email = request.email() != null ? request.email().trim().toLowerCase() : "";
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "No account found with this email"));

        user.setPassword(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);

        return Map.of("message", "Password has been successfully reset");
    }

    @Transactional
    public Map<String, String> forgotPassword(ForgotPasswordRequest request) {
        return resetPassword(request);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getCurrentUser(String email) {
        String cleanEmail = email != null ? email.trim().toLowerCase() : "";
        User user = userRepository.findByEmail(cleanEmail)
                .or(() -> userRepository.findByEmail(email))
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "User not found"));

        String role = isOfficialAdminEmail(user.getEmail()) ? "ADMIN" : user.getRole();

        return Map.of(
                "userId", user.getId(),
                "name", user.getName(),
                "email", user.getEmail(),
                "role", role
        );
    }
}
