package com.opencore.user.core;

public final class RequestContextHolder {
    private static final ThreadLocal<RequestContext> CTX = new ThreadLocal<>();

    private RequestContextHolder() {}

    public static void set(RequestContext ctx) {
        CTX.set(ctx);
    }

    public static RequestContext get() {
        return CTX.get();
    }

    public static void clear() {
        CTX.remove();
    }
}
