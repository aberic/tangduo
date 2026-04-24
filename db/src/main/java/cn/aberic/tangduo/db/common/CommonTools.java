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

/// 通用工具类
/// 用于处理字符串、日期等通用操作
public final class CommonTools {
    
    private CommonTools() {
        throw new AssertionError("工具类禁止实例化");
    }

    /// 判断字符串是否为【单个完整汉字】（包含基本汉字+所有扩展区汉字）
    /// @param str 待判断字符串
    /// @return 是单个汉字返回true，否则返回false
    public static boolean isSingleChinese(String str) {
        return !unSingleChinese(str);
    }

    /// 判断字符串是否为【单个完整汉字】（包含基本汉字+所有扩展区汉字）
    /// @param str 待判断字符串
    /// @return 是单个汉字返回true，否则返回false
    public static boolean unSingleChinese(String str) {
        // 先判断：字符串为null 或 长度不是1，直接返回false
        if (str == null || str.length() != 1) {
            return true;
        }
        // 获取字符的Unicode码点（int类型，支持5位及以上Unicode）
        int codePoint = str.codePointAt(0);

        // 原汉字区间逻辑不变，改用十进制码点比较
        return (codePoint < 0x4E00 || codePoint > 0x9FFF)    // 基本汉字
                && (codePoint < 0x3400 || codePoint > 0x4DBF)  // 扩展A
                && (codePoint < 0x20000 || codePoint > 0x2A6DF) // 扩展B
                && (codePoint < 0x2A700 || codePoint > 0x2B73F)
                && (codePoint < 0x2B740 || codePoint > 0x2B81F);
    }

    /// 判断字符串是否为【单个英文字母】（a-z A-Z）
    /// 
    public static boolean isSingleLetter(String str) {
        return !unSingleLetter(str);
    }

    /// 判断字符串是否为【单个英文字母】（a-z A-Z）
    /// @param str 待判断字符串
    /// @return 是单个英文字母返回true，否则返回false
    public static boolean unSingleLetter(String str) {
        // 必须非空 + 长度严格等于 1
        if (str == null || str.length() != 1) {
            return true;
        }
        char c = str.charAt(0);
        // 判断是否是 大写 或 小写 英文字母
        return (c < 'a' || c > 'z') && (c < 'A' || c > 'Z');
    }

    /// 生成索引名称
    /// @param indexName 索引名称
    /// @return 索引名称
    /// @see #indexName(String, String, String)
    public static String indexName(String indexName) {
        return indexName(indexName, "key");
    }

    /// 生成索引名称
    /// @param indexName 索引名称
    /// @return 索引名称
    /// @see #indexName(String, String, String)
    public static String indexName4datetime(String indexName) {
        return indexName(indexName, "datetime");
    }

    /// 生成索引名称
    /// @param value 值
    /// @return 索引名称
    /// @see #indexName(String, String, String)
    public static String indexName4hash(String value) {
        return indexName(String.valueOf(value.length()), "key");
    }

    /// 生成索引名称
    /// @param indexName 索引名称
    /// @param fieldName 字段名
    /// @return 索引名称
    /// @see #indexName(String, String, String)
    public static String indexName(String indexName, String fieldName) {
        return indexName("default", indexName, fieldName);
    }

    /// 生成索引名称
    /// @param schema  数据库模式
    /// @param indexName 索引名称
    /// @param fieldName 字段名
    /// @return 索引名称
    /// @see #indexName(String, String, String)
    public static String indexName(String schema, String indexName, String fieldName) {
        return String.format("%s_%s_%s", schema, indexName, fieldName).toLowerCase();
    }

}
