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

/// 排序策略
@AllArgsConstructor
@Data
public class Sort {

    String indexName;
    /// 选中的key，目标为json对象中的key，通过.的方式拼接，允许指定深层次，如 name，school.student.name 等
    String param;
    /// 下一页的起始度
    Long afterDegree;
    /// 是否升序排序
    boolean asc = true;

    public Sort(String indexName) {
        this.indexName = indexName;
    }

    /// 获取未处理的索引名称，如 name，school.student.name 都返回name
    public String getOriginIndexName() {
        String[] arr = param.split("\\.");
        return arr[arr.length - 1];
    }

}
