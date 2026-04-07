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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;

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
            return null;
        }
    }

    /// JSON → List<T> 对象
    public static <T> List<T> toList(String json, Class<T> clazz) {
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
}
