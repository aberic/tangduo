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

import java.util.Date;

@Data
public class LogEntity {

    private String id;

    private String traceId;       // 全链路ID

    private String level;          // DEBUG/INFO/WARN/ERROR

    private String message;        // 日志内容
    private String threadName;     // 线程名
    private String loggerName;     // 类全限定名
    private Long   timestamp;      // 时间戳

    private Date createTime;       // 时间

    private String exception;      // 异常堆栈
    private String serverIp;       // 机器IP

}
