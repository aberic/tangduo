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

import java.util.Arrays;
import java.util.List;

/// 列表工具类
public final class ListTools {

    private ListTools() {
        throw new AssertionError("工具类禁止实例化");
    }

    /// 列表 → 字符串
    /// @param list 待转换的列表
    /// @return 转换后的字符串
    /// <p>
    /// 转换规则：列表中的元素用逗号分隔。
    /// </p>
    public static String toString(List<String> list) {
        return String.join(",", list);
    }

    /// 字符串 → 列表
    /// @param str 待转换的字符串
    /// @return 转换后的列表
    /// <p>
    /// 转换规则：字符串中的元素用逗号分隔。
    /// </p>
    public static List<String> fromString(String str) {
        return Arrays.asList(str.split(","));
    }

}
