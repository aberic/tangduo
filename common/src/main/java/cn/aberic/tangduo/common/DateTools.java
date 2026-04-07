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

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.Date;

public class DateTools {

    private DateTools() {
        throw new IllegalStateException("Dates class");
    }

    /**
     * LocalDate 转 Date，默认时区偏移量+8
     *
     * @param localDate LocalDate
     *
     * @return Date
     */
    public static Date localDate2date8hours(LocalDate localDate) {
        return Date.from(localDate.atStartOfDay().toInstant(ZoneOffset.ofHours(8)));
    }

    /**
     * LocalDate 转 Date
     *
     * @param localDate LocalDate
     * @param hours     时区偏移量（以小时为单位），范围从 -18 到 +18，
     *
     * @return Date
     */
    public static Date localDate2date(LocalDate localDate, int hours) {
        return Date.from(localDate.atStartOfDay().toInstant(ZoneOffset.ofHours(hours)));
    }

    /**
     * LocalDate 转 Date，默认时区偏移量+8
     *
     * @param localDate LocalDate
     *
     * @return Date
     */
    public static Date localDate2date(LocalDate localDate) {
        return localDate2date(localDate, ZoneOffset.ofHours(8));
    }

    /**
     * LocalDate 转 Date
     *
     * @param localDate  LocalDate
     * @param zoneOffset 用于转换的偏移量，不能为空。如 ZoneOffset.UTC
     *
     * @return Date
     */
    public static Date localDate2date(LocalDate localDate, ZoneOffset zoneOffset) {
        return Date.from(localDate.atStartOfDay().toInstant(zoneOffset));
    }

    /**
     * LocalDateTime 转 Date，默认时区偏移量+8
     *
     * @param localDateTime LocalDateTime
     *
     * @return Date
     */
    public static Date localDateTime2date8hours(LocalDateTime localDateTime) {
        return localDateTime2date(localDateTime, 8);
    }

    /**
     * LocalDateTime 转 Date
     *
     * @param localDateTime LocalDateTime
     * @param hours         时区偏移量（以小时为单位），范围从 -18 到 +18，
     *
     * @return Date
     */
    public static Date localDateTime2date(LocalDateTime localDateTime, int hours) {
        return Date.from(localDateTime.toInstant(ZoneOffset.ofHours(hours)));
    }

    /**
     * LocalDateTime 转 Date，默认时区偏移量+8
     *
     * @param localDateTime LocalDateTime
     *
     * @return Date
     */
    public static Date localDateTime2date(LocalDateTime localDateTime) {
        return localDateTime2date(localDateTime, ZoneOffset.ofHours(8));
    }

    /**
     * LocalDateTime 转 Date
     *
     * @param localDateTime LocalDateTime
     * @param zoneOffset    用于转换的偏移量，不能为空。如 ZoneOffset.UTC
     *
     * @return Date
     */
    public static Date localDateTime2date(LocalDateTime localDateTime, ZoneOffset zoneOffset) {
        return Date.from(localDateTime.toInstant(zoneOffset));
    }

    /**
     * Date 转 LocalDate，默认时区偏移量+8
     *
     * @param date Date
     *
     * @return LocalDate
     */
    public static LocalDate date2localDate(Date date) {
        return date2localDate(date, "UTC", ZoneOffset.ofHours(8));
    }

    /**
     * Date 转 LocalDate
     *
     * @param date   Date
     * @param prefix 如果前缀为“GMT”、“UTC”或“UT”，则会返回带有该前缀和非零偏移量的时区标识符。如果前缀为空字符串“”，则返回时区偏移量。
     * @param offset 与格林威治/协调世界时的时差，例如 +02:00。使用如：ZoneOffset.ofHours(hours) 或 ZoneOffset.UTC
     *
     * @return LocalDate
     */
    public static LocalDate date2localDate(Date date, String prefix, ZoneOffset offset) {
        return date.toInstant().atZone(ZoneId.ofOffset(prefix, offset)).toLocalDate();
    }

    /**
     * Date 转 LocalDate，默认时区偏移量+8
     *
     * @param date Date
     *
     * @return LocalDate
     */
    public static LocalDate date2localDate8hours(Date date) {
        return date.toInstant().atZone(ZoneOffset.ofHours(8)).toLocalDate();
    }

    /**
     * Date 转 LocalDate
     *
     * @param date  Date
     * @param hours 时区偏移量（以小时为单位），范围从 -18 到 +18，
     *
     * @return LocalDate
     */
    public static LocalDate date2localDate(Date date, int hours) {
        return date.toInstant().atZone(ZoneOffset.ofHours(hours)).toLocalDate();
    }

    /**
     * 时间戳转 LocalDate，默认时区偏移量+8
     *
     * @param timestamp 时间戳
     *
     * @return LocalDate
     */
    public static LocalDate timestamp2localDate8hours(long timestamp) {
        return timestamp2localDate(timestamp, 8);
    }

    /**
     * 时间戳转 LocalDate
     *
     * @param timestamp 时间戳
     * @param hours     时区偏移量（以小时为单位），范围从 -18 到 +18，
     *
     * @return LocalDate
     */
    public static LocalDate timestamp2localDate(long timestamp, int hours) {
        return Instant.ofEpochMilli(timestamp).atZone(ZoneOffset.ofHours(hours)).toLocalDate();
    }

    /**
     * 时间戳转 LocalDate，默认时区偏移量+8
     *
     * @param timestamp 时间戳
     *
     * @return LocalDate
     */
    public static LocalDate timestamp2localDate(long timestamp) {
        return Instant.ofEpochMilli(timestamp).atZone(ZoneId.ofOffset("UTC", ZoneOffset.ofHours(8))).toLocalDate();
    }

    /**
     * 时间戳转 LocalDate
     *
     * @param timestamp 时间戳
     * @param prefix    如果前缀为“GMT”、“UTC”或“UT”，则会返回带有该前缀和非零偏移量的时区标识符。如果前缀为空字符串“”，则返回时区偏移量。
     * @param offset    与格林威治/协调世界时的时差，例如 +02:00。使用如：ZoneOffset.ofHours(hours) 或 ZoneOffset.UTC
     *
     * @return LocalDate
     */
    public static LocalDate timestamp2localDate(long timestamp, String prefix, ZoneOffset offset) {
        return Instant.ofEpochMilli(timestamp).atZone(ZoneId.ofOffset(prefix, offset)).toLocalDate();
    }

    /**
     * Date 转 LocalDateTime，默认时区偏移量+8
     *
     * @param date Date
     *
     * @return LocalDateTime
     */
    public static LocalDateTime date2localDateTime8hours(Date date) {
        return date.toInstant().atZone(ZoneOffset.ofHours(8)).toLocalDateTime();
    }

    /**
     * Date 转 LocalDateTime
     *
     * @param date  Date
     * @param hours 时区偏移量（以小时为单位），范围从 -18 到 +18，
     *
     * @return LocalDateTime
     */
    public static LocalDateTime date2localDateTime(Date date, int hours) {
        return date.toInstant().atZone(ZoneOffset.ofHours(hours)).toLocalDateTime();
    }

    /**
     * Date 转 LocalDateTime，默认时区偏移量+8
     *
     * @param date Date
     *
     * @return LocalDateTime
     */
    public static LocalDateTime date2localDateTime(Date date) {
        return date2localDateTime(date, "UTC", ZoneOffset.ofHours(8));
    }

    /**
     * Date 转 LocalDateTime
     *
     * @param date   Date
     * @param prefix 如果前缀为“GMT”、“UTC”或“UT”，则会返回带有该前缀和非零偏移量的时区标识符。如果前缀为空字符串“”，则返回时区偏移量。
     * @param offset 与格林威治/协调世界时的时差，例如 +02:00。使用如：ZoneOffset.ofHours(hours) 或 ZoneOffset.UTC
     *
     * @return LocalDateTime
     */
    public static LocalDateTime date2localDateTime(Date date, String prefix, ZoneOffset offset) {
        return date.toInstant().atZone(ZoneId.ofOffset(prefix, offset)).toLocalDateTime();
    }

    /**
     * 时间戳转 LocalDateTime，默认时区偏移量+8
     *
     * @param timestamp 时间戳
     *
     * @return LocalDateTime
     */
    public static LocalDateTime timestamp2localDateTime8hours(long timestamp) {
        return timestamp2localDateTime(timestamp, 8);
    }

    /**
     * 时间戳转 LocalDateTime
     *
     * @param timestamp 时间戳
     * @param hours     时区偏移量（以小时为单位），范围从 -18 到 +18，
     *
     * @return LocalDateTime
     */
    public static LocalDateTime timestamp2localDateTime(long timestamp, int hours) {
        return Instant.ofEpochMilli(timestamp).atZone(ZoneOffset.ofHours(hours)).toLocalDateTime();
    }

    /**
     * 时间戳转 LocalDateTime，默认时区偏移量+8
     *
     * @param timestamp 时间戳
     *
     * @return LocalDateTime
     */
    public static LocalDateTime timestamp2localDateTime(long timestamp) {
        return Instant.ofEpochMilli(timestamp).atZone(ZoneId.ofOffset("UTC", ZoneOffset.ofHours(8))).toLocalDateTime();
    }

    /**
     * 时间戳转 LocalDateTime
     *
     * @param timestamp 时间戳
     * @param prefix    如果前缀为“GMT”、“UTC”或“UT”，则会返回带有该前缀和非零偏移量的时区标识符。如果前缀为空字符串“”，则返回时区偏移量。
     * @param offset    与格林威治/协调世界时的时差，例如 +02:00。使用如：ZoneOffset.ofHours(hours) 或 ZoneOffset.UTC
     *
     * @return LocalDateTime
     */
    public static LocalDateTime timestamp2localDateTime(long timestamp, String prefix, ZoneOffset offset) {
        return Instant.ofEpochMilli(timestamp).atZone(ZoneId.ofOffset(prefix, offset)).toLocalDateTime();
    }

    /**
     * LocalDate 转 Timestamp，默认时区偏移量+8
     *
     * @param localDate LocalDate
     *
     * @return Date
     */
    public static long localDate2timestamp8hours(LocalDate localDate) {
        return localDate2timestamp(localDate, 8);
    }

    /**
     * LocalDate 转 Timestamp
     *
     * @param localDate LocalDate
     * @param hours     时区偏移量（以小时为单位），范围从 -18 到 +18，
     *
     * @return Date
     */
    public static long localDate2timestamp(LocalDate localDate, int hours) {
        return localDate.atStartOfDay(ZoneOffset.ofHours(hours)).toInstant().toEpochMilli();
    }

    /**
     * LocalDate 转 Timestamp，默认时区偏移量+8
     *
     * @param localDate LocalDate
     *
     * @return Date
     */
    public static long localDate2timestamp(LocalDate localDate) {
        return localDate2timestamp(localDate, "UTC", ZoneOffset.ofHours(8));
    }

    /**
     * LocalDate 转 Timestamp
     *
     * @param localDate LocalDate
     * @param prefix    如果前缀为“GMT”、“UTC”或“UT”，则会返回带有该前缀和非零偏移量的时区标识符。如果前缀为空字符串“”，则返回时区偏移量。
     * @param offset    与格林威治/协调世界时的时差，例如 +02:00。使用如：ZoneOffset.ofHours(hours) 或 ZoneOffset.UTC
     *
     * @return Date
     */
    public static long localDate2timestamp(LocalDate localDate, String prefix, ZoneOffset offset) {
        return localDate.atStartOfDay(ZoneId.ofOffset(prefix, offset)).toInstant().toEpochMilli();
    }

    /**
     * LocalDateTime 转 Timestamp，默认时区偏移量+8
     *
     * @param localDateTime LocalDateTime
     *
     * @return Date
     */
    public static long localDateTime2timestamp8hours(LocalDateTime localDateTime) {
        return localDateTime2timestamp(localDateTime, 8);
    }

    /**
     * LocalDateTime 转 Timestamp
     *
     * @param localDateTime LocalDateTime
     * @param hours         时区偏移量（以小时为单位），范围从 -18 到 +18，
     *
     * @return Date
     */
    public static long localDateTime2timestamp(LocalDateTime localDateTime, int hours) {
        return localDateTime.toInstant(ZoneOffset.ofHours(hours)).toEpochMilli();
    }

    /**
     * LocalDateTime 转 Timestamp，默认时区偏移量+8
     *
     * @param localDateTime LocalDateTime
     *
     * @return Date
     */
    public static long localDateTime2timestamp(LocalDateTime localDateTime) {
        return localDateTime2timestamp(localDateTime, "UTC", ZoneOffset.ofHours(8));
    }

    /**
     * LocalDateTime 转 Timestamp
     *
     * @param localDateTime LocalDateTime
     * @param prefix        如果前缀为“GMT”、“UTC”或“UT”，则会返回带有该前缀和非零偏移量的时区标识符。如果前缀为空字符串“”，则返回时区偏移量。
     * @param offset        与格林威治/协调世界时的时差，例如 +02:00。使用如：ZoneOffset.ofHours(hours) 或 ZoneOffset.UTC
     *
     * @return Date
     */
    public static long localDateTime2timestamp(LocalDateTime localDateTime, String prefix, ZoneOffset offset) {
        return localDateTime.atZone(ZoneId.ofOffset(prefix, offset)).toInstant().toEpochMilli();
    }

    /**
     * 将 LocalDate 格式化成指定 format 格式的字符串
     *
     * @param localDate LocalDate
     * @param format    yyyy年MM月dd日 HH时mm分ss秒 | yyyy-MM-dd HH:mm:ss |  yyyy-MM-dd HH:mm:ss.SSS 等
     *
     * @return 格式化后的字符串
     */
    public static String format(LocalDate localDate, String format) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(format);
        return localDate.format(formatter);
    }

    /**
     * 根据指定的 format 时间格式，将时间字符串转为 LocalDate
     *
     * @param dateStr 时间字符串，如"2026-02-28 16:27:39"
     * @param format  yyyy年MM月dd日 HH时mm分ss秒 | yyyy-MM-dd HH:mm:ss |  yyyy-MM-dd HH:mm:ss.SSS 等，必须与时间字符串格式一致
     *
     * @return LocalDate
     */
    public static LocalDate unformatLocalDate(String dateStr, String format) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(format);
        return LocalDate.parse(dateStr, formatter);
    }

    /**
     * 将 LocalDateTime 格式化成指定 format 格式的字符串
     *
     * @param localDateTime LocalDateTime
     * @param format        yyyy年MM月dd日 HH时mm分ss秒 | yyyy-MM-dd HH:mm:ss |  yyyy-MM-dd HH:mm:ss.SSS 等
     *
     * @return 格式化后的字符串
     */
    public static String format(LocalDateTime localDateTime, String format) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(format);
        return localDateTime.format(formatter);
    }

    /**
     * 根据指定的 format 时间格式，将时间字符串转为 LocalDate
     *
     * @param dateStr 时间字符串，如"2026-02-28 16:27:39"
     * @param format  yyyy年MM月dd日 HH时mm分ss秒 | yyyy-MM-dd HH:mm:ss |  yyyy-MM-dd HH:mm:ss.SSS 等，必须与时间字符串格式一致
     *
     * @return LocalDateTime
     */
    public static LocalDateTime unformatLocalDateTime(String dateStr, String format) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(format);
        return LocalDateTime.parse(dateStr, formatter);
    }

}
