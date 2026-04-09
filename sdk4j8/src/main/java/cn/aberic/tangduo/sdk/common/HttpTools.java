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

package cn.aberic.tangduo.sdk.common;

import okhttp3.*;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class HttpTools {

    // 单例 OkHttpClient，全局复用，不重复创建 = 不会 Too many open files
    private static final OkHttpClient CLIENT;

    static {
        CLIENT = new OkHttpClient.Builder()
                .connectTimeout(5, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .writeTimeout(10, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true) // 自动重连
                .connectionPool(new ConnectionPool(5, 1, TimeUnit.MINUTES)) // 连接池
                .build();
    }

    // ==================== GET 请求 ====================
    public static String get(String url) throws IOException {
        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();

        try (Response response = CLIENT.newCall(request).execute()) {
            if (!response.isSuccessful()) throw new IOException("请求失败：" + response.code());
            if (response.body() != null) {
                return response.body().string();
            }
        }
        return "";
    }

    // ==================== POST JSON ====================
    public static String postJson(String url, String json) throws IOException {
        RequestBody body = RequestBody.create(
                json,
                MediaType.parse("application/json; charset=utf-8")
        );

        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .build();

        try (Response response = CLIENT.newCall(request).execute()) {
            if (response.body() != null) {
                return response.body().string();
            }
        }
        return "";
    }

    // ==================== POST JSON ====================
    public static String putJson(String url, String json) throws IOException {
        RequestBody body = RequestBody.create(
                json,
                MediaType.parse("application/json; charset=utf-8")
        );

        Request request = new Request.Builder()
                .url(url)
                .put(body)
                .build();

        try (Response response = CLIENT.newCall(request).execute()) {
            if (response.body() != null) {
                return response.body().string();
            }
        }
        return "";
    }
}