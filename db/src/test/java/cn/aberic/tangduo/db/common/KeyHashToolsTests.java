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

public class KeyHashToolsTests {

    @Test
    void keyHash() {
        System.out.println(KeyHashTools.toLongKey("我在SpringBoot中使用IK分词器做中英文分词"));
        System.out.println(KeyHashTools.toLongKey("1"));
        System.out.println(KeyHashTools.toLongKey("100000"));
        System.out.println(KeyHashTools.toLongKey("9223372036854775807"));
        System.out.println(KeyHashTools.toLongKey("-64424581328"));
    }

}
