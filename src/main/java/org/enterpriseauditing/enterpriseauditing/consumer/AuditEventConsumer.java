package org.enterpriseauditing.enterpriseauditing.consumer;

import io.awspring.cloud.sqs.annotation.SqsListener;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.enterpriseauditing.enterpriseauditing.model.AuditEvent;
import org.enterpriseauditing.enterpriseauditing.repository.AuditEventRepository;
import org.enterpriseauditing.enterpriseauditing.service.DigitalSignatureService;
import org.enterpriseauditing.enterpriseauditing.service.RedisLockService;
import org.enterpriseauditing.enterpriseauditing.util.AuditEventCanonicalizer;
import org.enterpriseauditing.enterpriseauditing.util.HashUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class AuditEventConsumer {

    private static final Logger log =
            LoggerFactory.getLogger(AuditEventConsumer.class);

    private final AuditEventRepository auditEventRepository;
    private final RedisLockService redisLockService;
    private final DigitalSignatureService digitalSignatureService;
    private final MeterRegistry meterRegistry;
    private final Counter eventsProcessed;
    private final Counter eventsFailed;

    public AuditEventConsumer(
            AuditEventRepository auditEventRepository,
            RedisLockService redisLockService,
            DigitalSignatureService digitalSignatureService,
            MeterRegistry meterRegistry) {

        this.auditEventRepository = auditEventRepository;
        this.redisLockService = redisLockService;
        this.digitalSignatureService = digitalSignatureService;
        this.meterRegistry = meterRegistry;

        this.eventsProcessed =
                Counter.builder("audit.events.processed")
                        .description(
                                "Number of audit events successfully processed"
                        )
                        .register(meterRegistry);

        this.eventsFailed =
                Counter.builder("audit.events.failed")
                        .description(
                                "Number of audit events that failed processing"
                        )
                        .register(meterRegistry);
    }

    @SqsListener("${aws.sqs.audit-queue-name}")
    public void consume(AuditEvent auditEvent) {

        log.info(
                "Received audit event: {}",
                auditEvent.getId()
        );

        try {

            // 1. Validate incoming message
            validateAuditEvent(auditEvent);

            // 2. Idempotency check
            if (auditEventRepository.existsById(auditEvent.getId())) {

                log.info(
                        "Duplicate audit event ignored: {}",
                        auditEvent.getId()
                );

                return;
            }

            String lockKey = "audit-chain-lock";

            log.debug(
                    "Trying to acquire Redis lock for event: {}",
                    auditEvent.getId()
            );

            // 3. Try to acquire Redis distributed lock
            String lockValue =
                    redisLockService.acquireLockWithRetry(
                            lockKey,
                            Duration.ofSeconds(30),
                            10,
                            200
                    );

            // Lock was not acquired
            if (lockValue == null) {

                log.warn(
                        "Could NOT acquire Redis lock: {}",
                        auditEvent.getId()
                );

                throw new RuntimeException(
                        "Could not acquire audit chain lock"
                );
            }

            try {

                log.info(
                        "LOCK ACQUIRED: {}",
                        auditEvent.getId()
                );

                // ==========================================
                // CRITICAL SECTION
                // ==========================================

                // 4. Find latest event
                AuditEvent previousEvent =
                        auditEventRepository
                                .findTopByOrderByTimestampDesc();

                // 5. Link current event to previous event
                if (previousEvent != null) {

                    auditEvent.setPreviousHash(
                            previousEvent.getEventHash()
                    );
                }

                String hashInput =
                        AuditEventCanonicalizer
                                .buildHashInput(auditEvent);

                String eventHash =
                        HashUtil.sha256(hashInput);

                // 6. Generate digital signature
                String digitalSignature =
                        digitalSignatureService.sign(hashInput);

                auditEvent.setDigitalSignature(
                        digitalSignature
                );

                // 7. Set event hash
                auditEvent.setEventHash(eventHash);

                // 8. Save event
                AuditEvent savedEvent =
                        auditEventRepository.save(auditEvent);

                // 9. Record successful processing
                eventsProcessed.increment();

                log.info(
                        "Audit event saved successfully: {}",
                        savedEvent.getId()
                );

            } finally {

                // ==========================================
                // ALWAYS RELEASE THE LOCK
                // ==========================================

                redisLockService.releaseLock(
                        lockKey,
                        lockValue
                );

                log.debug(
                        "LOCK RELEASED: {}",
                        auditEvent.getId()
                );
            }

        } catch (Exception e) {

            eventsFailed.increment();

            log.error(
                    "Failed to process audit event: {}",
                    auditEvent != null
                            ? auditEvent.getId()
                            : "null",
                    e
            );

            // Re-throw so SQS can retry the message
            throw e;
        }
    }

    private void validateAuditEvent(AuditEvent auditEvent) {

        if (auditEvent == null) {
            throw new IllegalArgumentException(
                    "Audit event cannot be null"
            );
        }

        if (auditEvent.getId() == null ||
                auditEvent.getId().isBlank()) {

            throw new IllegalArgumentException(
                    "Audit event ID cannot be null or empty"
            );
        }

        if (auditEvent.getActorId() == null ||
                auditEvent.getActorId().isBlank()) {

            throw new IllegalArgumentException(
                    "Actor ID cannot be null or empty"
            );
        }

        if (auditEvent.getAction() == null ||
                auditEvent.getAction().isBlank()) {

            throw new IllegalArgumentException(
                    "Action cannot be null or empty"
            );
        }

        if (auditEvent.getTimestamp() == null) {

            throw new IllegalArgumentException(
                    "Timestamp cannot be null"
            );
        }
    }
}