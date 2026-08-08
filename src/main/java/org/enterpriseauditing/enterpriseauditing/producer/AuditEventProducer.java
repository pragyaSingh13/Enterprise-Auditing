package org.enterpriseauditing.enterpriseauditing.producer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.enterpriseauditing.enterpriseauditing.model.AuditEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

@Service
public class AuditEventProducer {

    private final SqsClient sqsClient;
    private final ObjectMapper objectMapper;
    private final String queueUrl;

    public AuditEventProducer(
            SqsClient sqsClient,
            ObjectMapper objectMapper,
            @Value("${aws.sqs.audit-queue-url}") String queueUrl) {

        this.sqsClient = sqsClient;
        this.objectMapper = objectMapper;
        this.queueUrl = queueUrl;
    }

    public void sendAuditEvent(AuditEvent auditEvent) {

        try {
            String messageBody = objectMapper.writeValueAsString(auditEvent);

            SendMessageRequest request = SendMessageRequest.builder()
                    .queueUrl(queueUrl)
                    .messageBody(messageBody)
                    .build();

            sqsClient.sendMessage(request);

        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize audit event", e);
        }
    }
}