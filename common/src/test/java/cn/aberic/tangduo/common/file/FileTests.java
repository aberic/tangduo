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

import cn.aberic.tangduo.common.Bytes;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class FileTests {

    @Test
    void writeString() throws IOException {
        String filepath = "tmp/writeString.txt";
        Writer.writeForce(filepath, "hello world!");
        System.out.println(Reader.readString(filepath));
    }

    @Test
    void writeStringBytes() throws IOException {
        String filepath = "tmp/writeStringBytes.txt";
        Writer.writeForce(filepath, "hello world!".getBytes(Charset.defaultCharset()));
        System.out.println(Reader.readString(filepath));
    }

    @Test
    void writeLong() throws IOException {
        String filepath = "tmp/writeLong.txt";
        Writer.writeForce(filepath, "999999999999");
        System.out.println(Reader.readString(filepath));
    }

    @Test
    void writeLongBytes() throws IOException {
        String filepath = "tmp/writeLongBytes.txt";
        Writer.writeForce(filepath, 999999999999L);
    }

    @Test
    void longTbytes() {
        long l = 999999999999999999L;
        byte[] bytes = Bytes.fromLong(l);
        System.out.println(l + " | " + Bytes.toLong(bytes));

        l = -999999999999999999L;
        bytes = Bytes.fromLong(l);
        System.out.println(l + " | " + Bytes.toLong(bytes));
    }

    @Test
    void appendLong() throws IOException {
        String filepath = "tmp/appendLong.txt";
        System.out.println(Writer.appendForce(filepath, 1));
        System.out.println(Writer.appendForce(filepath, 999999999999L));
        System.out.println(Writer.appendForce(filepath, 999999999999999999L));
        System.out.println(Bytes.toLong(Reader.read(filepath, 0, 8)));
        System.out.println(Bytes.toLong(Reader.read(filepath, 8, 8)));
        System.out.println(Bytes.toLong(Reader.read(filepath, 16, 8)));
    }

    @Test
    void writeRandom() throws IOException {
        String filepath = "tmp/writeRandom.txt";
        Writer.write(filepath, "0123456789");
        Writer.write(filepath, 0, "1");
        Writer.write(filepath, 1, "2");
        System.out.println(Reader.readString(filepath));
    }

    @Test
    void writeChannel1() throws IOException {
        writeChannel("tmp/writeChannel1.txt");
    }

    void writeChannel(String filepath) throws IOException {
        Writer.write(filepath, 0, 1024, ("测试".repeat(20) + "\n").repeat(50).getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void writeChannel2() throws IOException {
        String filepath = "tmp/writeChannel2.txt";
        Files.deleteIfExists(Path.of(filepath));
        writeChannel(filepath);
        Writer.write(filepath, ("测试".repeat(20) + "\n").getBytes(StandardCharsets.UTF_8).length, 1024, ("发布".repeat(20) + "\n").repeat(10).getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void writeChannel3() throws IOException {
        String filepath = "tmp/writeChannel3.txt";
        Files.deleteIfExists(Path.of(filepath));
        writeChannel(filepath);
        ByteBuffer buf = ByteBuffer.allocate(1024);
        try (RandomAccessFile file = new RandomAccessFile(filepath, "rw"); FileChannel channel = file.getChannel()) {
            long position = ("测试".repeat(20) + "\n").getBytes(StandardCharsets.UTF_8).length;
            System.out.println("position1 = " + position);
            channel.position(position);
            System.out.println("position2 = " + channel.position());
            byte[] bytes = "发布".getBytes(StandardCharsets.UTF_8);
            buf.put(bytes);
            buf.flip(); // 切换为写模式
            int res = channel.write(buf); // 将缓冲区的数据写回文件
            System.out.println("bytes length = " + bytes.length + " | res = " + res);
            System.out.println("position3 = " + channel.position());
            buf.clear(); // 清空缓冲区准备下一次使用
            channel.position(("测试".repeat(20) + "\n").repeat(10).getBytes(StandardCharsets.UTF_8).length);
            buf.put(bytes);
            buf.flip(); // 切换为写模式
            channel.write(buf); // 将缓冲区的数据写回文件
            buf.clear(); // 清空缓冲区准备下一次使用
            buf.put(bytes);
            buf.flip(); // 切换为写模式
            channel.position(channel.size());
            channel.write(buf); // 将缓冲区的数据写回文件
        } catch (IOException e) {
            throw new IOException(e);
        }
    }

    void channel(String filepath) throws IOException, InterruptedException {
        System.out.println("channel append seek = " + Channel.append(filepath, ("测试".repeat(20) + "\n").repeat(20).getBytes(StandardCharsets.UTF_8)));
        System.out.println("channel append seek = " + Channel.append(filepath, ("测试".repeat(20) + "\n").repeat(30).getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    void channel1() throws IOException, InterruptedException {
        String filepath = "tmp/channel1.txt";
        Files.deleteIfExists(Path.of(filepath));
        channel(filepath);
        byte[] bytes = "发布".getBytes(StandardCharsets.UTF_8);
        long position = ("测试".repeat(20) + "\n").getBytes(StandardCharsets.UTF_8).length;
        Channel.write(filepath, position, bytes);
        position = ("测试".repeat(20) + "\n").repeat(10).getBytes(StandardCharsets.UTF_8).length;
        Channel.write(filepath, position, bytes);

        System.out.println(new String(Channel.read(filepath, position, 12)));

        Channel.force(filepath);
    }

}