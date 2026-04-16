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

import cn.aberic.tangduo.index.engine.entity.Content;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.beans.BeanUtils;

/// 文档插入响应VO
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL) // 为null的字段不序列化
@Data
@NoArgsConstructor
public class DocPutResponseVO {
    /// 数据库名称
    String database;
    /// 索引名称
    String index;
    /// 键值
    String key;
    /// 度数
    long degree;
    /// 摘要
    String digests;
    /// 内容
    @JsonIgnore
    Content content;

    /// 构造函数
    /// @param doc 文档
    /// @param content 内容
    public DocPutResponseVO(Doc doc, Content content) {
        BeanUtils.copyProperties(doc, this);
        this.content = content;
    }

}
