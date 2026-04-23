package io.github.dmitriyiliyov.springoutbox.starter.consumer;

import io.github.dmitriyiliyov.springoutbox.starter.OutboxProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OutboxConsumerPropertiesUnitTests {

    @Test
    @DisplayName("UT init() when enabled null should disable consumer")
    void applyDefaults_whenEnabledNull_shouldDisable() {
        // given
        OutboxConsumerProperties props = new OutboxConsumerProperties();
        props.setEnabled(null);

        // when
        props.applyDefaults();

        // then
        assertThat(props.isEnabled()).isFalse();
        assertThat(props.getCleanUp()).isNotNull();
        assertThat(props.getCleanUp().isEnabled()).isFalse();
        assertThat(props.getCache()).isNotNull();
        assertThat(props.getCache().isEnabled()).isFalse();
    }

    @Test
    @DisplayName("UT init() when enabled false should disable consumer")
    void applyDefaults_whenEnabledFalse_shouldDisable() {
        // given
        OutboxConsumerProperties props = new OutboxConsumerProperties();
        props.setEnabled(false);

        // when
        props.applyDefaults();

        // then
        assertThat(props.isEnabled()).isFalse();
        assertThat(props.getCleanUp()).isNotNull();
        assertThat(props.getCleanUp().isEnabled()).isFalse();
        assertThat(props.getCache()).isNotNull();
        assertThat(props.getCache().isEnabled()).isFalse();
    }

    @Test
    @DisplayName("UT init() when enabled true and cleanUp/cache null should initialize defaults")
    void applyDefaults_whenEnabledTrueAndNestedNull_shouldInitializeDefaults() {
        // given
        OutboxConsumerProperties props = new OutboxConsumerProperties();
        props.setEnabled(true);
        OutboxConsumerProperties.CacheProperties cache = new OutboxConsumerProperties.CacheProperties();
        cache.setCacheName("cacheName");
        props.setCache(cache);

        // when
        props.applyDefaults();

        // then
        assertThat(props.isEnabled()).isTrue();
        assertThat(props.getCleanUp()).isNotNull();
        assertThat(props.getCleanUp().isEnabled()).isTrue();
        assertThat(props.getCache()).isNotNull();
        assertThat(props.getCache().isEnabled()).isTrue();
    }

    @Test
    @DisplayName("UT init() when enabled true and cleanUp disabled should keep cleanUp disabled")
    void applyDefaults_whenCleanUpDisabled_shouldKeepDisabled() {
        // given
        OutboxConsumerProperties props = new OutboxConsumerProperties();
        props.setEnabled(true);
        OutboxProperties.CleanUpProperties cleanUp = new OutboxProperties.CleanUpProperties();
        cleanUp.setEnabled(false);
        props.setCleanUp(cleanUp);
        OutboxConsumerProperties.CacheProperties cache = new OutboxConsumerProperties.CacheProperties();
        cache.setEnabled(false);
        props.setCache(cache);

        // when
        props.applyDefaults();

        // then
        assertThat(props.getCleanUp()).isNotNull();
        assertThat(props.getCleanUp().isEnabled()).isFalse();
    }

    @Test
    @DisplayName("UT init() when cache enabled true and cacheName null should throw NPE")
    void applyDefaults_whenCacheEnabledTrueAndNameNull_shouldThrowNPE() {
        // given
        OutboxConsumerProperties props = new OutboxConsumerProperties();
        props.setEnabled(true);
        OutboxConsumerProperties.CacheProperties cache = new OutboxConsumerProperties.CacheProperties();
        cache.setEnabled(true);
        cache.setCacheName(null);
        props.setCache(cache);

        // when + then
        assertThrows(NullPointerException.class, props::applyDefaults);
    }

    @Test
    @DisplayName("UT init() when cache enabled true and cacheName blank should throw IllegalArgumentException")
    void applyDefaults_whenCacheEnabledTrueAndNameBlank_shouldThrowIAE() {
        // given
        OutboxConsumerProperties props = new OutboxConsumerProperties();
        props.setEnabled(true);
        OutboxConsumerProperties.CacheProperties cache = new OutboxConsumerProperties.CacheProperties();
        cache.setEnabled(true);
        cache.setCacheName("   ");
        props.setCache(cache);

        // when + then
        assertThrows(IllegalArgumentException.class, props::applyDefaults);
    }

    @Test
    @DisplayName("UT init() when cache enabled false should disable cache and nullify name")
    void applyDefaults_whenCacheDisabled_shouldNullifyName() {
        // given
        OutboxConsumerProperties props = new OutboxConsumerProperties();
        props.setEnabled(true);
        OutboxConsumerProperties.CacheProperties cache = new OutboxConsumerProperties.CacheProperties();
        cache.setEnabled(false);
        cache.setCacheName("myCache");
        props.setCache(cache);

        // when
        props.applyDefaults();

        // then
        assertThat(props.getCache().isEnabled()).isFalse();
        assertThat(props.getCache().getCacheName()).isNull();
    }

    @Test
    @DisplayName("UT init() when CacheProperties is null")
    void applyDefaults_whenCachePropertiesIsNull_shouldPass() {
        // given
        OutboxConsumerProperties props = new OutboxConsumerProperties();
        props.setEnabled(true);
        props.setCache(null);

        // when
        props.applyDefaults();

        // then
        assertThat(props.getCache().isEnabled()).isFalse();
        assertThat(props.getCache().getCacheName()).isNull();
    }

    @Test
    @DisplayName("UT CacheProperties init() when enabled null should enable")
    void cache_applyDefaults_whenEnabledNull_shouldDisable() {
        // given
        String cacheName = "cache";
        OutboxConsumerProperties.CacheProperties cache = new OutboxConsumerProperties.CacheProperties();
        cache.setEnabled(null);
        cache.setCacheName(cacheName);

        // when
        cache.applyDefaults();

        // then
        assertThat(cache.isEnabled()).isTrue();
        assertEquals(cacheName, cache.getCacheName());
    }

    @Test
    @DisplayName("UT CacheProperties init() when enabled false should disable and nullify name")
    void cache_applyDefaults_whenEnabledFalse_shouldDisable() {
        // given
        OutboxConsumerProperties.CacheProperties cache = new OutboxConsumerProperties.CacheProperties();
        cache.setEnabled(false);
        cache.setCacheName("cache");

        // when
        cache.applyDefaults();

        // then
        assertThat(cache.isEnabled()).isFalse();
        assertThat(cache.getCacheName()).isNull();
    }

    @Test
    @DisplayName("UT CacheProperties init() when enabled true and name valid should pass")
    void cache_applyDefaults_whenEnabledTrueAndNameValid_shouldPass() {
        // given
        OutboxConsumerProperties.CacheProperties cache = new OutboxConsumerProperties.CacheProperties();
        cache.setEnabled(true);
        cache.setCacheName("validCache");

        // when
        cache.applyDefaults();

        // then
        assertThat(cache.isEnabled()).isTrue();
        assertThat(cache.getCacheName()).isEqualTo("validCache");
    }
}
