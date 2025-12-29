package com.opencore.billing.events;

import java.time.Instant;
import java.util.Map;

public record CloudEventEnvelope(
        String specversion,
        String id,
        String source,
        String type,
        String subject,
        Instant time,
        String datacontenttype,
        Map<String, Object> data
) {}
