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

package cn.aberic.tangduo.common;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class Maps {

    private Maps() {
        throw new IllegalStateException("Maps class");
    }

    public static byte[] map2bytes(Map<String, Object> map) {
        return map2string(map).getBytes(StandardCharsets.UTF_8);
    }

    public static String map2string(Map<String, Object> map) {
        Gson gson = new Gson();
        return gson.toJson(map);
    }

    public static Map<String, Object> bytes2map(byte[] bytes) {
        return string2map(new String(bytes));
    }

    public static Map<String, Object> string2map(String str) {
        Gson gson = new Gson();
        Type type = new TypeToken<Map<String, Object>>(){}.getType();
        return gson.fromJson(str, type);
    }

}
