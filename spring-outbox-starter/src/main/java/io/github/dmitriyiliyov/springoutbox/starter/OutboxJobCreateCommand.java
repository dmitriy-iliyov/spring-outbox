package io.github.dmitriyiliyov.springoutbox.starter;

/**
 * Command interface to create an outbox job.
 */
public interface OutboxJobCreateCommand {
    
    /**
     * Executes the creation of an outbox job.
     */
    void create();
}
