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

package cn.aberic.tangduo.db.common;

import java.nio.charset.StandardCharsets;

public final class KeyHashTools {

    private KeyHashTools() {
        throw new AssertionError("工具类禁止实例化");
    }

    // 固定种子（保证相同字符串永远生成相同key）
    private static final long SEED = 0x9E3779B97F4A7C15L;

    /// 任意字符串 → 稳定唯一 long（Btree 索引专用）
    /// @param str 待转换字符串
    /// @return 稳定唯一 long
    public static long toLongKey(String str) {
        if (str == null || str.isEmpty()) {
            return 0L;
        }
        try {
            return Long.parseLong(str);
        } catch (NumberFormatException e) {
            byte[] data = str.getBytes(StandardCharsets.UTF_8);
            return murmurHash64(data);
        }
    }

    /// double 转 long（Btree 索引 key 标准）
    /// @param d 待转换 double
    /// @return 稳定唯一 long
    public static long toLongKey(double d) {
        return Double.doubleToRawLongBits(d);
    }

    /// long 还原回 double
    /// @param l 待还原 long
    /// @return 还原后的 double
    public static double longToDouble(long l) {
        return Double.longBitsToDouble(l);
    }

    /// float 转 long（Btree 索引 key 标准）
    /// @param f 待转换 float
    /// @return 稳定唯一 long
    public static long toLongKey(float f) {
        // float 转 int 再转 long，保证 Btree 排序正确
        return Float.floatToRawIntBits(f) & 0xFFFFFFFFL;
    }

    /// long 还原回 float
    /// @param l 待还原 long
    /// @return 还原后的 float
    public static float longToFloat(long l) {
        return Float.intBitsToFloat((int) l);
    }

    /// MurmurHash64 最终版实现（官方最新，无废弃）
    /// @param data 待哈希数据
    /// @return 哈希值
    private static long murmurHash64(byte[] data) {
        final int len = data.length;
        long h = KeyHashTools.SEED;
        int i = 0;
        while (i <= len - 8) {
            long k =
                    ((long) data[i++] & 0xff) |
                            ((long) data[i++] & 0xff) << 8 |
                            ((long) data[i++] & 0xff) << 16 |
                            ((long) data[i++] & 0xff) << 24 |
                            ((long) data[i++] & 0xff) << 32 |
                            ((long) data[i++] & 0xff) << 40 |
                            ((long) data[i++] & 0xff) << 48 |
                            ((long) data[i++] & 0xff) << 56;

            k *= 0x87c37b91114253d5L;
            k = Long.rotateLeft(k, 31);
            k *= 0x4cf5ad432745937fL;
            h ^= k;
            h = Long.rotateLeft(h, 27);
            h = h * 5 + 0x52dce729;
        }

        long k = 0;
        int remaining = len - i;
        if (remaining > 0) {
            switch (remaining) {
                case 7:
                    k ^= ((long) data[i + 6] & 0xff) << 48;
                case 6:
                    k ^= ((long) data[i + 5] & 0xff) << 40;
                case 5:
                    k ^= ((long) data[i + 4] & 0xff) << 32;
                case 4:
                    k ^= ((long) data[i + 3] & 0xff) << 24;
                case 3:
                    k ^= ((long) data[i + 2] & 0xff) << 16;
                case 2:
                    k ^= ((long) data[i + 1] & 0xff) << 8;
                case 1:
                    k ^= ((long) data[i] & 0xff);
            }
            k *= 0x87c37b91114253d5L;
            k = Long.rotateLeft(k, 31);
            k *= 0x4cf5ad432745937fL;
            h ^= k;
        }

        h ^= len;
        h ^= h >>> 33;
        h *= 0xff51afd7ed558ccdL;
        h ^= h >>> 33;
        h *= 0xc4ceb9fe1a85ec53L;
        h ^= h >>> 33;
        return h;
    }

}
