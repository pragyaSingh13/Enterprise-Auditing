package org.enterpriseauditing.enterpriseauditing.dto;

import org.enterpriseauditing.enterpriseauditing.model.Role;

public record AuthResponse(
        String token,
        String username,
        Role role
) {
}