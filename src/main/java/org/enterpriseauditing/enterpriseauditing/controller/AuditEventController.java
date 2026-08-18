package org.enterpriseauditing.enterpriseauditing.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.enterpriseauditing.enterpriseauditing.dto.AuditChainVerificationResponse;
import org.enterpriseauditing.enterpriseauditing.dto.AuditEventRequest;
import org.enterpriseauditing.enterpriseauditing.model.AuditEvent;
import org.enterpriseauditing.enterpriseauditing.service.AuditChainVerificationService;
import org.enterpriseauditing.enterpriseauditing.service.AuditEventService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
@RestController
@RequestMapping("/api/v1/audit-events")
@RequiredArgsConstructor
public class AuditEventController {

    private final AuditChainVerificationService auditChainVerificationService;
    private final AuditEventService auditEventService;

    // USER, AUDITOR, ADMIN
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('USER', 'AUDITOR', 'ADMIN')")
    public AuditEvent createAuditEvent(
            @Valid @RequestBody AuditEventRequest request) {

        return auditEventService.createAuditEvent(request);
    }

    // USER, AUDITOR, ADMIN
    @GetMapping
    @PreAuthorize("hasAnyRole('USER', 'AUDITOR', 'ADMIN')")
    public List<AuditEvent> getAllAuditEvents() {
        return auditEventService.getAllAuditEvents();
    }

    // USER, AUDITOR, ADMIN
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER', 'AUDITOR', 'ADMIN')")
    public AuditEvent getAuditEventById(
            @PathVariable String id) {

        return auditEventService.getAuditEventById(id);
    }

    // USER, AUDITOR, ADMIN
    @GetMapping("/actor/{actorId}")
    @PreAuthorize("hasAnyRole('USER', 'AUDITOR', 'ADMIN')")
    public List<AuditEvent> getEventsByActor(
            @PathVariable String actorId) {

        return auditEventService.getEventsByActor(actorId);
    }

    // USER, AUDITOR, ADMIN
    @GetMapping("/resource/{resourceId}")
    @PreAuthorize("hasAnyRole('USER', 'AUDITOR', 'ADMIN')")
    public List<AuditEvent> getEventsByResource(
            @PathVariable String resourceId) {

        return auditEventService.getEventsByResource(resourceId);
    }

    // USER, AUDITOR, ADMIN
    @GetMapping("/action/{action}")
    @PreAuthorize("hasAnyRole('USER', 'AUDITOR', 'ADMIN')")
    public List<AuditEvent> getEventsByAction(
            @PathVariable String action) {

        return auditEventService.getEventsByAction(action);
    }

    // USER, AUDITOR, ADMIN
    @GetMapping("/actor/{actorId}/resource/{resourceId}")
    @PreAuthorize("hasAnyRole('USER', 'AUDITOR', 'ADMIN')")
    public List<AuditEvent> getEventsByActorAndResource(
            @PathVariable String actorId,
            @PathVariable String resourceId) {

        return auditEventService.getEventsByActorAndResource(
                actorId,
                resourceId
        );
    }

    // USER, AUDITOR, ADMIN
    @GetMapping("/resource/{resourceId}/action/{action}")
    @PreAuthorize("hasAnyRole('USER', 'AUDITOR', 'ADMIN')")
    public List<AuditEvent> getEventsByResourceAndAction(
            @PathVariable String resourceId,
            @PathVariable String action) {

        return auditEventService.getEventsByResourceAndAction(
                resourceId,
                action
        );
    }

    // USER, AUDITOR, ADMIN
    @GetMapping("/paged")
    @PreAuthorize("hasAnyRole('USER', 'AUDITOR', 'ADMIN')")
    public Page<AuditEvent> getAuditEvents(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size);

        return auditEventService.getAuditEvents(pageable);
    }

    // USER, AUDITOR, ADMIN
    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('USER', 'AUDITOR', 'ADMIN')")
    public Page<AuditEvent> searchAuditEvents(
            @RequestParam(required = false) String actorId,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String resourceType,
            @RequestParam(required = false) String resourceId,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size);

        return auditEventService.searchAuditEvents(
                actorId,
                action,
                resourceType,
                resourceId,
                from,
                to,
                pageable
        );
    }
    @PreAuthorize("hasAnyRole('AUDITOR', 'ADMIN')")
    @GetMapping("/verify")
    public AuditChainVerificationResponse verifyAuditChain() {
        return auditChainVerificationService.verifyChain();
    }
}
