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

import java.util.Date;

import lombok.Data;

/// 日志实体类，用于存储日志事件的详细信息
@Data
public class LogEntity {
    /// 日志ID
    private String id;
    /// 全链路ID
    private String traceId;
    /// 日志级别
    /// @see DEBUG
    /// @see INFO
    /// @see WARN
    /// @see ERROR
    private String level;
    /// 日志内容
    private String message;
    /// 线程名
    private String threadName;
    /// 类全限定名
    private String loggerName;
    /// 时间戳 单位：毫秒
    private Long timestamp;
    /// 创建时间
    private Date createTime;
    /// 异常堆栈
    private String exception;
    /// 机器IP
    private String serverIp;

}
