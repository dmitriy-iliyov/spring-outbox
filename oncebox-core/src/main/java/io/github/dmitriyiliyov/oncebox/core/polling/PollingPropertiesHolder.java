package io.github.dmitriyiliyov.oncebox.core.polling;

/**
 * Marker interface to combine contracts of {@link AdaptivePollingPropertiesHolder} and {@link FixedPollingPropertiesHolder}.
 */
public interface PollingPropertiesHolder extends AdaptivePollingPropertiesHolder, FixedPollingPropertiesHolder { }
