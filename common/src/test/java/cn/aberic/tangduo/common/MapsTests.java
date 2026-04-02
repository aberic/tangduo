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

import java.util.HashMap;
import java.util.Map;

public class MapsTests {

    @Test
    void demo() {
        Map<String, Object> map = new HashMap<>();
        map.put("a", "xyz");
        map.put("b", 123);
        map.put("c", 3.14);
        map.put("d", true);
        map.put("e", 0x09);
        map.put("f", new UserDemo("tangduo", 9));

        String str = Maps.map2string(map);
        System.out.println(str);

        Map<String, Object> map1 = Maps.string2map(str);
        System.out.println(map1);

        String str1 = Maps.map2string(map1);
        System.out.println(str1);
    }

    static class UserDemo {
        String name;
        int age;

        public UserDemo(String name, int age) {
            this.name = name;
            this.age = age;
        }
    }

}
