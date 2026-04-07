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
import cn.aberic.tangduo.common.ListTools;
import cn.aberic.tangduo.common.file.Filer;
import cn.aberic.tangduo.db.common.*;
import cn.aberic.tangduo.index.Index;
import cn.aberic.tangduo.index.engine.IEngine;
import cn.aberic.tangduo.index.engine.Transaction;
import com.fasterxml.jackson.core.JsonProcessingException;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
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
    void dbRemove() throws IOException, NoSuchFieldException, NoSuchMethodException {
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
    void dbRemoveCheck() throws IOException, NoSuchFieldException, NoSuchMethodException {
        String dbName = "dbRemove";
        DB db = DB.getInstance(rootpath, 10737418240L);
        assert !db.dbExist(dbName) : db.dbExist(dbName);
    }

    @Test
    @Order(2)
    void putAndGetFirst() throws IOException, NoSuchFieldException, NoSuchMethodException {
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
        db.put(dbName, new IEngine.Content(new Transaction(1), indexName, -64424581328L, "1", ByteTools.fromInt(1)));
        assert 1 == ByteTools.toInt(db.getFirst(dbName, indexName, -64424581328L, "1")) : ByteTools.toInt(db.getFirst(dbName, indexName, -64424581328L, "1"));
        db.put(dbName, new IEngine.Content(new Transaction(2), indexName, 0, "1", ByteTools.fromInt(1)));
        assert 1 == ByteTools.toInt(db.getFirst(dbName, indexName, 0, "1")) : ByteTools.toInt(db.getFirst(dbName, indexName, 0, "1"));
        db.put(dbName, new IEngine.Content(new Transaction(3), indexName, 1, "1", ByteTools.fromInt(1)));
        assert 1 == ByteTools.toInt(db.getFirst(dbName, indexName, 1, "1")) : ByteTools.toInt(db.getFirst(dbName, indexName, 1, "1"));
        db.put(dbName, new IEngine.Content(new Transaction(4), indexName, 9223372036854775807L, "1", ByteTools.fromLong(9223372036854775807L)));
        assert 9223372036854775807L == ByteTools.toLong(db.getFirst(dbName, indexName, 9223372036854775807L, "1")) : ByteTools.toLong(db.getFirst(dbName, indexName, 9223372036854775807L, "1"));
    }

    @Test
    @Order(2)
    void putAndGetFirstAndRemove() throws IOException, NoSuchFieldException, NoSuchMethodException {
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
        db.put(dbName, new IEngine.Content(new Transaction(1), indexName, -64424581328L, "1", ByteTools.fromInt(1)));
        db.put(dbName, new IEngine.Content(new Transaction(2), indexName, 0, "1", ByteTools.fromInt(1)));
        db.put(dbName, new IEngine.Content(new Transaction(3), indexName, 1, "1", ByteTools.fromInt(1)));
        db.put(dbName, new IEngine.Content(new Transaction(4), indexName, 9223372036854775807L, "1", ByteTools.fromLong(9223372036854775807L)));

        db.remove(dbName, indexName, -64424581328L, "1");
        assert null == db.getFirst(dbName, indexName, -64424581328L, "1") : ByteTools.toInt(db.getFirst(dbName, indexName, -64424581328L, "1"));
        db.remove(dbName, indexName, 0, "1");
        assert null == db.getFirst(dbName, indexName, 0, "1") : ByteTools.toInt(db.getFirst(dbName, indexName, 0, "1"));
        db.remove(dbName, indexName, 1, "1");
        assert null == db.getFirst(dbName, indexName, 1, "1") : ByteTools.toInt(db.getFirst(dbName, indexName, 1, "1"));
        db.remove(dbName, indexName, 9223372036854775807L, "1");
        assert null == db.getFirst(dbName, indexName, 9223372036854775807L, "1") : ByteTools.toLong(db.getFirst(dbName, indexName, 9223372036854775807L, "1"));
    }

    @Test
    @Order(2)
    void putPre() throws JsonProcessingException {
        long degree = System.currentTimeMillis();
        String key = String.valueOf(degree);
        List<DB.IndexName4KeyAndDegree> list = DB.parseIndexName4KeyAndDegree(value);
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
    void putString() throws IOException, NoSuchFieldException, NoSuchMethodException {
        String dbName = "putStringDB";
        Filer.deleteDirectory(Path.of(rootpath, dbName).toAbsolutePath().toString());
        String indexName = "putStringIndex";
        DB db = DB.getInstance(rootpath, 10737418240L);
        db.removeDB(dbName);
        try {
            db.createDB(dbName);
            db.createIndex(dbName, IEngine.UNITY, new Index.Info(1, indexName, true, true, false));
        } catch (InstanceAlreadyExistsException e) {
            System.out.println(e.getMessage());
        }

        String key = "1";
        long degree = KeyHashTools.toLongKey(key);
        IEngine.Content content = new IEngine.Content(new Transaction(), CommonTools.indexName(indexName), degree, key, ByteTools.fromString(value));
        long degree4datetime = System.currentTimeMillis();
        String key4datetime = String.valueOf(degree4datetime);
        List<DB.IndexName4KeyAndDegree> list = DB.parseIndexName4KeyAndDegree(value);
        List<String> indexNameList = HanlpTools.seg(value);
        indexNameList.forEach(idxName -> {
            if (list.stream().noneMatch(in4kd -> in4kd.indexName().equals(idxName))) {
                list.add(new DB.IndexName4KeyAndDegree(idxName, key4datetime, degree4datetime, false));
            }
        });
        list.forEach(indexName4KeyAndDegree -> {
            content.addItem(indexName4KeyAndDegree.indexName(), indexName4KeyAndDegree.degree(), indexName4KeyAndDegree.key());
        });
        log.debug("content = {}", content);
        db.put(dbName, content);

        int count = content.getItems().size();

        AtomicInteger successCount = new AtomicInteger();
        AtomicInteger failedCount = new AtomicInteger();
        content.getItems().forEach(item -> {
            List<byte[]> bytesList;
            try {
                bytesList = db.get(dbName, item.getIndexName(), item.getDegree(), item.getKey());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            if (Objects.isNull(bytesList) || bytesList.isEmpty()) {
                System.out.println("failed indexName=" + item.getIndexName() + "|degree=" + item.getDegree() + "|key=" + item.getKey());
                failedCount.addAndGet(1);
            } else {
                assert ByteTools.toString(bytesList.getFirst()).equals(value) : ByteTools.toString(bytesList.getFirst());
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
    void putStringContent() throws IOException, NoSuchFieldException, NoSuchMethodException {
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
        IEngine.Content content = db.put(dbName, indexName, key, value);
        log.debug("content = {}", content);

        int count = content.getItems().size();

        AtomicInteger successCount = new AtomicInteger();
        AtomicInteger failedCount = new AtomicInteger();
        content.getItems().forEach(item -> {
            List<byte[]> bytesList;
            try {
                bytesList = db.get(dbName, item.getIndexName(), item.getDegree(), item.getKey());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            if (Objects.isNull(bytesList) || bytesList.isEmpty()) {
                System.out.println("failed indexName=" + item.getIndexName() + "|degree=" + item.getDegree() + "|key=" + item.getKey());
                failedCount.addAndGet(1);
            } else {
                String valueFromDoc = bytesToDocItem(bytesList.getFirst()).getContent();
                assert valueFromDoc.equals(value) : valueFromDoc;
                System.out.println("success indexName=" + item.getIndexName() + "|degree=" + item.getDegree() + "|key=" + item.getKey());
                successCount.addAndGet(1);
            }
        });
        System.out.println("successCount = " + successCount.get() + ", failedCount = " + failedCount.get());
        assert count == successCount.get();
        assert 0 == failedCount.get();
    }

    private Bm25Tools.DocItem bytesToDocItem(byte[] value) {
        String valueStr = ByteTools.toString(value);
        String[] segList = valueStr.split("###@@@###");
        return new Bm25Tools.DocItem(segList[0], segList[1], ListTools.fromString(segList[2]));
    }

    @Test
    @Order(2)
    void putStringDefaultIndexContent() throws IOException, NoSuchFieldException {
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
        IEngine.Content content = db.put(dbName, key, value);
        log.debug("content = {}", content);

        int count = content.getItems().size();

        AtomicInteger successCount = new AtomicInteger();
        AtomicInteger failedCount = new AtomicInteger();
        content.getItems().forEach(item -> {
            List<byte[]> bytesList;
            try {
                bytesList = db.get(dbName, item.getIndexName(), item.getDegree(), item.getKey());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            if (Objects.isNull(bytesList) || bytesList.isEmpty()) {
                System.out.println("failed indexName=" + item.getIndexName() + "|degree=" + item.getDegree() + "|key=" + item.getKey());
                failedCount.addAndGet(1);
            } else {
                String valueFromDoc = bytesToDocItem(bytesList.getFirst()).getContent();
                assert valueFromDoc.equals(value) : valueFromDoc;
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
    void putStringDefaultIndexAndKeyContent() throws IOException, NoSuchFieldException {
        String dbName = "putStringDefaultIndexAndKeyContentDB";
        Filer.deleteDirectory(Path.of(rootpath, dbName).toAbsolutePath().toString());
        DB db = DB.getInstance(rootpath, 10737418240L);
        db.removeDB(dbName);
        try {
            db.createDB(dbName);
        } catch (InstanceAlreadyExistsException e) {
            System.out.println(e.getMessage());
        }

        IEngine.Content content = db.put(dbName, value);
        log.debug("content = {}", content);

        int count = content.getItems().size();

        AtomicInteger successCount = new AtomicInteger();
        AtomicInteger failedCount = new AtomicInteger();
        content.getItems().forEach(item -> {
            List<byte[]> bytesList;
            try {
                bytesList = db.get(dbName, item.getIndexName(), item.getDegree(), item.getKey());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            if (Objects.isNull(bytesList) || bytesList.isEmpty()) {
                System.out.println("failed indexName=" + item.getIndexName() + "|degree=" + item.getDegree() + "|key=" + item.getKey());
                failedCount.addAndGet(1);
            } else {
                String valueFromDoc = bytesToDocItem(bytesList.getFirst()).getContent();
                assert valueFromDoc.equals(value) : valueFromDoc;
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
    void putStringDefaultDatabaseIndexAndKeyContent() throws IOException, NoSuchFieldException {
        Filer.deleteDirectory(Path.of(rootpath, DB.DATABASE_NAME_DEFAULT).toAbsolutePath().toString());
        DB db = DB.getInstance(rootpath, 10737418240L);
        db.removeDB(DB.DATABASE_NAME_DEFAULT);

        IEngine.Content content = db.put(value);
        log.debug("content = {}", content);

        int count = content.getItems().size();

        AtomicInteger successCount = new AtomicInteger();
        AtomicInteger failedCount = new AtomicInteger();
        content.getItems().forEach(item -> {
            List<byte[]> bytesList;
            try {
                bytesList = db.get(DB.DATABASE_NAME_DEFAULT, item.getIndexName(), item.getDegree(), item.getKey());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            if (Objects.isNull(bytesList) || bytesList.isEmpty()) {
                System.out.println("failed indexName=" + item.getIndexName() + "|degree=" + item.getDegree() + "|key=" + item.getKey());
                failedCount.addAndGet(1);
            } else {
                String valueFromDoc = bytesToDocItem(bytesList.getFirst()).getContent();
                assert valueFromDoc.equals(value) : valueFromDoc;
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
    void putAndGetFirstTimesAsync() throws IOException, NoSuchFieldException, NoSuchMethodException, InterruptedException {
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
        int threadCount = 10000;
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
                        db.put(dbName, new IEngine.Content(new Transaction(finalI), finalIndexName, finalI, String.valueOf(finalI), ByteTools.fromInt(finalI)));
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
            log.debug("setAndGetTimes set success! 插入执行耗时：{}", timeStr);


            AtomicLong wrongCount = new AtomicLong(0);
            CountDownLatch latchGet = new CountDownLatch(threadCount); // 计数3
            start = System.currentTimeMillis();
            for (int i = startIndex; i < threadCount; i++) {
                int finalI = i;
                String finalIndexName = indexName;
                executor.execute(() -> {
                    try {
                        byte[] bytes = db.getFirst(dbName, finalIndexName, finalI, String.valueOf(finalI));
                        if (finalI != ByteTools.toInt(Objects.isNull(bytes) ? new byte[4] : bytes)) {
                            log.debug("i = {}, | read = {}", finalI, ByteTools.toInt(Objects.isNull(bytes) ? new byte[4] : bytes));
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
            log.debug("setAndGetTimes check over! 查询执行耗时：{},  wrongCount = {}", timeStr, wrongCount.get());
            assert wrongCount.get() == 0 : wrongCount;
        }
    }

    @Test
    @Order(3)
    void select() throws IOException, NoSuchFieldException {
        String dbName = "putAndGetFirstTimesAsyncDB";
        String indexName = "putAndGetFirstTimesAsyncIndex";
        DB db = DB.getInstance(rootpath, 10737418240L);
        indexName = CommonTools.indexName(indexName);
        long wrongCount = 0;
        for (int i = -500; i < 500; i++) {
            byte[] bytes = db.getFirst(dbName, indexName, i, String.valueOf(i));
            if (i != ByteTools.toInt(Objects.isNull(bytes) ? new byte[4] : bytes)) {
                log.debug("i = {}, | read = {}", i, ByteTools.toInt(Objects.isNull(bytes) ? new byte[4] : bytes));
                wrongCount++;
            }
        }
        log.debug("setAndGetTimes check over! wrongCount =  {}", wrongCount);

        IEngine.Search search = new IEngine.Search(indexName, -500, 500, true, true, 100, true);
        List<byte[]> bytesList = db.select(dbName, search);
        System.out.println("list size = " + bytesList.size());
        for (int i = 0; i < bytesList.size(); i++) {
            assert ByteTools.toInt(bytesList.get(i)) == -500 + i : ByteTools.toInt(bytesList.get(i)) + " != " + (-500 + i);
        }

        search = new IEngine.Search(indexName, -500, 500, false, false, 100, true);
        bytesList = db.select(dbName, search);
        System.out.println("list size = " + bytesList.size());
        for (int i = 0; i < bytesList.size(); i++) {
            assert ByteTools.toInt(bytesList.get(i)) == -499 + i : ByteTools.toInt(bytesList.get(i)) + " != " + (-499 + i);
        }

        search = new IEngine.Search(indexName, -500, 500, true, true, 100, false);
        bytesList = db.select(dbName, search);
        System.out.println("list size = " + bytesList.size());
        for (int i = 0; i < bytesList.size(); i++) {
            assert ByteTools.toInt(bytesList.get(i)) == 500 - i : ByteTools.toInt(bytesList.get(i)) + " != " + (500 - i);
        }

        search = new IEngine.Search(indexName, -500, 500, false, false, 100, false);
        bytesList = db.select(dbName, search);
        System.out.println("list size = " + bytesList.size());
        for (int i = 0; i < bytesList.size(); i++) {
            assert ByteTools.toInt(bytesList.get(i)) == 499 - i : ByteTools.toInt(bytesList.get(i)) + " != " + (499 - i);
        }

        search = new IEngine.Search(indexName, -50, 50, true, true, 100, true);
        bytesList = db.select(dbName, search);
        System.out.println("list size = " + bytesList.size());
        for (int i = 0; i < bytesList.size(); i++) {
            assert ByteTools.toInt(bytesList.get(i)) == -50 + i : ByteTools.toInt(bytesList.get(i)) + " != " + (-50 + i);
        }

        search = new IEngine.Search(indexName, -50, 50, false, false, 100, true);
        bytesList = db.select(dbName, search);
        System.out.println("list size = " + bytesList.size());
        for (int i = 0; i < bytesList.size(); i++) {
            System.out.print(ByteTools.toInt(bytesList.get(i)) + " ");
            assert ByteTools.toInt(bytesList.get(i)) == -49 + i : ByteTools.toInt(bytesList.get(i)) + " != " + (-49 + i);
        }
        System.out.println();

        search = new IEngine.Search(indexName, -50, 50, false, false, 100, true, bsList -> {
            List<byte[]> bl = new ArrayList<>();
            for (byte[] bytes : bsList) {
                if (0 != ByteTools.toInt(bytes)) {
                    bl.add(bytes);
                }
            }
            return bl;
        });
        bytesList = db.select(dbName, search);
        System.out.println("list size = " + bytesList.size());
        for (int i = 0; i < bytesList.size(); i++) {
            if (i < 49) {
                System.out.print(ByteTools.toInt(bytesList.get(i)) + " ");
                assert ByteTools.toInt(bytesList.get(i)) == -49 + i : ByteTools.toInt(bytesList.get(i)) + " != " + (-49 + i);
            } else {
                System.out.print(ByteTools.toInt(bytesList.get(i)) + " ");
                assert ByteTools.toInt(bytesList.get(i)) == -48 + i : ByteTools.toInt(bytesList.get(i)) + " != " + (-48 + i);
            }
        }
        System.out.println();
    }

    @Test
    @Order(2)
    void putAndGetFirstInSameIndex0() throws IOException, NoSuchFieldException, NoSuchMethodException {
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
        db.put(dbName, new IEngine.Content(new Transaction(1), indexName, -64424581328L, "1", ByteTools.fromInt(1)));
        assert 1 == ByteTools.toInt(db.getFirst(dbName, indexName, -64424581328L, "1")) : ByteTools.toInt(db.getFirst(dbName, indexName, -64424581328L, "1"));
        db.put(dbName, new IEngine.Content(new Transaction(2), indexName, 0, "1", ByteTools.fromInt(1)));
        assert 1 == ByteTools.toInt(db.getFirst(dbName, indexName, 0, "1")) : ByteTools.toInt(db.getFirst(dbName, indexName, 0, "1"));
        db.put(dbName, new IEngine.Content(new Transaction(3), indexName, 1, "1", ByteTools.fromInt(1)));
        assert 1 == ByteTools.toInt(db.getFirst(dbName, indexName, 1, "1")) : ByteTools.toInt(db.getFirst(dbName, indexName, 1, "1"));
        db.put(dbName, new IEngine.Content(new Transaction(4), indexName, 9223372036854775807L, "1", ByteTools.fromLong(9223372036854775807L)));
        assert 9223372036854775807L == ByteTools.toLong(db.getFirst(dbName, indexName, 9223372036854775807L, "1")) : ByteTools.toLong(db.getFirst(dbName, indexName, 9223372036854775807L, "1"));
    }

    @Test
    @Order(3)
    void putAndGetFirstInSameIndex1() throws IOException, NoSuchFieldException, NoSuchMethodException {
        String dbName = "putAndGetFirstInSameIndexDB";
        String indexName = "putAndGetFirstInSameIndex1Index";
        DB db = DB.getInstance(rootpath, 10737418240L);
        try {
            db.createIndex(dbName, IEngine.UNITY, new Index.Info(1, indexName, true, true, false));
        } catch (InstanceAlreadyExistsException e) {
            System.out.println(e.getMessage());
        }
        indexName = CommonTools.indexName(indexName);
        db.put(dbName, new IEngine.Content(new Transaction(1), indexName, -64424581328L, "1", ByteTools.fromInt(1)));
        assert 1 == ByteTools.toInt(db.getFirst(dbName, indexName, -64424581328L, "1")) : ByteTools.toInt(db.getFirst(dbName, indexName, -64424581328L, "1"));
        db.put(dbName, new IEngine.Content(new Transaction(2), indexName, 0, "1", ByteTools.fromInt(1)));
        assert 1 == ByteTools.toInt(db.getFirst(dbName, indexName, 0, "1")) : ByteTools.toInt(db.getFirst(dbName, indexName, 0, "1"));
        db.put(dbName, new IEngine.Content(new Transaction(3), indexName, 1, "1", ByteTools.fromInt(1)));
        assert 1 == ByteTools.toInt(db.getFirst(dbName, indexName, 1, "1")) : ByteTools.toInt(db.getFirst(dbName, indexName, 1, "1"));
        db.put(dbName, new IEngine.Content(new Transaction(4), indexName, 9223372036854775807L, "1", ByteTools.fromLong(9223372036854775807L)));
        assert 9223372036854775807L == ByteTools.toLong(db.getFirst(dbName, indexName, 9223372036854775807L, "1")) : ByteTools.toLong(db.getFirst(dbName, indexName, 9223372036854775807L, "1"));
    }

    @Test
    @Order(4)
    void putAndGetFirstInSameIndex2() throws IOException, NoSuchFieldException {
        String dbName = "putAndGetFirstInSameIndexDB";
        String indexName0 = "putAndGetFirstInSameIndex0Index";
        String indexName1 = "putAndGetFirstInSameIndex1Index";
        DB db = DB.getInstance(rootpath, 10737418240L);
        indexName0 = CommonTools.indexName(indexName0);
        indexName1 = CommonTools.indexName(indexName1);
        assert 1 == ByteTools.toInt(db.getFirst(dbName, indexName0, -64424581328L, "1")) : ByteTools.toInt(db.getFirst(dbName, indexName0, -64424581328L, "1"));
        assert 1 == ByteTools.toInt(db.getFirst(dbName, indexName0, 0, "1")) : ByteTools.toInt(db.getFirst(dbName, indexName0, 0, "1"));
        assert 1 == ByteTools.toInt(db.getFirst(dbName, indexName0, 1, "1")) : ByteTools.toInt(db.getFirst(dbName, indexName0, 1, "1"));
        assert 9223372036854775807L == ByteTools.toLong(db.getFirst(dbName, indexName0, 9223372036854775807L, "1")) : ByteTools.toLong(db.getFirst(dbName, indexName0, 9223372036854775807L, "1"));
        assert 1 == ByteTools.toInt(db.getFirst(dbName, indexName1, -64424581328L, "1")) : ByteTools.toInt(db.getFirst(dbName, indexName1, -64424581328L, "1"));
        assert 1 == ByteTools.toInt(db.getFirst(dbName, indexName1, 0, "1")) : ByteTools.toInt(db.getFirst(dbName, indexName1, 0, "1"));
        assert 1 == ByteTools.toInt(db.getFirst(dbName, indexName1, 1, "1")) : ByteTools.toInt(db.getFirst(dbName, indexName1, 1, "1"));
        assert 9223372036854775807L == ByteTools.toLong(db.getFirst(dbName, indexName1, 9223372036854775807L, "1")) : ByteTools.toLong(db.getFirst(dbName, indexName1, 9223372036854775807L, "1"));
    }

    @Test
    @Order(2)
    void deleteList() throws IOException, NoSuchFieldException {
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
            db.put(dbName, new IEngine.Content(new Transaction(i), indexName, i, String.valueOf(i), ByteTools.fromInt(i)));
        }
        log.debug("setAndGetTimes set success!");
        for (int i = -5000; i < count; i++) {
            assert i == ByteTools.toInt(db.getFirst(dbName, indexName, i, String.valueOf(i))) : i;
        }
        log.debug("setAndGetTimes check success!");

        IEngine.Search search = new IEngine.Search(indexName, -100, 100, false, false, true);
        List<byte[]> bytesList = db.delete(dbName, search);
        assert 199 == bytesList.size() : "199 != " + bytesList.size(); // (-99 —— 0) + (1 —— 99) = 199
        for (int i = 0; i < bytesList.size(); i++) {
            assert (i - 99) == ByteTools.toInt(bytesList.get(i)) : (i - 99) + " != " + ByteTools.toInt(bytesList.get(i)); // (-99 —— 0) + (1 —— 99) = 199
        }
        search = new IEngine.Search(indexName, -120, 150, false, false, 100, true);
        bytesList = db.select(dbName, search); // -99 —— 99 上一轮已删
        assert 70 == bytesList.size() : "70 != " + bytesList.size(); // -120——150总计271个数字，减去上一轮的199，还剩70个数字
        for (int i = 0; i < bytesList.size(); i++) {
            // (-99 —— 0) + (1 —— 99) 因获取不到，被过滤掉
            if (i < 20) { // 即 -120 —— -100 是可查到数字，但不包含 -120
                assert (i - 119) == ByteTools.toInt(bytesList.get(i)) : (i - 119) + " != " + ByteTools.toInt(bytesList.get(i));
            } else { // 100 —— 150 是可查到数字，但不包含 150，20以后从100开始计数
                assert (i + 80) == ByteTools.toInt(bytesList.get(i)) : (i + 80) + " != " + ByteTools.toInt(bytesList.get(i));
            }
        }
    }

}
