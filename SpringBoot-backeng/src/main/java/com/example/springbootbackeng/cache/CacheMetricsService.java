package com.example.springbootbackeng.cache;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.stereotype.Component;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

@Component
public class CacheMetricsService {

    private final ConcurrentHashMap<String, MetricCounter> counters = new ConcurrentHashMap<>();
    private final MeterRegistry meterRegistry;

    public CacheMetricsService(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    public void recordHit(String cacheName) {
        metric(cacheName).hit.incrementAndGet();
        counter("app.cache.hit.total", cacheName).increment();
    }

    public void recordMiss(String cacheName) {
        metric(cacheName).miss.incrementAndGet();
        counter("app.cache.miss.total", cacheName).increment();
    }

    public void recordNullHit(String cacheName) {
        metric(cacheName).nullHit.incrementAndGet();
        counter("app.cache.null.hit.total", cacheName).increment();
    }

    public void recordDbFallback(String cacheName) {
        metric(cacheName).dbFallback.incrementAndGet();
        counter("app.cache.db.fallback.total", cacheName).increment();
    }

    public void recordBloomReject(String cacheName) {
        metric(cacheName).bloomReject.incrementAndGet();
        counter("app.cache.bloom.reject.total", cacheName).increment();
    }

    public void recordRebuild(String cacheName, long costNanos) {
        MetricCounter metric = metric(cacheName);
        metric.rebuildCount.incrementAndGet();
        metric.rebuildCostNanos.addAndGet(Math.max(0L, costNanos));
        counter("app.cache.rebuild.total", cacheName).increment();
        Timer.builder("app.cache.rebuild.cost")
                .description("cache rebuild cost")
                .tag("cache", cacheName)
                .register(meterRegistry)
                .record(Math.max(0L, costNanos), TimeUnit.NANOSECONDS);
    }

    public Map<String, Object> snapshot() {
        Map<String, Object> result = new HashMap<>();
        counters.forEach((name, m) -> {
            long hit = m.hit.get();
            long miss = m.miss.get();
            long nullHit = m.nullHit.get();
            long dbFallback = m.dbFallback.get();
            long bloomReject = m.bloomReject.get();
            long rebuildCount = m.rebuildCount.get();
            long rebuildNanos = m.rebuildCostNanos.get();
            long totalRead = hit + miss + nullHit;

            Map<String, Object> item = new HashMap<>();
            item.put("hit", hit);
            item.put("miss", miss);
            item.put("nullHit", nullHit);
            item.put("dbFallback", dbFallback);
            item.put("bloomReject", bloomReject);
            item.put("rebuildCount", rebuildCount);
            item.put("hitRate", totalRead == 0 ? 0D : ((double) hit / totalRead));
            item.put("avgRebuildMs", rebuildCount == 0 ? 0D : (rebuildNanos / 1_000_000.0D / rebuildCount));
            result.put(name, item);
        });
        return result;
    }

    private MetricCounter metric(String cacheName) {
        return counters.computeIfAbsent(cacheName, k -> new MetricCounter());
    }

    private Counter counter(String name, String cacheName) {
        return Counter.builder(name)
                .tag("cache", cacheName)
                .register(meterRegistry);
    }

    private static final class MetricCounter {
        private final AtomicLong hit = new AtomicLong();
        private final AtomicLong miss = new AtomicLong();
        private final AtomicLong nullHit = new AtomicLong();
        private final AtomicLong dbFallback = new AtomicLong();
        private final AtomicLong bloomReject = new AtomicLong();
        private final AtomicLong rebuildCount = new AtomicLong();
        private final AtomicLong rebuildCostNanos = new AtomicLong();
    }
}
