package org.enterpriseauditing.enterpriseauditing.consumer;

import io.awspring.cloud.sqs.annotation.SqsListener;
import lombok.RequiredArgsConstructor;
import org.enterpriseauditing.enterpriseauditing.model.AuditEvent;
import org.enterpriseauditing.enterpriseauditing.repository.AuditEventRepository;
import org.enterpriseauditing.enterpriseauditing.service.RedisLockService;
import org.enterpriseauditing.enterpriseauditing.util.HashUtil;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class AuditEventConsumer {

    private final AuditEventRepository auditEventRepository;
    private final RedisLockService redisLockService;

    @SqsListener("${aws.sqs.audit-queue-name}")
    public void consume(AuditEvent auditEvent) {

        System.out.println(
                "Received audit event: " + auditEvent.getId()
        );

        // 1. Validate the incoming message
        validateAuditEvent(auditEvent);

        // 2. Idempotency check
        if (auditEventRepository.existsById(auditEvent.getId())) {
            System.out.println(
                    "Duplicate audit event ignored: "
                            + auditEvent.getId()
            );
            return;
        }

        String lockKey = "audit-chain-lock";

        System.out.println(
                "Trying to acquire Redis lock for event: "
                        + auditEvent.getId()
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

            System.out.println(
                    "Could NOT acquire Redis lock: "
                            + auditEvent.getId()
            );

            // Throw exception so SQS does not acknowledge
            // the message and can retry it
            throw new RuntimeException(
                    "Could not acquire audit chain lock"
            );
        }

        try {

            System.out.println(
                    "LOCK ACQUIRED: "
                            + auditEvent.getId()
            );

            Thread.sleep(5000);
            // ==========================================
            // CRITICAL SECTION
            // ==========================================

            // 4. Find the latest event from MongoDB
            AuditEvent previousEvent =
                    auditEventRepository
                            .findTopByOrderByTimestampDesc();

            // 5. Link current event to previous event
            if (previousEvent != null) {
                auditEvent.setPreviousHash(
                        previousEvent.getEventHash()
                );
            }

            // 6. Build hash input
            String hashInput =
                    buildHashInput(auditEvent);

            // 7. Generate SHA-256 hash
            String eventHash =
                    HashUtil.sha256(hashInput);

            // 8. Set current event hash
            auditEvent.setEventHash(eventHash);

            // 9. Save event to MongoDB
            AuditEvent savedEvent =
                    auditEventRepository.save(auditEvent);

            System.out.println(
                    "Audit event saved: "
                            + savedEvent.getId()
            );

        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {

            // ==========================================
            // ALWAYS RELEASE THE LOCK
            // ==========================================

            redisLockService.releaseLock(
                    lockKey,
                    lockValue
            );

            System.out.println(
                    "LOCK RELEASED: "
                            + auditEvent.getId()
            );
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