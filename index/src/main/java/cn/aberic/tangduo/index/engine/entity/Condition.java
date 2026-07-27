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

import java.math.BigDecimal;
import java.rmi.UnexpectedException;
import java.util.*;
import java.util.stream.Collectors;

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

    // 校验compareValue的类型是否匹配当前枚举
    public void validateValue() throws IllegalArgumentException {
        compare.validateValue(compareValue);
    }

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
        NE("ne"),
        /// 包含（在集合中），条件的compareValue必须是Collection类型（如List、Set）
        IN("in"),
        /// 不包含（不在集合中），条件的compareValue必须是Collection类型（如List、Set）
        NIN("nin"),
        /// 模糊匹配，条件的compareValue必须是字符串类型
        LIKE("like"),
        /// 不模糊匹配，条件的compareValue必须是字符串类型
        NLIKE("nlike"),
        /// 为空，条件无需设置compareValue（必须为null）
        IS_NULL("isnull"),
        /// 不为空，条件无需设置compareValue（必须为null）
        NOT_NULL("notnull");


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

        // 校验compareValue的类型是否匹配当前枚举
        private void validateValue(Object value) throws IllegalArgumentException {
            switch (this) {
                case IN:
                case NIN:
                    if (!(value instanceof Collection)) {
                        throw new IllegalArgumentException(this.type + " 条件的compareValue必须是Collection类型（如List、Set）");
                    }
                    break;
                case LIKE:
                case NLIKE:
                    if (value != null && !(value instanceof String)) {
                        throw new IllegalArgumentException(this.type + " 条件的compareValue必须是字符串类型");
                    }
                    break;
                case IS_NULL:
                case NOT_NULL:
                    if (value != null) {
                        throw new IllegalArgumentException(this.type + " 条件无需设置compareValue（必须为null）");
                    }
                    break;
                // 常规比较类型无需强制校验，兼容任意Object
                default:
                    break;
            }
        }
    }

    public static class Utils {

        /**
         * 精简Condition列表：相同param下保留最严格的区间条件 + EQ/NE与区间冲突校验
         *
         * @param conditions 原始条件列表
         * @return 精简后的条件列表
         * @throws UnexpectedException 1.同一param下多个EQ值不同；2.EQ值超出区间条件范围；
         */
        public static List<Condition> simplifyConditions(List<Condition> conditions) throws UnexpectedException {
            if (Objects.isNull(conditions) || conditions.isEmpty()) {
                return Collections.emptyList();
            }

            // 步骤1：按param分组
            Map<String, List<Condition>> paramGroupMap = conditions.stream()
                    .collect(Collectors.groupingBy(Condition::getParam));

            // 步骤2：遍历每组，精简条件
            List<Condition> simplified = new ArrayList<>();
            for (Map.Entry<String, List<Condition>> entry : paramGroupMap.entrySet()) {
                simplified.addAll(simplifySameParamConditions(entry.getValue()));
            }

            return simplified;
        }

        /**
         * 精简同一param下的条件列表 + EQ/NE与区间冲突校验
         *
         * @param sameParamConditions 同一param的条件列表
         * @return 精简后的条件列表
         * @throws UnexpectedException 1.多个EQ值不同；2.EQ值超出区间范围；
         */
        private static List<Condition> simplifySameParamConditions(List<Condition> sameParamConditions) throws UnexpectedException {
            if (sameParamConditions.size() == 1) {
                return sameParamConditions;
            }

            // 分离不同类型的条件：GT/GE、LT/LE、EQ、NE
            List<Condition> gtGeConditions = new ArrayList<>();
            List<Condition> ltLeConditions = new ArrayList<>();
            List<Condition> eqConditions = new ArrayList<>();
            List<Condition> neConditions = new ArrayList<>();

            String param = sameParamConditions.get(0).getParam();
            for (Condition condition : sameParamConditions) {
                Condition.Compare compare = condition.getCompare();
                switch (compare) {
                    case GT:
                    case GE:
                        gtGeConditions.add(condition);
                        break;
                    case LT:
                    case LE:
                        ltLeConditions.add(condition);
                        break;
                    case EQ:
                        eqConditions.add(condition);
                        break;
                    case NE:
                        neConditions.add(condition);
                        break;
                }
            }

            // 步骤3：处理EQ条件（原有冲突校验）
            List<Condition> processedEqConditions = processEqConditions(eqConditions, param);
            boolean hasEq = !processedEqConditions.isEmpty();
            BigDecimal eqValue = hasEq ? getBigDecimalValue(processedEqConditions.getFirst().getCompareValue()) : null;

            // 步骤4：精简区间条件（GT/GE/LT/LE）
            Condition simplifiedGtGe = simplifyGtGeConditions(gtGeConditions);
            Condition simplifiedLtLe = simplifyLtLeConditions(ltLeConditions);
            boolean hasRange = simplifiedGtGe != null || simplifiedLtLe != null;

            // 步骤5：EQ与区间条件的冲突校验（EQ合法则仅保留EQ，NE失效）
            if (hasEq && hasRange) {
                validateEqInRange(eqValue, simplifiedGtGe, simplifiedLtLe, param);
                // EQ值在区间内 → 仅保留EQ，区间和NE条件均失效
                return processedEqConditions;
            }

            // 步骤6：处理NE条件（无EQ时，校验NE与区间的关系）
            List<Condition> processedNeConditions = new ArrayList<>();
            if (!hasEq && hasRange) {
                processedNeConditions = processNeConditions(neConditions, simplifiedGtGe, simplifiedLtLe, param);
            } else {
                // 无区间条件 → 保留所有NE
                processedNeConditions = neConditions;
            }

            // 步骤7：合并最终条件（区间 + 处理后的NE + 无EQ则保留）
            List<Condition> simplified = new ArrayList<>();
            if (simplifiedGtGe != null) simplified.add(simplifiedGtGe);
            if (simplifiedLtLe != null) simplified.add(simplifiedLtLe);
            simplified.addAll(processedEqConditions);
            simplified.addAll(processedNeConditions);

            return simplified;
        }

        /**
         * 处理NE条件：
         * 1. NE值不在区间范围内 → 移除该NE（区间已排除该值，NE无意义）；
         * 2. NE值在区间范围内 → 保留该NE（区间内排除该值）；
         * 3. NE值非数值类型 → 保留（无法校验，默认有效）；
         *
         * @param neConditions  NE条件列表
         * @param gtGeCondition 精简后的GT/GE条件
         * @param ltLeCondition 精简后的LT/LE条件
         * @param param         参数名
         * @return 处理后的NE条件列表
         */
        private static List<Condition> processNeConditions(List<Condition> neConditions,
                                                           Condition gtGeCondition,
                                                           Condition ltLeCondition,
                                                           String param) {
            if (neConditions.isEmpty()) {
                return Collections.emptyList();
            }

            List<Condition> validNeConditions = new ArrayList<>();
            for (Condition ne : neConditions) {
                BigDecimal neValue = getBigDecimalValue(ne.getCompareValue());
                // 非数值类型无法校验，保留NE
                if (neValue == null) {
                    validNeConditions.add(ne);
                    continue;
                }

                // 校验NE值是否在区间内
                boolean isNeInRange = isValueInRange(neValue, gtGeCondition, ltLeCondition);
                // 仅当NE值在区间内时保留（不在则移除）
                if (isNeInRange) {
                    validNeConditions.add(ne);
                }
            }

            return validNeConditions;
        }

        /**
         * 校验数值是否在区间范围内（GT/GE为下限，LT/LE为上限）
         *
         * @param value         待校验数值
         * @param gtGeCondition 下限条件（GT/GE）
         * @param ltLeCondition 上限条件（LT/LE）
         * @return true=在区间内，false=不在区间内
         */
        private static boolean isValueInRange(BigDecimal value, Condition gtGeCondition, Condition ltLeCondition) {
            // 校验下限
            boolean passLower = true;
            if (gtGeCondition != null) {
                BigDecimal rangeMin = getBigDecimalValue(gtGeCondition.getCompareValue());
                Condition.Compare compare = gtGeCondition.getCompare();
                passLower = (compare == Condition.Compare.GT && value.compareTo(rangeMin) > 0)
                        || (compare == Condition.Compare.GE && value.compareTo(rangeMin) >= 0);
            }

            // 校验上限
            boolean passUpper = true;
            if (ltLeCondition != null) {
                BigDecimal rangeMax = getBigDecimalValue(ltLeCondition.getCompareValue());
                Condition.Compare compare = ltLeCondition.getCompare();
                passUpper = (compare == Condition.Compare.LT && value.compareTo(rangeMax) < 0)
                        || (compare == Condition.Compare.LE && value.compareTo(rangeMax) <= 0);
            }

            return passLower && passUpper;
        }

        /**
         * 校验EQ值是否落在区间条件范围内：
         * 1. EQ值 ≥ GT/GE值 且 ≤ LT/LE值 → 合法（仅保留EQ）；
         * 2. 否则 → 抛出异常（条件矛盾）；
         *
         * @param eqValue       EQ的数值
         * @param gtGeCondition 精简后的GT/GE条件
         * @param ltLeCondition 精简后的LT/LE条件
         * @param param         参数名
         * @throws UnexpectedException EQ值超出区间范围时抛出
         */
        private static void validateEqInRange(BigDecimal eqValue, Condition gtGeCondition, Condition ltLeCondition, String param) throws UnexpectedException {
            if (eqValue == null) {
                throw new UnexpectedException(String.format("param=%s的EQ值非数值类型，无法与区间条件校验", param));
            }

            // 校验是否在区间内（不在则抛异常）
            boolean isEqInRange = isValueInRange(eqValue, gtGeCondition, ltLeCondition);
            if (!isEqInRange) {
                throw new UnexpectedException(String.format(
                        "param=%s的EQ值=%s 超出区间条件范围[%s, %s]，条件矛盾",
                        param, eqValue,
                        gtGeCondition != null ? gtGeCondition.getCompare().name() + ":" + gtGeCondition.getCompareValue() : "无下限",
                        ltLeCondition != null ? ltLeCondition.getCompare().name() + ":" + ltLeCondition.getCompareValue() : "无上限"
                ));
            }
        }

        /**
         * 处理EQ条件：
         * 1. 多个EQ值不同 → 抛异常；
         * 2. 单个/多个相同EQ → 保留一个；
         *
         * @param eqConditions EQ条件列表
         * @param param        参数名
         * @return 处理后的EQ条件
         * @throws UnexpectedException 多个EQ值不同时抛出
         */
        private static List<Condition> processEqConditions(List<Condition> eqConditions, String param) throws UnexpectedException {
            if (eqConditions.isEmpty()) {
                return Collections.emptyList();
            }
            if (eqConditions.size() == 1) {
                return eqConditions;
            }

            // 校验所有EQ的compareValue是否相同
            Set<Object> eqValues = eqConditions.stream()
                    .map(Condition::getCompareValue)
                    .collect(Collectors.toSet());

            if (eqValues.size() > 1) {
                throw new UnexpectedException(String.format(
                        "param=%s存在多个EQ条件且compareValue不同，values=%s",
                        param, eqValues
                ));
            }

            // 所有EQ值相同，保留一个
            return Collections.singletonList(eqConditions.get(0));
        }

        /**
         * 精简GT/GE条件：保留最严格的下限（最大的数值）
         */
        private static Condition simplifyGtGeConditions(List<Condition> gtGeConditions) {
            if (gtGeConditions.isEmpty()) {
                return null;
            }
            if (gtGeConditions.size() == 1) {
                return gtGeConditions.get(0);
            }

            Map<BigDecimal, Condition> valueConditionMap = new HashMap<>();
            for (Condition condition : gtGeConditions) {
                BigDecimal value = getBigDecimalValue(condition.getCompareValue());
                if (value == null) {
                    return condition; // 非数值类型，无法合并
                }
                valueConditionMap.put(value, condition);
            }

            BigDecimal maxValue = Collections.max(valueConditionMap.keySet());
            List<Condition> sameMax = gtGeConditions.stream()
                    .filter(c -> getBigDecimalValue(c.getCompareValue()).equals(maxValue))
                    .collect(Collectors.toList());

            // 同值优先保留GE（更严格）
            return sameMax.stream()
                    .filter(c -> c.getCompare() == Condition.Compare.GE)
                    .findFirst()
                    .orElse(valueConditionMap.get(maxValue));
        }

        /**
         * 精简LT/LE条件：保留最严格的上限（最小的数值）
         */
        private static Condition simplifyLtLeConditions(List<Condition> ltLeConditions) {
            if (ltLeConditions.isEmpty()) {
                return null;
            }
            if (ltLeConditions.size() == 1) {
                return ltLeConditions.get(0);
            }

            Map<BigDecimal, Condition> valueConditionMap = new HashMap<>();
            for (Condition condition : ltLeConditions) {
                BigDecimal value = getBigDecimalValue(condition.getCompareValue());
                if (value == null) {
                    return condition; // 非数值类型，无法合并
                }
                valueConditionMap.put(value, condition);
            }

            BigDecimal minValue = Collections.min(valueConditionMap.keySet());
            List<Condition> sameMin = ltLeConditions.stream()
                    .filter(c -> getBigDecimalValue(c.getCompareValue()).equals(minValue))
                    .collect(Collectors.toList());

            // 同值优先保留LE（更严格）
            return sameMin.stream()
                    .filter(c -> c.getCompare() == Condition.Compare.LE)
                    .findFirst()
                    .orElse(valueConditionMap.get(minValue));
        }

        /**
         * 转换Object为BigDecimal（支持数值/字符串数值）
         */
        private static BigDecimal getBigDecimalValue(Object compareValue) {
            if (compareValue == null) {
                return null;
            }
            try {
                if (compareValue instanceof Number) {
                    return new BigDecimal(compareValue.toString());
                }
                return new BigDecimal(compareValue.toString().trim());
            } catch (NumberFormatException e) {
                return null; // 非数值类型
            }
        }

    }

}
