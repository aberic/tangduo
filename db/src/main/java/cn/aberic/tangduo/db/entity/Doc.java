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
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.beans.BeanUtils;

import java.util.List;
import java.util.Objects;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL) // 为null的字段不序列化
@Data
@NoArgsConstructor
public class Doc {

    String database;
    String index;
    String key;
    long degree;
    String digests;
    JsonNode value;
    /** 分词集合 */
    List<String> segList;

    public Doc(byte[] bytes) throws JsonParseException {
        Doc doc = JsonTools.toObj(ByteTools.toString(bytes), Doc.class);
        if (Objects.isNull(doc)) {
            throw new JsonParseException("doc parse error!");
        }
        BeanUtils.copyProperties(doc, this);
    }

    public Doc(String database, String index, String key, long degree, Object value) {
        this.database = database;
        this.index = index;
        this.key = key;
        this.degree = degree;
        this.value = JsonTools.toJsonNode(value);
        this.digests = SHA256Tools.sha256(String.valueOf(this.value));
    }

    public byte[] toBytes() {
        return ByteTools.fromString(Objects.requireNonNull(JsonTools.toJson(this)));
    }

}
