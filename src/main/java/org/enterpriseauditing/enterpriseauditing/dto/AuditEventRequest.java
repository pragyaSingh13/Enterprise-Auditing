package org.enterpriseauditing.enterpriseauditing.dto;

import jakarta.validation.constraints.NotBlank;

public record AuditEventRequest(

        @NotBlank
        String actorId,

        @NotBlank
        String action,

        @NotBlank
        String resourceType,

        @NotBlank
        String resourceId,

        String reason
) {
}