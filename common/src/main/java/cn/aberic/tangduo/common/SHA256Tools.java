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

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class SHA256Tools {

    private static final String SHA256 = "SHA-256";

    /**
     * 字符串 SHA-256 摘要（UTF-8）
     */
    public static String sha256(String input) {
        if (input == null) {
            return null;
        }
        return sha256(input.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 字节数组 SHA-256 摘要
     */
    public static String sha256(byte[] bytes) {
        try {
            MessageDigest digest = MessageDigest.getInstance(SHA256);
            byte[] hash = digest.digest(bytes);
            return bytesToHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("不支持的算法 SHA-256", e);
        }
    }

    /**
     * 文件 SHA-256（使用 FileChannel 高效读取，适合大文件）
     */
    public static String sha256File(String filePath) throws IOException {
        try (FileInputStream in = new FileInputStream(filePath);
             FileChannel channel = in.getChannel()) {

            MessageDigest digest = MessageDigest.getInstance(SHA256);
            ByteBuffer buffer = ByteBuffer.allocate(8192);

            while (channel.read(buffer) != -1) {
                buffer.flip();
                digest.update(buffer);
                buffer.clear();
            }

            byte[] hash = digest.digest();
            return bytesToHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("不支持的算法 SHA-256", e);
        }
    }

    /**
     * 字节数组转十六进制小写
     */
    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                sb.append('0');
            }
            sb.append(hex);
        }
        return sb.toString();
    }

    /**
     * 字节数组转十六进制大写
     */
    public static String bytesToHexUpperCase(byte[] bytes) {
        return bytesToHex(bytes).toUpperCase();
    }
}