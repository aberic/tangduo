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

public class Converts {

    private Converts() {
        throw new IllegalStateException("Converts class");
    }

    // ==================== int ====================
    public static int toInt(Object val, int defaultValue) {
        if (val == null) return defaultValue;
        try {
            if (val instanceof Number number) return number.intValue();
            return Integer.parseInt(val.toString().trim());
        } catch (Exception e) {
            return defaultValue;
        }
    }
    public static Integer toInt(Object val) {
        if (val == null) return null;
        try {
            if (val instanceof Number number) return number.intValue();
            return Integer.parseInt(val.toString().trim());
        } catch (Exception e) {
            return null;
        }
    }

    public static int toIntDefaultMaxValue(Object val) {
        return toInt(val, Integer.MAX_VALUE);
    }

    public static int toIntDefaultMinValue(Object val) {
        return toInt(val, Integer.MIN_VALUE);
    }

    // ==================== long ====================
    public static long toLong(Object val, long defaultValue) {
        if (val == null) return defaultValue;
        try {
            if (val instanceof Number number) return number.longValue();
            return Long.parseLong(val.toString().trim());
        } catch (Exception e) {
            return defaultValue;
        }
    }
    public static Long toLong(Object val) {
        if (val == null) return null;
        try {
            if (val instanceof Number number) return number.longValue();
            return Long.parseLong(val.toString().trim());
        } catch (Exception e) {
            return null;
        }
    }

    public static long toLongDefaultMaxValue(Object val) {
        return toLong(val, Long.MAX_VALUE);
    }

    public static long toLongDefaultMinValue(Object val) {
        return toLong(val, Long.MIN_VALUE);
    }

    // ==================== float ====================
    public static float toFloat(Object val, float defaultValue) {
        if (val == null) return defaultValue;
        try {
            if (val instanceof Number number) return number.floatValue();
            return Float.parseFloat(val.toString().trim());
        } catch (Exception e) {
            return defaultValue;
        }
    }

    public static Float toFloat(Object val) {
        if (val == null) return null;
        try {
            if (val instanceof Number number) return number.floatValue();
            return Float.parseFloat(val.toString().trim());
        } catch (Exception e) {
            return null;
        }
    }

    // ==================== double ====================
    public static double toDouble(Object val, double defaultValue) {
        if (val == null) return defaultValue;
        try {
            if (val instanceof Number number) return number.doubleValue();
            return Double.parseDouble(val.toString().trim());
        } catch (Exception e) {
            return defaultValue;
        }
    }

    public static Double toDouble(Object val) {
        if (val == null) return null;
        try {
            if (val instanceof Number number) return number.doubleValue();
            return Double.parseDouble(val.toString().trim());
        } catch (Exception e) {
            return null;
        }
    }

    // ==================== boolean ====================
    public static boolean toBoolean(Object val, boolean defaultValue) {
        if (val == null) return defaultValue;
        try {
            String str = val.toString().trim().toLowerCase();
            return "true".equals(str) || "1".equals(str);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    public static boolean toBoolean(Object val) {
        return toBoolean(val, false);
    }

    // ==================== String ====================
    public static String toString(Object val, String defaultValue) {
        if (val == null) return defaultValue;
        try {
            return val.toString().trim();
        } catch (Exception e) {
            return defaultValue;
        }
    }

    public static String toString(Object val) {
        return toString(val, "");
    }

    // ==================== BigDecimal ====================
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

    // ==================== Date & java.time ====================

    // 转 Date（兼容时间戳、标准字符串）
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

    // LocalDateTime
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

    // LocalDate
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

    // LocalTime
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
