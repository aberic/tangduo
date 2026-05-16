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
import cn.aberic.tangduo.db.common.CommonTools;
import cn.aberic.tangduo.db.entity.DocSelectResponseVO;
import cn.aberic.tangduo.index.Index;
import cn.aberic.tangduo.index.engine.IEngine;
import cn.aberic.tangduo.index.engine.entity.Hit;
import cn.aberic.tangduo.index.engine.entity.Select;
import cn.aberic.tangduo.index.engine.entity.Sort;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import javax.management.InstanceAlreadyExistsException;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Slf4j
public class DBConditionTests {

    final static String rootpath = "tmp/condition";

    @Test
    @Order(1)
    void init() {
        Filer.deleteDirectory(Path.of(rootpath).toAbsolutePath().toString());
    }

    record User(String name, int age, int a, int b, int c, int d) {}

    record Role(int id, int e, int f, int g, User user) {}

    Role role(int i) {
        User user = new User("name_" + i, i, i, i, i, i);
        return new Role(i, i, i, i, user);
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
    void putJsonListAndSearch() throws Exception {
        String dbName = "putJsonListAndSelectDB";
        String indexName = "putJsonListAndSelectIndex";
        DB db = DB.getInstance(rootpath, 10737418240L);
        db.removeDB(dbName);
        try {
            db.createDB(dbName);
            db.createIndex(dbName, IEngine.UNITY, new Index.Info(1, indexName, true, true, false));
        } catch (InstanceAlreadyExistsException e) {
            log.error(e.getMessage());
        }

        for (int i = 0; i < 1000; i++) {
            db.put(dbName, indexName, String.valueOf(i), false, role(i));
        }

        String selectIndexName = CommonTools.indexName(indexName);
        Select select = new Select(selectIndexName, -50, 50, true, false, 100, true);
        select.addCondition("user.age", "ge", 15);
        List<DocSelectResponseVO> bytesList = db.select(dbName, select);
        for (DocSelectResponseVO bytes : bytesList) {
            log.info("bytes.getValue() = {}", bytes.getValue());
        }

        System.out.println();

        select = new Select();
        select.setIndexName(selectIndexName);
        select.addCondition("user.age", "ge", 20);
        select.addCondition("user.age", "lt", 30);
        bytesList = db.select(dbName, select);
        for (DocSelectResponseVO bytes : bytesList) {
            System.out.println(bytes.getValue());
        }
    }

    @Test
    void string() {
        Object a = "hello";
        String aStr = String.valueOf(a);
        System.out.println(aStr);
    }

    @Test
    @Order(2)
    void afterDegree() throws Exception {
        String dbName = "afterDegreeDB";
        String indexName = "afterDegreeIndex";
        DB db = DB.getInstance(rootpath, 10737418240L);
        db.removeDB(dbName);
        try {
            db.createDB(dbName);
            db.createIndex(dbName, IEngine.UNITY, new Index.Info(1, indexName, true, true, false));
        } catch (InstanceAlreadyExistsException e) {
            System.out.println(e.getMessage());
        }
//        int threadCount = 10000000; // 已测 插入执行耗时：14.52.055，查询执行耗时：33.37.622s，wrongCount = 0
        int threadCount = 1000;
        int endIndex = threadCount / 2;
        int startIndex = endIndex - threadCount;
        log.info("startIndex = {}, endIndex = {}, count = {}", startIndex, endIndex, threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount); // 计数3

        try (ThreadPoolExecutor executor = new ThreadPoolExecutor(
                10,                  // 核心线程
                50,                  // 最大线程（关键！限制线程总数）
                60L, TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(200),  // 有界队列！！绝对不用无界 LinkedBlockingQueue
                new ThreadPoolExecutor.CallerRunsPolicy()  // 拒绝策略
        )) {
            for (int i = startIndex; i < endIndex; i++) {
                int finalI = i;
                String finalIndexName = indexName;
                executor.execute(() -> {
                    try {
                        db.put(dbName, finalIndexName, String.valueOf(finalI), false, role(finalI));
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    } finally {
                        latch.countDown();
                    }
                });
            }
            // 等待计数减到0（所有线程完成）
            latch.await();


            indexName = CommonTools.indexName(indexName);
            List<String> fields = new ArrayList<>();
            afterDegreeSelect(db, dbName, indexName, 0, true, fields);
            // {
            //   "database":"afterDegreeDB",
            //   "degree":10,
            //   "digests":"faa584c8e3fb55abced8621c730d34acd35d160b865b4bd52101d98a1ac44d13",
            //   "index":"default_afterdegreeindex_key",
            //   "key":"10",
            //   "value":{
            //     "id":10,
            //     "e":10,
            //     "f":10,
            //     "g":10,
            //     "user":{
            //       "name":"name_10",
            //       "age":10,
            //       "a":10,
            //       "b":10,
            //       "c":10,
            //       "d":10
            //     }
            //   }
            // }
            fields = List.of("id", "e", "f", "user.name", "user.age", "user.a", "user.b", "user.c");
            afterDegreeSelect(db, dbName, indexName, 9, true, fields);
            fields = List.of("id", "e", "user.name", "user.age", "user.a", "user.b");
            afterDegreeSelect(db, dbName, indexName, -5, true, fields);

            fields = List.of("id", "user.name", "user.age", "user.a");
            afterDegreeSelect(db, dbName, indexName, 0, false, fields);
            fields = List.of("id", "user.name", "user.age");
            afterDegreeSelect(db, dbName, indexName, 9, false, fields);
            fields = List.of("id", "user.name");
            afterDegreeSelect(db, dbName, indexName, -15, false, fields);

        }
    }

    void afterDegreeSelect(DB db, String dbName, String indexName, long afterDegree, boolean asc, List<String> fields) throws IOException {
        Select select = new Select();
        select.setLimit(10);
        Hit hit = new Hit();
        hit.setSort(new Sort(indexName, "user.age", afterDegree, asc));
        hit.setFields(fields);
        select.setHit(hit);
        List<DocSelectResponseVO> vos = db.select(dbName, select);
        System.out.println("list size = " + vos.size() + " | afterDegree = " + afterDegree + " | asc = " + asc);
        for (DocSelectResponseVO vo : vos) {
            System.out.println(vo.getValue());
        }
    }

}
