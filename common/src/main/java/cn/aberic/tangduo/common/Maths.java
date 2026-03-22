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
import java.math.RoundingMode;

public class Maths {

    private Maths() {
        throw new IllegalStateException("Maths class");
    }

    /**
     * 进行整数除法并得到浮点数结果，同时保留两位小数
     *
     * @param dividend 被除数
     * @param divisor  除数
     *
     * @return 保留两位小数的结果
     */
    public static double divide(long dividend, long divisor) {
        // 创建BigDecimal对象
        return new BigDecimal(dividend).divide(new BigDecimal(divisor), 2, RoundingMode.HALF_UP).doubleValue();
    }

}
