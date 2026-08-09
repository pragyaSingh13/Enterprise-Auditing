package org.enterpriseauditing.enterpriseauditing.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditChainVerificationResponse {

    private boolean valid;

    private int totalEvents;

    private int verifiedEvents;

    private Integer brokenAtPosition;

    private String brokenAtEventId;

    private String failureType;

    private String expectedHash;

    private String actualHash;

    private String message;

    private String brokenAtTimestamp;

    private String actorId;

    private String action;

    private String resourceType;
    
    private String resourceId;
}