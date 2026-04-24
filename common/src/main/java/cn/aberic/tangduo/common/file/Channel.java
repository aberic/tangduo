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

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.rmi.UnexpectedException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.LinkedBlockingQueue;
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
    /// 文件名，文件操作意图
    private static final Map<String, Intent> filepathIntentMap = new HashMap<>();
    private static Thread currentThread;

    /// 单线程真正写入磁盘（无竞争，最快）
    public static void startWriteThread() {
        // 避免重复启动线程
        if (currentThread != null && currentThread.isAlive()) {
            return;
        }
        log.info("untrace Channel startWriteThread");
        currentThread = new Thread(() -> {
            try {
                while (true) {
                    Intent.Buffer buffer = queue.take(); // 阻塞取数据
                    switch (buffer.status) {
                        case 0 -> { // 追加写
                            long channelSize = buffer.channel.size();
                            buffer.channel.position(channelSize);
                            buffer.buf.flip();
                            if (Objects.nonNull(buffer.appendCallback)) {
                                buffer.channel.write(buffer.buf); // 单线程写，无锁
                                buffer.appendCallback.seek(channelSize);
                                buffer.lock.lock();
                                try {
                                    buffer.isNotified.set(true);
                                    buffer.condition.signal();
                                } finally {
                                    buffer.lock.unlock();
                                }
                            }
                        }
                        case 1 -> { // 随机写
                            buffer.channel.position(buffer.seek);
                            buffer.buf.flip();
                            buffer.channel.write(buffer.buf); // 单线程写，无锁
                        }
                        case 2 -> { // 随机读
                            buffer.channel.position(buffer.seek);
                            buffer.channel.read(buffer.buf);
                            if (Objects.nonNull(buffer.readCallback)) {
                                buffer.readCallback.read(buffer.buf.array());
                                buffer.lock.lock();
                                try {
                                    buffer.isNotified.set(true);
                                    buffer.condition.signal();
                                } finally {
                                    buffer.lock.unlock();
                                }
                            }
                        }
                        default -> throw new UnexpectedException("Writer");
                    }
                    // 按需刷盘，不要每次都 force，非常慢
                }
            } catch (Exception e) {
                log.error("untrace Writer Thread {}, {}", "channel", e.getMessage(), e);
            }
        }, "channel");
        currentThread.start();
    }

    /// 获取指定文件的意图
    /// @param filepath 指定文件
    /// @return 指定文件的意图
    /// @throws IOException 指定文件不存在或无法定位数据！
    private static Intent getByFilepath(@NonNull String filepath) throws IOException {
        if (!filepathIntentMap.containsKey(filepath)) {
            synchronized (filepathIntentMap) {
                if (!filepathIntentMap.containsKey(filepath)) {
                    filepathIntentMap.put(filepath, new Intent(filepath));
                }
            }
        }
        return filepathIntentMap.get(filepath);
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
        AtomicLong atomicLong = new AtomicLong(-1);
        Lock lock = new ReentrantLock();
        Condition condition = lock.newCondition();
        AtomicBoolean isNotified = new AtomicBoolean(false);
        getByFilepath(filepath).append(bytes, lock, condition, isNotified, atomicLong::set);
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
    /// @param filepath 指定文件
    /// @param seek     指定起始位置
    /// @param bytes    指定字节数组
    /// @throws IOException 指定文件不存在或无法定位数据！
    public static void write(@NonNull String filepath, long seek, byte[] bytes) throws IOException {
        getByFilepath(filepath).write(seek, bytes);
    }

    /**
     * 在指定文件中指定起始位置开始读取字节数组
     *
     * @param filepath 指定文件
     * @param seek     指定起始位置
     */
    public static byte[] read(String filepath, long seek, int length) throws IOException {
        AtomicReference<byte[]> atomicReference = new AtomicReference<>();
        Lock lock = new ReentrantLock();
        Condition condition = lock.newCondition();
        AtomicBoolean isNotified = new AtomicBoolean(false);
        getByFilepath(filepath).read(seek, length, lock, condition, isNotified, atomicReference::set);
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
        getByFilepath(filepath).force();
    }

    /// 追加写回调接口
    public interface AppendCallback {
        /// 追加写回调接口
        /// @param seek 指定起始位置
        void seek(long seek);
    }

    /// 读取回调接口
    public interface ReadCallback {
        /// 读取回调接口
        /// @param res 指定读取结果
        void read(byte[] res);
    }



    static class Intent {
        /// 全局共享一个 Channel
        private final FileChannel channel;

        /// 缓冲区
        static class Buffer {
            /// 全局共享一个 Channel
            final FileChannel channel;
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
            /// @param buf 缓冲区
            /// @param lock 锁
            /// @param condition 条件变量
            /// @param isNotified 是否通知
            /// @param appendCallback 追加写回调接口
            public Buffer(FileChannel channel, ByteBuffer buf, Lock lock, Condition condition, AtomicBoolean isNotified, AppendCallback appendCallback) {
                this.channel = channel;
                this.buf = buf;
                this.appendCallback = appendCallback;
                this.lock = lock;
                this.condition = condition;
                this.isNotified = isNotified;
                this.status = 0;
            }

            /// write
            /// @param buf 缓冲区
            /// @param seek 指定起始位置
            public Buffer(FileChannel channel, ByteBuffer buf, long seek) {
                this.channel = channel;
                this.buf = buf;
                this.seek = seek;
                this.status = 1;
            }

            /// read
            /// @param seek 指定起始位置
            /// @param length 指定读取长度
            /// @param lock 锁
            /// @param condition 条件变量
            /// @param isNotified 是否通知
            /// @param readCallback 读取回调接口
            public Buffer(FileChannel channel, long seek, int length, Lock lock, Condition condition, AtomicBoolean isNotified, ReadCallback readCallback) {
                this.channel = channel;
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

        /// 构造函数
        /// @param path 指定文件
        /// @throws IOException 异常
        public Intent(String path) throws IOException {
            RandomAccessFile file = new RandomAccessFile(path, "rw");
            this.channel = file.getChannel();
        }

        // 多线程调用这个方法，只入队，不写文件
        /// @param bytes 指定写入数据
        /// @param lock 锁
        /// @param condition 条件变量
        /// @param isNotified 是否通知
        /// @param appendCallback 追加写回调接口
        public void append(byte[] bytes, Lock lock, Condition condition, AtomicBoolean isNotified, AppendCallback appendCallback) {
            ByteBuffer buf = ByteBuffer.allocate(bytes.length);
            buf.put(bytes);
            queue.offer(new Buffer(channel, buf, lock, condition, isNotified, appendCallback)); // 无锁，极快
        }

        // 多线程调用这个方法，只入队，不写文件
        /// @param seek 指定起始位置
        /// @param bytes 指定写入数据
        public void write(long seek, byte[] bytes) {
            ByteBuffer buf = ByteBuffer.allocate(bytes.length);
            buf.put(bytes);
            queue.offer(new Buffer(channel, buf, seek)); // 无锁，极快
        }

        /// 多线程读取文件
        /// @param seek 指定起始位置
        /// @param length 指定读取长度
        /// @param lock 锁
        /// @param condition 条件变量
        /// @param isNotified 是否通知
        /// @param readCallback 读取回调接口
        public void read(long seek, int length, Lock lock, Condition condition, AtomicBoolean isNotified, ReadCallback readCallback) {
            queue.offer(new Buffer(channel, seek, length, lock, condition, isNotified, readCallback)); // 无锁，极快
        }

        /// 强制写入磁盘
        /// @throws IOException 异常
        public void force() throws IOException {
            channel.force(true);
        }
    }

}
