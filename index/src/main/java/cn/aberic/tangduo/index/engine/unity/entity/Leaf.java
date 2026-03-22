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

import cn.aberic.tangduo.common.Bytes;
import cn.aberic.tangduo.common.file.Channel;
import cn.aberic.tangduo.index.engine.Common;
import cn.aberic.tangduo.index.engine.Datum;
import cn.aberic.tangduo.index.engine.Transaction;
import cn.aberic.tangduo.index.engine.unity.Unity;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.rmi.UnexpectedException;
import java.util.List;

/**
 * Btree联合索引的数据坐标数据<p>
 * 包含【默认声明2字节、4字节数据文件版本号、8字节数据坐标、默认收尾2字节】<p>
 */
@Slf4j
@Data
public class Leaf {

    // 写入文件内容开始
    /** 节点默认声明，值非默认即异常 */
    public static byte[] startBytes = {0x00, 0x7C};
    /** 4字节数据文件版本号 */
    byte[] dataFileVersionBytes = new byte[4];
    /** 8字节数据坐标 */
    byte[] dataSeekBytes = new byte[8];
    /** 节点默认收尾，值非默认即异常 */
    public static byte[] endBytes = {0x00, 0x7C};
    // 写入文件内容结束

    /** Leaf所属索引文件地址，如"tmp/unity.1.idx" */
    String indexFilepath;
    /** Leaf在索引文件中的起始偏移量 */
    long seek;
    Unity.ChildIndex childIndex;

    public Leaf(String indexFilepath, long seek) {
        this.indexFilepath = indexFilepath;
        this.seek = seek;
    }

    public Leaf(String rootPath, Unity.ChildIndex childIndex, int dataFileVersion, long fileMaxSize) throws IOException {
        this.childIndex = childIndex;
        dataFileVersionBytes = Bytes.fromInt(Common.dataFileVersion(rootPath, childIndex.getName(), dataFileVersion, fileMaxSize));
    }

    public Leaf(String rootPath, Unity.ChildIndex childIndex, String indexFilepath, int dataFileVersion, long fileMaxSize) throws IOException {
        this.childIndex = childIndex;
        this.indexFilepath = indexFilepath;
        dataFileVersionBytes = Bytes.fromInt(Common.dataFileVersion(rootPath, childIndex.getName(), dataFileVersion, fileMaxSize));
        seek = Channel.append(indexFilepath, toBytes());
    }

    private Datum getDatum(String indexName, String rootPath) throws IOException {
        byte[] bytes = Channel.read(indexFilepath, seek, 16); // 读取 默认声明2字节、4字节数据文件版本号、8字节数据坐标、默认收尾2字节
        byte[] start = Bytes.read(bytes, 0, 2);
        if (start[0] != startBytes[0] || start[1] != startBytes[1]) {
            throw new UnexpectedException("解析bytes与Node所需首默认值不匹配！");
        }
        byte[] end = Bytes.read(bytes, 14, 2);
        if (end[0] != endBytes[0] || end[1] != endBytes[1]) {
            throw new UnexpectedException("解析bytes与Node所需尾默认值不匹配！");
        }
        byte[] dataFileVersionBytes = Bytes.read(bytes, 2, 4); // 4字节数据文件版本号
        long dataSeek = Bytes.toLong(Bytes.read(bytes, 6, 8)); // 8字节数据坐标
        String dataFilepath = Common.dataFilepath(rootPath, indexName, Bytes.toInt(dataFileVersionBytes)).toString(); // 数据文件地址，数据将写入该文件中
        return new Datum(dataFilepath, dataSeek);
    }

    /**
     * 读取数据
     *
     * @param indexName 索引名（全名组合确保唯一性，如：库名+表名+索引名）
     * @param rootPath  数据根路径
     * @param key       原始key
     */
    public byte[] get(String indexName, String rootPath, String key) throws IOException {
        Datum datum = getDatum(indexName, rootPath);
        return datum.read(indexName, key);
    }

    /**
     * 读取数据
     *
     * @param indexName 索引名（全名组合确保唯一性，如：库名+表名+索引名）
     * @param rootPath  数据根路径
     */
    public List<byte[]> get(String indexName, String rootPath) throws IOException {
        Datum datum = getDatum(indexName, rootPath);
        return datum.read(indexName);
    }

    /**
     * 新增/更新数据
     *
     * @param indexName 索引名（全名组合确保唯一性，如：库名+表名+索引名）
     * @param rootPath  数据根路径
     * @param value     数据
     */
    public void set(Transaction transaction, String indexName, String rootPath, String key, byte[] value) throws IOException {
        Datum datum = getDatum(indexName, rootPath);
        long datumMateSeek = seek + 6;
        if (datum.getSeek() == 0) {
            datum.write(transaction, indexFilepath, datumMateSeek, indexName, key, value);
        } else {
            datum.update(transaction, datumMateSeek, indexName, key, value);
        }
    }

    /**
     * 新增/更新数据
     *
     * @param indexName 索引名（全名组合确保唯一性，如：库名+表名+索引名）
     * @param rootPath  数据根路径
     * @param value     数据
     */
    public void set(Transaction transaction, String indexName, String rootPath, String indexFilepath, long leafMateSeek, String key, byte[] value) throws IOException {
        seek = Channel.append(indexFilepath, toBytes());
        Channel.write(indexFilepath, leafMateSeek, Bytes.fromLong(seek)); // 更新下一节点在本节点的持久化数据
        long datumMateSeek = seek + 6; // 默认声明2字节、4字节数据文件版本号、8字节数据坐标、默认收尾2字节
        String dataFilepath = Common.dataFilepath(rootPath, indexName, Bytes.toInt(dataFileVersionBytes)).toString(); // 数据文件地址，数据将写入该文件中
        Datum datum = new Datum(dataFilepath, 0);
        datum.write(transaction, indexFilepath, datumMateSeek, indexName, key, value);
    }

    /** 默认声明2字节、4字节数据文件版本号、8字节数据坐标、默认收尾2字节 */
    public byte[] toBytes() throws IOException {
        return Bytes.join(startBytes, dataFileVersionBytes, dataSeekBytes, endBytes);
    }

}
