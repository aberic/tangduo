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

package cn.aberic.tangduo.index.engine;

import cn.aberic.tangduo.common.ByteTools;
import cn.aberic.tangduo.common.file.Channel;
import cn.aberic.tangduo.common.file.Reader;
import cn.aberic.tangduo.index.engine.entity.Content;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.Objects;

/**
 * 数据
 * 存在相同碰撞的可能
 * 数据结构：4字节数据长度+ 数据字节数组
 */
@Slf4j
@Data
public class Datum {

    /// 文件默认声明，值非默认即异常
    public static byte[] startBytes = {0x00, 0x7D};

    /// 数据文件地址，如"tmp/data.1.1.td"
    String filepath;
    /// 数据在文件中的起始偏移量
    long seek;

    /**
     * 构造数据对象，读取、更新数据使用
     *
     * @param filepath 数据文件地址
     * @param seek     数据在文件中的起始偏移量
     */
    public Datum(String filepath, long seek) {
        this.filepath = filepath;
        this.seek = seek;
    }

    /**
     * 指向本数据的偏移量不为0时，新增数据。传入原始key、数据文件地址、数据在文件中的起始偏移量和数据。
     * 会将数据追加到数据文件中，同时生成本数据新的起始偏移量。新偏移量会更新原偏移量的值
     * 4字节数据长度+ 数据字节数组
     *
     * @param datumMateSeek 指向数据坐标值在数据文件中的起始偏移量
     */
    public void writeOrUpdate(Content content, String indexFilepath, long datumMateSeek) throws IOException {
        // 4字节数据长度+ 数据字节数组
        byte[] dataLengthBytes = ByteTools.fromInt(content.getValue().length);
        long dataSeek = Channel.append(filepath, ByteTools.join(dataLengthBytes, content.getValue()));
        byte[] dataSeekBytes = ByteTools.fromLong(dataSeek);
        content.getTransaction().addTask(indexFilepath, datumMateSeek, dataSeekBytes, new byte[8]);
        if (Objects.isNull(content.getDataSeekBytes())) {
            content.setDataSeekBytes(dataSeekBytes);
        }
    }

    /// 读取指定传入原始key相匹配的数据
    public byte[] read() throws IOException {
        // 4字节数据长度+ 数据字节数组
        int dataLength = ByteTools.toInt(Reader.read(filepath, seek, 4)); // 4字节数据主体长度
        if (dataLength > 0) {
            return Reader.read(filepath, seek + 4, dataLength);
        }
        return null;
    }

    /// 删除指定传入原始key相匹配的数据
    public void delete() throws IOException {
        // 4字节数据长度+ 数据字节数组
        Channel.write(filepath, seek, new byte[4]);
    }

}
