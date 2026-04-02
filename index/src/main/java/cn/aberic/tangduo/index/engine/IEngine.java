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
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 索引引擎接口<p>
 * 0、Btree联合引擎<p>
 * 1、跳表引擎<p>
 * 2、Btree引擎<p>
 */
@Slf4j
public abstract class IEngine extends Number {

    /** Btree联合引擎 */
    public static final int UNITY = 0;
    /** 跳表引擎 */
    public static final int SKIP = 1;

    public abstract void force(long degree, String indexName) throws IOException;

    /**
     * Node插入数据data
     */
    public abstract void put(Content content) throws IOException;

    /**
     * 从Node中获取数据
     *
     * @param indexName 索引名（全名组合确保唯一性，如：库名+表名+索引名）
     * @param degree    主键（-9223372036854775807 —— 9223372036854775808）
     * @param key       原始key
     *
     * @return 数据
     */
    public abstract List<byte[]> get(String indexName, long degree, String key) throws IOException;

    /**
     * 从Node中删除数据
     *
     * @param indexName 索引名（全名组合确保唯一性，如：库名+表名+索引名）
     * @param degree    主键（-9223372036854775807 —— 9223372036854775808）
     * @param key       原始key
     */
    public abstract void remove(String indexName, long degree, String key) throws IOException;

    public abstract List<byte[]> select(Search search) throws IOException;

    public abstract List<byte[]> delete(Search search) throws IOException;

    @Override
    public long longValue() {
        return intValue();
    }

    @Override
    public float floatValue() {
        return intValue();
    }

    @Override
    public double doubleValue() {
        return intValue();
    }

    /** 写入对象 */
    @Data
    public static class Content {
        /** 事务 */
        Transaction transaction;
        /** 索引名（全名组合确保唯一性，如：库名+表名+索引名） */
        String indexName;
        /** 主键（-9223372036854775807 —— 9223372036854775808） */
        long degree;
        /** 原始key */
        String key;
        /** 数据 */
        byte[] value;

        /** 同一索引允许多个kv */
        List<Item> items = new ArrayList<>();

        // 存储结束后的内容
        /** 数据文件版本号 */
        byte[] dataFileVersionBytes;
        /** 数据偏移量 */
        byte[] dataSeekBytes;

        Lock lock = new ReentrantLock();
        Condition condition = lock.newCondition();
        AtomicBoolean isNotified = new AtomicBoolean(false);

        public Content(Transaction transaction, String indexName, long degree, String key, byte[] value) {
            this.transaction = transaction;
            this.indexName = indexName;
            this.degree = degree;
            this.key = key;
            this.value = value;
        }

        public void addItem(String indexName, long degree, String key) {
            items.add(new Item(indexName, degree, key));
        }

        public long getDegree(String indexName) {
            if (this.indexName.equals(indexName)) {
                return degree;
            }
            return Objects.requireNonNull(items.stream().filter(item -> item.indexName.equals(indexName)).findFirst().orElse(null)).degree;
        }

        public String getKey(String indexName) {
            if (this.indexName.equals(indexName)) {
                return key;
            }
            return Objects.requireNonNull(items.stream().filter(item -> item.indexName.equals(indexName)).findFirst().orElse(null)).key;
        }

        public Lock getLock(String indexName) {
            if (this.indexName.equals(indexName)) {
                return lock;
            }
            return Objects.requireNonNull(items.stream().filter(item -> item.indexName.equals(indexName)).findFirst().orElse(null)).lock;
        }

        public AtomicBoolean getIsNotified(String indexName) {
            if (this.indexName.equals(indexName)) {
                return isNotified;
            }
            return Objects.requireNonNull(items.stream().filter(item -> item.indexName.equals(indexName)).findFirst().orElse(null)).isNotified;
        }

        public Condition getCondition(String indexName) {
            if (this.indexName.equals(indexName)) {
                return condition;
            }
            return Objects.requireNonNull(items.stream().filter(item -> item.indexName.equals(indexName)).findFirst().orElse(null)).condition;
        }

        public String toString() {
            return String.format("WriteDataFileVersionAndDatumSeek - transactionId = %s, indexName = %s, degree = %s, key = %s, dataFileVersion = %s, dataSeek = %s",
                    transaction.number, indexName, degree, key,
                    Bytes.toInt(Objects.isNull(dataFileVersionBytes) ? new byte[4] : dataFileVersionBytes),
                    Bytes.toLong(Objects.isNull(dataSeekBytes) ? new byte[8] : dataSeekBytes));
        }

        @Data
        public static class Item {
            /** 索引名（全名组合确保唯一性，如：库名+表名+索引名） */
            String indexName;
            /** 主键（-9223372036854775807 —— 9223372036854775808） */
            long degree;
            /** 原始key */
            String key;
            Lock lock = new ReentrantLock();
            Condition condition = lock.newCondition();
            AtomicBoolean isNotified = new AtomicBoolean(false);

            public Item(String indexName, long degree, String key) {
                this.indexName = indexName;
                this.degree = degree;
                this.key = key;
            }
        }
    }

    /** 查询对象 */
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Search {
        /** 索引名（全名组合确保唯一性，如：库名+表名+索引名） */
        String indexName;
        /** 最小主键（-9223372036854775807 —— 9223372036854775808） */
        long degreeMin = Long.MIN_VALUE;
        /** 最大主键（-9223372036854775807 —— 9223372036854775808） */
        long degreeMax = Long.MAX_VALUE;
        /** 是否包含最小主键 */
        boolean includeMin = true;
        /** 是否包含最大主键 */
        boolean includeMax = true;
        Integer limit;
        boolean asc = true;
        boolean delete = false;

        SearchFilter searchFilter;

        public Search(String indexName, Integer limit, boolean asc) {
            this.indexName = indexName;
            this.limit = limit;
            this.asc = asc;
        }

        public Search(String indexName, long degreeMin, long degreeMax, boolean includeMin, boolean includeMax, boolean asc) {
            this.indexName = indexName;
            this.degreeMin = degreeMin;
            this.degreeMax = degreeMax;
            this.includeMin = includeMin;
            this.includeMax = includeMax;
            this.asc = asc;
        }

        public Search(String indexName, long degreeMin, long degreeMax, boolean includeMin, boolean includeMax, int limit, boolean asc) {
            this.indexName = indexName;
            this.degreeMin = degreeMin;
            this.degreeMax = degreeMax;
            this.includeMin = includeMin;
            this.includeMax = includeMax;
            this.limit = limit;
            this.asc = asc;
        }

        public Search(String indexName, long degreeMin, long degreeMax, boolean includeMin, boolean includeMax, int limit, boolean asc, SearchFilter searchFilter) {
            this.indexName = indexName;
            this.degreeMin = degreeMin;
            this.degreeMax = degreeMax;
            this.includeMin = includeMin;
            this.includeMax = includeMax;
            this.limit = limit;
            this.asc = asc;
            this.searchFilter = searchFilter;
        }
    }

    /** 自定义过滤接口 */
    public interface SearchFilter {

        /**
         * 过滤数据
         *
         * @param bytesList 待过滤数据集合
         *
         * @return 过滤后的数据集合
         */
        List<byte[]> filter(List<byte[]> bytesList);
    }
}
