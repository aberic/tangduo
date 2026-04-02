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

import java.util.List;

public class ListsTests {

    @Test
    void demo() {
        List<String> list = List.of("我", "喜欢", "Java");
        System.out.println(list);
        String s = Lists.toString(list);
        System.out.println(s);
        List<String> listFromString = Lists.fromString(s);
        System.out.println(listFromString);
    }

}
