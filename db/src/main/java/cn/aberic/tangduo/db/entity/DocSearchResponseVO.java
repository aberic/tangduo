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

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import cn.aberic.tangduo.common.JsonTools;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/// 文档搜索响应VO
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL) // 为null的字段不序列化
@Data
@AllArgsConstructor
@NoArgsConstructor
public class DocSearchResponseVO {
    /// 摘要
    private String digests;
    /// 内容
    private Object value;
    /// 分数
    private double score = 0.0;
    /// 预计算好的分词（关键：分词只做一次，不重复做）
    @JsonIgnore
    List<String> segList;

    /// 构造函数
    /// @param doc 文档
    public DocSearchResponseVO(Doc doc) {
        this.digests = doc.getDigests();
        this.segList = doc.getSegList();
        this.value = JsonTools.parseJsonNode(doc.value);
    }

    @Override
    public String toString() {
        return "score = " + score + ", value = " + value;
    }

}
