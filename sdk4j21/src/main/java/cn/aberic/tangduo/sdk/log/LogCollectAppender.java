/*
 * Copyright (c) 2026. Aberic - All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cn.aberic.tangduo.sdk.log;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.StackTraceElementProxy;
import ch.qos.logback.core.UnsynchronizedAppenderBase;
import cn.aberic.tangduo.common.JsonTools;
import org.slf4j.MDC;

import java.net.InetAddress;
import java.util.Date;
import java.util.Objects;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class LogCollectAppender extends UnsynchronizedAppenderBase<ILoggingEvent> {

    /// 日志服务器地址，可配置项
    private String serverUrl;
    private String appName;
    private String appKey;
    private int    batchSize = 20;
    private long   flushInterval = 1000;
    private static final int BATCH_SIZE = 100;
    private static final long FLUSH_INTERVAL_MS = 1000;

    private final LinkedBlockingQueue<String> queue = new LinkedBlockingQueue<>(5000);
    private final AtomicBoolean running = new AtomicBoolean(true);
    private String localIp;
    private SenderConfig config;

    /**
     * 业务项目在 logback.xml 中配置：
     * <serverUrl>http://127.0.0.1:8080/log/receive</serverUrl>
     */
    public void setServerUrl(String serverUrl) {
        this.serverUrl = serverUrl;
    }
    public void setAppName(String appName) {this.appName = appName;}
    public void setAppKey(String appKey) {this.appKey = appKey;}
    public void setBatchSize(int batchSize) {this.batchSize = batchSize;}
    public void setFlushInterval(long flushInterval) {this.flushInterval = flushInterval;}

    public LogCollectAppender() {
        this.localIp = getLocalIp();
        startFlushThread();
    }

    @Override
    public void start() {
        super.start();
        localIp = getLocalIp();
        config = new SenderConfig();
        config.setServerUrl(serverUrl);
        config.setAppName(appName);
        config.setAppKey(appKey);
    }

    private String getLocalIp() {
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (Exception e) {
            return "unknown";
        }
    }

    @Override
    protected void append(ILoggingEvent event) {
        if (!isStarted() || !running.get()) return;
        if (event.getLevel() == Level.DEBUG) return;

        try {
            LogEntity log = new LogEntity();
            log.setTraceId(MDC.get("traceId"));
            log.setLevel(event.getLevel().toString());
            log.setMessage(event.getFormattedMessage());
            log.setThreadName(event.getThreadName());
            log.setLoggerName(event.getLoggerName());
            log.setTimestamp(event.getTimeStamp());
            log.setCreateTime(new Date(event.getTimeStamp()));
            log.setServerIp(localIp);

            // ✅ 修复：兼容 logback 1.5.x 接口类型
            IThrowableProxy throwableProxy = event.getThrowableProxy();
            if (throwableProxy != null) {
                String stackTrace = getStackTrace(throwableProxy);
                log.setException(stackTrace);
            }

            queue.offer(Objects.requireNonNull(JsonTools.toJson(log)));
        } catch (Exception ignore) {}
    }

    // ✅ 修复：参数类型改为 IThrowableProxy，完全兼容 1.5.x
    private String getStackTrace(IThrowableProxy throwableProxy) {
        StringBuilder sb = new StringBuilder();
        sb.append(throwableProxy.getClassName()).append(": ").append(throwableProxy.getMessage()).append("\n");

        for (StackTraceElementProxy step : throwableProxy.getStackTraceElementProxyArray()) {
            sb.append("\tat ").append(step.getStackTraceElement()).append("\n");
        }

        // 递归拼接 cause 堆栈
        IThrowableProxy cause = throwableProxy.getCause();
        while (cause != null) {
            sb.append("\nCaused by: ").append(cause.getClassName()).append(": ").append(cause.getMessage()).append("\n");
            for (StackTraceElementProxy step : cause.getStackTraceElementProxyArray()) {
                sb.append("\tat ").append(step.getStackTraceElement()).append("\n");
            }
            cause = cause.getCause();
        }
        return sb.toString();
    }

    private void startFlushThread() {
        Thread t = new Thread(() -> {
            while (running.get()) {
                try {
                    LogBatchSender.sendBatch(config, queue, batchSize);
                    TimeUnit.MILLISECONDS.sleep(flushInterval);
                } catch (Exception ignored) {}
            }
        }, "log-collect");
        t.setDaemon(true);
        t.start();
    }

    @Override
    public void stop() {
        running.set(false);
        super.stop();
    }
}