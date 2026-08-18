package org.enterpriseauditing.enterpriseauditing.util;

import org.enterpriseauditing.enterpriseauditing.model.AuditEvent;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

public final class AuditEventCanonicalizer {

    private AuditEventCanonicalizer() {
    }

    public static String buildHashInput(AuditEvent event) {

        Instant timestamp =
                event.getTimestamp()
                        .truncatedTo(ChronoUnit.MILLIS);

        return String.join("|",
                nullToEmpty(event.getActorId()),
                nullToEmpty(event.getAction()),
                nullToEmpty(event.getResourceType()),
                nullToEmpty(event.getResourceId()),
                nullToEmpty(event.getReason()),
                timestamp.toString(),
                nullToEmpty(event.getPreviousHash())
        );
    }
    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}