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

package cn.aberic.tangduo.common;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/// 字节数组工具类
public final class ByteTools {

    private ByteTools() {
        throw new AssertionError("工具类禁止实例化");
    }

    /// 从字节数组中读取指定位置的字节数组
    /// @param bytes 待读取的字节数组
    /// @param position 读取位置
    /// @param length 读取长度
    /// @return 读取到的字节数组
    /// <p>
    /// 读取规则：从字节数组的指定位置开始读取指定长度的字节。
    /// </p>
    public static byte[] read(byte[] bytes, int position, int length) {
        return Arrays.copyOfRange(bytes, position, position + length);
    }

    /// 从字节数组中读取指定位置的字节数组
    /// @param bytes 待读取的字节数组
    /// @param position 读取位置
    /// @param length 读取长度
    /// @return 读取到的字节数组
    /// <p>
    /// 读取规则：从字节数组的指定位置开始读取指定长度的字节。
    /// </p>
    public static byte[] read(byte[] bytes, long position, int length) {
        return read(bytes, Math.toIntExact(position), length);
    }

    /// 合并多个字节数组
    /// @param bytes 多个字节数组
    /// @return 合并后的字节数组
    /// @throws IOException 如果合并过程中发生IO异常
    public static byte[] join(byte[]... bytes) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        for (byte[] bs : bytes) {
            outputStream.write(bs);
        }
        return outputStream.toByteArray();
    }

    /// 字符串转换为字节数组
    /// @param res 待转换的字符串
    /// @return 转换后的字节数组
    /// <p>
    /// 转换规则：将字符串按UTF-8编码转换为字节数组。
    /// </p>
    public static byte[] fromString(String res) {
        return res.getBytes(StandardCharsets.UTF_8);
    }

    /// 将字节数组转换为字符串
    /// @param bytes 待转换的字节数组
    /// @return 转换后的字符串
    /// <p>
    /// 转换规则：将字节数组按UTF-8编码解释为字符串。
    /// </p>
    public static String toString(byte[] bytes) {
        return new String(bytes, StandardCharsets.UTF_8);
    }

    /// 将长整数转换为字节数组
    /// @param l 待转换的长整数
    /// @return 转换后的字节数组
    /// <p>
    /// 转换规则：将长整数的每个字节从高到低存储在字节数组中。
    /// </p>
    public static byte[] fromLong(long l) {
        return new byte[]{
                (byte) (l >> 56),
                (byte) (l >> 48),
                (byte) (l >> 40),
                (byte) (l >> 32),
                (byte) (l >> 24),
                (byte) (l >> 16),
                (byte) (l >> 8),
                (byte) l,
        };
    }
    /// @param bytes 待转换的字节数组
    /// @return 转换后的长整数
    /// <p>
    /// 转换规则：将字节数组的前8个字节按大端序解释为长整数。
    /// </p>
    public static long toLong(byte[] bytes) {
        if (bytes.length > 8) {
            throw new RuntimeException(String.format("trans bytes 2 u64 out of bounds, except le 8, but receive %s", bytes.length));
        }
        if (bytes.length < 8) {
            try {
                bytes = join(new byte[8 - bytes.length], bytes);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        long result = 0;
        for (int i = 0; i < bytes.length; i++) {
            result |= ((long) bytes[i] & 0xFF) << (8 * (7 - i));
        }
        return result;
    }

    /// 将整数转换为字节数组
    /// @param i 待转换的整数
    /// @return 转换后的字节数组
    /// <p>
    /// 转换规则：将整数的每个字节从高到低存储在字节数组中。
    /// </p>
    public static byte[] fromInt(int i) {
        return new byte[]{
                (byte) ((i >> 24) & 0xFF),
                (byte) ((i >> 16) & 0xFF),
                (byte) ((i >> 8) & 0xFF),
                (byte) (i & 0xFF)
        };
    }

    /// @param bytes 待转换的字节数组
    /// @return 转换后的整数
    /// <p>
    /// 转换规则：将字节数组的前4个字节按大端序解释为整数。
    /// </p>
    public static int toInt(byte[] bytes) {
        if (bytes.length > 4) {
            throw new RuntimeException(String.format("trans bytes 2 u32 out of bounds, except le 4, but receive %s", bytes.length));
        }
        if (bytes.length < 4) {
            try {
                bytes = join(new byte[4 - bytes.length], bytes);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        int result = 0;
        for (int i = 0; i < bytes.length; i++) {
            result |= (bytes[i] & 0xFF) << (8 * (3 - i));
        }
        return result;
    }

    /// 将字节数组转换为布尔值
    /// <p>
    /// 非零值为 true，零值为 false。
    /// </p>
    /// @param b 待转换的字节数组
    /// @return 转换后的布尔值
    /// <p>
    /// 转换规则：非零值为 true，零值为 false。
    /// </p>
    /// <p> 
    /// 注意：本方法仅处理字节数组的第一个字节，其他字节将被忽略。
    /// </p>
    public static boolean toBool(byte b) {
        return b != 0x00;
    }
    /// <p>
    /// 非零值为 true，零值为 false。
    /// </p>
    /// @param b 待转换的布尔值
    /// @return 转换后的字节数组
    /// <p>
    /// 转换规则：true 转换为 0x01，false 转换为 0x00。
    /// </p>
    /// <p>
    /// 注意：该方法仅支持将布尔值转换为字节数组，不支持将字节数组转换为布尔值。
    /// </p>    
    public static byte fromBool(boolean b) {
        return b ? (byte) 0x01 : 0x00;
    }

    /// @param d 待转换的双精度浮点数
    /// @return 转换后的字节数组
    /// <p>
    /// 转换规则：将双精度浮点数的二进制表示转换为字节数组。
    /// </p>
    /// <p>
    /// 注意：该方法仅支持将双精度浮点数转换为字节数组，不支持将字节数组转换为双精度浮点数。
    /// </p>
    public static byte[] fromDouble(double d) {
        long longValue = Double.doubleToLongBits(d);
        return fromLong(longValue);
    }

    /// 将字节数组转换为双精度浮点数
    public static double toDouble(byte[] bytes) {
        long l = toLong(bytes);
        return Double.longBitsToDouble(l);
    }

}
