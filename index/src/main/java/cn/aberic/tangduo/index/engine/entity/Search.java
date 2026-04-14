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

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 查询对象 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Search {
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
    Integer limit = Integer.MAX_VALUE;
    boolean asc = true;
    boolean delete = false;

    SearchFilter searchFilter;
    Conditions conditions;

    public Search(String indexName, Integer limit) {
        this.indexName = indexName;
        this.limit = limit;
    }

    public Search(String indexName, Conditions conditions) {
        this.indexName = indexName;
        this.conditions = conditions;
    }

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

    public Search(String indexName, long degreeMin, long degreeMax, boolean includeMin, boolean includeMax, Integer limit, SearchFilter searchFilter) {
        this.indexName = indexName;
        this.degreeMin = degreeMin;
        this.degreeMax = degreeMax;
        this.includeMin = includeMin;
        this.includeMax = includeMax;
        this.limit = limit;
        this.searchFilter = searchFilter;
    }

    public Search(String indexName, long degreeMin, long degreeMax, boolean includeMin, boolean includeMax, int limit, boolean asc, Conditions conditions) {
        this.indexName = indexName;
        this.degreeMin = degreeMin;
        this.degreeMax = degreeMax;
        this.includeMin = includeMin;
        this.includeMax = includeMax;
        this.limit = limit;
        this.asc = asc;
        this.conditions = conditions;
    }
}