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

package cn.aberic.tangduo.common;

import lombok.extern.slf4j.Slf4j;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;

/// JSON 工具类
@Slf4j
public final class JsonTools {

    private JsonTools() {
        throw new AssertionError("工具类禁止实例化");
    }

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

    /// 对象 → JSON 字符串
    public static String toJson(Object obj) {
        try {
            return OBJECT_MAPPER.writeValueAsString(obj);
        } catch (Exception e) {
            return null;
        }
    }

    /// JSON → 单个对象
    public static <T> T toObj(String json, Class<T> clazz) {
        try {
            return OBJECT_MAPPER.readValue(json, clazz);
        } catch (Exception e) {
            log.error("JSON反序列化失败，json: {}, clazz: {}", json, clazz.getName(), e);
            return null;
        }
    }

    /// JSON → List<T> 对象
    public static <T> List<T> toList(String json) {
        try {
            return OBJECT_MAPPER.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            return null;
        }
    }

    /// JSON → Map<String, Object>
    public static Map<String, Object> toMap(String json) {
        try {
            return OBJECT_MAPPER.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            return null;
        }
    }

    /// 对象 → JsonNode 节点
    public static JsonNode toJsonNode(Object obj) {
        if (obj == null) {
            return null;
        }
        try {
            if (obj instanceof String) {
                String objStr = String.valueOf(obj);
                if (isJson(objStr)) {
                    return OBJECT_MAPPER.readTree(objStr);
                }
            }
            return OBJECT_MAPPER.valueToTree(obj);
        } catch (Exception e) {
            log.error("Object转JsonNode失败", e);
            return null;
        }
    }

    /**
     * 根据点分割路径获取 json 中的值
     *
     * @param json 原始 json 字符串
     * @param path 如 "student.age"、"values[0].value.city"
     *
     * @return 读到的值
     */
    public static Object getValueByPath(String json, String path) throws Exception {
        JsonNode root = OBJECT_MAPPER.readTree(json);
        String[] keys = path.split("\\.");

        JsonNode currentNode = root;
        for (String key : keys) {
            // 处理数组，例如 values[0]
            if (key.contains("[")) {
                String arrayName = key.substring(0, key.indexOf("["));
                String indexStr = key.substring(key.indexOf("[") + 1, key.indexOf("]"));
                int index = Integer.parseInt(indexStr);

                currentNode = currentNode.get(arrayName);
                currentNode = currentNode.get(index);
            } else {
                currentNode = currentNode.get(key);
            }

            if (currentNode == null || currentNode.isMissingNode()) {
                return null;
            }
        }

        // 返回合适的类型
        if (currentNode.isString()) {
            return currentNode.asString();
        } else if (currentNode.isNumber()) {
            return currentNode.numberValue();
        } else if (currentNode.isBoolean()) {
            return currentNode.asBoolean();
        } else if (currentNode.isObject() || currentNode.isArray()) {
            // 转成普通 Map / List
            return OBJECT_MAPPER.convertValue(currentNode, new TypeReference<>() {});
        } else {
            return currentNode.asString();
        }
    }

    /**
     * 自动识别 JsonNode 类型，转成对应 Java 对象
     * 对象 -> Map
     * 数组 -> List
     * 数字 -> Number
     * 字符串 -> String
     * 布尔 -> Boolean
     * 其他 -> String
     */
    public static Object parseJsonNode(JsonNode node) {
        if (node == null || node.isNull() || node.isMissingNode()) {
            return null;
        }

        // 对象 → Map
        if (node.isObject()) {
            return OBJECT_MAPPER.convertValue(node, Map.class);
        }

        // 数组 → List
        if (node.isArray()) {
            return OBJECT_MAPPER.convertValue(node, List.class);
        }

        // 数字 → Number
        if (node.isNumber()) {
            return node.numberValue();
        }

        // 字符串 → String
        if (node.isString()) {
            return node.asString();
        }

        // 布尔 → Boolean
        if (node.isBoolean()) {
            return node.asBoolean();
        }

        // 其他都返回字符串
        return node.asString();
    }

    /**
     * JSON 字符串 去空格、去换行、去回车
     * 只去掉【无效空格】，不会破坏字符串内部的空格
     */
    public static String compactJson(String json) {
        if (json == null || json.isEmpty()) {
            return json;
        }

        StringBuilder sb = new StringBuilder();
        boolean inString = false; // 是否在字符串内部
        char prev = 0;

        for (char c : json.toCharArray()) {
            // 换行、回车直接跳过
            if (c == '\n' || c == '\r') {
                continue;
            }

            // 处理引号，判断是否在字符串内部
            if (c == '"' && prev != '\\') {
                inString = !inString;
                sb.append(c);
            }
            // 如果不在字符串里，空格直接跳过
            else if (!inString && Character.isWhitespace(c)) {
                continue;
            }
            // 其他字符保留
            else {
                sb.append(c);
            }
            prev = c;
        }
        return sb.toString();
    }
}
