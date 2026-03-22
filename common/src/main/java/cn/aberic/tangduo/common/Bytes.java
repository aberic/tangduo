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

public class Bytes {

    private Bytes() {
        throw new IllegalStateException("Bytes class");
    }

    public static byte[] read(byte[] bytes, int position, int length) {
        return Arrays.copyOfRange(bytes, position, position + length);
    }

    public static byte[] read(byte[] bytes, long position, int length) {
        return read(bytes, Math.toIntExact(position), length);
    }

    public static byte[] join(byte[]... bytes) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        for (byte[] bs : bytes) {
            outputStream.write(bs);
        }
        return outputStream.toByteArray();
    }

    public static byte[] fromString(String res) {
        return res.getBytes(StandardCharsets.UTF_8);
    }

    public static String toString(byte[] bytes) {
        return new String(bytes, StandardCharsets.UTF_8);
    }

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

    public static long toLong(byte[] bytes) {
        if (bytes.length != 8) {
            throw new RuntimeException(String.format("trans bytes 2 u64 out of bounds, except eq 8, but receive %s", bytes.length));
        } else {
            long result = 0;
            for (int i = 0; i < bytes.length; i++) {
                result |= ((long) bytes[i] & 0xFF) << (8 * (7 - i));
            }
            return result;
        }
    }

    public static byte[] fromInt(int i) {
        return new byte[]{
                (byte) ((i >> 24) & 0xFF),
                (byte) ((i >> 16) & 0xFF),
                (byte) ((i >> 8) & 0xFF),
                (byte) (i & 0xFF)
        };
    }

    public static int toInt(byte[] bytes) {
        if (bytes.length != 4) {
            throw new RuntimeException(String.format("trans bytes 2 u32 out of bounds, except eq 4, but receive %s", bytes.length));
        } else {
            int result = 0;
            for (int i = 0; i < bytes.length; i++) {
                result |= (bytes[i] & 0xFF) << (8 * (3 - i));
            }
            return result;
        }
    }

    public static boolean toBool(byte b) {
        return b != 0x00;
    }

    public static byte fromBool(boolean b) {
        return b ? (byte) 0x01 : 0x00;
    }

    public static byte[] fromDouble(double d) {
        long longValue = Double.doubleToLongBits(d);
        return fromLong(longValue);
    }

    public static double toDouble(byte[] bytes) {
        long l = toLong(bytes);
        return Double.longBitsToDouble(l);
    }

}
