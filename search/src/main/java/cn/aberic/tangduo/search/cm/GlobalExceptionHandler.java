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

import cn.aberic.tangduo.common.http.Response;
import org.slf4j.MDC;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import static cn.aberic.tangduo.common.http.Response.failed;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(Exception.class)
    public Response handle(Exception e) {
        String traceId = MDC.get("traceId");
        Throwable root = getRootCause(e);

        if (root instanceof java.io.IOException) {
            return failed(Response.Status.Exception, "IO异常: " + root.getMessage() + " traceId:" + traceId);
        } else if (root instanceof NoSuchFieldException) {
            return failed(Response.Status.Exception, "字段不存在: " + root.getMessage() + " traceId:" + traceId);
        } else {
            return failed(Response.Status.Exception, "系统异常: " + root.getMessage() + " traceId:" + traceId);
        }
    }

    private Throwable getRootCause(Throwable e) {
        while (e.getCause() != null && e.getCause() != e) {
            e = e.getCause();
        }
        return e;
    }
}
