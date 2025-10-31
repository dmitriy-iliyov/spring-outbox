package io.github.dmitriyiliyov.springoutbox.utils;

public final class BeanNameUtils {

    private static final String DEFAULT_BEAN_NAME = "defaultEvent";

    private BeanNameUtils() {}

    public static String toBeanName(String eventType, String suffix) {
        if (eventType == null || eventType.isBlank()) {
            throw new IllegalArgumentException("eventType cannot be null or blank");
        }
        String cleaned = eventType.trim();
        cleaned = cleaned.replaceAll("[^\\p{IsAlphabetic}\\d]", "");
        if (cleaned.isEmpty()) {
            cleaned = DEFAULT_BEAN_NAME;
        }
        String beanName = cleaned.substring(0, 1).toLowerCase() + cleaned.substring(1);
        return beanName + suffix;
    }
}
