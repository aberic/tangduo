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

package cn.aberic.tangduo.search.cm;

import cn.aberic.tangduo.common.file.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Component
public class ChannelLifecycle implements SmartLifecycle {

    private final ChannelProperties channelProperties;
    private final AtomicBoolean running = new AtomicBoolean(false);

    public ChannelLifecycle(ChannelProperties channelProperties) {this.channelProperties = channelProperties;}

    /// 容器启动后自动执行
    @Override
    public void start() {
        if (running.compareAndSet(false, true)) {
            log.info("Channel lifecycle start, init write thread");
            // 1. 初始化LRU缓存
            Channel.init(channelProperties.getMaxCacheSize());
            log.info("maxCacheSize = {}", channelProperties.getMaxCacheSize());
            // 2. 启动写线程
            Channel.startWriteThread();
            // 3. 预热核心文件
            Channel.preloadFiles(channelProperties.getPreloadFiles());
        }
    }

    /**
     * 容器停止时自动执行
     */
    @Override
    public void stop() {
        if (running.compareAndSet(true, false)) {
            log.info("Channel lifecycle stop, shutdown resources");
            Channel.shutdown();
        }
    }

    @Override
    public boolean isRunning() {
        return running.get();
    }

    /**
     * 优先级：数值越小，停止时越先执行
     * 文件通道属于底层IO，建议在业务Bean之后、数据库之前关闭
     */
    @Override
    public int getPhase() {
        return 0;
    }

    /**
     * 是否在容器刷新完成后自动启动
     */
    @Override
    public boolean isAutoStartup() {
        return true;
    }

    @Override
    public void stop(Runnable callback) {
        stop();
        callback.run();
    }
}