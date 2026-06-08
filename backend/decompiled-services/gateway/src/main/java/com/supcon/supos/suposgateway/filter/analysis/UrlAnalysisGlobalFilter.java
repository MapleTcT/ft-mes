/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.lmax.disruptor.EventHandler
 *  com.lmax.disruptor.SleepingWaitStrategy
 *  com.lmax.disruptor.WaitStrategy
 *  com.lmax.disruptor.dsl.Disruptor
 *  com.lmax.disruptor.dsl.ProducerType
 *  com.lmax.disruptor.util.DaemonThreadFactory
 *  org.redisson.api.BatchOptions
 *  org.redisson.api.BatchOptions$ExecutionMode
 *  org.redisson.api.RBatch
 *  org.redisson.api.RMapAsync
 *  org.redisson.api.RedissonClient
 *  org.slf4j.Logger
 *  org.slf4j.LoggerFactory
 *  org.springframework.beans.factory.annotation.Autowired
 *  org.springframework.cloud.gateway.filter.GatewayFilterChain
 *  org.springframework.cloud.gateway.filter.GlobalFilter
 *  org.springframework.core.Ordered
 *  org.springframework.http.server.reactive.ServerHttpRequest
 *  org.springframework.web.server.ServerWebExchange
 *  reactor.core.publisher.Mono
 */
package com.supcon.supos.suposgateway.filter.analysis;

import com.lmax.disruptor.EventHandler;
import com.lmax.disruptor.SleepingWaitStrategy;
import com.lmax.disruptor.WaitStrategy;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;
import com.lmax.disruptor.util.DaemonThreadFactory;
import com.supcon.supos.suposgateway.filter.FilterOrder;
import com.supcon.supos.suposgateway.utils.DateUtils;
import com.supcon.supos.suposgateway.utils.HttpUtils;
import com.supcon.supos.suposgateway.utils.ILogger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import org.redisson.api.BatchOptions;
import org.redisson.api.RBatch;
import org.redisson.api.RMapAsync;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

public class UrlAnalysisGlobalFilter
implements GlobalFilter,
Ordered,
ILogger {
    private static final Logger LOGGER = LoggerFactory.getLogger(UrlAnalysisGlobalFilter.class);
    private static final int RETRIES = -1;
    private static final long ONE_MILLIS_IN_NS = 1000000L;
    private static final long SLEEP_TIME_NS = 20000000L;
    private static final int BUFFER_SIZE = 262144;
    private static final int MAX_BATCH_SIZE = 512;
    static final String REDIS_KEY_PREFIX = "gateway:url:analysis:";
    private static final long REDIS_EXPIRED_TIME = 35L;
    private Disruptor<PathInfo> disruptor;
    @Autowired
    private RedissonClient redissonClient;

    @PostConstruct
    public void start() {
        SleepingWaitStrategy waitStrategy = new SleepingWaitStrategy(-1, 20000000L);
        this.disruptor = new Disruptor(PathInfo::new, 262144, (ThreadFactory)DaemonThreadFactory.INSTANCE, ProducerType.MULTI, (WaitStrategy)waitStrategy);
        this.disruptor.handleEventsWith(new EventHandler[]{new BatchConsumeHandler(this.redissonClient)});
        this.disruptor.start();
    }

    @PreDestroy
    public void close() {
        if (this.disruptor != null) {
            this.disruptor.shutdown();
        }
    }

    @Override
    public Logger getLogger() {
        return LOGGER;
    }

    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();
        String tenantId = request.getHeaders().getFirst("X-Tenant-Id");
        this.offer(exchange, path, tenantId);
        return chain.filter(exchange);
    }

    public int getOrder() {
        return FilterOrder.URL_ANALYSIS.getOrder();
    }

    private void offer(ServerWebExchange exchange, String path, String tenantId) {
        boolean result = this.disruptor.getRingBuffer().tryPublishEvent((pathInfo, sequence, args) -> {
            pathInfo.setPath((String)args[0]);
            pathInfo.setTenantId((String)args[1]);
        }, new Object[]{path, tenantId});
        if (!result) {
            HttpUtils.logInfo((ILogger)this, exchange, "try publish failed, ring buffer is full", new Object[0]);
        }
    }

    static class BatchConsumeHandler
    implements EventHandler<PathInfo> {
        private List<PathInfo> batch = new ArrayList<PathInfo>(512);
        private RedissonClient redissonClient;

        public BatchConsumeHandler(RedissonClient redissonClient) {
            this.redissonClient = redissonClient;
        }

        public void onEvent(PathInfo event, long sequence, boolean endOfBatch) throws Exception {
            this.batch.add(event);
            if (endOfBatch || this.batch.size() >= 512) {
                this.processBatch(this.batch);
            }
        }

        private void processBatch(List<PathInfo> batch) {
            String dateStr = DateUtils.getDateStr();
            String overallKey = UrlAnalysisGlobalFilter.REDIS_KEY_PREFIX + dateStr + ":overall";
            HashMap dataMap = new HashMap();
            batch.forEach(pathInfo -> {
                String tenantKey = UrlAnalysisGlobalFilter.REDIS_KEY_PREFIX + dateStr + ":" + pathInfo.getTenantId();
                Map tenantMap = dataMap.computeIfAbsent(tenantKey, k -> new HashMap());
                Integer count = tenantMap.computeIfAbsent(pathInfo.getPath(), k -> 0);
                count = count + 1;
                tenantMap.put(pathInfo.getPath(), count);
                Map overallMap = dataMap.computeIfAbsent(overallKey, k -> new HashMap());
                count = overallMap.computeIfAbsent(pathInfo.getPath(), k -> 0);
                count = count + 1;
                overallMap.put(pathInfo.getPath(), count);
            });
            RBatch rBatch = this.redissonClient.createBatch(BatchOptions.defaults().executionMode(BatchOptions.ExecutionMode.IN_MEMORY_ATOMIC));
            for (Map.Entry entry : dataMap.entrySet()) {
                String key = (String)entry.getKey();
                Map countMap = (Map)entry.getValue();
                boolean isExists = this.redissonClient.getMap(key).isExists();
                RMapAsync rMapAsync = rBatch.getMap(key);
                for (Map.Entry entry0 : countMap.entrySet()) {
                    String pathKey = (String)entry0.getKey();
                    Integer delta = (Integer)entry0.getValue();
                    rMapAsync.addAndGetAsync((Object)pathKey, (Number)delta);
                }
                if (isExists) continue;
                rMapAsync.expireAsync(35L, TimeUnit.DAYS);
            }
            rBatch.execute();
            this.clear();
        }

        private void clear() {
            Iterator<PathInfo> iterator = this.batch.iterator();
            while (iterator.hasNext()) {
                PathInfo pathInfo = iterator.next();
                pathInfo.setPath(null);
                pathInfo.setTenantId(null);
                iterator.remove();
            }
        }
    }

    static class PathInfo {
        String path;
        String tenantId;

        PathInfo() {
        }

        public String getPath() {
            return this.path;
        }

        public void setPath(String path) {
            this.path = path;
        }

        public String getTenantId() {
            return this.tenantId;
        }

        public void setTenantId(String tenantId) {
            this.tenantId = tenantId;
        }
    }
}

