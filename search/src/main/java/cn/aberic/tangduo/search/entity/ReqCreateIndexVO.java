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

/// 创建索引请求体
@Data
public class ReqCreateIndexVO {

    /// 数据库名
    String database;
    /// 索引名（全名组合确保唯一性，如：库名+表名+索引名）
    String index;
    /// 版本号，4个字节
    int version = 1;
    /// 索引名称
    String name;
    /// 是否主键，主键也是唯一索引，1个字节
    boolean primary = false;
    /// 是否唯一索引，1个字节
    boolean unique = false;
    /// 是否允许为空，1个字节
    boolean nullable = true;

}
