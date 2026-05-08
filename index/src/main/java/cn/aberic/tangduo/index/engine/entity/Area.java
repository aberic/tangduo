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

package cn.aberic.tangduo.index.engine.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/// 区域策略
@NoArgsConstructor
@AllArgsConstructor
@Getter
public class Area {

    /// 区域起始度，最小主键（-9223372036854775807 —— 9223372036854775808）
    long startDegree = Long.MIN_VALUE;
    /// 区域终止度，最大主键（-9223372036854775807 —— 9223372036854775808）
    long endDegree = Long.MAX_VALUE;
    /// 是否包含区域起始度
    boolean includeStart = false;
    /// 是否包含区域终止度
    boolean includeEnd = false;

}
