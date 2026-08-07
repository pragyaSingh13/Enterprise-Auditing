package org.enterpriseauditing.enterpriseauditing.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.enterpriseauditing.enterpriseauditing.dto.AuditChainVerificationResponse;
import org.enterpriseauditing.enterpriseauditing.dto.AuditEventRequest;
import org.enterpriseauditing.enterpriseauditing.model.AuditEvent;
import org.enterpriseauditing.enterpriseauditing.service.AuditEventService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/v1/audit-events")
@RequiredArgsConstructor
public class AuditEventController {

    private final AuditEventService auditEventService;

    // Create audit event
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AuditEvent createAuditEvent(
            @Valid @RequestBody AuditEventRequest request) {

        return auditEventService.createAuditEvent(request);
    }

    // Get all audit events
    @GetMapping
    public List<AuditEvent> getAllAuditEvents() {
        return auditEventService.getAllAuditEvents();
    }

    // Get one audit event
    @GetMapping("/{id}")
    public AuditEvent getAuditEventById(@PathVariable String id) {
        return auditEventService.getAuditEventById(id);
    }

    // Get events performed by a particular actor
    @GetMapping("/actor/{actorId}")
    public List<AuditEvent> getEventsByActor(
            @PathVariable String actorId) {

        return auditEventService.getEventsByActor(actorId);
    }

    // Get history of a particular resource
    @GetMapping("/resource/{resourceId}")
    public List<AuditEvent> getEventsByResource(
            @PathVariable String resourceId) {

        return auditEventService.getEventsByResource(resourceId);
    }

    // Get events by action
    @GetMapping("/action/{action}")
    public List<AuditEvent> getEventsByAction(
            @PathVariable String action) {

        return auditEventService.getEventsByAction(action);
    }

    // Get events performed by an actor on a resource
    @GetMapping("/actor/{actorId}/resource/{resourceId}")
    public List<AuditEvent> getEventsByActorAndResource(
            @PathVariable String actorId,
            @PathVariable String resourceId) {

        return auditEventService.getEventsByActorAndResource(
                actorId,
                resourceId
        );
    }

    // Get events for a resource and action
    @GetMapping("/resource/{resourceId}/action/{action}")
    public List<AuditEvent> getEventsByResourceAndAction(
            @PathVariable String resourceId,
            @PathVariable String action) {

        return auditEventService.getEventsByResourceAndAction(
                resourceId,
                action
        );
    }


    @GetMapping("/paged")
    public Page<AuditEvent> getAuditEvents(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size);

        return auditEventService.getAuditEvents(pageable);
    }

    @GetMapping("/search")
    public Page<AuditEvent> searchAuditEvents(
            @RequestParam Instant from,
            @RequestParam Instant to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size);

        return auditEventService.getEventsByTimeRange(
                from,
                to,
                pageable
        );
    }

    @GetMapping("/verify")
    public AuditChainVerificationResponse verifyAuditChain() {
        return auditEventService.verifyAuditChain();
    }
}