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

package cn.aberic.tangduo.db.rich;

import cn.aberic.tangduo.common.Converts;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Objects;

/// 条件查询，查询过程中不满足条件的记录将被移除出结果集，json数据类型支持条件查询
public class Condition {
    /// 条件的key，如：school.student.age
    String param;
    /// 比较方法
    Compare compare;
    /// 数据类型，默认字符串类型
    DataType dataType = DataType.STRING;
    /// 待比较数据
    Object compareValue;

    /// 传入数据是否符合条件
    public boolean conform(String value) {
        String cv = Converts.toString(compareValue);
        return switch (compare) {
            case EQ -> value.equals(cv);
            case GE -> value.compareToIgnoreCase(cv) >= 0; // <0：s1小  |  =0：相等  |  >0：s1大
            case GT -> value.compareToIgnoreCase(cv) > 0;
            case LE -> value.compareToIgnoreCase(cv) <= 0;
            case LT -> value.compareToIgnoreCase(cv) < 0;
            case NE -> !value.equals(cv);
        };
    }

    /// 传入数据是否符合条件
    public boolean conform(int value) {
        Integer cv = Converts.toInt(compareValue);
        if (Objects.isNull(cv)) {
            return false;
        }
        return switch (compare) {
            case EQ -> value == cv;
            case GE -> value >= cv;
            case GT -> value > cv;
            case LE -> value <= cv;
            case LT -> value < cv;
            case NE -> value != cv;
        };
    }

    /// 传入数据是否符合条件
    public boolean conform(long value) {
        Long cv = Converts.toLong(compareValue);
        if (Objects.isNull(cv)) {
            return false;
        }
        return switch (compare) {
            case EQ -> value == cv;
            case GE -> value >= cv;
            case GT -> value > cv;
            case LE -> value <= cv;
            case LT -> value < cv;
            case NE -> value != cv;
        };
    }

    /// 传入数据是否符合条件
    public boolean conform(float value) {
        Float cv = Converts.toFloat(compareValue);
        if (Objects.isNull(cv)) {
            return false;
        }
        return switch (compare) {
            case EQ -> value == cv;
            case GE -> value >= cv;
            case GT -> value > cv;
            case LE -> value <= cv;
            case LT -> value < cv;
            case NE -> value != cv;
        };
    }

    /// 传入数据是否符合条件
    public boolean conform(double value) {
        Double cv = Converts.toDouble(compareValue);
        if (Objects.isNull(cv)) {
            return false;
        }
        return switch (compare) {
            case EQ -> value == cv;
            case GE -> value >= cv;
            case GT -> value > cv;
            case LE -> value <= cv;
            case LT -> value < cv;
            case NE -> value != cv;
        };
    }

    /// 传入数据是否符合条件
    public boolean conform(BigDecimal value) {
        BigDecimal cv = Converts.toBigDecimal(compareValue, null);
        if (Objects.isNull(cv)) {
            return false;
        }
        return switch (compare) {
            case EQ -> Objects.equals(value, cv);
            case GE -> value.compareTo(cv) >= 0; // <0：s1小  |  =0：相等  |  >0：s1大
            case GT -> value.compareTo(cv) > 0;
            case LE -> value.compareTo(cv) <= 0;
            case LT -> value.compareTo(cv) < 0;
            case NE -> !Objects.equals(value, cv);
        };
    }

    /// 传入数据是否符合条件
    public boolean conform(LocalDate value) {
        LocalDate cv = Converts.toLocalDate(compareValue, null);
        if (Objects.isNull(cv)) {
            return false;
        }
        return switch (compare) {
            case EQ -> value == cv;
            case GE -> !value.isBefore(cv);
            case GT -> value.isAfter(cv);
            case LE -> !value.isAfter(cv);
            case LT -> value.isBefore(cv);
            case NE -> value != cv;
        };
    }

    /// 传入数据是否符合条件
    public boolean conform(LocalDateTime value) {
        LocalDateTime cv = Converts.toLocalDateTime(compareValue, null);
        if (Objects.isNull(cv)) {
            return false;
        }
        return switch (compare) {
            case EQ -> value == cv;
            case GE -> !value.isBefore(cv);
            case GT -> value.isAfter(cv);
            case LE -> !value.isAfter(cv);
            case LT -> value.isBefore(cv);
            case NE -> value != cv;
        };
    }

    /// 传入数据是否符合条件
    public boolean conform(LocalTime value) {
        LocalTime cv = Converts.toLocalTime(compareValue, null);
        if (Objects.isNull(cv)) {
            return false;
        }
        return switch (compare) {
            case EQ -> value == cv;
            case GE -> !value.isBefore(cv);
            case GT -> value.isAfter(cv);
            case LE -> !value.isAfter(cv);
            case LT -> value.isBefore(cv);
            case NE -> value != cv;
        };
    }

    /// 传入数据是否符合条件
    public boolean conform(Object value) {
        return switch (dataType) {
            case STRING -> {
                String s1 = String.valueOf(value);
                String s2 = String.valueOf(compareValue);
                yield switch (compare) {
                    case EQ -> s1.equals(s2);
                    case GE -> s1.compareToIgnoreCase(s2) >= 0; // <0：s1小  |  =0：相等  |  >0：s1大
                    case GT -> s1.compareToIgnoreCase(s2) > 0;
                    case LE -> s1.compareToIgnoreCase(s2) <= 0;
                    case LT -> s1.compareToIgnoreCase(s2) < 0;
                    case NE -> !s1.equals(s2);
                };
            }
            case INT -> {
                int s1 = (int) value;
                int s2 = (int) compareValue;
                yield switch (compare) {
                    case EQ -> s1 == s2;
                    case GE -> s1 >= s2;
                    case GT -> s1 > s2;
                    case LE -> s1 <= s2;
                    case LT -> s1 < s2;
                    case NE -> s1 != s2;
                };
            }
            case LONG -> {
                long s1 = (long) value;
                long s2 = (long) compareValue;
                yield switch (compare) {
                    case EQ -> s1 == s2;
                    case GE -> s1 >= s2;
                    case GT -> s1 > s2;
                    case LE -> s1 <= s2;
                    case LT -> s1 < s2;
                    case NE -> s1 != s2;
                };
            }
            case FLOAT -> {
                float s1 = (float) value;
                float s2 = (float) compareValue;
                yield switch (compare) {
                    case EQ -> s1 == s2;
                    case GE -> s1 >= s2;
                    case GT -> s1 > s2;
                    case LE -> s1 <= s2;
                    case LT -> s1 < s2;
                    case NE -> s1 != s2;
                };
            }
            case DOUBLE -> {
                double s1 = (double) value;
                double s2 = (double) compareValue;
                yield switch (compare) {
                    case EQ -> s1 == s2;
                    case GE -> s1 >= s2;
                    case GT -> s1 > s2;
                    case LE -> s1 <= s2;
                    case LT -> s1 < s2;
                    case NE -> s1 != s2;
                };
            }
            case BOOLEAN -> {
                boolean s1 = (boolean) value;
                boolean s2 = (boolean) compareValue;
                if (compare == Compare.EQ) {
                    yield s1 == s2;
                } else {
                    yield s1 != s2;
                }
            }
            case DATE -> {
                LocalDate s1 = (LocalDate) value;
                LocalDate s2 = (LocalDate) compareValue;
                yield switch (compare) {
                    case EQ -> s1 == s2;
                    case GE -> !s1.isBefore(s2);
                    case GT -> s1.isAfter(s2);
                    case LE -> !s1.isAfter(s2);
                    case LT -> s1.isBefore(s2);
                    case NE -> s1 != s2;
                };
            }
            case DATETIME -> {
                LocalDateTime s1 = (LocalDateTime) value;
                LocalDateTime s2 = (LocalDateTime) compareValue;
                yield switch (compare) {
                    case EQ -> s1 == s2;
                    case GE -> !s1.isBefore(s2);
                    case GT -> s1.isAfter(s2);
                    case LE -> !s1.isAfter(s2);
                    case LT -> s1.isBefore(s2);
                    case NE -> s1 != s2;
                };
            }
            case TIME -> {
                LocalTime s1 = (LocalTime) value;
                LocalTime s2 = (LocalTime) compareValue;
                yield switch (compare) {
                    case EQ -> s1 == s2;
                    case GE -> !s1.isBefore(s2); // <0：s1小  |  =0：相等  |  >0：s1大
                    case GT -> s1.isAfter(s2);
                    case LE -> !s1.isAfter(s2);
                    case LT -> s1.isBefore(s2);
                    case NE -> s1 != s2;
                };
            }
            case BIG_DECIMAL -> {
                BigDecimal s1 = (BigDecimal) value;
                BigDecimal s2 = (BigDecimal) compareValue;
                yield switch (compare) {
                    case EQ -> Objects.equals(s1, s2);
                    case GE -> s1.compareTo(s2) >= 0; // <0：s1小  |  =0：相等  |  >0：s1大
                    case GT -> s1.compareTo(s2) > 0;
                    case LE -> s1.compareTo(s2) <= 0;
                    case LT -> s1.compareTo(s2) < 0;
                    case NE -> !Objects.equals(s1, s2);
                };
            }
        };
    }

    /// 比较方法
    public enum Compare {
        /// 大于
        GT,
        /// 大于等于
        GE,
        /// 小于
        LT,
        /// 小于等于
        LE,
        /// 等于
        EQ,
        /// 不等
        NE,
    }

    /// 数据类型
    public enum DataType {
        INT,
        LONG,
        FLOAT,
        DOUBLE,
        STRING,
        BIG_DECIMAL,
        BOOLEAN,
        DATE,
        TIME,
        DATETIME
    }
}
