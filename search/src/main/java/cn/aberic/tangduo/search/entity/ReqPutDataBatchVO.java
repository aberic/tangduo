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

package cn.aberic.tangduo.search.entity;

import lombok.Data;

import java.util.List;

@Data
public class ReqPutDataBatchVO {

    /// 数据库名
    String database;
    /// 是否分词
    boolean seg = true;
    /// 批量插入
    List<Value> values;

    @Data
    public static class Value {

        /// 索引名
        String index;
        /// 唯一键名
        String key;
        /// 插入
        Object value;

    }

}
