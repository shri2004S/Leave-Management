package LMS.LeaveManagement.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ManagerUpdateEmployeeRequest(
    @NotBlank(message = "Name is required")
    @Size(min = 3, max = 100, message = "Name must be between 3 and 100 characters")
    @Pattern(regexp = "^[A-Za-z\\s.'-]+$", message = "Name must contain only letters and spaces (no numbers allowed)")
    String name,

    @Size(max = 100, message = "Department must be at most 100 characters")
    String department,

    @Size(max = 100, message = "Designation must be at most 100 characters")
    String designation,

    @Size(max = 30, message = "Phone must be at most 30 characters")
    String phone,

    String role
) {}
