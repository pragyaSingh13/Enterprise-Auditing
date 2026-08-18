package org.enterpriseauditing.enterpriseauditing.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.enterpriseauditing.enterpriseauditing.model.Role;

public record RegisterRequest(

        @NotBlank(message = "Username cannot be empty")
        String username,
        
        @NotBlank(message = "Password cannot be empty")
        @Size(min = 8, message = "Password must be at least 8 characters")
        @Pattern(
                regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&]).+$",
                message = "Password must contain at least one uppercase letter, one lowercase letter, one number, and one special character"
        )
        String password,

        @NotNull(message = "Role cannot be null")
        Role role

) {
}