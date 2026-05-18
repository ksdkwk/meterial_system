package com.example.springbootbackeng.ratelimit;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

@Component
public class LocalMethodRateLimitService {

    private final ConcurrentHashMap<String, WindowCounter> counterMap = new ConcurrentHashMap<>();

    public boolean allow(String key, int limit, int periodSeconds) {
        long now = Instant.now().getEpochSecond();
        final int safeLimit = Math.max(1, limit);
        final int safePeriod = Math.max(1, periodSeconds);

        WindowCounter latest = counterMap.compute(key, (k, old) -> {
            if (old == null || now >= old.windowStartEpochSecond + safePeriod) {
                return new WindowCounter(now, 1);
            }
            if (old.count >= safeLimit) {
                return old;
            }
            return new WindowCounter(old.windowStartEpochSecond, old.count + 1);
        });

        return latest != null && latest.count <= safeLimit;
    }

    private static class WindowCounter {
        private final long windowStartEpochSecond;
        private final int count;

        private WindowCounter(long windowStartEpochSecond, int count) {
            this.windowStartEpochSecond = windowStartEpochSecond;
            this.count = count;
        }
    }
}
