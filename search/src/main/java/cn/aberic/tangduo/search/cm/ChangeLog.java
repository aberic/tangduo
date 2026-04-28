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

import cn.aberic.tangduo.common.JsonTools;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
public class ChangeLog {

    /// 无锁队列，线程安全
    private static final LinkedBlockingQueue<Change> queue = new LinkedBlockingQueue<>(10000);
    private static Thread currentThread;
    static final Lock lock = new ReentrantLock();

    record Change(String rootPath, String method, Object object) {}

    private ChangeLog() {
        throw new AssertionError("工具类禁止实例化");
    }

    /// 单线程真正写入磁盘（无竞争，最快）
    public static void startWriteThread() {
        // 避免重复启动线程
        if (currentThread != null && currentThread.isAlive()) {
            return;
        }
        log.info("untrace ChangeLog startWriteThread");
        currentThread = new Thread(() -> {
            try {
                while (true) {
                    Change change = queue.take(); // 阻塞取数据
                    append(change);
                }
            } catch (Exception e) {
                log.error("untrace Writer Thread {}, {}", "channel", e.getMessage(), e);
            }
        }, "channel");
        currentThread.start();
    }

    public static void append(String rootPath, String method, Object object) {
        queue.offer(new Change(rootPath, method, object));
    }

    private static void append(Change change) throws IOException {
        Path filepath = Path.of(change.rootPath, "change.log");
        lock.lock();
        StringBuilder sb = new StringBuilder();
        sb.append(change.method).append(" ");
        try {
            if (change.object instanceof String || change.object instanceof Number) {
                sb.append(change.object).append("\n");
            } else {
                sb.append(JsonTools.toJson(change.object)).append("\n");
            }
            Files.writeString(filepath, sb.toString(), StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,   // 不存在则创建
                    StandardOpenOption.APPEND // 追加模式
            );
        } finally {
            lock.unlock();
        }
    }

}
