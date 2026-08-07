package org.enterpriseauditing.enterpriseauditing.dto;

public record AuditChainVerificationResponse(
        boolean valid,
        String message
) {
}