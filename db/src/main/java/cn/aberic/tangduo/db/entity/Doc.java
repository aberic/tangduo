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

import cn.aberic.tangduo.common.ByteTools;
import cn.aberic.tangduo.common.JsonTools;
import cn.aberic.tangduo.common.SHA256Tools;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.boot.json.JsonParseException;
import tools.jackson.databind.JsonNode;

import java.util.List;
import java.util.Objects;

/// 文档实体
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL) // 为null的字段不序列化
@Data
@NoArgsConstructor
public class Doc {

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
    JsonNode value;
    /// 分词集合
    List<String> segList;

    /// 文档实体构造函数
    /// @param bytes 文档字节数组
    public Doc(byte[] bytes) throws JsonParseException {
        Doc doc = JsonTools.toObj(ByteTools.toString(bytes), Doc.class);
        if (Objects.isNull(doc)) {
            throw new JsonParseException();
        }
        BeanUtils.copyProperties(doc, this);
    }

    /// 文档实体构造函数
    /// @param database 数据库名称
    /// @param index 索引名称
    /// @param key 键值
    /// @param degree 度数
    /// @param value 内容
    public Doc(String database, String index, String key, long degree, Object value) {
        this.database = database;
        this.index = index;
        this.key = key;
        this.degree = degree;
        this.value = JsonTools.toJsonNode(value);
        this.digests = SHA256Tools.sha256(String.valueOf(this.value));
    }

    /// 文档实体转换为字节数组
    /// @return 文档字节数组
    public byte[] toBytes() {
        return ByteTools.fromString(Objects.requireNonNull(JsonTools.toJson(this)));
    }

}
