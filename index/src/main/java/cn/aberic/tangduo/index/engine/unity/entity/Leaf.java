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
import cn.aberic.tangduo.common.file.Reader;
import cn.aberic.tangduo.index.engine.Common;
import cn.aberic.tangduo.index.engine.Datum;
import cn.aberic.tangduo.index.engine.IEngine;
import cn.aberic.tangduo.index.engine.unity.Unity;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.rmi.UnexpectedException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Btree联合索引的数据坐标数据<p>
 * 包含【默认声明2字节、4字节数据文件版本号、8字节数据坐标、8字节下一碰撞key坐标（8字节下一碰撞key坐标 + 4字节数据文件版本号 + 8字节数据坐标 + 4字节key字节数组长度 + key字节数组）、4字节key字节数组长度、key字节数组、默认收尾2字节】<p>
 */
@Slf4j
@Data
public class Leaf {

    // 写入文件内容开始
    /** 节点默认声明，值非默认即异常 */
    public static byte[] startBytes = {0x00, 0x7C};
    /** 4字节数据文件版本号 */
    byte[] dataFileVersionBytes = new byte[4];
    /// 8字节下一碰撞key坐标
    byte[] nextKeySeekBytes = new byte[8];
    /// 8字节数据坐标
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

    /// 写入构造函数
    public Leaf(Unity.ChildIndex childIndex, String indexFilepath, long seek) {
        this.childIndex = childIndex;
        this.indexFilepath = indexFilepath;
        this.seek = seek;
    }

    /**
     * 新增/更新数据
     * 默认声明2字节、4字节数据文件版本号、8字节数据坐标、
     * 8字节下一碰撞key坐标（8字节下一碰撞key坐标 + 4字节数据文件版本号 + 8字节数据坐标 + 4字节key字节数组长度 + key字节数组）、
     * 4字节key字节数组长度、key字节数组、默认收尾2字节
     *
     * @param rootPath 数据根路径
     */
    public void put(IEngine.Content content, String rootPath, String indexName, long leafMateSeek, int dataFileVersion, long fileMaxSize) throws IOException {
        long dataMateSeek = -1;
        if (seek <= 0) { // 新写入
            if (Objects.nonNull(content.getDataFileVersionBytes())) { // 复用data
                dataSeekBytes = content.getDataSeekBytes();
                dataFileVersionBytes = content.getDataFileVersionBytes();
                byte[] keyBytes = Bytes.fromString(content.getKey(indexName));
                seek = Channel.append(indexFilepath, Bytes.join(startBytes, dataFileVersionBytes, dataSeekBytes, nextKeySeekBytes, Bytes.fromInt(keyBytes.length), keyBytes, endBytes));
                content.getTransaction().addTask(indexFilepath, leafMateSeek, Bytes.fromLong(seek), new byte[8]); // 更新下一节点在本节点的持久化数据
                return;
            }
            dataFileVersionBytes = Bytes.fromInt(Common.dataFileVersion(rootPath, dataFileVersion, fileMaxSize));
            // 默认声明2字节、4字节数据文件版本号、8字节数据坐标、
            // 8字节下一碰撞key坐标（8字节下一碰撞key坐标 + 4字节数据文件版本号 + 8字节数据坐标 + 4字节key字节数组长度 + key字节数组）、
            // 4字节key字节数组长度、key字节数组、默认收尾2字节
            byte[] keyBytes = Bytes.fromString(content.getKey(indexName));
            seek = Channel.append(indexFilepath, Bytes.join(startBytes, dataFileVersionBytes, dataSeekBytes, nextKeySeekBytes, Bytes.fromInt(keyBytes.length), keyBytes, endBytes));
            Channel.write(indexFilepath, leafMateSeek, Bytes.fromLong(seek)); // 更新下一节点在本节点的持久化数据
            dataMateSeek = seek + 6;
        } else { // 更新
            // 默认声明2字节、4字节数据文件版本号、8字节数据坐标、
            // 8字节下一碰撞key坐标（8字节下一碰撞key坐标 + 4字节数据文件版本号 + 8字节数据坐标 + 4字节key字节数组长度 + key字节数组）、
            // 4字节key字节数组长度、key字节数组、默认收尾2字节
            byte[] bytes = Reader.read(indexFilepath, seek, 26);
            byte[] start = Bytes.read(bytes, 0, 2);
            if (start[0] != startBytes[0] || start[1] != startBytes[1]) {
                throw new UnexpectedException("解析bytes与Node所需首默认值不匹配！");
            }
            dataFileVersionBytes = Bytes.read(bytes, 2, 4); // 4字节数据文件版本号
            dataSeekBytes = Bytes.read(bytes, 6, 8); // 8字节数据坐标
            nextKeySeekBytes = Bytes.read(bytes, 14, 8); // 8字节下一碰撞key坐标
            int keyLength = Bytes.toInt(Bytes.read(bytes, 22, 4)); // 4字节key字节数组长度
            byte[] keyBytesAndEndBytes = Reader.read(indexFilepath, seek + 26, keyLength + 2);
            byte[] end = Bytes.read(keyBytesAndEndBytes, keyLength, 2);
            if (end[0] != endBytes[0] || end[1] != endBytes[1]) {
                throw new UnexpectedException("解析bytes与Node所需尾默认值不匹配！");
            }
            String key = Bytes.toString(Bytes.read(keyBytesAndEndBytes, 0, keyLength));
            if (key.equals(content.getKey(indexName))) {
                dataMateSeek = seek + 6;
            } else {
                // 默认声明2字节、4字节数据文件版本号、8字节数据坐标、
                // 8字节下一碰撞key坐标（8字节下一碰撞key坐标 + 4字节数据文件版本号 + 8字节数据坐标 + 4字节key字节数组长度 + key字节数组）、
                // 4字节key字节数组长度、key字节数组、默认收尾2字节
                long nextKeyMateSeek = seek + 14;
                long nextKeySeekTmp;
                // 8字节下一碰撞key坐标 + 4字节数据文件版本号 + 8字节数据坐标 + 4字节key字节数组长度 + key字节数组
                while ((nextKeySeekTmp = Bytes.toLong(nextKeySeekBytes)) != 0) {
                    byte[] nextKeyPreBytes = Reader.read(indexFilepath, nextKeySeekTmp, 24);
                    nextKeySeekBytes = Bytes.read(nextKeyPreBytes, 0, 8);
                    if (!childIndex.isPrimary() && !childIndex.isUnique()) {
                        nextKeyMateSeek = nextKeySeekTmp;
                        continue;
                    }
                    dataFileVersionBytes = Bytes.read(nextKeyPreBytes, 8, 4);
                    keyLength = Bytes.toInt(Bytes.read(nextKeyPreBytes, 20, 4));
                    key = Bytes.toString(Reader.read(indexFilepath, nextKeySeekTmp + 24, keyLength));
                    if (key.equals(content.getKey(indexName))) {
                        dataMateSeek = nextKeySeekTmp + 12;
                        break;
                    }
                    nextKeyMateSeek = nextKeySeekTmp;
                }
                if (dataMateSeek == -1) { // 8字节下一碰撞key坐标 + 4字节数据文件版本号 + 8字节数据坐标 + 4字节key字节数组长度 + key字节数组
                    byte[] keyBytes = Bytes.fromString(content.getKey(indexName));
                    byte[] keyLengthBytes = Bytes.fromInt(keyBytes.length);
                    byte[] nextKeyDataBytes = Bytes.join(new byte[8], dataFileVersionBytes, new byte[8], keyLengthBytes, keyBytes);
                    long keySeek = Channel.append(indexFilepath, nextKeyDataBytes);
                    Channel.write(indexFilepath, nextKeyMateSeek, Bytes.fromLong(keySeek));
                    dataMateSeek = keySeek + 12;
                }
            }
            if (Objects.nonNull(content.getDataFileVersionBytes())) { // 复用data
                // 默认声明2字节、4字节数据文件版本号、8字节数据坐标、
                // 8字节下一碰撞key坐标（8字节下一碰撞key坐标 + 4字节数据文件版本号 + 8字节数据坐标 + 4字节key字节数组长度 + key字节数组）、
                // 4字节key字节数组长度、key字节数组、默认收尾2字节
                byte[] bytesNew = Bytes.join(content.getDataFileVersionBytes(), content.getDataSeekBytes());
                byte[] bytesOld = Bytes.join(dataFileVersionBytes, dataSeekBytes);
                content.getTransaction().addTask(indexFilepath, dataMateSeek - 4, bytesNew, bytesOld); // 更新下一节点在本节点的持久化数据
                return;
            }
        }
        // 新写入流程
        content.setDataFileVersionBytes(dataFileVersionBytes);
        String dataFilepath = Common.dataFilepath(rootPath, Bytes.toInt(dataFileVersionBytes)).toString(); // 数据文件地址，数据将写入该文件中
        Datum datum = new Datum(dataFilepath, 0);
        datum.writeOrUpdate(content, indexFilepath, dataMateSeek);
    }

    /**
     * 读取数据
     *
     * @param rootPath  数据根路径
     * @param key       原始key
     */
    public List<byte[]> get(String rootPath, String key) throws IOException {
        List<byte[]> bytesList = new ArrayList<>();
        // 读取 默认声明2字节、4字节数据文件版本号、8字节数据坐标、
        // 8字节下一碰撞key坐标（8字节下一碰撞key坐标 + 4字节数据文件版本号 + 8字节数据坐标 + 4字节key字节数组长度 + key字节数组）、
        // 4字节key字节数组长度、key字节数组、默认收尾2字节
        byte[] bytes = Reader.read(indexFilepath, seek, 26);
        byte[] start = Bytes.read(bytes, 0, 2);
        if (start[0] != startBytes[0] || start[1] != startBytes[1]) {
            throw new UnexpectedException("解析bytes与Node所需首默认值不匹配！");
        }
        dataFileVersionBytes = Bytes.read(bytes, 2, 4); // 4字节数据文件版本号
        dataSeekBytes = Bytes.read(bytes, 6, 8); // 8字节数据坐标
        nextKeySeekBytes = Bytes.read(bytes, 14, 8); // 8字节下一碰撞key坐标
        int keyLength = Bytes.toInt(Bytes.read(bytes, 22, 4)); // 4字节key字节数组长度
        byte[] keyBytesAndEndBytes = Reader.read(indexFilepath, seek + 26, keyLength + 2);
        byte[] end = Bytes.read(keyBytesAndEndBytes, keyLength, 2);
        if (end[0] != endBytes[0] || end[1] != endBytes[1]) {
            throw new UnexpectedException("解析bytes与Node所需尾默认值不匹配！");
        }
        String keyRead = Bytes.toString(Bytes.read(keyBytesAndEndBytes, 0, keyLength));
        if (keyRead.equals(key)) {
            long dataSeek = Bytes.toLong(dataSeekBytes);
            if (dataSeek > 0) {
                String dataFilepath = Common.dataFilepath(rootPath, Bytes.toInt(dataFileVersionBytes)).toString(); // 数据文件地址，数据将写入该文件中
                Datum datum = new Datum(dataFilepath, dataSeek);
                bytesList.add(datum.read());
            }
        }
        // 默认声明2字节、4字节数据文件版本号、8字节数据坐标、
        // 8字节下一碰撞key坐标（8字节下一碰撞key坐标 + 4字节数据文件版本号 + 8字节数据坐标 + 4字节key字节数组长度 + key字节数组）、
        // 4字节key字节数组长度、key字节数组、默认收尾2字节
        long nextKeySeekTmp;
        // 8字节下一碰撞key坐标 + 4字节数据文件版本号 + 8字节数据坐标 + 4字节key字节数组长度 + key字节数组
        while ((nextKeySeekTmp = Bytes.toLong(nextKeySeekBytes)) != 0) {
            byte[] nextKeyPreBytes = Reader.read(indexFilepath, nextKeySeekTmp, 24);
            nextKeySeekBytes = Bytes.read(nextKeyPreBytes, 0, 8);
            dataFileVersionBytes = Bytes.read(nextKeyPreBytes, 8, 4);
            keyLength = Bytes.toInt(Bytes.read(nextKeyPreBytes, 20, 4));
            keyRead = Bytes.toString(Reader.read(indexFilepath, nextKeySeekTmp + 24, keyLength));
            if (keyRead.equals(key)) {
                long dataSeek = Bytes.toLong(Bytes.read(nextKeyPreBytes, 12, 8));
                if (dataSeek > 0) {
                    String dataFilepath = Common.dataFilepath(rootPath, Bytes.toInt(dataFileVersionBytes)).toString(); // 数据文件地址，数据将写入该文件中
                    Datum datum = new Datum(dataFilepath, dataSeek);
                    bytesList.add(datum.read());
                }
            }
        }
        return bytesList;
    }

    /**
     * 读取数据
     *
     * @param rootPath  数据根路径
     */
    public List<byte[]> select(String rootPath) throws IOException {
        List<byte[]>  bytesList = new ArrayList<>();
        // 读取 默认声明2字节、4字节数据文件版本号、8字节数据坐标、
        // 8字节下一碰撞key坐标（8字节下一碰撞key坐标 + 4字节数据文件版本号 + 8字节数据坐标 + 4字节key字节数组长度 + key字节数组）、
        // 4字节key字节数组长度、key字节数组、默认收尾2字节
        byte[] bytes = Reader.read(indexFilepath, seek, 26);
        byte[] start = Bytes.read(bytes, 0, 2);
        if (start[0] != startBytes[0] || start[1] != startBytes[1]) {
            throw new UnexpectedException("解析bytes与Node所需首默认值不匹配！");
        }
        dataFileVersionBytes = Bytes.read(bytes, 2, 4); // 4字节数据文件版本号
        dataSeekBytes = Bytes.read(bytes, 6, 8); // 8字节数据坐标
        nextKeySeekBytes = Bytes.read(bytes, 14, 8); // 8字节下一碰撞key坐标
        int keyLength = Bytes.toInt(Bytes.read(bytes, 22, 4)); // 4字节key字节数组长度
        byte[] keyBytesAndEndBytes = Reader.read(indexFilepath, seek + 26, keyLength + 2);
        byte[] end = Bytes.read(keyBytesAndEndBytes, keyLength, 2);
        if (end[0] != endBytes[0] || end[1] != endBytes[1]) {
            throw new UnexpectedException("解析bytes与Node所需尾默认值不匹配！");
        }
        long dataSeek = Bytes.toLong(dataSeekBytes);
        if (dataSeek > 0) {
            String dataFilepath = Common.dataFilepath(rootPath, Bytes.toInt(dataFileVersionBytes)).toString(); // 数据文件地址，数据将写入该文件中
            Datum datum = new Datum(dataFilepath, dataSeek);
            bytesList.add(datum.read());
        }
        // 默认声明2字节、4字节数据文件版本号、8字节数据坐标、
        // 8字节下一碰撞key坐标（8字节下一碰撞key坐标 + 4字节数据文件版本号 + 8字节数据坐标 + 4字节key字节数组长度 + key字节数组）、
        // 4字节key字节数组长度、key字节数组、默认收尾2字节
        long nextKeySeekTmp;
        // 8字节下一碰撞key坐标 + 4字节数据文件版本号 + 8字节数据坐标 + 4字节key字节数组长度 + key字节数组
        while ((nextKeySeekTmp = Bytes.toLong(nextKeySeekBytes)) != 0) {
            byte[] nextKeyPreBytes = Reader.read(indexFilepath, nextKeySeekTmp, 24);
            nextKeySeekBytes = Bytes.read(nextKeyPreBytes, 0, 8);
            dataFileVersionBytes = Bytes.read(nextKeyPreBytes, 8, 4);
            dataSeekBytes = Bytes.read(bytes, 12, 8); // 8字节数据坐标
            dataSeek = Bytes.toLong(dataSeekBytes);
            if (dataSeek > 0) {
                String dataFilepath = Common.dataFilepath(rootPath, Bytes.toInt(dataFileVersionBytes)).toString(); // 数据文件地址，数据将写入该文件中
                Datum datum = new Datum(dataFilepath, dataSeek);
                bytesList.add(datum.read());
            }
        }
        return bytesList;
    }

    /**
     * 新增/更新数据
     * 默认声明2字节、4字节数据文件版本号、8字节数据坐标、
     * 8字节下一碰撞key坐标（8字节下一碰撞key坐标 + 4字节数据文件版本号 + 8字节数据坐标 + 4字节key字节数组长度 + key字节数组）、
     * 4字节key字节数组长度、key字节数组、默认收尾2字节
     *
     * @param rootPath 数据根路径
     */
    public void delete(String rootPath, String key) throws IOException {
        if (seek <= 0) {
            return;
        }
        // 默认声明2字节、4字节数据文件版本号、8字节数据坐标、
        // 8字节下一碰撞key坐标（8字节下一碰撞key坐标 + 4字节数据文件版本号 + 8字节数据坐标 + 4字节key字节数组长度 + key字节数组）、
        // 4字节key字节数组长度、key字节数组、默认收尾2字节
        byte[] bytes = Reader.read(indexFilepath, seek, 26);
        byte[] start = Bytes.read(bytes, 0, 2);
        if (start[0] != startBytes[0] || start[1] != startBytes[1]) {
            throw new UnexpectedException("解析bytes与Node所需首默认值不匹配！");
        }
        dataFileVersionBytes = Bytes.read(bytes, 2, 4); // 4字节数据文件版本号
        dataSeekBytes = Bytes.read(bytes, 6, 8); // 8字节数据坐标
        nextKeySeekBytes = Bytes.read(bytes, 14, 8); // 8字节下一碰撞key坐标
        int keyLength = Bytes.toInt(Bytes.read(bytes, 22, 4)); // 4字节key字节数组长度
        byte[] keyBytesAndEndBytes = Reader.read(indexFilepath, seek + 26, keyLength + 2);
        byte[] end = Bytes.read(keyBytesAndEndBytes, keyLength, 2);
        if (end[0] != endBytes[0] || end[1] != endBytes[1]) {
            throw new UnexpectedException("解析bytes与Node所需尾默认值不匹配！");
        }
        String keyFromRead = Bytes.toString(Bytes.read(keyBytesAndEndBytes, 0, keyLength));
        if (keyFromRead.equals(key)) {
            long dataSeek = Bytes.toLong(dataSeekBytes);
            if (dataSeek > 0) {
                String dataFilepath = Common.dataFilepath(rootPath, Bytes.toInt(dataFileVersionBytes)).toString(); // 数据文件地址，数据将写入该文件中
                Datum datum = new Datum(dataFilepath, dataSeek);
                datum.delete();
            }
        }
        // 默认声明2字节、4字节数据文件版本号、8字节数据坐标、
        // 8字节下一碰撞key坐标（8字节下一碰撞key坐标 + 4字节数据文件版本号 + 8字节数据坐标 + 4字节key字节数组长度 + key字节数组）、
        // 4字节key字节数组长度、key字节数组、默认收尾2字节
        long nextKeySeekTmp;
        // 8字节下一碰撞key坐标 + 4字节数据文件版本号 + 8字节数据坐标 + 4字节key字节数组长度 + key字节数组
        while ((nextKeySeekTmp = Bytes.toLong(nextKeySeekBytes)) != 0) {
            byte[] nextKeyPreBytes = Reader.read(indexFilepath, nextKeySeekTmp, 24);
            nextKeySeekBytes = Bytes.read(nextKeyPreBytes, 0, 8);
            dataFileVersionBytes = Bytes.read(nextKeyPreBytes, 8, 4);
            dataSeekBytes = Bytes.read(nextKeyPreBytes, 12, 8); // 8字节数据坐标
            keyLength = Bytes.toInt(Bytes.read(nextKeyPreBytes, 20, 4));
            keyFromRead = Bytes.toString(Reader.read(indexFilepath, nextKeySeekTmp + 24, keyLength));
            if (keyFromRead.equals(key)) {
                long dataSeek = Bytes.toLong(dataSeekBytes);
                if (dataSeek > 0) {
                    String dataFilepath = Common.dataFilepath(rootPath, Bytes.toInt(dataFileVersionBytes)).toString(); // 数据文件地址，数据将写入该文件中
                    Datum datum = new Datum(dataFilepath, dataSeek);
                    datum.delete();
                }
            }
        }
    }

}
