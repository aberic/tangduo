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

package cn.aberic.tangduo.index.engine.unity.entity;

import cn.aberic.tangduo.common.ByteTools;
import cn.aberic.tangduo.common.file.Channel;
import cn.aberic.tangduo.common.file.Reader;
import cn.aberic.tangduo.index.engine.INode;
import cn.aberic.tangduo.index.engine.entity.Content;
import cn.aberic.tangduo.index.engine.unity.Unity;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.rmi.UnexpectedException;
import java.util.ArrayList;
import java.util.List;

/**
 * Btree引擎节点<p>
 * -------------------------------------------------------------------------------------------------------------------------------------<p>
 * | 1…256(16777216)                         | 1层：1个节点，每个节点含256个节点和16777216个数据坐标<p>
 * -------------------------------------------------------------------------------------------------------------------------------------<p>
 * | 1…256 | … | 65280…65536(65536)          | 2层：256个节点，每个节点含256个节点和65536个数据坐标<p>
 * -------------------------------------------------------------------------------------------------------------------------------------<p>
 * | 1…256 | … | 16776960…16777216(256)      | 3层：65536个节点，每个节点含256个节点和256个数据坐标<p>
 * -------------------------------------------------------------------------------------------------------------------------------------<p>
 * | 1…256 | … | 4294966940…4294967296       | 4层：16777216个节点，每个节点含256个数据坐标，总计4294967296个数据坐标<p>
 * -------------------------------------------------------------------------------------------------------------------------------------<p>
 * <p>
 * -------------------------------------------------------------------------------------------------------------------------------------<p>
 * | -32767…0…32768(281474976710656)                                                                         | 1层：1个节点，每个节点含65536个节点和281474976710656个数据坐标<p>
 * -------------------------------------------------------------------------------------------------------------------------------------<p>
 * | -2147483647…-2147418111 | … | -65535…0 | … | 2147450880…2147483648(4294967296)                          | 2层：65536个节点，每个节点含65536个节点和4294967296个数据坐标<p>
 * -------------------------------------------------------------------------------------------------------------------------------------<p>
 * | -140737488355327…-140737488289791 | … | -65535…0 | … | 140737488289792…140737488355328(65536)           | 3层：4294967296个节点，每个节点含65536个节点和65536个数据坐标<p>
 * -------------------------------------------------------------------------------------------------------------------------------------<p>
 * | -9223372036854775807…-9223372036854710271 | … | -65535…0 | … | 9223372036854710272…9223372036854775808  | 4层：281474976710656个节点，每个节点含65536个数据坐标，总计18446744073709551616个数据坐标<p>
 * -------------------------------------------------------------------------------------------------------------------------------------<p>
 * <p>
 * -------------------------------------------------------------------------------------------------------------------------------------<p>
 * | -9223372036854775807…-9223090561878065151 | … | -281474976710655…0 | 1…281474976710656 | … | ★9223090561878065152…9223372036854775808  | 1层：65536个节点，间距：281474976710656<p>
 * -------------------------------------------------------------------------------------------------------------------------------------<p>
 * | 1…4294967296 | … | 281470681743360…281474976710656   | 进入1层某一节点后所见2层形态：65536个节点，间距：4294967296<p>
 * -------------------------------------------------------------------------------------------------------------------------------------<p>
 * | 1…65536 | … | 4294901760…4294967296           | 进入2层某一节点后所见3层形态：65536个节点，间距：65536<p>
 * -------------------------------------------------------------------------------------------------------------------------------------<p>
 * | 1…65536  | 进入3层某一节点后所见4层形态：65536个节点，间距：1<p>
 * -------------------------------------------------------------------------------------------------------------------------------------<p>
 * <p>
 * -------------------------------------------------------------------------------------------------------------------------------------<p>
 * | -9223372036854775807…-9223090561878065151 | … | -281474976710655…0 | 1…281474976710656 | … | ★9223090561878065152…9223372036854775808  | 1层：65536个节点，间距：281474976710656<p>
 * -------------------------------------------------------------------------------------------------------------------------------------<p>
 * | -281474976710655…-281470681743359 | … | -4294967295…0   | 进入1层某一节点后所见2层形态：65536个节点，间距：4294967296<p>
 * -------------------------------------------------------------------------------------------------------------------------------------<p>
 * | -4294967295…-4294901759 | … | -65535…0           | 进入2层某一节点后所见3层形态：65536个节点，间距：65536<p>
 * -------------------------------------------------------------------------------------------------------------------------------------<p>
 * | -65535…0  | 进入3层某一节点后所见4层形态：65536个节点，间距：1<p>
 * -------------------------------------------------------------------------------------------------------------------------------------<p>
 * 2^64 = 18446744073709551616 | 1844京6744兆737亿955万1616<p>
 * 节点包含【2字节节点默认声明、524288字节65536个节点数据或数据坐标数据、2字节节点默认收尾】<p>
 * 节点数据和数据坐标数据即64位文件偏移量<p>
 */
@Slf4j
@Data
public class Node implements INode {

    /// 节点字节长度
    public static int NODE_LENGTH = 2052;
    /// 数据字节长度
    public static int DATA_LENGTH = 2048;

    // 写入文件内容开始
    /// 节点默认声明，值非默认即异常
    public static byte[] startBytes = {0x00, 0x7B};
    /// 节点数据或数据坐标数据，256 * 8 = 2048
    byte[] data;
    /// 节点默认收尾，值非默认即异常
    public static byte[] endBytes = {0x00, 0x7B};
    // 写入文件内容结束
    /// 节点在索引文件中的起始偏移量
    long seek;
    /// Leaf所属索引文件地址，如"tmp/unity/testIndex/1_4294967296.idx"
    String indexFilepath;

    public Node() {
        data = new byte[DATA_LENGTH];
    }

    /// 在文件中查找或创建当前节点
    /// 当seek为-1时，表示追加写入
    /// 当seek为0时，表示节点不存在，需要新建节点
    /// 当seek大于0时，表示节点存在，需要读取节点数据
    /// @param indexFilepath 索引文件，如"tmp/unity.1.idx"
    /// @param mateSeek      当前节点坐标信息在文件中的起始偏移量
    /// @param seek          当前节点在文件中的起始偏移量
    public Node(String indexFilepath, long mateSeek, long seek) throws IOException {
        this.indexFilepath = indexFilepath;
        if (seek <= 0) { // 节点不存在，需要新建节点
            data = new byte[DATA_LENGTH];
            seek = Channel.append(indexFilepath, toBytes());
            Channel.write(indexFilepath, mateSeek, ByteTools.fromLong(seek)); // 更新下一节点在本节点的持久化数据
        } else {
            byte[] bytes = Channel.read(indexFilepath, seek, Node.NODE_LENGTH);
            if (bytes.length != NODE_LENGTH) {
                throw new UnexpectedException("解析bytes与Node所需长度不匹配！");
            }
            if (bytes[0] != startBytes[0] || bytes[1] != startBytes[1] || bytes[NODE_LENGTH - 2] != endBytes[0] || bytes[NODE_LENGTH - 1] != endBytes[1]) {
                throw new UnexpectedException("解析bytes与Node所需首尾默认值不匹配！");
            }
            data = ByteTools.read(bytes, 2, NODE_LENGTH - 4);
        }
        this.seek = seek;
    }

    /**
     * 根据传入的节点数据生成Node对象
     *
     * @param indexFilepath 索引文件，如"tmp/unity.1.idx"
     * @param seek          当前节点在文件中的起始偏移量
     */
    public Node(String indexFilepath, long seek, boolean get) throws IOException {
        this.indexFilepath = indexFilepath;
        byte[] bytes;
        if (get) {
            bytes = Reader.read(indexFilepath, seek, Node.NODE_LENGTH);
        } else {
            bytes = Channel.read(indexFilepath, seek, Node.NODE_LENGTH);
        }
        if (bytes[0] != startBytes[0] || bytes[1] != startBytes[1] || bytes[NODE_LENGTH - 2] != endBytes[0] || bytes[NODE_LENGTH - 1] != endBytes[1]) {
            throw new UnexpectedException("解析bytes与Node所需首尾默认值不匹配！");
        }
        data = ByteTools.read(bytes, 2, NODE_LENGTH - 4);
        this.seek = seek;
    }

    /// 从指定文件中的指定起始位置开始写入/更新数据坐标数据
    /// @param content       数据内容
    /// @param childIndex    子节点索引
    /// @param rootPath      数据根路径
    /// @param indexName     索引名称
    /// @param position      数据坐标在节点字节数组中的位置
    /// @param leafMateSeek  Leaf在索引文件中的起始偏移量
    /// @param dataFileVersion 数据文件版本号，如 1，与索引版本号结合使用，如1.1，区分相同索引下的不同数据文件
    /// @param dataFileMaxSize 数据文件大小阈值，单位byte
    /// @throws Exception    异常
    public void put(Content content, Unity.ChildIndex childIndex, String rootPath, String indexName, long position, long leafMateSeek,
                    int dataFileVersion, long dataFileMaxSize) throws Exception {
        long leafSeek = ByteTools.toLong(ByteTools.read(data, position * 8, 8)); // Leaf在索引文件中的起始偏移量
        Leaf leaf = new Leaf(childIndex, indexFilepath, leafSeek);
        // 更新 indexDataSeek
        leaf.put(content, rootPath, indexName, leafMateSeek, dataFileVersion, dataFileMaxSize);
    }

    /**
     * 从指定文件中的指定起始位置开始读取数据坐标数据
     *
     * @param rootPath 数据根路径
     * @param position 数据坐标在节点字节数组中的位置
     * @param key      原始key
     */
    public List<byte[]> get(String rootPath, long position, String key) throws IOException {
        long leafSeek = ByteTools.toLong(ByteTools.read(data, position * 8, 8)); // Leaf在索引文件中的起始偏移量
        if (leafSeek <= 0) {
            return null;
        } else {
            Leaf leaf = new Leaf(indexFilepath, leafSeek);
            return leaf.get(rootPath, key);
        }
    }

    /**
     * 从指定文件中的指定起始位置开始删除数据坐标数据
     *
     * @param rootPath 数据根路径
     * @param position 数据坐标在节点字节数组中的位置
     * @param key      原始key
     */
    public void delete(String rootPath, long position, String key) throws IOException {
        long leafSeek = ByteTools.toLong(ByteTools.read(data, position * 8, 8)); // Leaf在索引文件中的起始偏移量
        if (leafSeek > 0) {
            Leaf leaf = new Leaf(indexFilepath, leafSeek);
            leaf.delete(rootPath, key);
        }
    }

    /**
     * 从指定文件中的指定起始位置开始读取数据坐标数据
     *
     * @param rootPath 数据根路径
     * @param position 数据坐标在节点字节数组中的位置
     */
    public List<byte[]> select(String rootPath, long position) throws IOException {
        long leafSeek = ByteTools.toLong(ByteTools.read(data, position * 8, 8)); // Leaf在索引文件中的起始偏移量
        if (leafSeek <= 0) {
            return new ArrayList<>();
        } else {
            Leaf leaf = new Leaf(indexFilepath, leafSeek);
            return leaf.select(rootPath);
        }
    }

    /**
     * 转成存储字节数组
     *
     * @return 存储字节数组
     */
    public byte[] toBytes() throws IOException {
        return ByteTools.join(startBytes, data, endBytes);
    }

    /// 有内容节点数量
    public int valueCount() {
        int count = 0;
        int position = 2;
        while (position <= data.length) {
            byte[] bytes = ByteTools.read(data, position, 8);
            if (ByteTools.toLong(bytes) > 0) {
                count++;
            }
            position += 8;
        }
        return count;
    }

}
