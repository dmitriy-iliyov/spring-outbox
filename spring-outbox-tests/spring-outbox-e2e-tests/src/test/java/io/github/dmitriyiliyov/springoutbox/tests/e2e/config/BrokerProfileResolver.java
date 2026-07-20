package io.github.dmitriyiliyov.springoutbox.tests.e2e.config;

import org.springframework.test.context.ActiveProfilesResolver;

/**
 * Activates the Spring profile matching the broker selected via {@code -Dbroker} (default kafka),
 * so only that broker's configuration and yaml are loaded.
 */
public class BrokerProfileResolver implements ActiveProfilesResolver {

    @Override
    public String[] resolve(Class<?> testClass) {
        return new String[] { BrokerType.current().getProfile() };
    }
}
