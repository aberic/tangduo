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

package cn.aberic.tangduo.index.engine;

import cn.aberic.tangduo.common.JsonTools;
import cn.aberic.tangduo.index.engine.entity.Conditions;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.rmi.UnexpectedException;
import java.util.ArrayList;
import java.util.List;

public class CommonTests {

    @Test
    void pathTest() {
        int indexVersion = 1;
        String filepath1 = "tmp/pathTest.1.11.td";
        String filepath2 = "tmp/pathTest.1.12.td";
        String filepath3 = "tmp/pathTest.2.21.idx";
        List<Path> pathList = new ArrayList<>();
        pathList.add(Path.of(filepath1));
        pathList.add(Path.of(filepath2));
        pathList.add(Path.of(filepath3));
        for (Path path : pathList) {
            String filepath = path.toString();
            if (filepath.endsWith(".td")) {
                String[] pathArr = path.getFileName().toString().split("\\.");
                int iv = Integer.parseInt(pathArr[pathArr.length - 3]);
                if (iv == indexVersion) {
                    System.out.println("version = " + pathArr[pathArr.length - 2]);
                }
            }
        }
    }

    @Test
    void conditions2json() throws UnexpectedException {
        Conditions conditions = new Conditions();
        conditions.addCondition("param1", "gt", "cv1");
        conditions.addCondition("param2", "lt", "cv2");
        System.out.println(JsonTools.toJson(conditions));
        // {
        //    "conditions": [
        //        {
        //            "param": "param1",
        //            "compare": "GT",
        //            "compareValue": "cv1"
        //        },
        //        {
        //            "param": "param2",
        //            "compare": "LT",
        //            "compareValue": "cv2"
        //        }
        //    ]
        //}
    }

}
