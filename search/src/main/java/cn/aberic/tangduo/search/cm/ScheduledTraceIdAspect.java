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

package cn.aberic.tangduo.search.cm;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import java.util.UUID;

/// 定时任务 traceId 切面
@Aspect
@Component
public class ScheduledTraceIdAspect {
    /// traceId 键
    private static final String TRACE_ID = "traceId";

    // 拦截所有 @Scheduled 方法
    /// @param joinPoint 连接点
    /// @return 连接点
    /// @throws Throwable 异常
    @Pointcut("@annotation(org.springframework.scheduling.annotation.Scheduled)")
    public void scheduledPointcut() {}

    /// 环绕通知
    /// @param joinPoint 连接点
    /// @return 连接点
    /// @throws Throwable 异常
    @Around("scheduledPointcut()")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        try {
            // 生成 traceId
            String traceId = UUID.randomUUID().toString().replace("-", "");
            MDC.put(TRACE_ID, traceId);

            // 执行定时任务
            return joinPoint.proceed();
        } finally {
            // 必须清除
            MDC.clear();
        }
    }
}