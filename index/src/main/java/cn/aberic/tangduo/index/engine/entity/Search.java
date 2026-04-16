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
import org.springframework.beans.BeanUtils;

import java.rmi.UnexpectedException;
import java.util.ArrayList;
import java.util.List;

/// 查询对象
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Search {

    /// 索引名（全名组合确保唯一性，如：库名+表名+索引名）
    String indexName;
    /// 最小主键（-9223372036854775807 —— 9223372036854775808）
    long degreeMin = Long.MIN_VALUE;
    /// 最大主键（-9223372036854775807 —— 9223372036854775808）
    long degreeMax = Long.MAX_VALUE;
    /// 是否包含最小主键
    boolean includeMin = true;
    /// 是否包含最大主键
    boolean includeMax = true;
    Integer limit = Integer.MAX_VALUE;
    /// 是否升序排序
    boolean asc = true;
    /// 是否删除操作
    boolean delete = false;
    /// 过滤器
    SearchFilter searchFilter;
    /// 查询条件
    List<Condition> conditions = new ArrayList<>();

    /// 构造方法
    /// @param indexName 索引名（全名组合确保唯一性，如：库名+表名+索引名）
    public Search(String indexName) {
        this.indexName = indexName;
    }

    /// 构造方法
    /// @param indexName 索引名（全名组合确保唯一性，如：库名+表名+索引名）
    /// @param limit     查询数量
    public Search(String indexName, Integer limit) {
        this.indexName = indexName;
        this.limit = limit;
    }

    /// 构造方法
    /// @param search    查询对象
    /// @param indexName 索引名（全名组合确保唯一性，如：库名+表名+索引名）
    /// @param limit     查询数量
    /// @param asc       是否升序排序
    /// @param searchFilter 过滤器
    public Search(Search search, String indexName, Integer limit, boolean asc, SearchFilter searchFilter) {
        BeanUtils.copyProperties(search, this);
        this.indexName = indexName;
        this.limit = limit;
        this.asc = asc;
        this.searchFilter = searchFilter;
    }

    /// 构造方法
    /// @param indexName 索引名（全名组合确保唯一性，如：库名+表名+索引名）
    /// @param limit     查询数量
    /// @param asc       是否升序排序
    public Search(String indexName, Integer limit, boolean asc) {
        this.indexName = indexName;
        this.limit = limit;
        this.asc = asc;
    }

    /// 构造方法
    /// @param indexName 索引名（全名组合确保唯一性，如：库名+表名+索引名）
    /// @param degreeMin 最小主键（-9223372036854775807 —— 9223372036854775808）
    /// @param degreeMax 最大主键（-9223372036854775807 —— 9223372036854775808）
    /// @param includeMin 是否包含最小主键
    /// @param includeMax 是否包含最大主键
    /// @param asc        是否升序排序
    public Search(String indexName, long degreeMin, long degreeMax, boolean includeMin, boolean includeMax, boolean asc) {
        this.indexName = indexName;
        this.degreeMin = degreeMin;
        this.degreeMax = degreeMax;
        this.includeMin = includeMin;
        this.includeMax = includeMax;
        this.asc = asc;
    }

    /// 构造方法
    /// @param indexName 索引名（全名组合确保唯一性，如：库名+表名+索引名）
    /// @param degreeMin 最小主键（-9223372036854775807 —— 9223372036854775808）
    /// @param degreeMax 最大主键（-9223372036854775807 —— 9223372036854775808）
    /// @param includeMin 是否包含最小主键
    /// @param includeMax 是否包含最大主键
    /// @param limit      查询数量
    /// @param asc        是否升序排序  
    public Search(String indexName, long degreeMin, long degreeMax, boolean includeMin, boolean includeMax, int limit, boolean asc) {
        this.indexName = indexName;
        this.degreeMin = degreeMin;
        this.degreeMax = degreeMax;
        this.includeMin = includeMin;
        this.includeMax = includeMax;
        this.limit = limit;
        this.asc = asc;
    }

    /// 构造方法
    /// @param indexName 索引名（全名组合确保唯一性，如：库名+表名+索引名）
    /// @param degreeMin 最小主键（-9223372036854775807 —— 9223372036854775808）
    /// @param degreeMax 最大主键（-9223372036854775807 —— 9223372036854775808）
    /// @param includeMin 是否包含最小主键
    /// @param includeMax 是否包含最大主键
    /// @param limit      查询数量
    /// @param asc        是否升序排序  
    /// @param searchFilter 过滤器
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

    /// 新增条件
    ///
    /// @param param        选中的key，目标为json对象中的key，通过.的方式拼接，允许指定深层次，如 name，school.student.name 等
    /// @param compare      条件 gt/ge/lt/le/eq/ne 大于/大于等于/小于/小于等于/等于/不等
    /// @param compareValue 要比较的值，大于或等于当前Object的内容
    public void addCondition(String param, String compare, Object compareValue) throws UnexpectedException {
        conditions.add(new Condition(param, Condition.Compare.getByType(compare), compareValue));
    }

}