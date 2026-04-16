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

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Map;

/// HTTP 工具类
@Slf4j
public final class HttpTools {

    private HttpTools() {
        throw new AssertionError("工具类禁止实例化");
    }

    /// HTTP 客户端
    private static final RestClient REST_CLIENT;

    static {
        /// Java 21 原生 HttpClient，性能最好
        HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();

        /// HTTP 请求工厂
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
        factory.setReadTimeout(Duration.ofSeconds(5));

        /// HTTP 客户端
        REST_CLIENT = RestClient.builder().requestFactory(factory).build();
    }

    // ==================== GET ====================
    /// GET 请求
    /// @param url 请求 URL
    /// @param headers 请求头
    /// @return 响应体
    public static String get(String url, Map<String, String> headers) {
        try {
            return REST_CLIENT.get()
                    .uri(url)
                    .headers(h -> h.setAll(headers))
                    .retrieve()
                    .body(String.class);
        } catch (Exception e) {
            return null;
        }
    }

    /// GET 请求
    /// @param url 请求 URL
    /// @return 响应体
    public static String get(String url) {
        return get(url, Map.of());
    }

    // ==================== POST JSON ====================
    /// POST 请求
    /// @param url 请求 URL
    /// @param body 请求体
    /// @param headers 请求头
    /// @return 响应体
    public static String postJson(String url, Object body, Map<String, String> headers) {
        try {
            return REST_CLIENT.post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .headers(h -> h.setAll(headers))
                    .body(body)
                    .retrieve()
                    .body(String.class);
        } catch (Exception e) {
            return null;
        }
    }

    /// POST 请求
    /// @param url 请求 URL
    /// @param body 请求体
    /// @return 响应体
    public static String postJson(String url, Object body) {
        return postJson(url, body, Map.of());
    }

    // ==================== POST JSON ====================
    /// PUT 请求
    /// @param url 请求 URL
    /// @param body 请求体
    /// @param headers 请求头
    /// @return 响应体
    public static String putJson(String url, Object body, Map<String, String> headers) {
        try {
            return REST_CLIENT.put()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .headers(h -> h.setAll(headers))
                    .body(body)
                    .retrieve()
                    .body(String.class);
        } catch (Exception e) {
            log.error(e.getMessage());
            return null;
        }
    }

    /// PUT 请求
    /// @param url 请求 URL
    /// @param body 请求体
    /// @return 响应体
    public static String putJson(String url, Object body) {
        return putJson(url, body, Map.of());
    }

}
