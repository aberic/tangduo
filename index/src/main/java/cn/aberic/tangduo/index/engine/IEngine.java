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

import cn.aberic.tangduo.index.engine.entity.Content;
import cn.aberic.tangduo.index.engine.entity.Search;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.List;

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

}
