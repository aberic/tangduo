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

import jakarta.annotation.Nullable;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.util.CollectionUtils;

import java.rmi.UnexpectedException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/// 查询对象
@Data
@NoArgsConstructor
public class Select {

    /// 处理后的索引名（全名组合确保唯一性，如：库名+表名+索引名）
    String indexName;
    /// 限定取出数量
    Integer limit = Integer.MAX_VALUE;
    /// 是否删除操作
    boolean delete = false;
    /// 命中策略
    Hit hit = new Hit();
    /// 过滤器
    SelectFilter selectFilter;

    /// 构造方法，db用
    ///
    /// @param indexName 索引名（全名组合确保唯一性，如：库名+表名+索引名）
    /// @param limit     查询数量
    public Select(String indexName, Integer limit) {
        this.indexName = indexName;
        this.limit = limit;
    }

    /// 构造方法，db用
    ///
    /// @param select    查询对象
    /// @param indexName 索引名（全名组合确保唯一性，如：库名+表名+索引名）
    /// @param asc       是否升序排序
    public Select(Select select, String indexName, boolean asc) {
        BeanUtils.copyProperties(select, this);
        this.indexName = indexName;
        this.hit = new Hit(indexName, asc);
    }

    /// 构造方法，测试用
    ///
    /// @param indexName  索引名（全名组合确保唯一性，如：库名+表名+索引名）
    /// @param degreeMin  最小主键（-9223372036854775807 —— 9223372036854775808）
    /// @param degreeMax  最大主键（-9223372036854775807 —— 9223372036854775808）
    /// @param includeMin 是否包含最小主键
    /// @param includeMax 是否包含最大主键
    /// @param asc        是否升序排序
    public Select(String indexName, long degreeMin, long degreeMax, boolean includeMin, boolean includeMax, boolean asc) {
        hit = new Hit(indexName, asc);
        hit.area.startDegree = degreeMin;
        hit.area.endDegree = degreeMax;
        hit.area.includeStart = includeMin;
        hit.area.includeEnd = includeMax;
    }

    /// 构造方法，测试用
    ///
    /// @param indexName  索引名（全名组合确保唯一性，如：库名+表名+索引名）
    /// @param degreeMin  最小主键（-9223372036854775807 —— 9223372036854775808）
    /// @param degreeMax  最大主键（-9223372036854775807 —— 9223372036854775808）
    /// @param includeMin 是否包含最小主键
    /// @param includeMax 是否包含最大主键
    /// @param limit      查询数量
    /// @param asc        是否升序排序
    public Select(String indexName, long degreeMin, long degreeMax, boolean includeMin, boolean includeMax, int limit, boolean asc) {
        this.indexName = indexName;
        this.limit = limit;
        hit = new Hit(indexName, asc);
        hit.area.startDegree = degreeMin;
        hit.area.endDegree = degreeMax;
        hit.area.includeStart = includeMin;
        hit.area.includeEnd = includeMax;
    }

    /// 构造方法
    ///
    /// @param indexName    索引名（全名组合确保唯一性，如：库名+表名+索引名）
    /// @param limit        查询数量
    /// @param delete       是否删除操作
    /// @param hit          命中策略
    /// @param selectFilter 自定义过滤接口
    public Select(@Nullable String indexName, Integer limit, boolean delete, Hit hit, SelectFilter selectFilter) {
        this.indexName = indexName;
        this.limit = limit;
        this.delete = delete;
        if (hit.sortIsNull()) {
            if (StringUtils.isEmpty(indexName)) {
                throw new NullPointerException("indexName and sort both null!");
            }
            hit.sort = new Sort(indexName);
        }
        this.hit = hit;
        this.selectFilter = selectFilter;
    }

    public String getSortIndexName() {
        if (hit.sortNotNull()) {
            return hit.sort.indexName;
        } else {
            return null;
        }
    }

    /// 是否升序排序
    public boolean isAsc() {
        if (hit.sortNotNull()) {
            return hit.sort.asc;
        } else {
            return true;
        }
    }

    public long getDegreeMin() {
        if (Objects.isNull(hit.sort)) {
            return hit.area.startDegree;
        }
        if (hit.sort.asc) {
            return Objects.nonNull(hit.sort.afterDegree) && indexName.equals(hit.sort.indexName) ? hit.sort.afterDegree : hit.area.startDegree;
        }
        return hit.area.startDegree;
    }

    public long getDegreeMax() {
        if (Objects.isNull(hit.sort)) {
            return hit.area.endDegree;
        }
        if (hit.sort.asc) {
            return hit.area.endDegree;
        }
        return Objects.nonNull(hit.sort.afterDegree) && indexName.equals(hit.sort.indexName) ? hit.sort.afterDegree : hit.area.endDegree;
    }

    public boolean isIncludeMin() {
        return hit.area.includeStart;
    }

    public boolean isIncludeMax() {
        return hit.area.includeEnd;
    }

    public Long sortAfterDegree() {
        if (Objects.isNull(hit.sort)) {
            return null;
        }
        return hit.sort.afterDegree;
    }

    public void reHit() {
        hit.reSet(indexName);
    }

    public void reSetFields() {
        if (CollectionUtils.isEmpty(hit.fields)) {
            return;
        }
        List<String> temp = new ArrayList<>(hit.fields);
        temp.replaceAll(s -> "value." + s);
        hit.setFields(temp);
    }

    /// 新增条件
    ///
    /// @param param        选中的key，目标为json对象中的key，通过.的方式拼接，允许指定深层次，如 name，school.student.name 等
    /// @param compare      条件 gt/ge/lt/le/eq/ne 大于/大于等于/小于/小于等于/等于/不等
    /// @param compareValue 要比较的值，大于或等于当前Object的内容
    public void addCondition(String param, String compare, Object compareValue) throws UnexpectedException {
        hit.addCondition(param, compare, compareValue);
    }

    /// 新增要返回并显示的key
    public void addField(String field) {
        hit.fields.add(field);
    }


    /// 排序
    public void setSort(Sort sort) {
        hit.sort = sort;
    }

}