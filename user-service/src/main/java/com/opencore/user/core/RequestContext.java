package com.opencore.user.core;

import java.util.UUID;

public record RequestContext(
        UUID actorUserId,
        UUID orgId,
        String ip
) {}
