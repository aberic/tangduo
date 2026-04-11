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

package cn.aberic.tangduo.db;

import cn.aberic.tangduo.common.JsonTools;
import cn.aberic.tangduo.common.file.Filer;
import cn.aberic.tangduo.db.entity.Doc;
import cn.aberic.tangduo.index.Index;
import cn.aberic.tangduo.index.engine.IEngine;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import javax.management.InstanceAlreadyExistsException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Stream;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Slf4j
public class DBConditionTests {

    final static String rootpath = "tmp/condition";

    @Test
    @Order(1)
    void init() {
        try (Stream<Path> stream = Files.walk(Paths.get(rootpath))) {
            stream.forEach(f -> {
                try {
                    Files.delete(f);
                } catch (IOException ignore) {}
            });
        } catch (IOException e) {
            log.warn(e.getMessage());
        }
    }

    record User(String name, int age) {}

    record Role(int id, User user) {}

    Role role(int i) {
        User user = new User("name" + i, i);
        return new Role(i, user);
    }

    @Test
    void jsonChecked() {
        Role role = role(100);
        String jsonStr = JsonTools.toJson(role);
        System.out.println(jsonStr);
        System.out.println();
        String jsonStrAgain = JsonTools.toJson(jsonStr);
        System.out.println(jsonStrAgain);
        System.out.println();
        String jsonStrThree = JsonTools.toJson(jsonStrAgain);
        System.out.println(jsonStrThree);
    }

    @Test
    @Order(2)
    void putJsonListAndSelect() throws IOException, NoSuchFieldException, NoSuchMethodException {
        String dbName = "putJsonListAndSelectDB";
        Filer.deleteDirectory(Path.of(rootpath, dbName).toAbsolutePath().toString());
        String indexName = "putJsonListAndSelectIndex";
        DB db = DB.getInstance(rootpath, 10737418240L);
        db.removeDB(dbName);
        try {
            db.createDB(dbName);
            db.createIndex(dbName, IEngine.UNITY, new Index.Info(1, indexName, true, true, false));
        } catch (InstanceAlreadyExistsException e) {
            System.out.println(e.getMessage());
        }

        for (int i = 0; i < 1000; i++) {
            db.put(dbName, indexName, String.valueOf(i), false, role(i));
        }

        IEngine.Conditions conditions = new IEngine.Conditions();
        conditions.addCondition("user.age", "ge", 15);
        IEngine.Search search = new IEngine.Search(indexName, -50, 50, true, false, 100, true, conditions);
        List<byte[]> bytesList = db.select(dbName, search);
        for (byte[] bytes : bytesList) {
            System.out.println(new Doc(bytes).getValue());
        }

        System.out.println();

        IEngine.Conditions conditions2 = new IEngine.Conditions();
        conditions2.addCondition("user.age", "ge", 20);
        conditions2.addCondition("user.age", "lt", 30);
        search = new IEngine.Search(indexName, conditions2);
        bytesList = db.select(dbName, search);
        for (byte[] bytes : bytesList) {
            System.out.println(new Doc(bytes).getValue());
        }
    }

    @Test
    void string() {
        Object a = "hello";
        String aStr = String.valueOf(a);
        System.out.println(aStr);
    }

}
