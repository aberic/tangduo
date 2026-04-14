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

import cn.aberic.tangduo.index.engine.entity.Conditions;
import lombok.Data;

@Data
public class ReqSelectDataVO {

    /// 插入、读取
    String database;
    /** 索引名（全名组合确保唯一性，如：库名+表名+索引名） */
    String index;
    /** 最小主键（-9223372036854775807 —— 9223372036854775808） */
    long degreeMin = Long.MIN_VALUE;
    /** 最大主键（-9223372036854775807 —— 9223372036854775808） */
    long degreeMax = Long.MAX_VALUE;
    /** 是否包含最小主键 */
    boolean includeMin = true;
    /** 是否包含最大主键 */
    boolean includeMax = true;
    Integer limit = 10;
    boolean asc = true;

    Conditions conditions;

}
