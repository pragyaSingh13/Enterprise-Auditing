package org.enterpriseauditing.enterpriseauditing.consumer;

import io.awspring.cloud.sqs.annotation.SqsListener;
import lombok.RequiredArgsConstructor;
import org.enterpriseauditing.enterpriseauditing.model.AuditEvent;
import org.enterpriseauditing.enterpriseauditing.repository.AuditEventRepository;
import org.enterpriseauditing.enterpriseauditing.util.HashUtil;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AuditEventConsumer {

    private final AuditEventRepository auditEventRepository;

    @SqsListener("${aws.sqs.audit-queue-name}")
    public void consume(AuditEvent auditEvent) {

        System.out.println(
                "Processing audit event from SQS: "
                        + auditEvent.getActorId()
        );

        // 1. Find the most recent event already stored in MongoDB
        AuditEvent previousEvent =
                auditEventRepository.findTopByOrderByTimestampDesc();

        // 2. Set the previous hash
        if (previousEvent != null) {
            auditEvent.setPreviousHash(
                    previousEvent.getEventHash()
            );
        }

        // 3. Build the input used for hashing
        String hashInput = buildHashInput(auditEvent);

        // 4. Generate SHA-256 hash
        String eventHash = HashUtil.sha256(hashInput);

        // 5. Store the hash in the event
        auditEvent.setEventHash(eventHash);

        // 6. Save the final event to MongoDB
        AuditEvent savedEvent =
                auditEventRepository.save(auditEvent);

        System.out.println(
                "Audit event saved to MongoDB. ID: "
                        + savedEvent.getId()
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
}