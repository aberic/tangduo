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

package cn.aberic.tangduo.index.engine.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.rmi.UnexpectedException;

/// 条件
/// 用于查询时，指定要比较的key和要比较的值
/// 例如：name=张三，age>=18
@NoArgsConstructor
@AllArgsConstructor
@Getter
public class Condition {

    /// 选中的key，目标为json对象中的key，通过.的方式拼接，允许指定深层次，如 name，school.student.name 等
    String param;
    /// 条件 gt/ge/lt/le/eq/ne 大于/大于等于/小于/小于等于/等于/不等
    Compare compare;
    /// 要比较的值，大于或等于当前Object的内容
    Object compareValue;

    /// 条件枚举
    public enum Compare {
        /// 大于
        GT("gt"),
        /// 大于等于
        GE("ge"),
        /// 小于
        LT("lt"),
        /// 小于等于
        LE("le"),
        /// 等于
        EQ("eq"),
        /// 不等
        NE("ne");


        // 成员变量
        private final String type;

        // 构造方法
        Compare(String type) {
            this.type = type;
        }

        /**
         * 通过类型获取对应处理枚举
         *
         * @param type 类型
         *
         * @return 处理枚举
         */
        public static Compare getByType(String type) throws UnexpectedException {
            for (Compare compare : values()) {
                if (compare.type.equals(type)) {
                    return compare;
                }
            }
            throw new UnexpectedException(type);
        }
    }

}
