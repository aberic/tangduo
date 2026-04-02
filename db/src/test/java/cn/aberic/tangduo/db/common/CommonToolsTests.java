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

import org.junit.jupiter.api.Test;

public class CommonToolsTests {

    @Test
    void isSingle() {
        System.out.println(CommonTools.isSingleLetter("我在SpringBoot中使用IK分词器做中英文分词")); // false
        System.out.println(CommonTools.isSingleLetter("Spring Boot"));  // false
        System.out.println(CommonTools.isSingleLetter("Spring"));  // false
        System.out.println(CommonTools.isSingleLetter("S"));  // true
        System.out.println(CommonTools.isSingleLetter("a"));  // true
        System.out.println(CommonTools.isSingleLetter("我"));  // false
        System.out.println(CommonTools.isSingleChinese("我在SpringBoot中使用IK分词器做中英文分词"));  // false
        System.out.println(CommonTools.isSingleChinese("我"));  // true
    }

}
