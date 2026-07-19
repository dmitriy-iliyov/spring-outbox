package io.github.dmitriyiliyov.springoutbox.tests.e2e.domain;

/**
 * Event types and topic declared in application.yaml.
 */
public final class E2eEvents {

    public static final String TOPIC = "e2e.events";

    public static final String DEFAULT_EVENT = "e2e-event";
    public static final String AOP_EVENT = "e2e-aop-event";
    public static final String RETRY_EVENT = "e2e-retry-event";
    public static final String DLQ_EVENT = "e2e-dlq-event";

    private E2eEvents() {}
}
