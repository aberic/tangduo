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

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;

/// 对象转换工具类
public final class Converts {

    private Converts() {
        throw new AssertionError("工具类禁止实例化");
    }

    /// 将对象转换为整数，默认值为指定值
    /// @param val 待转换的对象
    /// @param defaultValue 默认值
    /// @return 转换后的整数，若转换失败则返回默认值
    /// <p>
    /// 转换规则：
    /// 1. 如果对象为 null，则返回默认值。
    /// 2. 如果对象为 Number 类型，则直接调用 intValue() 方法返回整数。
    /// 3. 否则，尝试将对象转换为字符串并解析为整数。
    /// </p>
    public static int toInt(Object val, int defaultValue) {
        if (val == null) return defaultValue;
        try {
            if (val instanceof Number number) return number.intValue();
            return Integer.parseInt(val.toString().trim());
        } catch (Exception e) {
            return defaultValue;
        }
    }
    /// @param val 待转换的对象
    /// @return 转换后的整数，转换失败返回 null
    /// <p>
    /// 转换规则：如果对象为 null，则返回 null；如果对象为数字类型，则直接调用 intValue() 方法；否则，尝试将对象转换为整数。
    /// </p>
    /// <p>
    /// 注意：本方法仅支持将整数类型的对象转换为整数，不支持将其他类型的对象转换为整数。
    /// </p>
    public static Integer toInt(Object val) {
        if (val == null) return null;
        try {
            if (val instanceof Number number) return number.intValue();
            return Integer.parseInt(val.toString().trim());
        } catch (Exception e) {
            return null;
        }
    }

    /// @param val 待转换的对象
    /// @return 转换后的整数，转换失败返回 null
    /// <p>
    /// 转换规则：如果对象为 null，则返回 null；如果对象为数字类型，则直接调用 intValue() 方法；否则，尝试将对象转换为整数。
    /// </p>
    /// <p>
    /// 注意：本方法仅支持将整数类型的对象转换为整数，不支持将其他类型的对象转换为整数。
    /// </p>
    public static Integer toIntDefaultMaxValue(Object val) {
        return toInt(val, Integer.MAX_VALUE);
    }
    /// @param val 待转换的对象
    /// @return 转换后的整数，转换失败返回默认值
    /// <p>
    /// 转换规则：如果对象为 null，则返回默认值；如果对象为数字类型，则直接调用 intValue() 方法；否则，尝试将对象转换为整数。
    /// </p>
    /// <p>
    /// 注意：本方法仅支持将整数类型的对象转换为整数，不支持将其他类型的对象转换为整数。
    /// </p>
    public static int toIntDefaultMinValue(Object val) {
        return toInt(val, Integer.MIN_VALUE);
    }

    /// @param val 待转换的对象
    /// @param defaultValue 默认值
    /// @return 转换后的长整数，若转换失败则返回默认值
    /// <p>
    /// 转换规则：
    /// 1. 如果对象为 null，则返回默认值。
    /// 2. 如果对象为 Number 类型，则直接调用 longValue() 方法返回长整数。
    /// 3. 否则，尝试将对象转换为字符串并解析为长整数。
    /// </p>
    public static long toLong(Object val, long defaultValue) {
        if (val == null) return defaultValue;
        try {
            if (val instanceof Number number) return number.longValue();
            return Long.parseLong(val.toString().trim());
        } catch (Exception e) {
            return defaultValue;
        }
    }

    /// 将对象转换为长整数
    /// @param val 待转换的对象
    /// @return 转换后的长整数，若转换失败则返回 null
    /// <p>
    /// 转换规则：
    /// 1. 如果对象为 null，则返回 null。
    /// 2. 如果对象为 Number 类型，则直接调用 longValue() 方法返回长整数。
    /// 3. 否则，尝试将对象转换为字符串并解析为长整数。
    /// </p>
    public static Long toLong(Object val) {
        if (val == null) return null;
        try {
            if (val instanceof Number number) return number.longValue();
            return Long.parseLong(val.toString().trim());
        } catch (Exception e) {
            return null;
        }
    }

    /// 将对象转换为长整数，默认值为最大长整数
    /// @param val 待转换的对象
    /// @return 转换后的长整数，若转换失败则返回默认值
    /// <p>
    /// 转换规则：
    /// 1. 如果对象为 null，则返回默认值。
    /// 2. 如果对象为 Number 类型，则直接调用 longValue() 方法返回长整数。
    /// 3. 否则，尝试将对象转换为字符串并解析为长整数。
    /// </p>
    public static long toLongDefaultMaxValue(Object val) {
        return toLong(val, Long.MAX_VALUE);
    }

    /// 将对象转换为长整数，默认值为最小长整数
    /// @param val 待转换的对象
    /// @return 转换后的长整数，若转换失败则返回默认值
    /// <p>
    /// 转换规则：
    /// 1. 如果对象为 null，则返回默认值。
    /// 2. 如果对象为 Number 类型，则直接调用 longValue() 方法返回长整数。
    /// 3. 否则，尝试将对象转换为字符串并解析为长整数。
    /// </p>
    public static long toLongDefaultMinValue(Object val) {
        return toLong(val, Long.MIN_VALUE);
    }

    /// @param val 待转换的对象
    /// @param defaultValue 默认值
    /// @return 转换后的浮点数，若转换失败则返回默认值
    /// <p>
    /// 转换规则：
    /// 1. 如果对象为 null，则返回默认值。
    /// 2. 如果对象为 Number 类型，则直接调用 floatValue() 方法返回浮点数。
    /// 3. 否则，尝试将对象转换为字符串并解析为浮点数。
    /// </p>
    public static float toFloat(Object val, float defaultValue) {
        if (val == null) return defaultValue;
        try {
            if (val instanceof Number number) return number.floatValue();
            return Float.parseFloat(val.toString().trim());
        } catch (Exception e) {
            return defaultValue;
        }
    }

    /// 将对象转换为浮点数
    /// @param val 待转换的对象
    /// @return 转换后的浮点数，若转换失败则返回 null
    /// <p>
    /// 转换规则：
    /// 1. 如果对象为 null，则返回 null。
    /// 2. 如果对象为 Number 类型，则直接调用 floatValue() 方法返回浮点数。
    /// 3. 否则，尝试将对象转换为字符串并解析为浮点数。
    /// </p>
    public static Float toFloat(Object val) {
        if (val == null) return null;
        try {
            if (val instanceof Number number) return number.floatValue();
            return Float.parseFloat(val.toString().trim());
        } catch (Exception e) {
            return null;
        }
    }

    /// @param val 待转换的对象
    /// @param defaultValue 默认值
    /// @return 转换后的浮点数，若转换失败则返回默认值
    /// <p>
    /// 转换规则：
    /// 1. 如果对象为 null，则返回默认值。
    /// 2. 如果对象为 Number 类型，则直接调用 doubleValue() 方法返回浮点数。
    /// 3. 否则，尝试将对象转换为字符串并解析为浮点数。
    /// </p>
    public static double toDouble(Object val, double defaultValue) {
        if (val == null) return defaultValue;
        try {
            if (val instanceof Number number) return number.doubleValue();
            return Double.parseDouble(val.toString().trim());
        } catch (Exception e) {
            return defaultValue;
        }
    }

    /// 将对象转换为浮点数
    /// @param val 待转换的对象
    /// @return 转换后的浮点数，若转换失败则返回 null
    /// <p>
    /// 转换规则：
    /// 1. 如果对象为 null，则返回 null。
    /// 2. 如果对象为 Number 类型，则直接调用 doubleValue() 方法返回浮点数。
    /// 3. 否则，尝试将对象转换为字符串并解析为浮点数。
    /// </p>
    public static Double toDouble(Object val) {
        if (val == null) return null;
        try {
            if (val instanceof Number number) return number.doubleValue();
            return Double.parseDouble(val.toString().trim());
        } catch (Exception e) {
            return null;
        }
    }

    /// 将对象转换为布尔值
    /// @param val 待转换的对象
    /// @param defaultValue 默认值
    /// @return 转换后的布尔值，若转换失败则返回默认值
    /// <p>
    /// 转换规则：
    /// 1. 如果对象为 null，则返回默认值。
    /// 2. 如果对象为 String 类型，则将字符串转换为小写后判断是否为 "true" 或 "1"。
    /// 3. 否则，返回默认值。
    /// </p>
    public static boolean toBoolean(Object val, boolean defaultValue) {
        if (val == null) return defaultValue;
        try {
            String str = val.toString().trim().toLowerCase();
            return "true".equals(str) || "1".equals(str);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    /// 将对象转换为布尔值，默认值为 false
    /// @param val 待转换的对象
    /// @return 转换后的布尔值，若转换失败则返回 false
    public static boolean toBoolean(Object val) {
        return toBoolean(val, false);
    }

    /// 将对象转换为字符串，默认值为空字符串
    /// @param val 待转换的对象
    /// @return 转换后的字符串，若转换失败则返回默认值
    /// <p>
    /// 转换规则：
    /// 1. 如果对象为 null，则返回默认值。
    /// 2. 否则，返回对象的字符串表示。
    /// </p>
    public static String toString(Object val, String defaultValue) {
        if (val == null) return defaultValue;
        try {
            return val.toString().trim();
        } catch (Exception e) {
            return defaultValue;
        }
    }

    /// 将对象转换为字符串，默认值为空字符串
    /// @param val 待转换的对象
    /// @return 转换后的字符串，若转换失败则返回空字符串
    /// <p>
    /// 转换规则：
    /// 1. 如果对象为 null，则返回空字符串。
    /// 2. 否则，返回对象的字符串表示。
    /// </p>
    public static String toString(Object val) {
        return toString(val, "");
    }

    /// 将对象转换为 BigDecimal
    /// @param val 待转换的对象
    /// @param defaultValue 默认值
    /// @return 转换后的 BigDecimal，若转换失败则返回默认值
    /// <p>
    /// 转换规则：
    /// 1. 如果对象为 null，则返回默认值。
    /// 2. 如果对象为 BigDecimal 类型，则直接返回对象。
    /// 3. 如果对象为 Number 类型，则直接调用 doubleValue() 方法返回 BigDecimal。
    /// 4. 否则，尝试将对象转换为字符串并解析为 BigDecimal。
    /// </p>
    public static BigDecimal toBigDecimal(Object val, BigDecimal defaultValue) {
        if (val == null) return defaultValue;
        try {
            if (val instanceof BigDecimal bd) return bd;
            if (val instanceof Number number) return BigDecimal.valueOf(number.doubleValue());
            return new BigDecimal(val.toString().trim());
        } catch (Exception e) {
            return defaultValue;
        }
    }


    /// 将对象转换为 Date（兼容时间戳、标准字符串）
    /// @param val 待转换的对象
    /// @param defaultValue 默认值
    /// @return 转换后的 Date，若转换失败则返回默认值
    /// <p>
    /// 转换规则：
    /// 1. 如果对象为 null，则返回默认值。
    /// 2. 如果对象为 Date 类型，则直接返回对象。
    /// 3. 如果对象为 Long 类型，则直接调用 Date() 构造函数返回 Date。
    /// 4. 否则，尝试将对象转换为字符串并解析为 Date。
    /// </p>
    public static Date toDate(Object val, Date defaultValue) {
        if (val == null) return defaultValue;
        try {
            if (val instanceof Date date) return date;
            if (val instanceof Long lon) return new Date(lon);
            String str = val.toString().trim();
            return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(str);
        } catch (ParseException e) {
            return defaultValue;
        }
    }

    /// 将对象转换为 LocalDateTime（兼容时间戳、标准字符串）
    /// @param val 待转换的对象
    /// @param defaultValue 默认值
    /// @return 转换后的 LocalDateTime，若转换失败则返回默认值
    /// <p>
    /// 转换规则：
    /// 1. 如果对象为 null，则返回默认值。
    /// 2. 如果对象为 LocalDateTime 类型，则直接返回对象。
    /// 3. 如果对象为 Date 类型，则将 Date转换为 LocalDateTime。
    /// 4. 如果对象为 Long 类型，则将 Long转换为 LocalDateTime。
    /// 5. 否则，尝试将对象转换为字符串并解析为 LocalDateTime。
    /// </p>
    public static LocalDateTime toLocalDateTime(Object val, LocalDateTime defaultValue) {
        if (val == null) return defaultValue;
        try {
            switch (val) {
                case LocalDateTime ldt -> {
                    return ldt;
                }
                case Date date -> {
                    return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
                }
                case Long lon -> {
                    return new Date(lon).toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
                }
                default -> {
                }
            }
            String str = val.toString().trim();
            return LocalDateTime.parse(str, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        } catch (Exception e) {
            return defaultValue;
        }
    }

    /// 将对象转换为 LocalDate（兼容标准字符串）
    /// @param val 待转换的对象
    /// @param defaultValue 默认值
    /// @return 转换后的 LocalDate，若转换失败则返回默认值
    /// <p>
    /// 转换规则：
    /// 1. 如果对象为 null，则返回默认值。
    /// 2. 如果对象为 LocalDate 类型，则直接返回对象。
    /// 3. 否则，尝试将对象转换为字符串并解析为 LocalDate。
    /// </p>
    public static LocalDate toLocalDate(Object val, LocalDate defaultValue) {
        if (val == null) return defaultValue;
        try {
            if (val instanceof LocalDate ld) return ld;
            String str = val.toString().trim();
            return LocalDate.parse(str, DateTimeFormatter.ISO_LOCAL_DATE);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    /// 将对象转换为 LocalTime（兼容标准字符串）
    /// @param val 待转换的对象
    /// @param defaultValue 默认值
    /// @return 转换后的 LocalTime，若转换失败则返回默认值
    /// <p>
    /// 转换规则：
    /// 1. 如果对象为 null，则返回默认值。
    /// 2. 如果对象为 LocalTime 类型，则直接返回对象。
    /// 3. 否则，尝试将对象转换为字符串并解析为 LocalTime。
    /// </p>
    public static LocalTime toLocalTime(Object val, LocalTime defaultValue) {
        if (val == null) return defaultValue;
        try {
            if (val instanceof LocalTime lt) return lt;
            String str = val.toString().trim();
            return LocalTime.parse(str, DateTimeFormatter.ISO_LOCAL_TIME);
        } catch (Exception e) {
            return defaultValue;
        }
    }


}
