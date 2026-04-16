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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/// GZIP 工具类
public final class GzipTools {

    private GzipTools() {
        throw new AssertionError("工具类禁止实例化");
    }

    /**
     * 压缩字节数组
     * <p>
     * 压缩后的字节数组长度通常小于原始字节数组长度。
     * </p>
     * @param data 待压缩的字节数组
     * @return 压缩后的字节数组
     * @throws Exception 压缩过程中可能抛出的异常

     */
    @SuppressWarnings({"resource", "all"})
    public static byte[] compress(byte[] data) throws Exception {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
                GZIPOutputStream gzip = new GZIPOutputStream(baos)) {
            gzip.write(data);
            gzip.finish();
            return baos.toByteArray();
        }
    }

    /**
     * 解压缩字节数组
     * <p>
     * 解压缩后的字节数组长度通常大于压缩后的字节数组长度。
     * </p>
     * @param compressed 待解压缩的字节数组
     * @return 解压缩后的字节数组
     * @throws IOException 解压缩过程中可能抛出的异常
     */
    @SuppressWarnings({"resource", "all"})
    public static byte[] decompress(byte[] compressed) throws IOException {
        if (compressed == null || compressed.length == 0) {
            return null;
        }
        ByteArrayInputStream in = new ByteArrayInputStream(compressed);
        GZIPInputStream gis = new GZIPInputStream(in);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int len;
        while ((len = gis.read(buffer)) > 0) {
            out.write(buffer, 0, len);
        }
        return out.toByteArray();
    }

}
