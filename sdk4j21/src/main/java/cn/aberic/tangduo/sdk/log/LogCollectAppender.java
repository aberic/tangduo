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

import java.net.InetAddress;
import java.util.Date;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.MDC;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.StackTraceElementProxy;
import ch.qos.logback.core.UnsynchronizedAppenderBase;
import lombok.Setter;

/// 日志采集器
@Setter
public class LogCollectAppender extends UnsynchronizedAppenderBase<ILoggingEvent> {

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

    /// 日志队列，用于存储待发送的日志事件
    private final LinkedBlockingQueue<LogEntity> queue = new LinkedBlockingQueue<>(5000);
    /// 发送配置
    private final AtomicBoolean running = new AtomicBoolean(true);
    /// 本地IP，用于发送日志时的服务器IP
    private String localIp;
    /// 发送配置
    private SenderConfig config;

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

    /// 获取本地IP
    /// @return 本地IP
    private String getLocalIp() {
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (Exception e) {
            return "unknown";
        }
    }

    /// 处理日志事件
    /// @param event 日志事件
    @Override
    protected void append(ILoggingEvent event) {
        if (!isStarted() || !running.get())
            return;
        if (event.getLevel() == Level.DEBUG || event.getLevel() == Level.TRACE)
            return;

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

            queue.offer(log);
        } catch (Exception ignore) {
        }
    }

    /// 获取异常堆栈信息
    /// @param throwableProxy 异常代理
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
            sb.append("\nCaused by: ").append(cause.getClassName()).append(": ").append(cause.getMessage())
                    .append("\n");
            for (StackTraceElementProxy step : cause.getStackTraceElementProxyArray()) {
                sb.append("\tat ").append(step.getStackTraceElement()).append("\n");
            }
            cause = cause.getCause();
        }
        return sb.toString();
    }

    /// 启动批量发送线程
    /// @param config 发送配置
    /// @param queue 日志队列
    /// @param batchSize 批量大小
    /// @param flushInterval 刷新间隔
    private void startFlushThread() {
        Thread t = new Thread(() -> {
            while (running.get()) {
                try {
                    LogBatchSender.sendBatch(config, queue, batchSize);
                    TimeUnit.MILLISECONDS.sleep(flushInterval);
                } catch (Exception ignored) {
                }
            }
        }, "log-collect");
        t.setDaemon(true);
        t.start();
    }

    /// 停止批量发送线程
    @Override
    public void stop() {
        running.set(false);
        super.stop();
    }
}