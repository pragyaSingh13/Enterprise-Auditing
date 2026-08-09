package org.enterpriseauditing.enterpriseauditing.dto;

import jakarta.validation.constraints.NotBlank;

public record RegisterRequest(

        @NotBlank(message = "Username cannot be empty")
        String username,

        @NotBlank(message = "Password cannot be empty")
        String password

) {
}