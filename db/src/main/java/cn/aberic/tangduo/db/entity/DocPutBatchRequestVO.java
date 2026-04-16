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

package cn.aberic.tangduo.db.entity;

import lombok.AllArgsConstructor;
import lombok.Data;

/// 文档批量插入请求VO
@AllArgsConstructor
@Data
public class DocPutBatchRequestVO {

    /// 索引名称
    String index;
    /// 度数
    Long degree;
    /// 键值
    String key;
    /// 是否分词
    boolean seg = false;
    /// 内容
    Object value;

    /// 文档批量插入请求VO
    /// @param index 索引名称
    /// @param key 键值
    /// @param value 值
    public DocPutBatchRequestVO(String index, String key, Object value) {
        this.index = index;
        this.key = key;
        this.value = value;
    }

    /// 文档批量插入请求VO
    /// @param index 索引名称
    /// @param key 键值
    /// @param seg 是否分片
    /// @param value 值
    public DocPutBatchRequestVO(String index, String key, boolean seg, Object value) {
        this.index = index;
        this.key = key;
        this.seg = seg;
        this.value = value;
    }
}
