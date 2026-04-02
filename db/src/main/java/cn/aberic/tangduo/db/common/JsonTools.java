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

package cn.aberic.tangduo.db.common;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.*;

public class JsonTools {

    /// 全局单例
    public static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /**
     * 判断字符串是否是合法的 JSON
     * 支持：{}、[]、纯值（数字/字符串/布尔/null）
     */
    public static boolean isJson(String str) {
        if (str == null || str.isBlank()) {
            return false;
        }
        String trim = str.trim();
        // 快速判断：JSON 必须以 { 或 [ 开头（标准JSON格式）
        if (!(trim.startsWith("{") || trim.startsWith("["))) {
            return false;
        }
        try {
            // 尝试解析，能解析就是合法 JSON
            OBJECT_MAPPER.readTree(str);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public record IndexName4KeyAndDegree(String indexName, String key, Long degree) {}
    public static List<IndexName4KeyAndDegree> parseIndexName4KeyAndDegree(String str) throws JsonProcessingException {
        List<IndexName4KeyAndDegree> list = new ArrayList<>();
        if (Objects.isNull(str) || !isJson(str)) {
            return list;
        }
        long degree = System.currentTimeMillis();
        String key = String.valueOf(degree);
        // 解析字符串
        JsonNode node = OBJECT_MAPPER.readTree(str);
        node.forEachEntry((indexName, jsonNode) -> {
            if (jsonNode.isTextual()) {
                if (jsonNode.asText().length() < 30) {
                    list.add(new IndexName4KeyAndDegree(CommonTools.indexName(indexName), jsonNode.asText(), KeyHashTools.toLongKey(jsonNode.asText())));
                    list.add(new IndexName4KeyAndDegree(CommonTools.indexName4datetime(indexName), key, degree));
                }
            } else if (jsonNode.isLong() || jsonNode.isInt()) {
                list.add(new IndexName4KeyAndDegree(CommonTools.indexName(indexName), jsonNode.asText(), jsonNode.asLong()));
                list.add(new IndexName4KeyAndDegree(CommonTools.indexName4datetime(indexName), key, degree));
            } else if (jsonNode.isDouble() || jsonNode.isFloat()) {
                list.add(new IndexName4KeyAndDegree(CommonTools.indexName(indexName), jsonNode.asText(), KeyHashTools.toLongKey(jsonNode.asDouble())));
                list.add(new IndexName4KeyAndDegree(CommonTools.indexName4datetime(indexName), key, degree));
            } else {
                list.add(new IndexName4KeyAndDegree(CommonTools.indexName4datetime(indexName), key, degree));
            }
        });
        return list;
    }

}
