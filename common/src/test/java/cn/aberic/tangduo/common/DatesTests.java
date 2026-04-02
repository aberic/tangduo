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

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Date;

public class DatesTests {

    static String dataStr = "2026-02-28 16:27:39.666";
    static String format = "yyyy-MM-dd HH:mm:ss.SSS";

    @Test
    void localData() {
        LocalDate localDate = Dates.unformatLocalDate(dataStr, format);
        Date date = Dates.localDate2date(localDate);
        System.out.println(localDate);
        System.out.println(date);
        System.out.println(Dates.localDate2date(localDate, ZoneOffset.ofHours(8)));
        System.out.println(Dates.localDate2date8hours(localDate));
        System.out.println(Dates.localDate2date(localDate, 8));
        long timestamp = date.getTime();
        System.out.println(timestamp);
        System.out.println(Dates.localDate2timestamp(localDate));
        System.out.println(Dates.timestamp2localDate(timestamp));
    }

    @Test
    void localDataTime() {
        LocalDateTime localDateTime = Dates.unformatLocalDateTime(dataStr, format);
        Date date = Dates.localDateTime2date(localDateTime);
        System.out.println(localDateTime);
        System.out.println(date);
        System.out.println(Dates.localDateTime2date(localDateTime, ZoneOffset.ofHours(8)));
        System.out.println(Dates.localDateTime2date8hours(localDateTime));
        System.out.println(Dates.localDateTime2date(localDateTime, 8));
        long timestamp = date.getTime();
        System.out.println(timestamp);
        System.out.println(Dates.localDateTime2timestamp(localDateTime));
        System.out.println(Dates.timestamp2localDateTime(timestamp));


        System.out.println(localDateTime.toLocalTime());
    }

}
