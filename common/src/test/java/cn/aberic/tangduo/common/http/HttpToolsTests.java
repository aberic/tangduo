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

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

public class HttpToolsTests {

    @Test
    void getBaidu() {
        System.out.println(HttpTools.get("https://www.baidu.com"));
    }

    @Test
    void get() {
        System.out.println(HttpTools.get("http://127.0.0.1:19219/db/test/坚持阅读能带来什么"));
    }

    @Test
    void put() {
        List<String> list = new ArrayList<>();
        list.add("""
                底层是 Java 21 官方 HttpClient，性能极高
                RestClient 是 Spring 未来唯一长期支持的同步客户端
                语法现代、简洁、链式调用
                自动 JSON 序列化（用 Jackson）
                超时可控、不阻塞、不拖死业务
                不会像 RestTemplate 那样被标记过时
                """);
        System.out.println(HttpTools.put("http://127.0.0.1:19219/db/batch/test", list));
    }

}
