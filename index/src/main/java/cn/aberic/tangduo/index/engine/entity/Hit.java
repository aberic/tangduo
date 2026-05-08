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
import org.apache.commons.lang3.StringUtils;

import java.rmi.UnexpectedException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/// 命中策略
@NoArgsConstructor
@AllArgsConstructor
@Data
public class Hit {

    /// 条件集合
    /// 用于查询时，指定要比较的key和要比较的值
    /// 例如：name=张三，age>=18
    List<Condition> conditions = new ArrayList<>();
    /// 排序策略
    Sort sort;
    /// 区域策略
    Area area = new Area();
    /// 要返回并显示的key，目标为json对象中的key，通过.的方式拼接，允许指定深层次，如 name，school.student.name 等
    List<String> fields = new ArrayList<>();

    public Hit(String indexName, boolean asc) {
        sort = new Sort(indexName);
        sort.asc = asc;
    }

    public boolean sortIsNull() {
        return !sortNotNull();
    }

    public boolean sortNotNull() {
        if (Objects.isNull(sort)) {
            return false;
        }
        return !StringUtils.isEmpty(sort.param) || !StringUtils.isEmpty(sort.indexName);
    }

    public void setSortIndexName(String indexName) {
        sort.indexName = indexName;
    }

    /// 新增条件
    ///
    /// @param param        选中的key，目标为json对象中的key，通过.的方式拼接，允许指定深层次，如 name，school.student.name 等
    /// @param compare      条件 gt/ge/lt/le/eq/ne 大于/大于等于/小于/小于等于/等于/不等
    /// @param compareValue 要比较的值，大于或等于当前Object的内容
    public void addCondition(String param, String compare, Object compareValue) throws UnexpectedException {
        conditions.add(new Condition(param, Condition.Compare.getByType(compare), compareValue));
    }

    /// 新增要返回并显示的key
    public void addField(String field) {
        fields.add(field);
    }

}
