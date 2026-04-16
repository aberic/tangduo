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

package cn.aberic.tangduo.index.engine.entity;

import cn.aberic.tangduo.common.ByteTools;
import cn.aberic.tangduo.index.engine.Transaction;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/// 写入对象
@Data
public class Content {

    /// 事务
    Transaction transaction;
    /// 索引名（全名组合确保唯一性，如：库名+表名+索引名）
    String indexName;
    /// 主键（-9223372036854775807 —— 9223372036854775808）
    long degree;
    /// 原始key
    String key;
    /// 数据
    byte[] value;

    /// 默认不允许自动创建索引
    boolean autoCreateIndex;

    /// 同一索引允许多个kv
    List<Item> items = new ArrayList<>();

    // 存储结束后的内容
    /// 数据文件版本号
    byte[] dataFileVersionBytes;
    /// 数据偏移量
    byte[] dataSeekBytes;

    Lock lock = new ReentrantLock();
    /// 条件
    Condition condition = lock.newCondition();
    /// 是否通知
    AtomicBoolean isNotified = new AtomicBoolean(false);

    /// 构造方法
    /// @param indexName 索引名（全名组合确保唯一性，如：库名+表名+索引名）
    /// @param degree 主键（-9223372036854775807 —— 9223372036854775808）
    /// @param key 原始key
    /// @param value 数据
    public Content(String indexName, long degree, String key, byte[] value) {
        this.indexName = indexName;
        this.degree = degree;
        this.key = key;
        this.value = value;
        autoCreateIndex = true;
    }

    /// 构造方法
    /// @param transaction 事务
    /// @param indexName 索引名（全名组合确保唯一性，如：库名+表名+索引名）
    /// @param degree 主键（-9223372036854775807 —— 9223372036854775808）
    /// @param key 原始key
    /// @param value 数据
    public Content(Transaction transaction, String indexName, long degree, String key, byte[] value) {
        this.transaction = transaction;
        this.indexName = indexName;
        this.degree = degree;
        this.key = key;
        this.value = value;
        autoCreateIndex = true;
    }

    /// 添加写入对象项
    /// @param indexName 索引名（全名组合确保唯一性，如：库名+表名+索引名）
    /// @param degree 主键（-9223372036854775807 —— 9223372036854775808）
    /// @param key 原始key
    public void addItem(String indexName, long degree, String key) {
        items.add(new Item(indexName, degree, key));
    }

    /// 获取主键
    /// @param indexName 索引名（全名组合确保唯一性，如：库名+表名+索引名）
    /// @return 主键
    public long getDegree(String indexName) {
        if (this.indexName.equals(indexName)) {
            return degree;
        }
        return Objects.requireNonNull(items.stream().filter(item -> item.indexName.equals(indexName)).findFirst().orElse(null)).degree;
    }

    /// 获取原始key
    /// @param indexName 索引名（全名组合确保唯一性，如：库名+表名+索引名）
    /// @return 原始key
    public String getKey(String indexName) {
        if (this.indexName.equals(indexName)) {
            return key;
        }
        return Objects.requireNonNull(items.stream().filter(item -> item.indexName.equals(indexName)).findFirst().orElse(null)).key;
    }

    /// 获取锁
    /// @param indexName 索引名（全名组合确保唯一性，如：库名+表名+索引名）
    /// @return 锁
    public Lock getLock(String indexName) {
        if (this.indexName.equals(indexName)) {
            return lock;
        }
        return Objects.requireNonNull(items.stream().filter(item -> item.indexName.equals(indexName)).findFirst().orElse(null)).lock;
    }

    /// 获取是否通知
    /// @param indexName 索引名（全名组合确保唯一性，如：库名+表名+索引名）
    /// @return 是否通知
    public AtomicBoolean getIsNotified(String indexName) {
        if (this.indexName.equals(indexName)) {
            return isNotified;
        }
        return Objects.requireNonNull(items.stream().filter(item -> item.indexName.equals(indexName)).findFirst().orElse(null)).isNotified;
    }

    /// 获取条件
    /// @param indexName 索引名（全名组合确保唯一性，如：库名+表名+索引名）
    /// @return 条件
    public Condition getCondition(String indexName) {
        if (this.indexName.equals(indexName)) {
            return condition;
        }
        return Objects.requireNonNull(items.stream().filter(item -> item.indexName.equals(indexName)).findFirst().orElse(null)).condition;
    }

    public String toString() {
        return String.format("WriteDataFileVersionAndDatumSeek - transactionId = %s, indexName = %s, degree = %s, key = %s, dataFileVersion = %s, dataSeek = %s",
                transaction.getNumber(), indexName, degree, key,
                ByteTools.toInt(Objects.isNull(dataFileVersionBytes) ? new byte[4] : dataFileVersionBytes),
                ByteTools.toLong(Objects.isNull(dataSeekBytes) ? new byte[8] : dataSeekBytes));
    }

    /// 写入对象项
    @Data
    public static class Item {
        /// 索引名（全名组合确保唯一性，如：库名+表名+索引名）
        String indexName;
        /// 主键（-9223372036854775807 —— 9223372036854775808）
        long degree;
        /// 原始key
        String key;
        Lock lock = new ReentrantLock();
        Condition condition = lock.newCondition();
        /// 是否通知
        AtomicBoolean isNotified = new AtomicBoolean(false);

        /// 构造方法
        /// @param indexName 索引名（全名组合确保唯一性，如：库名+表名+索引名）
        /// @param degree 主键（-9223372036854775807 —— 9223372036854775808）
        /// @param key 原始key
        public Item(String indexName, long degree, String key) {
            this.indexName = indexName;
            this.degree = degree;
            this.key = key;
        }
    }

}
