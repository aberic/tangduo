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
public class Channel {

    public Channel() {
        throw new IllegalStateException("Channel class");
    }

    /** 文件名，文件操作意图 */
    private static final Map<String, Intent> filepathIntentMap = new HashMap<>();

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

    /**
     * 向指定文件中指定起始位置开始写入指定字节数组
     *
     * @param filepath 指定文件
     * @param seek     指定起始位置
     * @param bytes    指定字节数组
     */
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


    public interface AppendCallback {
        void seek(long seek);
    }

    public interface ReadCallback {
        void read(byte[] res);
    }

    static class Intent {
        // 全局共享一个 Channel
        private final FileChannel channel;
        // 无锁队列，线程安全
        private final LinkedBlockingQueue<Buffer> queue = new LinkedBlockingQueue<>(10000);

        static class Buffer {
            ByteBuffer buf;
            // append 系列参数
            AppendCallback appendCallback;
            // write 系列参数
            long seek;
            // read 系列参数 + write 系列参数
            int length;
            ReadCallback readCallback;
            AtomicBoolean isNotified;

            Lock lock;
            Condition condition;

            /** 0-追加写、1-随机写、2-随机读 */
            int status;

            /** append */
            public Buffer(ByteBuffer buf, Lock lock, Condition condition, AtomicBoolean isNotified, AppendCallback appendCallback) {
                this.buf = buf;
                this.appendCallback = appendCallback;
                this.lock = lock;
                this.condition = condition;
                this.isNotified = isNotified;
                this.status = 0;
            }

            /** write */
            public Buffer(ByteBuffer buf, long seek) {
                this.buf = buf;
                this.seek = seek;
                this.status = 1;
            }

            /** read */
            public Buffer(long seek, int length, Lock lock, Condition condition, AtomicBoolean isNotified, ReadCallback readCallback) {
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

        public Intent(String path) throws IOException {
            RandomAccessFile file = new RandomAccessFile(path, "rw");
            this.channel = file.getChannel();
            // 启动单线程写入线程
            startWriteThread(path);
        }

        // 多线程调用这个方法，只入队，不写文件
        public void append(byte[] bytes, Lock lock, Condition condition, AtomicBoolean isNotified, AppendCallback appendCallback) {
            ByteBuffer buf = ByteBuffer.allocate(bytes.length);
            buf.put(bytes);
            queue.offer(new Buffer(buf, lock, condition, isNotified, appendCallback)); // 无锁，极快
        }

        // 多线程调用这个方法，只入队，不写文件
        public void write(long seek, byte[] bytes) {
            ByteBuffer buf = ByteBuffer.allocate(bytes.length);
            buf.put(bytes);
            queue.offer(new Buffer(buf, seek)); // 无锁，极快
        }

        public void read(long seek, int length, Lock lock, Condition condition, AtomicBoolean isNotified, ReadCallback readCallback) {
            queue.offer(new Buffer(seek, length, lock, condition, isNotified, readCallback)); // 无锁，极快
        }

        public void force() throws IOException {
            channel.force(true);
        }

        // 单线程真正写入磁盘（无竞争，最快）
        private void startWriteThread(String path) {
            new Thread(() -> {
                try {
                    while (true) {
                        Buffer buffer = queue.take(); // 阻塞取数据
                        switch (buffer.status) {
                            case 0 -> { // 追加写
                                long channelSize = channel.size();
                                channel.position(channelSize);
                                buffer.buf.flip();
                                if (Objects.nonNull(buffer.appendCallback)) {
                                    channel.write(buffer.buf); // 单线程写，无锁
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
                                channel.position(buffer.seek);
                                buffer.buf.flip();
                                channel.write(buffer.buf); // 单线程写，无锁
                            }
                            case 2 -> { // 随机读
                                channel.position(buffer.seek);
                                channel.read(buffer.buf);
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
                    log.error("Writer Thread {}, {}", path, e.getMessage(), e);
                }
            }, path).start();
        }
    }

}
