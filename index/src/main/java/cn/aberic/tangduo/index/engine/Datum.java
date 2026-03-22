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

import cn.aberic.tangduo.common.Bytes;
import cn.aberic.tangduo.common.file.Channel;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 数据
 * 存在相同碰撞的可能
 * 数据结构：4字节数据主体长度+【8字节子数据偏移量+…+8字节子数据偏移量】
 */
@Slf4j
@Data
public class Datum {

    /** 数据文件地址，如"tmp/data.1.1.td */
    String filepath;
    /** 数据在文件中的起始偏移量 */
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
     * 读取指定传入原始key相匹配的数据
     *
     * @param indexName 索引名（全名组合确保唯一性，如：库名+表名+索引名）
     * @param key       原始key
     */
    public byte[] read(String indexName, String key) throws IOException {
        int mainDataLength = Bytes.toInt(Channel.read(filepath, seek, 4)); // 4字节数据主体长度
        byte[] itemSeekListBytes = Channel.read(filepath, seek + 4, mainDataLength); // 【8字节子数据偏移量+…+8字节子数据偏移量】
        int size = mainDataLength / 8;
        for (int i = 0; i < size; i++) {
            byte[] itemSeekBytes = Bytes.read(itemSeekListBytes, i * 8, 8); // 8字节子数据偏移量字节数组
            Item item = new Item(filepath, Bytes.toLong(itemSeekBytes));
            if (item.indexName.equals(indexName) && item.key.equals(key)) {
                return item.getData();
            }
        }
        return null;
    }

    /**
     * 读取指定传入原始key相匹配的数据
     *
     * @param indexName 索引名（全名组合确保唯一性，如：库名+表名+索引名）
     */
    public List<byte[]> read(String indexName) throws IOException {
        List<byte[]> bytesList = new ArrayList<>();
        int mainDataLength = Bytes.toInt(Channel.read(filepath, seek, 4)); // 4字节数据主体长度
        byte[] itemSeekListBytes = Channel.read(filepath, seek + 4, mainDataLength); // 【8字节子数据偏移量+…+8字节子数据偏移量】
        int size = mainDataLength / 8;
        for (int i = 0; i < size; i++) {
            byte[] itemSeekBytes = Bytes.read(itemSeekListBytes, i * 8, 8); // 8字节子数据偏移量字节数组
            Item item = new Item(filepath, Bytes.toLong(itemSeekBytes));
            if (item.indexName.equals(indexName)) {
                bytesList.add(item.getData());
            }
        }
        return bytesList;
    }

    /**
     * 指向本数据的偏移量为0时，新增数据。传入原始key、数据文件地址和数据。
     * 会将数据追加到数据文件中，同时生成本数据的起始偏移量
     *
     * @param datumMateSeek 指向数据坐标值在数据文件中的起始偏移量
     * @param indexName     索引名（全名组合确保唯一性，如：库名+表名+索引名）
     * @param key           原始key
     * @param value         数据
     */
    public void write(Transaction transaction, String indexFilepath, long datumMateSeek, String indexName, String key, byte[] value) throws IOException {
        byte[] itemBytes = new Item(indexName, key, value).toBytes(); // 索引名称字节数组长度(4字节)+索引名称字节数组+原始key字节数组长度(4字节)+原始key字节数组+4字节子数据长度+子数据
        long itemSeek = Channel.append(filepath, itemBytes); // 8字节子数据偏移量
        byte[] itemSeekBytes = Bytes.fromLong(itemSeek); // 8字节子数据偏移量字节数组
        byte[] mainDataLengthBytes = Bytes.fromInt(8);
        // 4字节数据主体长度+【8字节子数据偏移量】
        byte[] mainData = Bytes.join(mainDataLengthBytes, itemSeekBytes);

        long datumSeek = Channel.append(filepath, mainData);
        transaction.addTask(indexFilepath, datumMateSeek, Bytes.fromLong(datumSeek), new byte[8]);
        seek = datumSeek;
    }

    /**
     * 指向本数据的偏移量不为0时，新增数据。传入原始key、数据文件地址、数据在文件中的起始偏移量和数据。
     * 会将数据追加到数据文件中，同时生成本数据新的起始偏移量。新偏移量会更新原偏移量的值
     *
     * @param datumMateSeek 指向数据坐标值在数据文件中的起始偏移量
     * @param key           原始key
     * @param value         数据
     */
    public void update(Transaction transaction, long datumMateSeek, String indexName, String key, byte[] value) throws IOException {
        // 先读取旧数据
        int mainDataLength = Bytes.toInt(Channel.read(filepath, seek, 4)); // 读取4字节数据主体长度
        byte[] itemSeekListBytes = Channel.read(filepath, seek + 4, mainDataLength); // 【8字节子数据偏移量+…+8字节子数据偏移量】
        int sizeTmp = (mainDataLength / 8) + 1;
        Transaction.Task task = null;
        byte[][] bytesArr = new byte[sizeTmp][8];
        int position = 0;
        while (position < mainDataLength) { // 遍历旧数据，如果存在则更新偏移量即可
            // 8字节子数据偏移量
            byte[] itemSeekBytes = Bytes.read(itemSeekListBytes, position, 8);
            bytesArr[position / 8] = itemSeekBytes;
            long itemSeek = Bytes.toLong(itemSeekBytes);
            // 索引名称字节数组长度(4字节)+索引名称字节数组+原始key字节数组长度(4字节)+原始key字节数组+4字节子数据长度+子数据
            Item item = new Item(filepath, itemSeek);
            if (item.key.equals(key)) { // 原始key相同，更新
                long itemMateSeek = seek + 4 + position;
                byte[] itemBytes = new Item(indexName, key, value).toBytes(); // 索引名称字节数组长度(4字节)+索引名称字节数组+原始key字节数组长度(4字节)+原始key字节数组+4字节子数据长度+子数据
                long itemSeekNew = Channel.append(filepath, itemBytes); // 8字节子数据偏移量
                task = new Transaction.Task(filepath, itemMateSeek, Bytes.fromLong(itemSeekNew), Bytes.fromLong(itemSeek));
                break;
            }
            position += 8;
        }
        if (Objects.isNull(task)) { // 在历史数据中未匹配到key，新增key对应数据
            byte[] itemBytes = new Item(indexName, key, value).toBytes(); // 索引名称字节数组长度(4字节)+索引名称字节数组+原始key字节数组长度(4字节)+原始key字节数组+4字节子数据长度+子数据
            long itemSeek = Channel.append(filepath, itemBytes); // 8字节子数据偏移量
            bytesArr[position / 8] = Bytes.fromLong(itemSeek);
            byte[] itemSeekListBytesNew = Bytes.join(bytesArr);
            byte[] mainDataLengthBytes = Bytes.fromInt(itemSeekListBytesNew.length);
            // 新建数据结构：4字节数据主体长度+【8字节子数据偏移量+…+8字节子数据偏移量】
            byte[] mainDataBytes = Bytes.join(mainDataLengthBytes, itemSeekListBytesNew);
            long datumSeek = Channel.append(filepath, mainDataBytes); // 8字节子数据偏移量
            task = new Transaction.Task(filepath, datumMateSeek, Bytes.fromLong(datumSeek), Bytes.fromLong(seek));
        }
        transaction.addTask(task);
    }

    /** 索引名称字节数组长度(4字节)+索引名称字节数组+原始key字节数组长度(4字节)+原始key字节数组+4字节子数据长度+子数据 */
    static class Item {

        // 写入文件开始
        /** 索引名称 */
        String indexName;
        /** 原始key */
        String key;
        /** 4字节子数据长度 */
        int dataLength;
        /** 子数据 */
        byte[] data;
        // 写入文件结束

        /** 数据文件地址，如"tmp/data.1.1.td */
        String filepath;
        /** 8字节子数据偏移量 */
        long seek;
        int indexNameLength;
        int keyLength;

        /**
         * 根据当前数据所在偏移量读取数据内容，只获取原始key信息，如要获取数据，则继续调用getData方法
         *
         * @param filepath 数据文件地址，如"tmp/data.1.1.td
         * @param seek     8字节子数据偏移量
         */
        public Item(String filepath, long seek) throws IOException {
            this.filepath = filepath;
            this.seek = seek;
            indexNameLength = Bytes.toInt(Channel.read(filepath, seek, 4));
            byte[] indexNameBytes = Channel.read(filepath, seek + 4, indexNameLength);
            indexName = Bytes.toString(indexNameBytes);
            keyLength = Bytes.toInt(Channel.read(filepath, seek + 4 + indexNameLength, 4));
            byte[] keyByte = Channel.read(filepath, seek + 4 + indexNameLength + 4, keyLength);
            key = Bytes.toString(keyByte);
        }

        public Item(String indexName, String key, byte[] data) {
            this.indexName = indexName;
            this.key = key;
            this.dataLength = data.length;
            this.data = data;
        }

        /**
         * 必须经过‘public Item(String filepath, long seek)’初始化后的item才可获取结果，或filepath、seek和keyLength必须有值
         *
         * @return data
         */
        public byte[] getData() throws IOException {
            dataLength = Bytes.toInt(Channel.read(filepath, seek + 4 + indexNameLength + 4 + keyLength, 4));
            data = Channel.read(filepath, seek + 4 + indexNameLength + 4 + keyLength + 4, dataLength);
            return data;
        }

        public byte[] toBytes() throws IOException {
            byte[] indexNameBytes = Bytes.fromString(indexName);
            byte[] indexNameBytesLengthBytes = Bytes.fromInt(indexNameBytes.length);
            byte[] keyBytes = Bytes.fromString(key);
            byte[] keyBytesLengthBytes = Bytes.fromInt(keyBytes.length);
            return Bytes.join(indexNameBytesLengthBytes, indexNameBytes, keyBytesLengthBytes, keyBytes, Bytes.fromInt(dataLength), data);
        }
    }
}
