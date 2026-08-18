package org.enterpriseauditing.enterpriseauditing.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.enterpriseauditing.enterpriseauditing.dto.AuditChainVerificationResponse;
import org.enterpriseauditing.enterpriseauditing.model.AuditEvent;
import org.enterpriseauditing.enterpriseauditing.repository.AuditEventRepository;
import org.enterpriseauditing.enterpriseauditing.util.AuditEventCanonicalizer;
import org.enterpriseauditing.enterpriseauditing.util.HashUtil;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.util.List;
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditChainVerificationService {

    private final AuditEventRepository auditEventRepository;
    private final DigitalSignatureService digitalSignatureService;
    private final MeterRegistry meterRegistry;

    private Counter hashVerificationFailures;

    @PostConstruct
    private void initializeMetrics() {

        hashVerificationFailures =
                Counter.builder(
                                "audit.hash.verification.failures"
                        )
                        .description(
                                "Number of audit hash-chain verification failures"
                        )
                        .register(meterRegistry);
    }

    @PreAuthorize("hasAnyRole('AUDITOR', 'ADMIN')")
    public AuditChainVerificationResponse verifyChain() {

        log.info("Starting audit chain integrity verification");

        List<AuditEvent> events =
                auditEventRepository.findAllByOrderByTimestampAsc();

        if (events.isEmpty()) {

            log.info("Audit chain is empty");

            return AuditChainVerificationResponse.builder()
                    .valid(true)
                    .totalEvents(0)
                    .verifiedEvents(0)
                    .message("Audit chain is empty")
                    .failureType("EMPTY_CHAIN")
                    .build();
        }

        AuditEvent previousEvent = null;

        for (int i = 0; i < events.size(); i++) {

            AuditEvent currentEvent = events.get(i);

            log.debug(
                    "Verifying event {} at position {}",
                    currentEvent.getId(),
                    i + 1
            );

            // ==========================================
            // 1. VERIFY PREVIOUS HASH LINK
            // ==========================================

            if (previousEvent == null) {

                if (currentEvent.getPreviousHash() != null) {

                    log.error(
                            "First event contains previousHash. Event ID: {}",
                            currentEvent.getId()
                    );

                    return failure(
                            events,
                            i,
                            currentEvent,
                            "FIRST_EVENT_INVALID",
                            null,
                            currentEvent.getPreviousHash(),
                            "First event must not contain previousHash"
                    );
                }

            } else {

                String expectedPreviousHash =
                        previousEvent.getEventHash();

                String actualPreviousHash =
                        currentEvent.getPreviousHash();

                if (!expectedPreviousHash.equals(actualPreviousHash)) {

                    log.error(
                            "Previous hash mismatch. Event ID: {}",
                            currentEvent.getId()
                    );

                    return failure(
                            events,
                            i,
                            currentEvent,
                            "PREVIOUS_HASH_MISMATCH",
                            expectedPreviousHash,
                            actualPreviousHash,
                            "Current event previousHash does not match previous eventHash"
                    );
                }
            }

            // ==========================================
            // 2. BUILD CANONICAL EVENT DATA
            // ==========================================

            String hashInput =
                    AuditEventCanonicalizer.buildHashInput(
                            currentEvent
                    );

            // ==========================================
            // 3. VERIFY EVENT HASH
            // ==========================================

            String calculatedHash =
                    HashUtil.sha256(hashInput);

            String storedHash =
                    currentEvent.getEventHash();

            if (!calculatedHash.equals(storedHash)) {

                log.error(
                        "Event hash mismatch. Event ID: {}",
                        currentEvent.getId()
                );

                return failure(
                        events,
                        i,
                        currentEvent,
                        "HASH_MISMATCH",
                        calculatedHash,
                        storedHash,
                        "Stored eventHash does not match calculated hash"
                );
            }

            // ==========================================
            // 4. VERIFY DIGITAL SIGNATURE
            // ==========================================

            String digitalSignature =
                    currentEvent.getDigitalSignature();

            if (digitalSignature == null ||
                    digitalSignature.isBlank()) {

                log.error(
                        "Digital signature is missing. Event ID: {}",
                        currentEvent.getId()
                );

                return failure(
                        events,
                        i,
                        currentEvent,
                        "SIGNATURE_MISSING",
                        null,
                        null,
                        "Digital signature is missing"
                );
            }

            boolean signatureValid =
                    digitalSignatureService.verify(
                            hashInput,
                            digitalSignature
                    );

            if (!signatureValid) {

                log.error(
                        "Digital signature verification failed. Event ID: {}",
                        currentEvent.getId()
                );

                return failure(
                        events,
                        i,
                        currentEvent,
                        "SIGNATURE_MISMATCH",
                        null,
                        digitalSignature,
                        "Digital signature verification failed"
                );
            }

            log.debug(
                    "Event verified successfully: {}",
                    currentEvent.getId()
            );

            previousEvent = currentEvent;
        }

        log.info(
                "Audit chain verification successful. {} events verified",
                events.size()
        );

        return AuditChainVerificationResponse.builder()
                .valid(true)
                .totalEvents(events.size())
                .verifiedEvents(events.size())
                .message("Audit chain and digital signatures are valid")
                .failureType("VALID")
                .build();
    }

    private AuditChainVerificationResponse failure(
            List<AuditEvent> events,
            int index,
            AuditEvent event,
            String failureType,
            String expectedHash,
            String actualHash,
            String message) {

        /*
         * Serious security event:
         * the audit chain or its signature has failed integrity verification.
         */
        hashVerificationFailures.increment();

        log.error(
                "AUDIT INTEGRITY FAILURE: type={}, eventId={}",
                failureType,
                event.getId()
        );

        return AuditChainVerificationResponse.builder()
                .valid(false)
                .totalEvents(events.size())
                .verifiedEvents(index)
                .brokenAtPosition(index + 1)
                .brokenAtEventId(event.getId())
                .brokenAtTimestamp(
                        event.getTimestamp() != null
                                ? event.getTimestamp().toString()
                                : null
                )
                .actorId(event.getActorId())
                .action(event.getAction())
                .resourceType(event.getResourceType())
                .resourceId(event.getResourceId())
                .failureType(failureType)
                .expectedHash(expectedHash)
                .actualHash(actualHash)
                .message(message)
                .build();
    }


    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}