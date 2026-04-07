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

import cn.aberic.tangduo.common.ByteTools;
import lombok.NonNull;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.Arrays;

public class Writer {

    public Writer() {
        throw new IllegalStateException("Writer class");
    }

    /**
     * 向指定文件中写入指定字符串，如果文件不存在，则创建文件
     *
     * @param filepath 指定文件
     * @param res      指定字符串
     */
    public static void writeForce(@NonNull String filepath, String res) throws IOException {
        try {
            write(filepath, res);
        } catch (NoSuchFileException e) {
            Filer.createFile(filepath);
            writeForce(filepath, res);
        } catch (IOException e) {
            throw new IOException(e);
        }
    }

    /**
     * 向指定文件中写入指定字符串
     *
     * @param filepath 指定文件
     * @param res      指定字符串
     */
    public static void write(@NonNull String filepath, String res) throws IOException {
        Files.writeString(Path.of(filepath), res);
    }

    /**
     * 向指定文件中写入指定字节数组
     *
     * @param filepath 指定文件
     * @param bytes    指定字节数组
     */
    public static void writeForce(@NonNull String filepath, byte[] bytes) throws IOException {
        try {
            write(filepath, bytes);
        } catch (NoSuchFileException e) {
            Filer.createFile(filepath);
            writeForce(filepath, bytes);
        } catch (IOException e) {
            throw new IOException(e);
        }
    }

    /**
     * 向指定文件中写入指定字节数组
     *
     * @param filepath 指定文件
     * @param bytes    指定字节数组
     */
    public static void write(@NonNull String filepath, byte[] bytes) throws IOException {
        Files.write(Path.of(filepath), bytes);
    }

    /**
     * 向指定文件中写入指定int64
     *
     * @param filepath 指定文件
     * @param l        指定int64
     */
    public static void writeForce(@NonNull String filepath, long l) throws IOException {
        try {
            write(filepath, l);
        } catch (NoSuchFileException e) {
            Filer.createFile(filepath);
            writeForce(filepath, ByteTools.fromLong(l));
        } catch (IOException e) {
            throw new IOException(e);
        }
    }

    /**
     * 向指定文件中写入指定int64
     *
     * @param filepath 指定文件
     * @param l        指定int64
     */
    public static void write(@NonNull String filepath, long l) throws IOException {
        write(filepath, ByteTools.fromLong(l));
    }

    /**
     * 向指定文件中追加指定int64
     *
     * @param filepath 指定文件
     * @param l        指定int64
     */
    public synchronized static long append(@NonNull String filepath, long l) throws IOException {
        return append(filepath, ByteTools.fromLong(l));
    }

    /**
     * 向指定文件中追加指定字节数组
     *
     * @param filepath 指定文件
     * @param bytes    指定字节数组
     *
     * @return 字节数组写入后在文件中的起始坐标
     */
    public synchronized static long append(@NonNull String filepath, byte[] bytes) throws IOException {
        // 默认分配4MB的缓冲区，即 4*1024*1024 byte
        int bufferSize = 4194304;
        if (bytes.length < bufferSize) {
            bufferSize = bytes.length;
        }
        return append(filepath, bufferSize, bytes);
    }

    /**
     * 向指定文件中追加指定字节数组
     *
     * @param filepath 指定文件
     * @param bytes    指定字节数组
     *
     * @return 字节数组写入后在文件中的起始坐标
     */
    public synchronized static long append(@NonNull String filepath, int bufferSize, byte[] bytes) throws IOException {
        long size = Files.size(Path.of(filepath));
        ByteBuffer buf = ByteBuffer.allocate(bufferSize);
        try (FileOutputStream fos = new FileOutputStream(filepath, true); FileChannel channel = fos.getChannel()) {
            int start = 0;
            while (start < bytes.length) {
                int to = start + bufferSize;
                if (to > bytes.length) {
                    to = bytes.length;
                    buf.put(Arrays.copyOfRange(bytes, start, to));
                    buf.flip(); // 切换为写模式
                    channel.write(buf); // 将缓冲区的数据写回文件
                    break;
                }
                buf.put(Arrays.copyOfRange(bytes, start, to));
                buf.flip(); // 切换为写模式
                while (buf.hasRemaining()) {
                    channel.write(buf); // 将缓冲区的数据写回文件
                }
                start += bufferSize;
                buf.clear(); // 清空缓冲区准备下一次使用
            }
        } catch (IOException e) {
            throw new IOException(e);
        }
        return size;
    }

    /**
     * 向指定文件中追加指定int64
     *
     * @param filepath 指定文件
     * @param l        指定int64
     */
    public synchronized static long appendForce(@NonNull String filepath, long l) throws IOException {
        return appendForce(filepath, ByteTools.fromLong(l));
    }

    /**
     * 向指定文件中追加指定字节数组
     *
     * @param filepath 指定文件
     * @param bytes    指定字节数组
     */
    public synchronized static long appendForce(@NonNull String filepath, byte[] bytes) throws IOException {
        try {
            return append(filepath, bytes);
        } catch (NoSuchFileException e) {
            Filer.createFile(filepath);
            return appendForce(filepath, bytes);
        } catch (IOException e) {
            throw new IOException(e);
        }
    }

    /**
     * 向指定文件中指定起始位置开始写入指定字符串
     *
     * @param filepath 指定文件
     * @param seek     指定起始位置
     * @param res      指定字符串
     */
    public static void write(@NonNull String filepath, long seek, String res) throws IOException {
        write(filepath, seek, res.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 向指定文件中指定起始位置开始写入指定int64
     *
     * @param filepath 指定文件
     * @param seek     指定起始位置
     * @param l        指定int64
     */
    public static void write(@NonNull String filepath, long seek, long l) throws IOException {
        write(filepath, seek, ByteTools.fromLong(l));
    }

    /**
     * 向指定文件中指定起始位置开始写入指定字节数组
     *
     * @param filepath 指定文件
     * @param seek     指定起始位置
     * @param bytes    指定字节数组
     */
    public static void write(@NonNull String filepath, long seek, byte[] bytes) throws IOException {
        // 默认分配4MB的缓冲区，即 4*1024*1024 byte
        int bufferSize = 4194304;
        if (bytes.length < bufferSize) {
            write(filepath, seek, bytes.length, bytes);
        } else {
            write(filepath, seek, 4194304, bytes);
        }
    }

    /**
     * 向指定文件中指定起始位置开始写入指定字节数组
     *
     * @param filepath 指定文件
     * @param seek     指定起始位置
     * @param bytes    指定字节数组
     */
    public static void write(@NonNull String filepath, long seek, int bufferSize, byte[] bytes) throws IOException {
        try (RandomAccessFile file = new RandomAccessFile(filepath, "rw"); FileChannel channel = file.getChannel()) {
            write(channel, seek, bufferSize, bytes);
        } catch (IOException e) {
            throw new IOException(e);
        }
    }

    /**
     * 向指定文件中指定起始位置开始写入指定字节数组
     *
     * @param channel IO通道
     * @param seek    指定起始位置
     * @param bytes   指定字节数组
     */
    public static void write(@NonNull FileChannel channel, long seek, byte[] bytes) throws IOException {
        // 默认分配4MB的缓冲区，即 4*1024*1024 byte
        int bufferSize = 4194304;
        if (bytes.length < bufferSize) {
            write(channel, seek, bytes.length, bytes);
        } else {
            write(channel, seek, 4194304, bytes);
        }
    }

    /**
     * 向指定文件中指定起始位置开始写入指定字节数组
     *
     * @param channel IO通道
     * @param seek    指定起始位置
     * @param bytes   指定字节数组
     */
    public static void write(@NonNull FileChannel channel, long seek, int bufferSize, byte[] bytes) throws IOException {
        ByteBuffer buf = ByteBuffer.allocate(bufferSize);
        channel.position(seek);
        int start = 0;
        while (start < bytes.length) {
            int to = start + bufferSize;
            if (to > bytes.length) {
                to = bytes.length;
                buf.put(Arrays.copyOfRange(bytes, start, to));
                buf.flip(); // 切换为写模式
                channel.write(buf); // 将缓冲区的数据写回文件
                break;
            } else {
                buf.put(Arrays.copyOfRange(bytes, start, to));
                buf.flip(); // 切换为写模式
                while (buf.hasRemaining()) {
                    channel.write(buf); // 将缓冲区的数据写回文件
                }
                start += bufferSize;
                buf.clear(); // 清空缓冲区准备下一次使用
            }
        }
    }

}
