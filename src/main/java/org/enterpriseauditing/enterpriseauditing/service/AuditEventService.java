package org.enterpriseauditing.enterpriseauditing.service;

import lombok.RequiredArgsConstructor;
import org.enterpriseauditing.enterpriseauditing.dto.AuditChainVerificationResponse;
import org.enterpriseauditing.enterpriseauditing.dto.AuditEventRequest;
import org.enterpriseauditing.enterpriseauditing.model.AuditEvent;
import org.enterpriseauditing.enterpriseauditing.repository.AuditEventRepository;
import org.enterpriseauditing.enterpriseauditing.util.HashUtil;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class AuditEventService {

    private final AuditEventRepository auditEventRepository;

    // Create a new audit event
    public AuditEvent createAuditEvent(AuditEventRequest request) {

        AuditEvent auditEvent = new AuditEvent();

        auditEvent.setActorId(request.actorId());
        auditEvent.setAction(request.action());
        auditEvent.setResourceType(request.resourceType());
        auditEvent.setResourceId(request.resourceId());
        auditEvent.setReason(request.reason());
        auditEvent.setTimestamp(Instant.now());

        // Find the previous event in the chain
        AuditEvent previousEvent =
                auditEventRepository.findTopByOrderByTimestampDesc();

        if (previousEvent != null) {
            auditEvent.setPreviousHash(previousEvent.getEventHash());
        }

        // Generate hash for the current event
        String hashInput = buildHashInput(auditEvent);

        String eventHash = HashUtil.sha256(hashInput);

        auditEvent.setEventHash(eventHash);

        return auditEventRepository.save(auditEvent);
    }
    // Get all audit events
    public List<AuditEvent> getAllAuditEvents() {
        return auditEventRepository.findAll();
    }

    // Get a single audit event by ID
    public AuditEvent getAuditEventById(String id) {
        return auditEventRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Audit event not found: " + id)
                );
    }

    // Get all actions performed by a particular user
    public List<AuditEvent> getEventsByActor(String actorId) {
        return auditEventRepository.findByActorId(actorId);
    }

    // Get complete history of a resource
    public List<AuditEvent> getEventsByResource(String resourceId) {
        return auditEventRepository.findByResourceId(resourceId);
    }

    // Get events for a particular action
    public List<AuditEvent> getEventsByAction(String action) {
        return auditEventRepository.findByAction(action);
    }

    // Get events performed by a user on a particular resource
    public List<AuditEvent> getEventsByActorAndResource(
            String actorId,
            String resourceId
    ) {
        return auditEventRepository.findByActorIdAndResourceId(
                actorId,
                resourceId
        );
    }

    // Get events for a resource and action
    public List<AuditEvent> getEventsByResourceAndAction(
            String resourceId,
            String action
    ) {
        return auditEventRepository.findByResourceIdAndAction(
                resourceId,
                action
        );
    }

    public Page<AuditEvent> getAuditEvents(Pageable pageable) {
        return auditEventRepository.findAll(pageable);
    }

    public Page<AuditEvent> getEventsByTimeRange(
            Instant from,
            Instant to,
            Pageable pageable) {

        return auditEventRepository.findByTimestampBetween(
                from,
                to,
                pageable
        );
    }

    private String buildHashInput(AuditEvent event) {

        return String.join("|",
                nullToEmpty(event.getActorId()),
                nullToEmpty(event.getAction()),
                nullToEmpty(event.getResourceType()),
                nullToEmpty(event.getResourceId()),
                nullToEmpty(event.getReason()),
                event.getTimestamp().toString(),
                nullToEmpty(event.getPreviousHash())
        );
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    public AuditChainVerificationResponse verifyAuditChain() {

        List<AuditEvent> events =
                auditEventRepository.findAll(
                        Sort.by(Sort.Direction.ASC, "timestamp")
                );

        String previousHash = null;

        for (AuditEvent event : events) {

            // Check that the event points to the previous event
            if (!Objects.equals(
                    previousHash,
                    event.getPreviousHash())) {

                return new AuditChainVerificationResponse(
                        false,
                        "Audit chain tampering detected at event: "
                                + event.getId()
                );
            }

            // Recalculate the hash
            String hashInput = buildHashInput(event);

            String calculatedHash = HashUtil.sha256(hashInput);

            // Compare calculated hash with stored hash
            if (!Objects.equals(
                    calculatedHash,
                    event.getEventHash())) {

                return new AuditChainVerificationResponse(
                        false,
                        "Audit event has been modified: "
                                + event.getId()
                );
            }

            previousHash = event.getEventHash();
        }

        return new AuditChainVerificationResponse(
                true,
                "Audit chain is valid"
        );
    }
}