package io.github.dmitriyiliyov.oncebox.starter;

/**
 * Command interface to create an outbox job.
 */
public interface OutboxJobCreateCommand {
    
    /**
     * Executes the creation of an outbox job.
     */
    void create();
}
