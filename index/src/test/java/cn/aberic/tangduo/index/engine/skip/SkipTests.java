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

package cn.aberic.tangduo.index.engine.skip;

import org.junit.jupiter.api.Test;

public class SkipTests {

    @Test
    void nodeKey() {
        int nodeLength = 117;
        long firstNodeKey = 0;
        long newNodeKey = 380008;
        int len = (int) (Math.log10(newNodeKey) + 1);
        System.out.println(len); // 6
        System.out.println(Math.pow(10, len - 1));
    }

    @Test
    void t() {
        System.out.println(Math.pow(2, 64));
        System.out.println(Math.divideExact(-9223372036854773509L, 281474976710656L));
        System.out.println(9223372036854775807L - 2998);
        System.out.println(9223372036854775807L - 9223372036854772809L);
        System.out.println(281474976710656L * 32767);
        System.out.println(9223090561878065152L - 9223372036854772809L);
        System.out.println(9223372036854772809L - 9223372036854775807L);
        System.out.println(Math.divideExact(-281474976707657L, 4294967296L));
        System.out.println(Math.divideExact(-429496795L, 4294967296L));
        System.out.println(Math.divideExact(-4294964297L, 65536));
    }

    @Test
    void mods() {
        System.out.println(26 % 27);
        System.out.println(28 % 27);
        System.out.println(54 % 27);
    }

}
