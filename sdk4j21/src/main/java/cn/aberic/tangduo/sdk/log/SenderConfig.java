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

package cn.aberic.tangduo.sdk.log;

import lombok.Data;

/// 日志发送配置类，用于配置日志发送的参数
@Data
public class SenderConfig {
    /// 日志服务器URL
    private String serverUrl;
    /// 应用名称
    private String appName;
    /// 应用密钥
    private String appKey;

}
