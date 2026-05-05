package io.github.dmitriyiliyov.springoutbox.starter;

public final class DistributedLockPropertiesResolver {

    public static LockDurations resolve(OutboxProperties.DistributedLockProperties distributedLockProperties,
                               OutboxProperties.PollingProperties pollingProperties) {
        Long lockAtLeastForDst = null;
        Long lockAtMostForDst = null;
        if (distributedLockProperties.isResolveByPollingProperties()) {
            switch (pollingProperties.getType()) {
                case ADAPTIVE -> {
                    lockAtLeastForDst = pollingProperties.getMinFixedDelay().toMillis();
                    lockAtMostForDst = pollingProperties.getMaxFixedDelay().toMillis();
                }
                case FIXED -> {
                    lockAtLeastForDst = pollingProperties.getFixedDelay().toMillis();
                    lockAtMostForDst = pollingProperties.getFixedDelay().toMillis();
                }
            }
        } else {
            lockAtLeastForDst = distributedLockProperties.getLockAtLeastFor().toMillis();
            lockAtMostForDst = distributedLockProperties.getLockAtMostFor().toMillis();
        }
        return new LockDurations(lockAtLeastForDst, lockAtMostForDst);
    }

    public record LockDurations(
            Long atLeastFor,
            Long atMostFor
    ) {}
}
