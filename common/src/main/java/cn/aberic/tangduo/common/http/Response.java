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

package cn.aberic.tangduo.common.http;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Response {

    private int code;
    private Object data;
    @JsonProperty("error")
    private String errorMessage;

    public Response(int code, Object data, String errorMessage) {
        this.code = code;
        this.data = data;
        this.errorMessage = errorMessage;
    }

    public static Response success() {
        return new Response(Status.Success.code, null, null);
    }

    public static Response success(Object data) {
        return new Response(Status.Success.code, data, null);
    }

    public static Response failed(Status status, String errorMessage) {
        return new Response(status.code, null, errorMessage);
    }

    public static Response failed(Exception e) {
        return failed(Status.Exception, e.getMessage());
    }

    // 递归获取根异常
    private static Throwable getRootCause(Throwable e) {
        while (e.getCause() != null && e.getCause() != e) {
            e = e.getCause();
        }
        return e;
    }

    public enum Status {
        Success(200, "请求成功！"),
        Exception(32000, "请求异常！"),
        LoginCaptchaInvalid(32100, "验证码错误！"),
        LoginCaptchaExpires(32101, "验证码过期！"),
        LoginCaptchaUnCompliance(32102, "验证码使用不合规！"),
        LoginInvalid(32110, "登录失效！"),
        LoginUserNotExist(32111, "用户不存在！"),
        LoginUserPassError(32112, "用户名密码错误！"),
        LoginDeviceInvalid(32113, "登录设备异常！"),
        NoPermission(32114, "无权限！"),
        EncodeError(32115, "数据编译错误！"),
        NotFound(32200, "数据不存在或无法定位数据！"),
        Sql(32201, "数据请求错误！"),
        Type(32202, "类型错误！"),
        None(32203, "请求参数为空！"),
        Unmatched(32204, "数据类型或数据值不匹配！"),
        JsonFormat(32205, "数据格式化异常！"),
        Attribute(32206, "属性错误！"),
        Exist(32207, "数据已存在！"),
        InUse(32208, "资源被占用！"),
        Timeout(32209, "请求业务超时！"),
        ConnectionRefused(32210, "请求其他服务被拒！"),
        Offline(32400, "用户离线！"),
        UnImpl(32900, "方法未实现！"),
        ImportException(32401, "文件导入异常"),
        ExportException(32402, "文件导出异常"),
        Custom(33000, "自定义异常！"),
        EnqueueSuccess(201, "入队列成功！");

        private final int code;
        private final String brief;

        public int code() {
            return this.code;
        }

        public String brief() {
            return this.brief;
        }

        Status(int code, String brief) {
            this.code = code;
            this.brief = brief;
        }
    }

}
