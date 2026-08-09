package org.enterpriseauditing.enterpriseauditing.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.enterpriseauditing.enterpriseauditing.dto.AuditChainVerificationResponse;
import org.enterpriseauditing.enterpriseauditing.model.AuditEvent;
import org.enterpriseauditing.enterpriseauditing.repository.AuditEventRepository;
import org.enterpriseauditing.enterpriseauditing.util.HashUtil;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditChainVerificationService {

    private final AuditEventRepository auditEventRepository;

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

            // First event must not have previousHash
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

            // Recalculate current event hash
            String hashInput = buildHashInput(currentEvent);

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
                .message("Audit chain is valid")
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
}