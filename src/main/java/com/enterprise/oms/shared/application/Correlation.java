package com.enterprise.oms.shared.application;

/** Names shared by the request filter (infrastructure) and the error handler (api). */
public final class Correlation {

    public static final String HEADER = "X-Correlation-ID";
    public static final String MDC_KEY = "correlationId";

    private Correlation() {
    }
}
