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

import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class JsonToolsTests {

    public record Student(String name, int age) {}

    @Test
    void list() {
        List<Student> list = List.of(new Student("1", 2), new Student("3", 4));
        System.out.println(Arrays.toString(list.toArray()));
        String json = JsonTools.toJson(list);
        System.out.println(json);

        List<Student> list1 = JsonTools.toList(json);
        assert list1 != null;
        System.out.println(Arrays.toString(list1.toArray()));
    }

    @Test
    void map() {
        Map<String, Object> map = Map.of("name", "test", "age", 20);
        map.forEach((key, value) -> System.out.println("Key: " + key + ", Value: " + value));
        String json = JsonTools.toJson(map);
        Map<String, Object> map1 = JsonTools.toMap(json);
        assert map1 != null;
        map1.forEach((key, value) -> System.out.println("Key: " + key + ", Value: " + value));
    }

    String jsonStr = """
            {
              "database": "national",
              "index": "schools",
              "student": {
                "name": "n",
                "age": 12
              },
              "rating1": 4.5,
              "rating2": 4.5555,
              "values": [
                {
                  "key": "key",
                  "value": {
                    "name": "City School",
                    "description": "ICSE",
                    "street": "West End",
                    "city": "Meerut",
                    "state": "UP",
                    "zip": "250002",
                    "location": [
                      28.9926174,
                      77.692485
                    ],
                    "fees": 3500,
                    "tags": [
                      "fully computerized"
                    ],
                    "rating": "4.5"
                  }
                },
                {
                  "key": "key",
                  "value": {
                    "name": "City School",
                    "description": "ICSE",
                    "street": "West End",
                    "city": "Meerut",
                    "state": "UP",
                    "zip": "250002",
                    "location": [
                      28.9926174,
                      77.692485
                    ],
                    "fees": 3500,
                    "tags": [
                      "fully computerized"
                    ],
                    "rating": "4.5"
                  }
                }
              ]
            }""";

    @Test
    void getValueByPath() throws Exception {
        Object obj = JsonTools.getValueByPath(jsonStr, "student.name");
        assert obj instanceof String;
        assert Objects.equals(JsonTools.getValueByPath(jsonStr, "student.name"), "n");

        obj = JsonTools.getValueByPath(jsonStr, "student.age");
        assert obj instanceof Integer;
        assert (int) obj == 12;

        obj = JsonTools.getValueByPath(jsonStr, "values");
        assert obj instanceof List<?>;

        obj = JsonTools.getValueByPath(jsonStr, "values[0].value.name");
        assert obj instanceof String;
        assert Objects.equals(JsonTools.getValueByPath(jsonStr, "values[0].value.name"), "City School");

        obj = JsonTools.getValueByPath(jsonStr, "student.age");
        assert obj instanceof Number;
        obj = JsonTools.getValueByPath(jsonStr, "rating1");
        assert obj instanceof Number : obj;
        obj = JsonTools.getValueByPath(jsonStr, "rating2");
        assert obj instanceof Double : obj;
    }

    String json = """
            {
              "user": {
                "name": "张三",
                "pwd": "123456",
                "info": {
                  "phone": "1380000",
                  "email": "a@b.com"
                }
              },
              "list": [
                {"id":1,"age":20},
                {"id":2,"age":25}
              ]
            }
            """;
    /// 只保留这些路径
    List<String> keep = List.of(
            "user.name",
            "user.age",
            "user.info.phone",
            "list.0.age"
    );

    @Test
    void keepOnlyPaths() {
        JsonNode root = JsonTools.OBJECT_MAPPER.readTree(json);
        // 执行过滤
        JsonNode result = JsonTools.keepOnlyPaths(root, keep);
        // 输出结果
        System.out.println(JsonTools.OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(result));

        System.out.println();

        // 执行过滤
        result = JsonTools.keepOnlyPaths(json, keep);
        // 输出结果
        System.out.println(JsonTools.toJson(result));

        System.out.println();

        // 输出结果
        System.out.println(JsonTools.keepOnlyPathsStr(json, keep));
    }

}
