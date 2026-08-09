package org.enterpriseauditing.enterpriseauditing.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "audit_events")
@CompoundIndex(
        name = "actor_timestamp_idx",
        def = "{'actorId': 1, 'timestamp': -1}"
)
@CompoundIndex(
        name = "resource_timestamp_idx",
        def = "{'resourceId': 1, 'timestamp': -1}"
)
public class AuditEvent {

    @Id
    private String id;

    // User/service that performed the action
    private String actorId;

    // Type of action performed
    private String action;

    // Type of resource being accessed/modified
    private String resourceType;

    // Specific resource being accessed/modified
    private String resourceId;

    // Business justification for the action
    private String reason;

    // When the action occurred
    private Instant timestamp;

    // Hash of this event
    private String eventHash;

    // Hash of the previous event
    private String previousHash;

    // Digital signature of this audit event
    private String digitalSignature;
}