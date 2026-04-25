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

package cn.aberic.tangduo.client;

import cn.aberic.tangduo.sdk.log.MdcTraceTransfer;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Service
/// Spring 启动时就创建、初始化
/// 只要满足下面任意一条，必须加：
/// 1、定时任务希望项目启动就自动跑
/// 2、类里有 @PostConstruct 方法
/// 3、不想让它懒加载、延迟启动
@Lazy(value = false)
public class AutoIncludeScheduled {

    @Value("${spring.config.activate.on-profile:prod}")
    String profile;
    // 防定时任务重复执行（核心：避免多次调用叠加创建线程）
    private final AtomicBoolean taskRunning = new AtomicBoolean(false);

    ThreadPoolExecutor poolExecutor = new ThreadPoolExecutor(
            32,
            256,
            60L,
            TimeUnit.SECONDS,
            new SynchronousQueue<>(),
            new ThreadPoolExecutor.CallerRunsPolicy()
    );

    /// initialDelay 项目启动后，等 3 秒再第一次执行。
    /// fixedDelay 上一次执行完之后，等 3 秒，再执行下一次。等上一次跑完再计时（安全、不会叠加）
    /// fixedRate：到点就执行，不管上一次完没完（可能并发堆积）
    @Scheduled(initialDelay = 3000, fixedDelay = 3000)
    public void traceTest() {
        // 防止上一次任务没执行完，重复创建线程
        if (!taskRunning.compareAndSet(false, true)) {
            return;
        }
        try {
            if (profile.equals("dev")) {
                MDC.clear();
                log.info(DocGenerator.generateDoc());
                traceCycle(10);
            }
        } finally {
            taskRunning.set(false);
        }
    }

    private void traceCycle(int times) {
        poolExecutor.execute(MdcTraceTransfer.wrapRunnable(() -> {
            try {
                if (times > 0) {
                    Thread.sleep(1000); // 虚拟线程里 sleep 几乎无开销
                    log.info(DocGenerator.generateDoc());
                    Thread.sleep(10);
                    traceCycle(times - 1);
                }
                // 任务
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }));
    }

}
