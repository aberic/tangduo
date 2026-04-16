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

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

/// 日志耗时切面
@Slf4j
@Aspect
@Component
public class LogCostAspect {

    /// 控制器方法切点
    /// @param joinPoint 连接点
    /// @return 连接点
    /// @throws Throwable 异常
    @Pointcut("execution(* cn.aberic.tangduo.search.controller..*.*(..))")
    public void controllerPointcut() {}

    /// 环绕通知
    /// @param joinPoint 连接点
    /// @return 连接点
    /// @throws Throwable 异常
    @Around("controllerPointcut()")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        long start = System.currentTimeMillis();
        String traceId = MDC.get("traceId");

        Object proceed = joinPoint.proceed();

        long ms = System.currentTimeMillis() - start;
        long min = ms / 1000 / 60;
        long sec = (ms / 1000) % 60;
        long millis = ms % 1000;
        String cost = String.format("%02d.%02d.%03d", min, sec, millis);

        log.debug("[traceId:{}] {} 耗时: {}", traceId, joinPoint.getSignature().getName(), cost);
        return proceed;
    }

}