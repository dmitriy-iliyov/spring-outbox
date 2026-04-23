package io.github.dmitriyiliyov.springoutbox.core;

/**
 * Marker interface to combine contracts of {@link AdaptivePollingPropertiesHolder} and {@link FixedPollingPropertiesHolder}.
 */
public interface PollingPropertiesHolder extends AdaptivePollingPropertiesHolder, FixedPollingPropertiesHolder { }
