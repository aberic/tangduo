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
import lombok.Setter;
import org.slf4j.MDC;

import java.net.InetAddress;
import java.util.Date;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/// 日志采集器
@Setter
public class LogCollectAppender extends UnsynchronizedAppenderBase<ILoggingEvent> {

    public LogCollectAppender() {}

    private static class Holder {
        private static final LogCollectAppender INSTANCE = new LogCollectAppender();
    }

    public static LogCollectAppender getInstance() {
        return Holder.INSTANCE;
    }

    /// 日志队列，用于存储待发送的日志事件
    private final LinkedBlockingQueue<LogEntity> queue = new LinkedBlockingQueue<>(5000);
    /// 发送配置
    private final AtomicBoolean running = new AtomicBoolean(true);
    /// 本地IP，用于发送日志时的服务器IP
    private String localIp;
    /// 待比较日志级别
    private Level compareLevel;
    /// 日志发送线程（实例级）
    private Thread flushThread;

    /// 日志服务器地址，可配置项
    private String serverUrl;
    /// 应用名称，可配置项
    private String appName;
    /// 应用密钥，可配置项
    private String appKey;
    /// 批量大小，可配置项
    private int batchSize = 20;
    /// 刷新间隔，可配置项
    private long flushInterval = 1000;
    /// 捕获的日志级别
    private String level = "INFO";
    /// 发送配置
    private SenderConfig config;

    static {
        // 仅初始化本地IP，不启动线程
        try {
            Holder.INSTANCE.localIp = InetAddress.getLocalHost().getHostAddress();
        } catch (Exception e) {
            Holder.INSTANCE.localIp = "unknown";
        }
    }

    @Override
    public void start() {
        super.start();
        localIp = getLocalIp();
        compareLevel = Level.toLevel(level);
        config = new SenderConfig();
        config.setServerUrl(serverUrl);
        config.setAppName(appName);
        config.setAppKey(appKey);
        config.setBatchSize(batchSize);
        config.setFlushInterval(flushInterval);
        config.setLevel(level);

        // 启动实例级的日志发送线程（仅在start()时启动）
        running.set(true);
        startFlushThread();
        addInfo("日志采集Appender启动成功 - 单线程安全模式");
    }

    /// 获取本地IP
    ///
    /// @return 本地IP
    private static String getLocalIp() {
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (Exception e) {
            return "unknown";
        }
    }

    /// 处理日志事件
    ///
    /// @param event 日志事件
    @Override
    protected void append(ILoggingEvent event) {
        if (!isStarted() || !running.get()) return;
        if (!event.getLevel().isGreaterOrEqual(compareLevel)) return;
        // 禁止采集日志框架自身日志（防止递归死循环！）
        String loggerName = event.getLoggerName();
        if (loggerName.startsWith("org.slf4j")
                || loggerName.startsWith("ch.qos.logback")
                || loggerName.startsWith("cn.aberic.tangduo.sdk.log")
                || loggerName.startsWith("cn.aberic.tangduo.common.http")) {
            return;
        }
        String msg = event.getFormattedMessage();
        if (msg.contains("untrace") || msg.contains("Failed to start thread")) return;

        try {
            LogEntity log = new LogEntity();
            // 补充：无TraceId时自动生成
            String traceId = MDC.get("traceId");
            if (traceId == null || traceId.isEmpty()) {
                traceId = MdcTraceTransfer.getOrGenerateTraceId();
            }
            log.setTraceId(traceId);
            log.setLevel(event.getLevel().toString());
            log.setMessage(event.getFormattedMessage());
            log.setThreadName(event.getThreadName());
            log.setLoggerName(loggerName);
            log.setTimestamp(event.getTimeStamp());
            log.setCreateTime(new Date(event.getTimeStamp()));
            log.setServerIp(localIp);

            IThrowableProxy throwableProxy = event.getThrowableProxy();
            if (throwableProxy != null) {
                String stackTrace = getStackTrace(throwableProxy);
                log.setException(stackTrace);
            }

            queue.offer(log);
        } catch (Exception ignore) {}
    }

    /// 获取异常堆栈信息
    ///
    /// @param throwableProxy 异常代理
    ///
    /// @return 异常堆栈信息
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

    /// 启动批量发送线程
    private void startFlushThread() {
        // 避免重复启动线程
        if (flushThread != null && flushThread.isAlive()) {
            return;
        }
        addInfo("启动批量发送线程");
        flushThread = new Thread(() -> {
            while (running.get()) {
                try {
                    LogBatchSender.sendBatch(config, queue, batchSize);
                    TimeUnit.MILLISECONDS.sleep(flushInterval);
                } catch (Exception ignored) {}
            }
        }, "log-collect");
        flushThread.setDaemon(true);
        flushThread.start();
    }

    /// 停止批量发送线程
    @Override
    public void stop() {
        if (!running.get()) {
            return;
        }
        running.set(false);
        if (flushThread != null && flushThread.isAlive()) {
            flushThread.interrupt(); // 中断线程（优雅停止）
            try {
                flushThread.join(1000); // 等待1秒，确保线程退出
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        // 发送剩余日志（关键：避免队列中日志丢失）
        if (!queue.isEmpty()) {
            try {
                LogBatchSender.sendBatch(config, queue, queue.size());
            } catch (Exception e) {
                addError("发送剩余日志失败", e);
            }
        }
        super.stop();
        addInfo("日志采集Appender已停止");
    }
}