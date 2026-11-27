package LMS.LeaveManagement.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record EmployeeRequest(
    @NotBlank(message = "Name is required")
    @Size(min = 3, max = 100, message = "Name must be between 3 and 100 characters")
    @Pattern(regexp = "^[A-Za-z\\s.'-]+$", message = "Name must contain only letters and spaces (no numbers allowed)")
    String name,

    @Email(message = "Enter a valid email")
    @Size(max = 150)
    String email,

    @Size(max = 72, message = "Password must be at most 72 characters")
    String password,

    String role,

    Long managerId,

    @Size(max = 100)
    String department,

    @Size(max = 100)
    String designation,

    @Size(max = 30)
    String phone
) {}
