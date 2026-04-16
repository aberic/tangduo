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

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;

/// 文件读取工具类
public final class Reader {

    private Reader() {
        throw new AssertionError("工具类禁止实例化");
    }

    /// 读取文件内容
    /// @param filepath 指定文件路径
    /// @throws IOException 异常
    public static byte[] read(String filepath) throws IOException {
        return Files.readAllBytes(Path.of(filepath));
    }

    /// 读取文件内容
    /// @param filepath 指定文件路径
    /// @param seek 指定读取位置
    /// @param length 指定读取长度
    /// @throws IOException 异常
    public static byte[] read(String filepath, long seek, int length) throws IOException {
        try (RandomAccessFile file = new RandomAccessFile(filepath, "r")) {
            file.seek(seek); // 移动到指定位置
            byte[] buffer = new byte[length];
            file.read(buffer, 0, length); // 在当前位置写入数据
            return buffer;
        } catch (IOException e) {
            throw new IOException(e);
        }
    }

    /// 读取文件内容
    /// @param filepath 指定文件路径
    /// @throws IOException 异常
    public static String readString(String filepath) throws IOException {
        return Files.readString(Path.of(filepath));
    }

    /// 读取文件内容
    /// @param filepath 指定文件路径
    /// @param seek 指定读取位置
    /// @param length 指定读取长度
    /// @throws IOException 异常
    public static String readString(String filepath, long seek, int length) throws IOException {
        return new String(read(filepath, seek, length));
    }
}
