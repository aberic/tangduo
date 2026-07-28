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

package cn.aberic.tangduo.common.file;

import lombok.Getter;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import java.io.Closeable;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
public final class Channel {

    private Channel() {
        throw new AssertionError("工具类禁止实例化");
    }

    /// 无锁队列，线程安全
    private static final LinkedBlockingQueue<Intent.Buffer> queue = new LinkedBlockingQueue<>(10000);
    /// 写线程实例（volatile 保证多线程可见性）
    private static volatile Thread currentThread;
    /// 写线程运行标志
    private static volatile boolean running = false;
    /// 是否接收新请求
    private static volatile boolean acceptNewRequest = true;
    /// 专用锁对象：final 修饰，引用不可变，彻底消除同步警告
    private static final Object CACHE_LOCK = new Object();
    /// LRU文件意图缓存
    private static volatile FileIntentLRUCache intentCache;
    /// 默认最大缓存文件数
    private static final int DEFAULT_MAX_CACHE_SIZE = 100;

    /// 初始化缓存配置（Spring启动时调用，或手动调用）
    ///
    /// @param maxCacheSize 最大缓存文件句柄数
    public static void init(int maxCacheSize) {
        if (intentCache != null) {
            return;
        }
        synchronized (CACHE_LOCK) {
            if (intentCache != null) {
                return;
            }
            int size = maxCacheSize > 0 ? maxCacheSize : DEFAULT_MAX_CACHE_SIZE;
            intentCache = new FileIntentLRUCache(size);
            log.info("Channel LRU cache initialized, max size: {}", size);
        }
    }

    /// 预热指定文件列表
    ///
    /// @param filepaths 需要预热的文件路径集合
    public static void preloadFiles(Collection<String> filepaths) {
        if (filepaths == null || filepaths.isEmpty()) {
            return;
        }
        if (intentCache == null) {
            init(DEFAULT_MAX_CACHE_SIZE);
        }
        int success = 0;
        for (String filepath : filepaths) {
            try {
                getByFilepath(filepath);
                success++;
            } catch (Exception e) {
                log.warn("Preload file failed: {}", filepath, e);
            }
        }
        log.info("Channel preload complete, success: {}, total: {}", success, filepaths.size());
    }

    /// 判断是否还能接收新请求
    /// 所有对外读写方法入口都要先调用该校验
    private static void checkState() throws IOException {
        if (!acceptNewRequest) {
            throw new IOException("Channel is shutting down, reject new request");
        }
    }

    /// 单线程真正写入磁盘（无竞争，最快）
    public static void startWriteThread() {
        // 避免重复启动线程
        if (currentThread != null && currentThread.isAlive()) {
            return;
        }
        if (intentCache == null) {
            init(DEFAULT_MAX_CACHE_SIZE);
        }
        running = true;
        acceptNewRequest = true;
        log.info("Channel untrace Channel startWriteThread");
        currentThread = new Thread(() -> {
            // 停止标志关闭 + 队列清空 才退出，保证剩余数据落盘
            while (running || !queue.isEmpty()) {
                try {
                    Intent.Buffer buffer = queue.poll(1, TimeUnit.SECONDS); // 阻塞取数据
                    if (buffer == null) {
                        continue;
                    }
                    // 动态获取channel，避免淘汰关闭后任务失败
                    Intent intent;
                    try {
                        intent = getByFilepath(buffer.filepath);
                    } catch (IOException e) {
                        log.error("Get channel failed for file: {}", buffer.filepath, e);
                        // 异常兜底：唤醒等待线程，避免永久阻塞
                        signalTaskFinish(buffer);
                        continue;
                    }
                    FileChannel channel = intent.getChannel();
                    try {
                        switch (buffer.status) {
                            case 0 -> { // 追加写
                                long channelSize = channel.size();
                                channel.position(channelSize);
                                buffer.buf.flip();
                                if (Objects.nonNull(buffer.appendCallback)) {
                                    channel.write(buffer.buf); // 单线程写，无锁
                                    buffer.appendCallback.seek(channelSize);
                                    signalTaskFinish(buffer);
                                }
                            }
                            case 1 -> { // 随机写
                                channel.position(buffer.seek);
                                buffer.buf.flip();
                                channel.write(buffer.buf); // 单线程写，无锁
                            }
                            case 2 -> { // 随机读
                                channel.position(buffer.seek);
                                channel.read(buffer.buf);
                                if (Objects.nonNull(buffer.readCallback)) {
                                    buffer.readCallback.read(buffer.buf.array());
                                    signalTaskFinish(buffer);
                                }
                            }
                            default -> log.error("Channel unknown buffer status: {}", buffer.status);
                        }
                    } catch (IOException e) {
                        log.error("Channel process task error, file: {}", buffer.filepath, e);
                        signalTaskFinish(buffer); // 异常也唤醒
                    }
                    // 按需刷盘，不要每次都 force，非常慢
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    // 单条任务异常不终止线程，继续处理后续任务
                    log.error("Channel untrace Writer Thread {}, {}", "channel", e.getMessage(), e);
                }
            }
            log.info("Channel thread stopped");
        }, "channel");
        currentThread.start();
    }

    /// 统一唤醒等待线程的兜底方法
    private static void signalTaskFinish(Intent.Buffer buffer) {
        if (buffer.lock != null && buffer.isNotified != null && buffer.condition != null) {
            buffer.lock.lock();
            try {
                buffer.isNotified.set(true);
                buffer.condition.signal();
            } finally {
                buffer.lock.unlock();
            }
        }
    }

    /// 获取指定文件的意图（双重检查锁 + 有效性校验）
    ///
    /// @param filepath 指定文件
    ///
    /// @return 指定文件的意图
    ///
    /// @throws IOException 指定文件不存在或无法定位数据！
    private static Intent getByFilepath(@NonNull String filepath) throws IOException {
        Intent intent;
        synchronized (CACHE_LOCK) {
            intent = intentCache.get(filepath);
            if (intent == null || !intent.isOpen()) {
                intent = new Intent(filepath);
                intentCache.put(filepath, intent);
            }
        }
        return intent;
    }

    /// 主动关闭指定文件并从缓存中移除
    /// <p>适用场景：文件删除重建、主动释放冷文件、异常重置通道等</p>
    ///
    /// @param filepath 文件路径
    public static void closeFile(String filepath) {
        synchronized (CACHE_LOCK) {
            Intent intent = intentCache.remove(filepath);
            if (intent != null) {
                try {
                    intent.close();
                } catch (IOException e) {
                    log.error("Close file {} error", filepath, e);
                }
            }
        }
    }

    /**
     * 优雅停机：停止写线程、关闭所有文件、清空缓存
     * 程序退出前必须调用此方法，避免文件句柄泄漏与数据丢失
     */
    public static void shutdown() {
        // 1. 先拒绝新请求，防止队列持续新增
        acceptNewRequest = false;
        log.info("Channel stop accepting new requests, start shutdown");
        // 2. 标记写线程停止
        running = false;
        // 3. 等待写线程排空队列
        if (currentThread != null) {
            try {
                currentThread.join(5000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("Channel shutdown wait interrupted");
            }
        }
        // 4. 遍历所有文件，执行一次强制刷盘，再关闭
        synchronized (CACHE_LOCK) {
            for (Intent intent : intentCache.values()) {
                try {
                    intent.force();
                    intent.close();
                } catch (IOException e) {
                    log.error("Close file error", e);
                }
            }
            intentCache.clear();
        }
        // 5. 清空队列
        queue.clear();
        log.info("Channel shutdown complete");
    }

    /**
     * 向指定文件中追加指定字节数组
     *
     * @param filepath 指定文件
     * @param bytes    指定字节数组
     *
     * @return 字节数组写入后在文件中的起始坐标
     */
    public static long append(@NonNull String filepath, byte[] bytes) throws IOException {
        checkState();
        AtomicLong atomicLong = new AtomicLong(-1);
        Lock lock = new ReentrantLock();
        Condition condition = lock.newCondition();
        AtomicBoolean isNotified = new AtomicBoolean(false);
        getByFilepath(filepath).append(filepath, bytes, lock, condition, isNotified, atomicLong::set);
        lock.lock();
        try {
            while (!isNotified.get()) {
                condition.await();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            lock.unlock();
        }
        return atomicLong.get();
    }

    /// 向指定文件中指定起始位置开始写入指定字节数组
    ///
    /// @param filepath 指定文件
    /// @param seek     指定起始位置
    /// @param bytes    指定字节数组
    ///
    /// @throws IOException 指定文件不存在或无法定位数据！
    public static void write(@NonNull String filepath, long seek, byte[] bytes) throws IOException {
        checkState();
        getByFilepath(filepath).write(filepath, seek, bytes);
    }

    /**
     * 在指定文件中指定起始位置开始读取字节数组
     *
     * @param filepath 指定文件
     * @param seek     指定起始位置
     */
    public static byte[] read(String filepath, long seek, int length) throws IOException {
        checkState();
        AtomicReference<byte[]> atomicReference = new AtomicReference<>();
        Lock lock = new ReentrantLock();
        Condition condition = lock.newCondition();
        AtomicBoolean isNotified = new AtomicBoolean(false);
        getByFilepath(filepath).read(filepath, seek, length, lock, condition, isNotified, atomicReference::set);
        lock.lock();
        try {
            while (!isNotified.get()) {
                condition.await();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            lock.unlock();
        }
        return atomicReference.get();
    }

    /**
     * 刷盘
     *
     * @param filepath 指定文件
     */
    public static void force(String filepath) throws IOException {
        checkState();
        getByFilepath(filepath).force();
    }

    /// 追加写回调接口
    public interface AppendCallback {
        /// 追加写回调接口
        ///
        /// @param seek 指定起始位置
        void seek(long seek);
    }

    /// 读取回调接口
    public interface ReadCallback {
        /// 读取回调接口
        ///
        /// @param res 指定读取结果
        void read(byte[] res);
    }

    /**
     * LRU文件意图缓存内部实现
     * 基于LinkedHashMap，访问后自动排序，超容量自动淘汰并关闭资源
     */
    private static class FileIntentLRUCache {
        private final LinkedHashMap<String, Intent> cache;

        public FileIntentLRUCache(int maxSize) {
            // accessOrder=true：按访问顺序排序，get操作后元素移到尾部
            this.cache = new LinkedHashMap<>(16, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, Intent> eldest) {
                    if (size() > maxSize) {
                        // 淘汰前主动关闭文件句柄
                        try {
                            eldest.getValue().close();
                            log.debug("LRU eliminate idle file: {}", eldest.getKey());
                        } catch (IOException e) {
                            log.error("Close eliminated file error: {}", eldest.getKey(), e);
                        }
                        return true;
                    }
                    return false;
                }
            };
        }

        public Intent get(String key) {
            return cache.get(key);
        }

        public void put(String key, Intent intent) {
            cache.put(key, intent);
        }

        public Intent remove(String key) {
            return cache.remove(key);
        }

        public Collection<Intent> values() {
            return cache.values();
        }

        public void clear() {
            cache.clear();
        }
    }

    @Getter
    static class Intent implements Closeable {
        /// 强持有 RandomAccessFile，防止GC意外回收，同时作为统一关闭入口
        private final RandomAccessFile raf;
        /// 全局共享一个 Channel
        private final FileChannel channel;

        /// 缓冲区任务类：只存filepath，不持有channel引用
        static class Buffer {
            final String filepath;
            /// 缓冲区
            ByteBuffer buf;
            /// append 系列参数
            AppendCallback appendCallback;
            /// write 系列参数
            long seek;
            /// read 系列参数 + write 系列参数
            int length;
            /// read 系列参数 + write 系列参数
            ReadCallback readCallback;
            /// 是否通知
            AtomicBoolean isNotified;
            /// 锁
            Lock lock;
            /// 条件变量
            Condition condition;
            /// 0-追加写、1-随机写、2-随机读
            int status;

            /// append
            ///
            /// @param buf            缓冲区
            /// @param lock           锁
            /// @param condition      条件变量
            /// @param isNotified     是否通知
            /// @param appendCallback 追加写回调接口
            public Buffer(String filepath, ByteBuffer buf, Lock lock, Condition condition, AtomicBoolean isNotified, AppendCallback appendCallback) {
                this.filepath = filepath;
                this.buf = buf;
                this.appendCallback = appendCallback;
                this.lock = lock;
                this.condition = condition;
                this.isNotified = isNotified;
                this.status = 0;
            }

            /// write
            ///
            /// @param buf  缓冲区
            /// @param seek 指定起始位置
            public Buffer(String filepath, ByteBuffer buf, long seek) {
                this.filepath = filepath;
                this.buf = buf;
                this.seek = seek;
                this.status = 1;
            }

            /// read
            ///
            /// @param seek         指定起始位置
            /// @param length       指定读取长度
            /// @param lock         锁
            /// @param condition    条件变量
            /// @param isNotified   是否通知
            /// @param readCallback 读取回调接口
            public Buffer(String filepath, long seek, int length, Lock lock, Condition condition, AtomicBoolean isNotified, ReadCallback readCallback) {
                this.filepath = filepath;
                this.buf = ByteBuffer.allocate(length);
                this.seek = seek;
                this.length = length;
                this.readCallback = readCallback;
                this.lock = lock;
                this.condition = condition;
                this.isNotified = isNotified;
                this.status = 2;
            }
        }

        /// 构造函数：保证异常安全
        ///
        /// @param path 指定文件
        ///
        /// @throws IOException 异常
        public Intent(String path) throws IOException {
            // 第一步：打开文件
            this.raf = new RandomAccessFile(path, "rw");
            try {
                // 第二步：获取通道；后续所有初始化逻辑都放在try块内
                this.channel = raf.getChannel();
                // 若未来新增其他初始化逻辑，统一写在这里
            } catch (RuntimeException e) {
                // 构造失败：主动关闭已打开的文件，避免资源泄漏
                raf.close();
                throw e;
            }
        }

        /// 通道是否处于打开状态
        public boolean isOpen() {
            return channel.isOpen();
        }

        /// 统一资源释放：关闭文件同时自动关闭关联的 FileChannel
        @Override
        public void close() throws IOException {
            if (raf != null) {
                raf.close();
            }
        }

        // 多线程调用这个方法，只入队，不写文件

        /// @param bytes          指定写入数据
        /// @param lock           锁
        /// @param condition      条件变量
        /// @param isNotified     是否通知
        /// @param appendCallback 追加写回调接口
        public void append(String filepath, byte[] bytes, Lock lock, Condition condition, AtomicBoolean isNotified, AppendCallback appendCallback) {
            ByteBuffer buf = ByteBuffer.allocate(bytes.length);
            buf.put(bytes);
            try {
                queue.put(new Buffer(filepath, buf, lock, condition, isNotified, appendCallback)); // 无锁，极快
            } catch (InterruptedException e) {
                log.error("Channel append 队列已满，阻塞或增加失败，{}", e.getMessage());
            }
        }

        // 多线程调用这个方法，只入队，不写文件

        /// @param seek  指定起始位置
        /// @param bytes 指定写入数据
        public void write(String filepath, long seek, byte[] bytes) {
            ByteBuffer buf = ByteBuffer.allocate(bytes.length);
            buf.put(bytes);
            try {
                queue.put(new Buffer(filepath, buf, seek)); // 无锁，极快
            } catch (InterruptedException e) {
                log.error("Channel write 队列已满，阻塞或增加失败，{}", e.getMessage());
            }
        }

        /// 多线程读取文件
        ///
        /// @param seek         指定起始位置
        /// @param length       指定读取长度
        /// @param lock         锁
        /// @param condition    条件变量
        /// @param isNotified   是否通知
        /// @param readCallback 读取回调接口
        public void read(String filepath, long seek, int length, Lock lock, Condition condition, AtomicBoolean isNotified, ReadCallback readCallback) {
            try {
                queue.put(new Buffer(filepath, seek, length, lock, condition, isNotified, readCallback)); // 无锁，极快
            } catch (InterruptedException e) {
                log.error("Channel read 队列已满，阻塞或增加失败，{}", e.getMessage());
            }
        }

        /// 强制写入磁盘
        ///
        /// @throws IOException 异常
        public void force() throws IOException {
            channel.force(true);
        }
    }

}
