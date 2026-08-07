package org.enterpriseauditing.enterpriseauditing.repository;

import org.enterpriseauditing.enterpriseauditing.model.AuditEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.Instant;
import java.util.List;

public interface AuditEventRepository
        extends MongoRepository<AuditEvent, String> {

    List<AuditEvent> findByActorId(String actorId);

    List<AuditEvent> findByResourceId(String resourceId);

    List<AuditEvent> findByAction(String action);

    List<AuditEvent> findByActorIdAndResourceId(
            String actorId,
            String resourceId
    );

    List<AuditEvent> findByResourceIdAndAction(
            String resourceId,
            String action
    );

    Page<AuditEvent> findByTimestampBetween(
            Instant from,
            Instant to,
            Pageable pageable
    );

    AuditEvent findTopByOrderByTimestampDesc();
}