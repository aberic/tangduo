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

package cn.aberic.tangduo.common.file;

import cn.aberic.tangduo.common.Maths;
import org.junit.jupiter.api.Test;

public class MathsTests {

    @Test
    void divide() {
        double d1 = Maths.divide(10, 3);
        System.out.println(d1); // 3.33
        System.out.println((int) d1); // 3
        System.out.println(Math.divideExact(10, 3)); // 3
        System.out.println();
        double d2 = Maths.divide(9, 3);
        System.out.println(d2); // 3.0
        System.out.println((int) d2); // 3
        System.out.println(Math.divideExact(9, 3)); // 3
        System.out.println();
        double d3 = Maths.divide(3, 10);
        System.out.println(d3); // 0.3
        System.out.println((int) d3); // 0
        System.out.println(Math.divideExact(3, 10)); // 0

        System.out.println(9223372036854775806L/2); // 4611686018427387903   4611686018427387904
    }

}
