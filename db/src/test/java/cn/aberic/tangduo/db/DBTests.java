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

import cn.aberic.tangduo.common.ByteTools;
import cn.aberic.tangduo.common.JsonTools;
import cn.aberic.tangduo.common.file.Filer;
import cn.aberic.tangduo.db.common.CommonTools;
import cn.aberic.tangduo.db.common.IkTokenizerTools;
import cn.aberic.tangduo.db.entity.*;
import cn.aberic.tangduo.index.Index;
import cn.aberic.tangduo.index.engine.IEngine;
import cn.aberic.tangduo.index.engine.entity.Condition;
import cn.aberic.tangduo.index.engine.entity.Content;
import cn.aberic.tangduo.index.engine.entity.Search;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.util.CollectionUtils;

import javax.management.InstanceAlreadyExistsException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.rmi.UnexpectedException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Slf4j
public class DBTests {
    final static String value = """
            {
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
              "rating": 4.5,
              "rating1": 4.6,
              "brief": "我在SpringBoot中使用IK分词器做中英文分词",
              "love": "i love u",
              "traceid": "21b37450fd4e420591a524faef30b67e"
            }""";
    final static String rootpath = "tmp/dbTest";

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

    @Test
    @Order(2)
    void dbRemove() throws Exception {
        String dbName = "dbRemove";
        Filer.deleteDirectory(Path.of(rootpath, dbName).toAbsolutePath().toString());
        DB db = DB.getInstance(rootpath, 10737418240L);
        db.removeDB(dbName);
        try {
            db.createDB(dbName);
        } catch (InstanceAlreadyExistsException e) {
            System.out.println(e.getMessage());
        }
        assert db.dbExist(dbName) : db.dbExist(dbName);
        db.removeDB(dbName);
        assert !db.dbExist(dbName) : db.dbExist(dbName);
    }

    @Test
    @Order(3)
    void dbRemoveCheck() throws Exception {
        String dbName = "dbRemove";
        DB db = DB.getInstance(rootpath, 10737418240L);
        assert !db.dbExist(dbName) : db.dbExist(dbName);
    }

    @Test
    @Order(2)
    void putAndGetFirst() throws Exception, NoSuchMethodException {
        String dbName = "putAndGetFirstDB";
        Filer.deleteDirectory(Path.of(rootpath, dbName).toAbsolutePath().toString());
        String indexName = "putAndGetFirstIndex";
        DB db = DB.getInstance(rootpath, 10737418240L);
        db.removeDB(dbName);
        try {
            db.createDB(dbName);
            db.createIndex(dbName, IEngine.UNITY, new Index.Info(1, indexName, true, true, false));
        } catch (InstanceAlreadyExistsException e) {
            System.out.println(e.getMessage());
        }
        indexName = CommonTools.indexName(indexName);
        DocPutResponseVO putVO = db.put(dbName, indexName, "-64424581328", false, 1);
        int value = (int) db.getFirst(dbName, indexName, putVO.getDegree(), "-64424581328").getValue();
        assert 1 == value : value;
        putVO = db.put(dbName, indexName, "0", false, 1);
        value = (int) db.getFirst(dbName, indexName, putVO.getDegree(), "0").getValue();
        assert 1 == value : value;
        putVO = db.put(dbName, indexName, "1", false, 1);
        value = (int) db.getFirst(dbName, indexName, putVO.getDegree(), "1").getValue();
        assert 1 == value : value;
        putVO = db.put(dbName, indexName, "9223372036854775807", false, 1);
        value = (int) db.getFirst(dbName, indexName, putVO.getDegree(), "9223372036854775807").getValue();
        assert 1 == value : value;
    }

    @Test
    @Order(2)
    void putAndGetFirstAndRemove() throws Exception, NoSuchMethodException {
        String dbName = "putAndGetFirstAndRemoveDB";
        Filer.deleteDirectory(Path.of(rootpath, dbName).toAbsolutePath().toString());
        String indexName = "putAndGetFirstAndRemoveIndex";
        DB db = DB.getInstance(rootpath, 10737418240L);
        db.removeDB(dbName);
        try {
            db.createDB(dbName);
            db.createIndex(dbName, IEngine.UNITY, new Index.Info(1, indexName, true, true, false));
        } catch (InstanceAlreadyExistsException e) {
            System.out.println(e.getMessage());
        }
        indexName = CommonTools.indexName(indexName);

        db.put(dbName, indexName, "-64424581328", false, 1);
        db.put(dbName, indexName, "0", false, 1);
        db.put(dbName, indexName, "1", false, 1);
        db.put(dbName, indexName, "9223372036854775807", false, 1);

        db.remove(dbName, indexName, "-64424581328");
        db.remove(dbName, indexName, "0");
        db.remove(dbName, indexName, "1");
        db.remove(dbName, indexName, "9223372036854775807");

        assert null == db.getFirst(dbName, indexName, "-64424581328") : db.getFirst(dbName, indexName, "-64424581328");
        assert null == db.getFirst(dbName, indexName, "0") : db.getFirst(dbName, indexName, "0");
        assert null == db.getFirst(dbName, indexName, "1") : db.getFirst(dbName, indexName, "1");
        assert null == db.getFirst(dbName, indexName, "9223372036854775807") : db.getFirst(dbName, indexName, "9223372036854775807");
    }

    @Test
    @Order(2)
    void putPre() {
        long degree = System.currentTimeMillis();
        String key = String.valueOf(degree);
        List<DB.IndexName4KeyAndDegree> list = DB.parseJsonStr2IndexName4KeyAndDegree(value);
        System.out.println(list);
        List<String> indexNameList = IkTokenizerTools.tokenize(value);
        indexNameList.forEach(idxName -> {
            if (list.stream().noneMatch(in4kd -> in4kd.indexName().equals(idxName))) {
                list.add(new DB.IndexName4KeyAndDegree(idxName + ".datetime", key, degree, false));
            }
        });
        System.out.println(list.stream().filter(item -> item.indexName().equals("name.datetime")).findFirst().orElse(null));
        System.out.println(list);
        list.forEach(System.out::println);
    }

    @Test
    @Order(2)
    void putStringContent() throws Exception, NoSuchMethodException {
        String dbName = "putStringContentDB";
        Filer.deleteDirectory(Path.of(rootpath, dbName).toAbsolutePath().toString());
        String indexName = "putStringContentIndex";
        DB db = DB.getInstance(rootpath, 10737418240L);
        db.removeDB(dbName);
        try {
            db.createDB(dbName);
            db.createIndex(dbName, IEngine.UNITY, new Index.Info(1, indexName, true, true, false));
        } catch (InstanceAlreadyExistsException e) {
            System.out.println(e.getMessage());
        }

        String key = "1";
        Content content = db.put(dbName, indexName, key, value).getContent();
        log.info("content = {}", content);

        int count = content.getItems().size();

        AtomicInteger successCount = new AtomicInteger();
        AtomicInteger failedCount = new AtomicInteger();
        content.getItems().forEach(item -> {
            List<DocGetResponseVO> docGetResponseVOList;
            try {
                docGetResponseVOList = db.getForceIndex(dbName, item.getIndexName(), item.getDegree(), item.getKey());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            if (Objects.isNull(docGetResponseVOList) || docGetResponseVOList.isEmpty()) {
                System.out.println("failed indexName=" + item.getIndexName() + "|degree=" + item.getDegree() + "|key=" + item.getKey());
                failedCount.addAndGet(1);
            } else {
                String valueFromDoc = JsonTools.toJson(docGetResponseVOList.getFirst().getValue());
                assert valueFromDoc == null || valueFromDoc.equals(JsonTools.compactJson(value)) : valueFromDoc;
                System.out.println("success indexName=" + item.getIndexName() + "|degree=" + item.getDegree() + "|key=" + item.getKey());
                successCount.addAndGet(1);
            }
        });
        System.out.println("successCount = " + successCount.get() + ", failedCount = " + failedCount.get());
        assert count == successCount.get();
        assert 0 == failedCount.get();
    }

    @Test
    @Order(2)
    void putStringDefaultIndexContent() throws Exception {
        String dbName = "putStringDefaultIndexContentDB";
        Filer.deleteDirectory(Path.of(rootpath, dbName).toAbsolutePath().toString());
        DB db = DB.getInstance(rootpath, 10737418240L);
        db.removeDB(dbName);
        try {
            db.createDB(dbName);
        } catch (InstanceAlreadyExistsException e) {
            System.out.println(e.getMessage());
        }

        String key = "1";
        Content content = db.put(dbName, key, value).getContent();
        log.info("content = {}", content);

        int count = content.getItems().size();

        AtomicInteger successCount = new AtomicInteger();
        AtomicInteger failedCount = new AtomicInteger();
        content.getItems().forEach(item -> {
            List<DocGetResponseVO> docGetResponseVOList;
            try {
                docGetResponseVOList = db.getForceIndex(dbName, item.getIndexName(), item.getDegree(), item.getKey());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            if (Objects.isNull(docGetResponseVOList) || docGetResponseVOList.isEmpty()) {
                System.out.println("failed indexName=" + item.getIndexName() + "|degree=" + item.getDegree() + "|key=" + item.getKey());
                failedCount.addAndGet(1);
            } else {
                String valueFromDoc = JsonTools.toJson(docGetResponseVOList.getFirst().getValue());
                assert valueFromDoc == null || valueFromDoc.equals(JsonTools.compactJson(value)) : valueFromDoc;
                System.out.println("success indexName=" + item.getIndexName() + "|degree=" + item.getDegree() + "|key=" + item.getKey());
                successCount.addAndGet(1);
            }
        });
        System.out.println("successCount = " + successCount.get() + ", failedCount = " + failedCount.get());
        assert count == successCount.get();
        assert 0 == failedCount.get();
    }

    @Test
    @Order(2)
    void putStringDefaultIndexAndKeyContent() throws Exception {
        String dbName = "putStringDefaultIndexAndKeyContentDB";
        Filer.deleteDirectory(Path.of(rootpath, dbName).toAbsolutePath().toString());
        DB db = DB.getInstance(rootpath, 10737418240L);
        db.removeDB(dbName);
        try {
            db.createDB(dbName);
        } catch (InstanceAlreadyExistsException e) {
            System.out.println(e.getMessage());
        }

        Content content = db.put(dbName, value).getContent();
        log.info("content = {}", content);

        int count = content.getItems().size();

        AtomicInteger successCount = new AtomicInteger();
        AtomicInteger failedCount = new AtomicInteger();
        content.getItems().forEach(item -> {
            List<DocGetResponseVO> docGetResponseVOList;
            try {
                docGetResponseVOList = db.getForceIndex(dbName, item.getIndexName(), item.getDegree(), item.getKey());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            if (Objects.isNull(docGetResponseVOList) || docGetResponseVOList.isEmpty()) {
                System.out.println("failed indexName=" + item.getIndexName() + "|degree=" + item.getDegree() + "|key=" + item.getKey());
                failedCount.addAndGet(1);
            } else {
                String valueFromDoc = JsonTools.toJson(docGetResponseVOList.getFirst().getValue());
                assert valueFromDoc == null || valueFromDoc.equals(JsonTools.compactJson(value)) : valueFromDoc;
                System.out.println("success indexName=" + item.getIndexName() + "|degree=" + item.getDegree() + "|key=" + item.getKey());
                successCount.addAndGet(1);
            }
        });
        System.out.println("successCount = " + successCount.get() + ", failedCount = " + failedCount.get());
        assert count == successCount.get();
        assert 0 == failedCount.get();
    }

    @Test
    @Order(2)
    void putStringDefaultDatabaseIndexAndKeyContent() throws Exception {
        Filer.deleteDirectory(Path.of(rootpath, DB.DATABASE_NAME_DEFAULT).toAbsolutePath().toString());
        DB db = DB.getInstance(rootpath, 10737418240L);
        db.removeDB(DB.DATABASE_NAME_DEFAULT);

        Content content = db.put(value).getContent();
        log.info("content = {}", content);

        int count = content.getItems().size();

        AtomicInteger successCount = new AtomicInteger();
        AtomicInteger failedCount = new AtomicInteger();
        content.getItems().forEach(item -> {
            List<DocGetResponseVO> docGetResponseVOList;
            try {
                docGetResponseVOList = db.getForceIndex(DB.DATABASE_NAME_DEFAULT, item.getIndexName(), item.getDegree(), item.getKey());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            if (Objects.isNull(docGetResponseVOList) || docGetResponseVOList.isEmpty()) {
                System.out.println("failed indexName=" + item.getIndexName() + "|degree=" + item.getDegree() + "|key=" + item.getKey());
                failedCount.addAndGet(1);
            } else {
                String valueFromDoc = JsonTools.toJson(docGetResponseVOList.getFirst().getValue());
                assert valueFromDoc == null || valueFromDoc.equals(JsonTools.compactJson(value)) : valueFromDoc;
                System.out.println("success indexName=" + item.getIndexName() + "|degree=" + item.getDegree() + "|key=" + item.getKey());
                successCount.addAndGet(1);
            }
        });
        System.out.println("successCount = " + successCount.get() + ", failedCount = " + failedCount.get());
        assert count == successCount.get();
        assert 0 == failedCount.get();
    }

    @Test
    @Order(2)
    void putAndSelectFirstTimesAsync() throws Exception, NoSuchMethodException, InterruptedException {
        String dbName = "putAndGetFirstTimesAsyncDB";
        Filer.deleteDirectory(Path.of(rootpath, dbName).toAbsolutePath().toString());
        String indexName = "putAndGetFirstTimesAsyncIndex";
        DB db = DB.getInstance(rootpath, 10737418240L);
        db.removeDB(dbName);
        try {
            db.createDB(dbName);
            db.createIndex(dbName, IEngine.UNITY, new Index.Info(1, indexName, true, true, false));
        } catch (InstanceAlreadyExistsException e) {
            System.out.println(e.getMessage());
        }
//        int threadCount = 10000000; // 已测 插入执行耗时：14.52.055，查询执行耗时：33.37.622s，wrongCount = 0
        int threadCount = 100000;
        int startIndex = threadCount / 2 - threadCount;
        CountDownLatch latch = new CountDownLatch(threadCount); // 计数3
        indexName = CommonTools.indexName(indexName);
        long start = System.currentTimeMillis();

        try (ThreadPoolExecutor executor = new ThreadPoolExecutor(
                10,                  // 核心线程
                50,                  // 最大线程（关键！限制线程总数）
                60L, TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(200),  // 有界队列！！绝对不用无界 LinkedBlockingQueue
                new ThreadPoolExecutor.CallerRunsPolicy()  // 拒绝策略
        )) {
            for (int i = startIndex; i < threadCount; i++) {
                int finalI = i;
                String finalIndexName = indexName;
                executor.execute(() -> {
                    try {
                        db.put(dbName, finalIndexName, String.valueOf(finalI), false, finalI);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    } finally {
                        latch.countDown();
                    }
                });
            }
            // 等待计数减到0（所有线程完成）
            latch.await();
            long ms = System.currentTimeMillis() - start;
            long minutes = ms / (1000 * 60);
            long seconds = (ms / 1000) % 60;
            long millis = ms % 1000;
            String timeStr = String.format("%02d.%02d.%03d", minutes, seconds, millis);
            log.info("setAndGetTimes set success! 插入执行耗时：{}", timeStr);


            AtomicLong wrongCount = new AtomicLong(0);
            CountDownLatch latchGet = new CountDownLatch(threadCount); // 计数3
            start = System.currentTimeMillis();
            for (int i = startIndex; i < threadCount; i++) {
                long finalI = i;
                String finalIndexName = indexName;
                executor.execute(() -> {
                    try {
                        int value = (int) db.getFirst(dbName, finalIndexName, String.valueOf(finalI)).getValue();
                        if (finalI != value) {
                            log.info("i = {}, | read = {}", finalI, value);
                            wrongCount.getAndAdd(1);
                        }
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    } finally {
                        latchGet.countDown();
                    }
                });
            }
            latchGet.await();
            ms = System.currentTimeMillis() - start;
            minutes = ms / (1000 * 60);
            seconds = (ms / 1000) % 60;
            millis = ms % 1000;
            timeStr = String.format("%02d.%02d.%03d", minutes, seconds, millis);
            log.info("setAndGetTimes check over! 查询执行耗时：{},  wrongCount = {}", timeStr, wrongCount.get());
            assert wrongCount.get() == 0 : wrongCount;
        }
    }

    @Test
    @Order(3)
    void select() throws Exception {
        String dbName = "putAndGetFirstTimesAsyncDB";
        String indexName = "putAndGetFirstTimesAsyncIndex";
        DB db = DB.getInstance(rootpath, 10737418240L);
        indexName = CommonTools.indexName(indexName);
        long wrongCount = 0;
        for (long i = -500; i < 500; i++) {
            int value = (int) db.getFirst(dbName, indexName, String.valueOf(i)).getValue();
            if (i != value) {
                log.info("i = {}, | read = {}", i, value);
                wrongCount++;
            }
        }
        log.info("setAndGetTimes check over! wrongCount =  {}", wrongCount);

        Search search = new Search(indexName, -500, 500, true, true, 100, true);
        List<DocSearchResponseVO> bytesList = db.select(dbName, search);
        System.out.println("list size = " + bytesList.size());
        for (int i = 0; i < bytesList.size(); i++) {
            int value = (int) bytesList.get(i).getValue();
            assert value == -500 + i : value + " != " + (-500 + i);
        }

        search = new Search(indexName, -500, 500, false, false, 100, true);
        bytesList = db.select(dbName, search);
        System.out.println("list size = " + bytesList.size());
        for (int i = 0; i < bytesList.size(); i++) {
            int value = (int) bytesList.get(i).getValue();
            assert value == -499 + i : value + " != " + (-499 + i);
        }

        search = new Search(indexName, -500, 500, true, true, 100, false);
        bytesList = db.select(dbName, search);
        System.out.println("list size = " + bytesList.size());
        for (int i = 0; i < bytesList.size(); i++) {
            int value = (int) bytesList.get(i).getValue();
            assert value == 500 - i : value + " != " + (500 - i);
        }

        search = new Search(indexName, -500, 500, false, false, 100, false);
        bytesList = db.select(dbName, search);
        System.out.println("list size = " + bytesList.size());
        for (int i = 0; i < bytesList.size(); i++) {
            int value = (int) bytesList.get(i).getValue();
            assert value == 499 - i : value + " != " + (499 - i);
        }

        search = new Search(indexName, -50, 50, true, true, 100, true);
        bytesList = db.select(dbName, search);
        System.out.println("list size = " + bytesList.size());
        for (int i = 0; i < bytesList.size(); i++) {
            int value = (int) bytesList.get(i).getValue();
            assert value == -50 + i : value + " != " + (-50 + i);
        }

        search = new Search(indexName, -50, 50, false, false, 100, true);
        bytesList = db.select(dbName, search);
        System.out.println("list size = " + bytesList.size());
        for (int i = 0; i < bytesList.size(); i++) {
            int value = (int) bytesList.get(i).getValue();
            System.out.print(value + " ");
            assert value == -49 + i : value + " != " + (-49 + i);
        }
        System.out.println();

        search = new Search(indexName, -50, 50, false, false, 100, true, (bsList, conditionList) -> {
            List<byte[]> bl = new ArrayList<>();
            for (byte[] bytes : bsList) {
                int value = new Doc(bytes).getValue().asInt();
                if (0 != value) {
                    bl.add(bytes);
                }
            }
            return bl;
        });
        bytesList = db.select(dbName, search);
        System.out.println("list size = " + bytesList.size());
        for (int i = 0; i < bytesList.size(); i++) {
            int value = (int) bytesList.get(i).getValue();
            System.out.print(value + " ");
            if (i < 49) {
                assert value == -49 + i : value + " != " + (-49 + i);
            } else {
                assert value == -48 + i : value + " != " + (-48 + i);
            }
        }
        System.out.println();
    }

    @Test
    @Order(2)
    void putAndGetFirstInSameIndex0() throws Exception, NoSuchMethodException {
        String dbName = "putAndGetFirstInSameIndexDB";
        Filer.deleteDirectory(Path.of(rootpath, dbName).toAbsolutePath().toString());
        String indexName = "putAndGetFirstInSameIndex0Index";
        DB db = DB.getInstance(rootpath, 10737418240L);
        db.removeDB(dbName);
        try {
            db.createDB(dbName);
            db.createIndex(dbName, IEngine.UNITY, new Index.Info(1, indexName, true, true, false));
        } catch (InstanceAlreadyExistsException e) {
            System.out.println(e.getMessage());
        }
        indexName = CommonTools.indexName(indexName);

        DocPutResponseVO putVO = db.put(dbName, indexName, "-64424581328", false, 1);
        int value = (int) db.getFirst(dbName, indexName, putVO.getDegree(), "-64424581328").getValue();
        assert 1 == value : value;
        putVO = db.put(dbName, indexName, "0", false, 1);
        value = (int) db.getFirst(dbName, indexName, putVO.getDegree(), "0").getValue();
        assert 1 == value : value;
        putVO = db.put(dbName, indexName, "1", false, 1);
        value = (int) db.getFirst(dbName, indexName, putVO.getDegree(), "1").getValue();
        assert 1 == value : value;
        putVO = db.put(dbName, indexName, "9223372036854775807", false, 1);
        value = (int) db.getFirst(dbName, indexName, putVO.getDegree(), "9223372036854775807").getValue();
        assert 1 == value : value;
    }

    @Test
    @Order(3)
    void putAndGetFirstInSameIndex1() throws Exception, NoSuchMethodException {
        String dbName = "putAndGetFirstInSameIndexDB";
        String indexName = "putAndGetFirstInSameIndex1Index";
        DB db = DB.getInstance(rootpath, 10737418240L);
        try {
            db.createIndex(dbName, IEngine.UNITY, new Index.Info(1, indexName, true, true, false));
        } catch (InstanceAlreadyExistsException e) {
            System.out.println(e.getMessage());
        }
        indexName = CommonTools.indexName(indexName);

        DocPutResponseVO putVO = db.put(dbName, indexName, "-64424581328", false, 1);
        int value = (int) db.getFirst(dbName, indexName, putVO.getDegree(), "-64424581328").getValue();
        assert 1 == value : value;
        putVO = db.put(dbName, indexName, "0", false, 1);
        value = (int) db.getFirst(dbName, indexName, putVO.getDegree(), "0").getValue();
        assert 1 == value : value;
        putVO = db.put(dbName, indexName, "1", false, 1);
        value = (int) db.getFirst(dbName, indexName, putVO.getDegree(), "1").getValue();
        assert 1 == value : value;
        putVO = db.put(dbName, indexName, "9223372036854775807", false, 1);
        value = (int) db.getFirst(dbName, indexName, putVO.getDegree(), "9223372036854775807").getValue();
        assert 1 == value : value;
    }

    @Test
    @Order(4)
    void putAndGetFirstInSameIndex2() throws Exception {
        String dbName = "putAndGetFirstInSameIndexDB";
        String indexName0 = "putAndGetFirstInSameIndex0Index";
        String indexName1 = "putAndGetFirstInSameIndex1Index";
        DB db = DB.getInstance(rootpath, 10737418240L);
        indexName0 = CommonTools.indexName(indexName0);
        indexName1 = CommonTools.indexName(indexName1);
        int value = (int) db.getFirst(dbName, indexName0, "-64424581328").getValue();
        assert 1 == value : value;
        value = (int) db.getFirst(dbName, indexName0, "0").getValue();
        assert 1 == value : value;
        value = (int) db.getFirst(dbName, indexName0, "1").getValue();
        assert 1 == value : value;
        value = (int) db.getFirst(dbName, indexName0, "9223372036854775807").getValue();
        assert 1 == value : value;

        value = (int) db.getFirst(dbName, indexName1, "-64424581328").getValue();
        assert 1 == value : value;
        value = (int) db.getFirst(dbName, indexName1, "0").getValue();
        assert 1 == value : value;
        value = (int) db.getFirst(dbName, indexName1, "1").getValue();
        assert 1 == value : value;
        value = (int) db.getFirst(dbName, indexName1, "9223372036854775807").getValue();
        assert 1 == value : value;
    }

    @Test
    @Order(2)
    void deleteList() throws Exception {
        String dbName = "deleteListDB";
        Filer.deleteDirectory(Path.of(rootpath, dbName).toAbsolutePath().toString());
        String indexName = "deleteListIndex";
        DB db = DB.getInstance(rootpath, 10737418240L);
        db.removeDB(dbName);
        try {
            db.createDB(dbName);
            db.createIndex(dbName, IEngine.UNITY, new Index.Info(1, indexName, true, true, false));
        } catch (InstanceAlreadyExistsException | NoSuchMethodException e) {
            System.out.println(e.getMessage());
        }
        int count = 10000;
        for (int i = -5000; i < count; i++) {
            db.put(dbName, indexName, String.valueOf(i), false, i);
        }
        log.info("setAndGetTimes set success!");
        for (long i = -5000; i < count; i++) {
            int value = (int) db.getFirst(dbName, indexName, String.valueOf(i)).getValue();
            assert i == value : i + " != " + value;
        }
        log.info("setAndGetTimes check success!");

        Search search = new Search(CommonTools.indexName(indexName), -100, 100, false, false, 200, true);
        List<DocSearchResponseVO> bytesList = db.delete(dbName, search);
        assert 199 == bytesList.size() : "199 != " + bytesList.size(); // (-99 —— 0) + (1 —— 99) = 199
        for (int i = 0; i < bytesList.size(); i++) {
            int value = (int) bytesList.get(i).getValue();
            assert (i - 99) == value : (i - 99) + " != " + value; // (-99 —— 0) + (1 —— 99) = 199
        }
        search = new Search(CommonTools.indexName(indexName), -120, 150, false, false, 100, true);
        bytesList = db.select(dbName, search); // -99 —— 99 上一轮已删
        assert 70 == bytesList.size() : "70 != " + bytesList.size(); // -120——150总计271个数字，减去上一轮的199，还剩70个数字
        for (int i = 0; i < bytesList.size(); i++) {
            int value = (int) bytesList.get(i).getValue();
            // (-99 —— 0) + (1 —— 99) 因获取不到，被过滤掉
            if (i < 20) { // 即 -120 —— -100 是可查到数字，但不包含 -120
                assert (i - 119) == value : (i - 119) + " != " + value;
            } else { // 100 —— 150 是可查到数字，但不包含 150，20以后从100开始计数
                assert (i + 80) == value : (i + 80) + " != " + value;
            }
        }
    }

    @Test
    void filterCondition() throws UnexpectedException {
        String value1 = """
                {
                  "name": "Lily",
                  "age": 18
                }""";
        String value2 = """
                {
                  "name": "Tom",
                  "age": 19
                }""";
        String value3 = """
                {
                  "name": "Sam",
                  "age": 20
                }""";
        byte[] bytes1 = ByteTools.fromString(value1);
        byte[] bytes2 = ByteTools.fromString(value2);
        byte[] bytes3 = ByteTools.fromString(value3);
        List<byte[]> bytesList = List.of(bytes1, bytes2, bytes3);

        List<Condition> conditions1 = new ArrayList<>();
        conditions1.add(new Condition("name", Condition.Compare.EQ, "Lily"));
        List<byte[]> resList = doFilter(bytesList, conditions1);
        resList.forEach(bytes -> System.out.println(ByteTools.toString(bytes)));
        System.out.println("===================================================");

        List<Condition> conditions2 = new ArrayList<>();
        conditions2.add(new Condition("name", Condition.Compare.NE, "Lily"));
        resList = doFilter(bytesList, conditions2);
        resList.forEach(bytes -> System.out.println(ByteTools.toString(bytes)));
        System.out.println("===================================================");

        List<Condition> conditions3 = new ArrayList<>();
        conditions3.add(new Condition("name", Condition.Compare.EQ, "Lily"));
        conditions3.add(new Condition("age", Condition.Compare.GT, 19));
        resList = doFilter(bytesList, conditions3);
        resList.forEach(bytes -> System.out.println(ByteTools.toString(bytes)));
        System.out.println("===================================================");

        List<Condition> conditions4 = new ArrayList<>();
        conditions4.add(new Condition("name", Condition.Compare.NE, "Lily"));
        conditions3.add(new Condition("age", Condition.Compare.GE, 19));
        resList = doFilter(bytesList, conditions4);
        resList.forEach(bytes -> System.out.println(ByteTools.toString(bytes)));
        System.out.println("===================================================");
    }

    private List<byte[]> doFilter(List<byte[]> bytesList, List<Condition> conditions) {
        if (CollectionUtils.isEmpty(conditions)) {
            return bytesList;
        }
        return bytesList.stream().filter(bytes -> {
            for (Condition condition : conditions) {
                String value = ByteTools.toString(bytes);
                if (!JsonTools.isJson(value)) {
                    continue;
                }
                Object obj = null;
                try {
                    obj = JsonTools.getValueByPath(value, condition.getParam());
                } catch (Exception ignore) {}
                boolean pass;
                if (obj instanceof String) {
                    pass = switch (condition.getCompare()) {
                        case EQ -> obj.equals(condition.getCompareValue());
                        case NE -> !obj.equals(condition.getCompareValue());
                        default -> false;
                    };
                } else if (obj instanceof Number) {
                    int compareNumber = compareNumber((Number) obj, (Number) condition.getCompareValue());
                    pass = switch (condition.getCompare()) {
                        case EQ -> compareNumber == 0;
                        case NE -> compareNumber != 0;
                        case GE -> compareNumber > 0 || compareNumber == 0;
                        case GT -> compareNumber > 0;
                        case LE -> compareNumber < 0 || compareNumber == 0;
                        case LT -> compareNumber < 0;
                    };
                } else {
                    pass = false;
                }
                if (!pass) {
                    return false;
                }
            }
            return true;
        }).collect(Collectors.toList());
    }

    /**
     * 比较两个 Number 大小
     *
     * @return 负数：n1 < n2
     * 0：n1 == n2
     * 正数：n1 > n2
     */
    private int compareNumber(Number n1, Number n2) {
        if (n1 == null && n2 == null) {
            return 0;
        }
        if (n1 == null) {
            return -1;
        }
        if (n2 == null) {
            return 1;
        }
        // 统一转成 double 比较
        return Double.compare(n1.doubleValue(), n2.doubleValue());
    }

    @Test
    @Order(2)
    void putAndSelectFirstBatch() throws Exception, NoSuchMethodException, InterruptedException {
        String dbName = "putAndSelectFirstBatchDB";
        Filer.deleteDirectory(Path.of(rootpath, dbName).toAbsolutePath().toString());
        String indexName = "putAndSelectFirstBatchIndex";
        DB db = DB.getInstance(rootpath, 10737418240L);
        db.removeDB(dbName);
        try {
            db.createDB(dbName);
            db.createIndex(dbName, IEngine.UNITY, new Index.Info(1, indexName, true, true, false));
        } catch (InstanceAlreadyExistsException e) {
            System.out.println(e.getMessage());
        }
//        int threadCount = 10000000; // 已测 插入执行耗时：14.52.055，查询执行耗时：33.37.622s，wrongCount = 0
        int threadCount = 300000;
        int startIndex = threadCount / 2 - threadCount;
        int endIndex = threadCount / 2;
        indexName = CommonTools.indexName(indexName);
        long start = System.currentTimeMillis();

        List<DocPutBatchRequestVO> batchRequestVOS = new ArrayList<>();
        Integer iTmp = null;
        for (int i = startIndex; i < endIndex; i++) {
            if (Objects.isNull(iTmp)) {
                log.info("put i = {}", i);
            }
            batchRequestVOS.add(new DocPutBatchRequestVO(indexName, String.valueOf(i), i));
            iTmp = i;
        }
        log.info("put i = {}", iTmp);
        db.put(dbName, batchRequestVOS);
        long ms = System.currentTimeMillis() - start;
        long minutes = ms / (1000 * 60);
        long seconds = (ms / 1000) % 60;
        long millis = ms % 1000;
        String timeStr = String.format("%02d.%02d.%03d", minutes, seconds, millis);
        log.info("putAndSelectFirstBatch set success! 插入执行耗时：{}", timeStr);

        try (ThreadPoolExecutor executor = new ThreadPoolExecutor(
                10,                  // 核心线程
                50,                  // 最大线程（关键！限制线程总数）
                60L, TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(200),  // 有界队列！！绝对不用无界 LinkedBlockingQueue
                new ThreadPoolExecutor.CallerRunsPolicy()  // 拒绝策略
        )) {
            AtomicLong wrongCount = new AtomicLong(0);
            CountDownLatch latchGet = new CountDownLatch(threadCount); // 计数3
            start = System.currentTimeMillis();
            Integer iTmp2 = null;
            for (int i = startIndex; i < endIndex; i++) {
                if (Objects.isNull(iTmp2)) {
                    log.info("get i = {}", i);
                }
                long finalI = i;
                String finalIndexName = indexName;
                executor.execute(() -> {
                    try {
                        DocGetResponseVO getResponseVO = db.getFirst(dbName, finalIndexName, String.valueOf(finalI));
                        if (Objects.isNull(getResponseVO)) {
                            // log.info("i = {}, | read = {}", finalI, null);
                            wrongCount.getAndAdd(1);
                        } else {
                            int value = (int) getResponseVO.getValue();
                            if (finalI != value) {
                                // log.info("i = {}, | read = {}", finalI, value);
                                wrongCount.getAndAdd(1);
                            }
                        }
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    } finally {
                        latchGet.countDown();
                    }
                });
                iTmp2 = i;
            }
            log.info("get i = {}", iTmp2);
            latchGet.await();
            ms = System.currentTimeMillis() - start;
            minutes = ms / (1000 * 60);
            seconds = (ms / 1000) % 60;
            millis = ms % 1000;
            timeStr = String.format("%02d.%02d.%03d", minutes, seconds, millis);
            log.info("putAndSelectFirstBatch check over! 查询执行耗时：{},  wrongCount = {}", timeStr, wrongCount.get());
            assert wrongCount.get() == 0 : wrongCount;
        }
    }

}
