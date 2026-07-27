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

package cn.aberic.tangduo.index.engine.unity;

import cn.aberic.tangduo.common.ByteTools;
import cn.aberic.tangduo.common.JsonTools;
import cn.aberic.tangduo.common.file.Filer;
import cn.aberic.tangduo.index.Index;
import cn.aberic.tangduo.index.engine.IEngine;
import cn.aberic.tangduo.index.engine.Transaction;
import cn.aberic.tangduo.index.engine.entity.*;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.util.CollectionUtils;

import javax.management.InstanceAlreadyExistsException;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Slf4j
public class IndexTests {

    final String rootpath = "tmp/index";
    /** 数据文件大小阈值，单位byte，默认1MB */
    private static final long DATA_FILE_DEFAULT_SIZE = 1048576L;

    @Test
    @Order(1)
    void init() {
        Filer.deleteDirectory(Path.of(rootpath).toAbsolutePath().toString());
    }

    @Test
    @Order(2)
    void create() throws IOException, NoSuchFieldException, NoSuchMethodException {
        String indexName = "create";
        Index index = new Index(rootpath, DATA_FILE_DEFAULT_SIZE);
        index.removeIndex(indexName);
        try {
            index.createIndex(IEngine.UNITY, new Index.Info(1, indexName, true, true, false));
        } catch (InstanceAlreadyExistsException e) {
            System.out.println(e.getMessage());
        }
    }

    @Test
    @Order(3)
    void putAndGetFirstOne() throws IOException, NoSuchFieldException {
        String indexName = "putAndGetFirstOne";
        Index index = new Index(rootpath, DATA_FILE_DEFAULT_SIZE);
        index.removeIndex(indexName);

        index.put(new Content(new Transaction(1), indexName, 1, "1", ByteTools.fromInt(1)));
        assert 1 == ByteTools.toInt(index.getFirst(indexName, 1, "1")) : ByteTools.toInt(index.getFirst(indexName, 1, "1"));
    }

    @Test
    @Order(3)
    void putAndGetFirst() throws IOException, NoSuchFieldException {
        String indexName = "putAndGetFirst";
        Index index = new Index(rootpath, DATA_FILE_DEFAULT_SIZE);
        index.removeIndex(indexName);

        index.put(new Content(new Transaction(), indexName, -64424581328L, "1", ByteTools.fromInt(1)));
        assert 1 == ByteTools.toInt(index.getFirst(indexName, -64424581328L, "1")) : ByteTools.toInt(index.getFirst(indexName, -64424581328L, "1"));
        index.put(new Content(new Transaction(), indexName, 0, "1", ByteTools.fromInt(1)));
        assert 1 == ByteTools.toInt(index.getFirst(indexName, 0, "1")) : ByteTools.toInt(index.getFirst(indexName, 0, "1"));
        index.put(new Content(new Transaction(), indexName, 1, "1", ByteTools.fromInt(1)));
        assert 1 == ByteTools.toInt(index.getLast(indexName, 1, "1")) : ByteTools.toInt(index.getLast(indexName, 1, "1"));
        index.put(new Content(new Transaction(), indexName, 9223372036854775807L, "1", ByteTools.fromLong(9223372036854775807L)));
        assert 9223372036854775807L == ByteTools.toLong(index.getFirst(indexName, 9223372036854775807L, "1")) : ByteTools.toLong(index.getFirst(indexName, 9223372036854775807L, "1"));
    }

    @Test
    @Order(3)
    void putAndGetFirstAndRemove() throws IOException, NoSuchFieldException {
        String indexName = "putAndGetFirstAndRemove";
        Index index = new Index(rootpath, DATA_FILE_DEFAULT_SIZE);
        index.removeIndex(indexName);

        index.put(new Content(new Transaction(1), indexName, -64424581328L, "1", ByteTools.fromInt(1)));
        index.put(new Content(new Transaction(2), indexName, 0, "1", ByteTools.fromInt(1)));
        index.put(new Content(new Transaction(3), indexName, 1, "1", ByteTools.fromInt(1)));
        index.put(new Content(new Transaction(4), indexName, 9223372036854775807L, "1", ByteTools.fromLong(9223372036854775807L)));

        index.remove(indexName, -64424581328L, "1");
        assert null == index.getFirst(indexName, -64424581328L, "1") : Arrays.toString(index.getFirst(indexName, -64424581328L, "1"));
        index.remove(indexName, 0, "1");
        assert null == index.getFirst(indexName, 0, "1") : Arrays.toString(index.getFirst(indexName, 0, "1"));
        index.remove(indexName, 1, "1");
        assert null == index.getFirst(indexName, 1, "1") : Arrays.toString(index.getFirst(indexName, 1, "1"));
        index.remove(indexName, 9223372036854775807L, "1");
        assert null == index.getFirst(indexName, 9223372036854775807L, "1") : Arrays.toString(index.getFirst(indexName, 9223372036854775807L, "1"));
    }

    @Test
    void divTest() {
        System.out.println(9223372036854775807L - 9223372032559808512L);
    }

    @Test
    void pathTest() {
        Path path = Paths.get("tmp/index/unity/setAndGet/0_4294967295.idx");
        // 提取文件名（不含后缀）
        String fileName = path.getFileName().toString();
        System.out.println("fileName = " + fileName);
        // 0_4294967296 或 neg_9223371968135299072_9223371972430266367.idx
        String baseName = fileName.substring(0, fileName.lastIndexOf("."));
        System.out.println("baseName = " + baseName);
    }

    private static final long DEGREE = 4294967296L;

    private long reDegree(long degree) {
        if (degree >= DEGREE) {
            // degree = 64424581328      ——     4294967296 * 15 = 64424509440
            long div = degree / DEGREE; // 15    ——    15.000016737729311
            return degree - DEGREE * div;
        } else if (degree < 0) {
            degree = degree + Long.MAX_VALUE; // 先变为正数
            if (degree < 0) {
                throw new IndexOutOfBoundsException("degree min is -" + Long.MAX_VALUE);
            }
            return reDegree(degree);
        } else {
            return degree;
        }
    }

    @Test
    void reDegreeTest() {
        System.out.println(reDegree(429496729)); // 429496729
        System.out.println(reDegree(64424581328L)); // 71888
        System.out.println(reDegree(-429496729)); // 3865470566
        System.out.println(reDegree(-64424581328L)); // 4294895407
    }

    private String getFirstDegreeInterval(long degree) {
        if (degree >= 0) {
            // degree = 64424581328      ——     4294967296 * 15 = 64424509440
            long div = degree / DEGREE; // 15    ——    15.000016737729311
            String degreeIntervalStart = String.valueOf(DEGREE * div); // 64424509440
            String degreeIntervalEnd = String.valueOf(DEGREE * (div + 1) - 1); // 68719476736
            return degreeIntervalStart + "_" + degreeIntervalEnd; // 64424509440_68719476736
        } else { // 18446744073709551616
            degree = degree + Long.MAX_VALUE; // 先变为正数
            if (degree < 0) {
                throw new IndexOutOfBoundsException("degree min is -" + Long.MAX_VALUE);
            }
            long div = degree / DEGREE;
            String degreeIntervalStart = String.valueOf(DEGREE * div);
            String degreeIntervalEnd = String.valueOf(DEGREE * (div + 1) - 1);
            return "neg_" + degreeIntervalStart + "_" + degreeIntervalEnd;
        }
    }

    @Test
    void getFirstDegreeIntervalTest() {
        System.out.println(getFirstDegreeInterval(4294967296L)); // 4294967296_8589934591
        System.out.println(getFirstDegreeInterval(64424581328L)); // 64424509440_68719476735
        System.out.println(getFirstDegreeInterval(-429496729)); // neg_9223372032559808512_9223372036854775807
        System.out.println(getFirstDegreeInterval(-64424581328L)); // neg_9223371968135299072_9223371972430266367
    }

    @Test
    @Order(3)
    void resetAndGetFirst() throws IOException, NoSuchFieldException {
        String indexName = "resetAndGetFirst";
        Index index = new Index(rootpath, DATA_FILE_DEFAULT_SIZE);
        index.removeIndex(indexName);

        index.put(new Content(new Transaction(), indexName, 1, "1", ByteTools.fromInt(1)));
        assert 1 == ByteTools.toInt(index.getFirst(indexName, 1, "1")) : "1 !=" + ByteTools.toInt(index.getFirst(indexName, 1, "1"));
        index.put(new Content(new Transaction(), indexName, 1, "1", ByteTools.fromInt(2)));
        assert 2 == ByteTools.toInt(index.getLast(indexName, 1, "1")) : "2 !=" + ByteTools.toInt(index.getFirst(indexName, 1, "1"));
    }

    @Test
    @Order(3)
    void putAndGetFirstTimes() throws IOException, NoSuchFieldException {
        String indexName = "putAndGetFirstTimes";
        Index index = new Index(rootpath, DATA_FILE_DEFAULT_SIZE);
        index.removeIndex(indexName);

        int count = 100000;
        for (int i = 0; i < count; i++) {
            index.put(new Content(new Transaction(i), indexName, i, String.valueOf(i), ByteTools.fromInt(i)));
        }
        log.info("putAndGetFirstTimes set success!");
        for (int i = 0; i < count; i++) {
            assert i == ByteTools.toInt(index.getFirst(indexName, i, String.valueOf(i))) : i;
        }
        log.info("putAndGetFirstTimes check success!");
    }

    @Test
    @Order(3)
    void putAndGetFirstTimesAsync() throws IOException, NoSuchFieldException, InterruptedException {
        String indexName = "putAndGetFirstTimesAsync";
        Index index = new Index(rootpath, DATA_FILE_DEFAULT_SIZE);
        index.removeIndex(indexName);

        int threadCount = 100000; // 10000000 1小时16分钟
        int startIndex = threadCount / 2 - threadCount;
        CountDownLatch latch = new CountDownLatch(threadCount); // 计数3

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
                executor.execute(() -> {
                    try {
                        index.put(new Content(new Transaction(finalI), indexName, finalI, String.valueOf(finalI), ByteTools.fromInt(finalI)));
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    } finally {
                        latch.countDown();
                    }
                });
            }
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
        try (ThreadPoolExecutor executor = new ThreadPoolExecutor(
                10,                  // 核心线程
                50,                  // 最大线程（关键！限制线程总数）
                60L, TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(200),  // 有界队列！！绝对不用无界 LinkedBlockingQueue
                new ThreadPoolExecutor.CallerRunsPolicy()  // 拒绝策略
        )) {
            for (int i = startIndex; i < threadCount; i++) {
                int finalI = i;
                executor.execute(() -> {
                    try {
                        byte[] bytes = index.getFirst(indexName, finalI, String.valueOf(finalI));
                        if (finalI != ByteTools.toInt(Objects.isNull(bytes) ? new byte[4] : bytes)) {
                            log.info("i = {}, | read = {}", finalI, ByteTools.toInt(Objects.isNull(bytes) ? new byte[4] : bytes));
                            wrongCount.getAndAdd(1);
                        }
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    } finally {
                        latchGet.countDown();
                    }
                });
            }
        }
        ms = System.currentTimeMillis() - start;
        minutes = ms / (1000 * 60);
        seconds = (ms / 1000) % 60;
        millis = ms % 1000;
        timeStr = String.format("%02d.%02d.%03d", minutes, seconds, millis);
        log.info("setAndGetTimes check over! 查询执行耗时：{},  wrongCount = {}", timeStr, wrongCount.get());
        assert wrongCount.get() == 0 : wrongCount;
    }

    @Test
    @Order(4)
    void select() throws IOException, NoSuchFieldException {
        String indexName = "putAndGetFirstTimesAsync";
        Index index = new Index(rootpath, DATA_FILE_DEFAULT_SIZE);
        long wrongCount = 0;
        for (int i = -500; i < 500; i++) {
            byte[] bytes = index.getFirst(indexName, i, String.valueOf(i));
            if (i != ByteTools.toInt(Objects.isNull(bytes) ? new byte[4] : bytes)) {
                log.info("select i = {}, | read = {}", i, ByteTools.toInt(Objects.isNull(bytes) ? new byte[4] : bytes));
                wrongCount++;
            }
        }
        log.info("select check over! wrongCount =  {}", wrongCount);

        List<byte[]> bytesList;

        Hit hit1 = new Hit(indexName, true);
        hit1.setArea(new Area(-500, 500, true, true));
        Select select1 = new Select(null, 100, false, hit1, null);
        bytesList = index.select(select1);
        log.info("select hit1 list size =  {}", bytesList.size());
        for (int i = 0; i < bytesList.size(); i++) {
            System.out.print(ByteTools.toInt(bytesList.get(i)) + " ");
            assert ByteTools.toInt(bytesList.get(i)) == -500 + i : ByteTools.toInt(bytesList.get(i)) + " != " + (-500 + i);
        }

        Hit hit2 = new Hit(indexName, true);
        hit2.setArea(new Area(-500, 500, false, false));
        Select select2 = new Select(null, 100, false, hit2, null);
        bytesList = index.select(select2);
        log.info("select hit2 list size =  {}", bytesList.size());
        for (int i = 0; i < bytesList.size(); i++) {
            assert ByteTools.toInt(bytesList.get(i)) == -499 + i : ByteTools.toInt(bytesList.get(i)) + " != " + (-499 + i);
        }


        Hit hit3 = new Hit(indexName, false);
        hit3.setArea(new Area(-500, 500, true, true));
        Select select3 = new Select(null, 100, false, hit3, null);
        bytesList = index.select(select3);
        log.info("select hit3 list size =  {}", bytesList.size());
        for (int i = 0; i < bytesList.size(); i++) {
            assert ByteTools.toInt(bytesList.get(i)) == 500 - i : ByteTools.toInt(bytesList.get(i)) + " != " + (500 - i);
        }

        Hit hit4 = new Hit(indexName, false);
        hit4.setArea(new Area(-500, 500, false, false));
        Select select4 = new Select(null, 100, false, hit4, null);
        bytesList = index.select(select4);
        log.info("select hit4 list size =  {}", bytesList.size());
        for (int i = 0; i < bytesList.size(); i++) {
            assert ByteTools.toInt(bytesList.get(i)) == 499 - i : ByteTools.toInt(bytesList.get(i)) + " != " + (499 - i);
        }

        Hit hit5 = new Hit(indexName, true);
        hit5.setArea(new Area(-50, 50, true, true));
        Select select5 = new Select(null, 100, false, hit5, null);
        bytesList = index.select(select5);
        log.info("select hit5 list size =  {}", bytesList.size());
        for (int i = 0; i < bytesList.size(); i++) {
            assert ByteTools.toInt(bytesList.get(i)) == -50 + i : ByteTools.toInt(bytesList.get(i)) + " != " + (-50 + i);
        }

        Hit hit6 = new Hit(indexName, true);
        hit6.setArea(new Area(-50, 50, false, false));
        Select select6 = new Select(null, 100, false, hit6, null);
        bytesList = index.select(select6);
        log.info("select hit6 list size =  {}", bytesList.size());
        for (int i = 0; i < bytesList.size(); i++) {
            System.out.print(ByteTools.toInt(bytesList.get(i)) + " ");
            assert ByteTools.toInt(bytesList.get(i)) == -49 + i : ByteTools.toInt(bytesList.get(i)) + " != " + (-49 + i);
        }
        System.out.println();

        Hit hit7 = new Hit(indexName, true);
        hit7.setArea(new Area(-50, 50, false, false));
        Select select7 = new Select(null, 100, true, hit7, (bsList, conditionList) -> {
            List<byte[]> bl = new ArrayList<>();
            for (byte[] bytes : bsList) {
                if (0 != ByteTools.toInt(bytes)) {
                    bl.add(bytes);
                }
            }
            return bl;
        });
        bytesList = index.select(select7);
        log.info("select hit7 list size =  {}", bytesList.size());
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
    @Order(4)
    void selectOld() throws IOException, NoSuchFieldException {
        String indexName = "putAndGetFirstTimesAsync";
        Index index = new Index(rootpath, DATA_FILE_DEFAULT_SIZE);
        long wrongCount = 0;
        for (int i = -500; i < 500; i++) {
            byte[] bytes = index.getFirst(indexName, i, String.valueOf(i));
            if (i != ByteTools.toInt(Objects.isNull(bytes) ? new byte[4] : bytes)) {
                log.info("select i = {}, | read = {}", i, ByteTools.toInt(Objects.isNull(bytes) ? new byte[4] : bytes));
                wrongCount++;
            }
        }
        log.info("select check over! wrongCount =  {}", wrongCount);

        Select select = new Select(indexName, -500, 500, true, true, 100, true);
        List<byte[]> bytesList = index.select(select);
        System.out.println("list size = " + bytesList.size());
        for (int i = 0; i < bytesList.size(); i++) {
            assert ByteTools.toInt(bytesList.get(i)) == -500 + i : ByteTools.toInt(bytesList.get(i)) + " != " + (-500 + i);
        }

        select = new Select(indexName, -500, 500, false, false, 100, true);
        bytesList = index.select(select);
        System.out.println("list size = " + bytesList.size());
        for (int i = 0; i < bytesList.size(); i++) {
            assert ByteTools.toInt(bytesList.get(i)) == -499 + i : ByteTools.toInt(bytesList.get(i)) + " != " + (-499 + i);
        }

        select = new Select(indexName, -500, 500, true, true, 100, false);
        bytesList = index.select(select);
        System.out.println("list size = " + bytesList.size());
        for (int i = 0; i < bytesList.size(); i++) {
            assert ByteTools.toInt(bytesList.get(i)) == 500 - i : ByteTools.toInt(bytesList.get(i)) + " != " + (500 - i);
        }

        select = new Select(indexName, -500, 500, false, false, 100, false);
        bytesList = index.select(select);
        System.out.println("list size = " + bytesList.size());
        for (int i = 0; i < bytesList.size(); i++) {
            assert ByteTools.toInt(bytesList.get(i)) == 499 - i : ByteTools.toInt(bytesList.get(i)) + " != " + (499 - i);
        }

        select = new Select(indexName, -50, 50, true, true, 100, true);
        bytesList = index.select(select);
        System.out.println("list size = " + bytesList.size());
        for (int i = 0; i < bytesList.size(); i++) {
            assert ByteTools.toInt(bytesList.get(i)) == -50 + i : ByteTools.toInt(bytesList.get(i)) + " != " + (-50 + i);
        }

        select = new Select(indexName, -50, 50, false, false, 100, true);
        bytesList = index.select(select);
        System.out.println("list size = " + bytesList.size());
        for (int i = 0; i < bytesList.size(); i++) {
            System.out.print(ByteTools.toInt(bytesList.get(i)) + " ");
            assert ByteTools.toInt(bytesList.get(i)) == -49 + i : ByteTools.toInt(bytesList.get(i)) + " != " + (-49 + i);
        }
        System.out.println();

        Hit hit = new Hit(indexName, true);
        hit.setArea(new Area(-50, 50, false, false));
        select = new Select(null, 100, true, hit, (bsList, conditionList) -> {
            List<byte[]> bl = new ArrayList<>();
            for (byte[] bytes : bsList) {
                if (0 != ByteTools.toInt(bytes)) {
                    bl.add(bytes);
                }
            }
            return bl;
        });
        bytesList = index.select(select);
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
    @Order(3)
    void putAndGetFirstBatch() throws IOException, NoSuchFieldException {
        String indexName = "putAndGetFirstBatch";
        Index index = new Index(rootpath, DATA_FILE_DEFAULT_SIZE);
        index.removeIndex(indexName);

        int threadCount = 300000;
        int startIndex = threadCount / 2 - threadCount;
        int endIndex = threadCount / 2;

        List<Content> contentList = new ArrayList<>();
        for (int i = startIndex; i < endIndex; i++) {
            contentList.add(new Content(indexName, i, String.valueOf(i), ByteTools.fromInt(i)));
        }
        index.put(contentList);
        log.info("putAndGetFirstBatch set success!");

        AtomicLong wrongCount = new AtomicLong(0);
        CountDownLatch latchGet = new CountDownLatch(threadCount); // 计数3
        try (ThreadPoolExecutor executor = new ThreadPoolExecutor(
                10,                  // 核心线程
                50,                  // 最大线程（关键！限制线程总数）
                60L, TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(200),  // 有界队列！！绝对不用无界 LinkedBlockingQueue
                new ThreadPoolExecutor.CallerRunsPolicy()  // 拒绝策略
        )) {
            for (int i = startIndex; i < endIndex; i++) {
                int finalI = i;
                executor.execute(() -> {
                    try {
                        byte[] bytes = index.getFirst(indexName, finalI, String.valueOf(finalI));
                        if (Objects.isNull(bytes)) {
                            wrongCount.getAndAdd(1);
                            // log.info("i = {}", finalI);
                        } else {
                            if (finalI != ByteTools.toInt(bytes)) {
                                // log.info("i = {}, | read = {}", finalI, ByteTools.toInt(bytes));
                                wrongCount.getAndAdd(1);
                            }
                        }
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    } finally {
                        latchGet.countDown();
                    }
                });
            }
        }
        log.info("putAndGetFirstBatch check over! wrongCount = {}", wrongCount.get());
        assert wrongCount.get() == 0 : wrongCount;
    }

    @Test
    @Order(4)
    void putAndGetFirstBatchTmp() throws IOException, NoSuchFieldException {
        String indexName = "putAndGetFirstBatch";
        Index index = new Index(rootpath, DATA_FILE_DEFAULT_SIZE);

        int threadCount = 150000;
        int startIndex = threadCount / 2 - threadCount;
        int endIndex = threadCount / 2;

        AtomicLong wrongCount = new AtomicLong(0);
        CountDownLatch latchGet = new CountDownLatch(threadCount); // 计数3
        try (ThreadPoolExecutor executor = new ThreadPoolExecutor(
                10,                  // 核心线程
                50,                  // 最大线程（关键！限制线程总数）
                60L, TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(200),  // 有界队列！！绝对不用无界 LinkedBlockingQueue
                new ThreadPoolExecutor.CallerRunsPolicy()  // 拒绝策略
        )) {
            for (int i = startIndex; i < endIndex; i++) {
                int finalI = i;
                executor.execute(() -> {
                    try {
                        byte[] bytes = index.getFirst(indexName, finalI, String.valueOf(finalI));
                        if (Objects.isNull(bytes)) {
                            wrongCount.getAndAdd(1);
                            log.info("i = {}", finalI);
                        } else {
                            if (finalI != ByteTools.toInt(bytes)) {
                                log.info("i = {}, | read = {}", finalI, ByteTools.toInt(bytes));
                                wrongCount.getAndAdd(1);
                            }
                        }
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    } finally {
                        latchGet.countDown();
                    }
                });
            }
        }
        log.info("putAndGetFirstBatchTmp check over! wrongCount = {}", wrongCount.get());
        assert wrongCount.get() == 0 : wrongCount;
    }

    @Test
    @Order(3)
    void putAndGetFirstAndAreaSelectTimes() throws IOException, NoSuchFieldException {
        String indexName = "putAndGetFirstAndAreaSelectTimes";
        Index index = new Index(rootpath, DATA_FILE_DEFAULT_SIZE);
        index.removeIndex(indexName);

        int count = 10;
        for (int i = 0; i < count; i++) {
            index.put(new Content(new Transaction(i), indexName, i, String.valueOf(i), ByteTools.fromInt(i)));
        }
        log.info("putAndGetFirstAndAreaSelectTimes 0 set success!");
        for (int i = 0; i < count; i++) {
            assert i == ByteTools.toInt(index.getFirst(indexName, i, String.valueOf(i))) : i;
        }

        for (int i = 0; i < count; i++) {
            long degree = -64424581328L + i;
            index.put(new Content(new Transaction(i), indexName, degree, String.valueOf(i), ByteTools.fromLong(degree)));
        }
        log.info("putAndGetFirstAndAreaSelectTimes -64424581328L set success!");
        for (int i = 0; i < count; i++) {
            long degree = -64424581328L + i;
            assert degree == ByteTools.toLong(index.getFirst(indexName, degree, String.valueOf(i))) : degree;
        }

        for (int i = 0; i < count; i++) {
            long degree = 9223372036854775507L + i;
            index.put(new Content(new Transaction(i), indexName, degree, String.valueOf(i), ByteTools.fromLong(degree)));
        }
        log.info("putAndGetFirstAndAreaSelectTimes set 9223372036854775507L success!");
        for (int i = 0; i < count; i++) {
            long degree = 9223372036854775507L + i;
            assert degree == ByteTools.toLong(index.getFirst(indexName, degree, String.valueOf(i))) : degree;
        }

        log.info("putAndGetFirstAndAreaSelectTimes check success!");
    }

    @Test
    @Order(4)
    void selectAreaTimes() throws IOException, NoSuchFieldException {
        String indexName = "putAndGetFirstAndAreaSelectTimes";
        Index index = new Index(rootpath, DATA_FILE_DEFAULT_SIZE);

        Select select = new Select();
        select.setSize(15);
        select.setHit(new Hit(indexName, false));
        List<byte[]> bytesList = index.select(select);
        System.out.println("list size = " + bytesList.size());
        for (byte[] bytes : bytesList) {
            System.out.println(ByteTools.toLong(bytes));
        }
    }

    @Test
    @Order(3)
    void deleteList() throws IOException, NoSuchFieldException {
        String indexName = "deleteList";
        Index index = new Index(rootpath, DATA_FILE_DEFAULT_SIZE);
        index.removeIndex(indexName);

        int count = 10000;
        for (int i = -5000; i < count; i++) {
            index.put(new Content(new Transaction(), indexName, i, String.valueOf(i), ByteTools.fromInt(i)));
        }
        log.info("setAndGetTimes set success!");
        for (int i = -5000; i < count; i++) {
            assert i == ByteTools.toInt(index.getFirst(indexName, i, String.valueOf(i))) : i;
        }
        log.info("setAndGetTimes check success!");

        Select select = new Select(indexName, -100, 100, false, false, true);
        List<byte[]> bytesList = index.delete(select);
        assert 199 == bytesList.size() : "199 != " + bytesList.size(); // (-99 —— 0) + (1 —— 99) = 199
        for (int i = 0; i < bytesList.size(); i++) {
            assert (i - 99) == ByteTools.toInt(bytesList.get(i)) : (i - 99) + " != " + ByteTools.toInt(bytesList.get(i)); // (-99 —— 0) + (1 —— 99) = 199
        }
        select = new Select(indexName, -120, 150, false, false, 100, true);
        bytesList = index.select(select); // -99 —— 99 上一轮已删
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

    @Test
    @Order(3)
    void mutilPutAndGetFirst() throws IOException, NoSuchFieldException {
        String indexName1 = "mutilPutAndGetFirst_index1";
        String indexName2 = "mutilPutAndGetFirst_index2";
        String indexName3 = "mutilPutAndGetFirst_index3";
        String indexName4 = "mutilPutAndGetFirst_index4";
        Index index = new Index(rootpath, DATA_FILE_DEFAULT_SIZE);
        index.removeIndex(indexName1);
        index.removeIndex(indexName2);
        index.removeIndex(indexName3);
        index.removeIndex(indexName4);

        Content content1 = new Content(new Transaction(1), indexName1, -64424581328L, "1", ByteTools.fromInt(1));
        index.put(content1);
        Content content2 = new Content(new Transaction(2), indexName2, 0, "1", ByteTools.fromInt(1));
        content2.setDataFileVersionBytes(content1.getDataFileVersionBytes());
        content2.setDataSeekBytes(content1.getDataSeekBytes());
        index.put(content2);
        Content content3 = new Content(new Transaction(3), indexName3, 1, "1", ByteTools.fromInt(1));
        content3.setDataFileVersionBytes(content1.getDataFileVersionBytes());
        content3.setDataSeekBytes(content1.getDataSeekBytes());
        index.put(content3);
        Content content4 = new Content(new Transaction(4), indexName4, 9223372036854775807L, "1", ByteTools.fromInt(1));
        content4.setDataFileVersionBytes(content1.getDataFileVersionBytes());
        content4.setDataSeekBytes(content1.getDataSeekBytes());
        index.put(content4);
        assert 1 == ByteTools.toInt(index.getFirst(indexName1, -64424581328L, "1")) : ByteTools.toInt(index.getFirst(indexName1, -64424581328L, "1"));
        assert 1 == ByteTools.toInt(index.getFirst(indexName2, 0, "1")) : ByteTools.toInt(index.getFirst(indexName2, 0, "1"));
        assert 1 == ByteTools.toInt(index.getFirst(indexName3, 1, "1")) : ByteTools.toInt(index.getFirst(indexName3, 1, "1"));
        assert 1 == ByteTools.toInt(index.getFirst(indexName4, 9223372036854775807L, "1")) : ByteTools.toInt(index.getFirst(indexName4, 9223372036854775807L, "1"));
    }

    @Test
    @Order(3)
    void mutilPutAndGetFirstAuto() throws IOException, NoSuchFieldException {
        String indexName1 = "mutilPutAndGetFirstAuto_index1";
        String indexName2 = "mutilPutAndGetFirstAuto_index2";
        String indexName3 = "mutilPutAndGetFirstAuto_index3";
        String indexName4 = "mutilPutAndGetFirstAuto_index4";
        Index index = new Index(rootpath, DATA_FILE_DEFAULT_SIZE);
        index.removeIndex(indexName1);
        index.removeIndex(indexName2);
        index.removeIndex(indexName3);
        index.removeIndex(indexName4);

        Content content = new Content(new Transaction(1), indexName1, -64424581328L, "1", ByteTools.fromInt(1));
        content.addItem(indexName2, 0, "2");
        content.addItem(indexName3, 1, "3");
        content.addItem(indexName4, 9223372036854775807L, "4");
        index.put(content);
        assert 1 == ByteTools.toInt(index.getFirst(indexName1, -64424581328L, "1")) : ByteTools.toInt(index.getFirst(indexName1, -64424581328L, "1"));
        assert 1 == ByteTools.toInt(index.getFirst(indexName2, 0, "2")) : ByteTools.toInt(index.getFirst(indexName2, 0, "2"));
        assert 1 == ByteTools.toInt(index.getFirst(indexName3, 1, "3")) : ByteTools.toInt(index.getFirst(indexName3, 1, "3"));
        assert 1 == ByteTools.toInt(index.getFirst(indexName4, 9223372036854775807L, "4")) : ByteTools.toInt(index.getFirst(indexName4, 9223372036854775807L, "4"));
    }

    @Test
    @Order(3)
    void mutilPutBatchAndGetFirst() throws IOException, NoSuchFieldException {
        String indexName1 = "mutilPutBatchAndGetFirst_index1";
        String indexName2 = "mutilPutBatchAndGetFirst_index2";
        String indexName3 = "mutilPutBatchAndGetFirst_index3";
        String indexName4 = "mutilPutBatchAndGetFirst_index4";
        Index index = new Index(rootpath, DATA_FILE_DEFAULT_SIZE);
        index.removeIndex(indexName1);
        index.removeIndex(indexName2);
        index.removeIndex(indexName3);
        index.removeIndex(indexName4);

        List<Content> contentList = new ArrayList<>();
        contentList.add(new Content(indexName1, -64424581328L, "1", ByteTools.fromInt(1)));
        contentList.add(new Content(indexName2, 0, "1", ByteTools.fromInt(1)));
        contentList.add(new Content(indexName3, 1, "1", ByteTools.fromInt(1)));
        contentList.add(new Content(indexName4, 9223372036854775807L, "1", ByteTools.fromInt(1)));
        index.put(contentList);
        assert 1 == ByteTools.toInt(index.getFirst(indexName1, -64424581328L, "1")) : ByteTools.toInt(index.getFirst(indexName1, -64424581328L, "1"));
        assert 1 == ByteTools.toInt(index.getFirst(indexName2, 0, "1")) : ByteTools.toInt(index.getFirst(indexName2, 0, "1"));
        assert 1 == ByteTools.toInt(index.getFirst(indexName3, 1, "1")) : ByteTools.toInt(index.getFirst(indexName3, 1, "1"));
        assert 1 == ByteTools.toInt(index.getFirst(indexName4, 9223372036854775807L, "1")) : ByteTools.toInt(index.getFirst(indexName4, 9223372036854775807L, "1"));
    }

    record User(String name, int age, int a, int b, int c, int d) {}

    record Role(int id, int e, int f, int g, User user) {}

    Role role(int i) {
        User user = new User("name_" + i, i, i, i, i, i);
        return new Role(i, i, i, i, user);
    }

    @Test
    @Order(3)
    void afterDegree() throws IOException, NoSuchFieldException, InterruptedException {
        String indexName = "afterDegree";
        Index index = new Index(rootpath, DATA_FILE_DEFAULT_SIZE);
        index.removeIndex(indexName);

        int threadCount = 100; // 10000000 1小时16分钟
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
                executor.execute(() -> {
                    try {
                        index.put(new Content(new Transaction(finalI), indexName, finalI, String.valueOf(finalI), ByteTools.fromString(Objects.requireNonNull(JsonTools.toJson(role(finalI))))));
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    } finally {
                        latch.countDown();
                    }
                });
            }
        }
        // 等待计数减到0（所有线程完成）
        latch.await();

        List<String> fields = new ArrayList<>();
        afterDegreeSelect(index, indexName, 0, true, fields);
        fields = List.of("id", "e", "f", "user.name", "user.age", "user.a", "user.b", "user.c");
        afterDegreeSelect(index, indexName, 9, true, fields);
        fields = List.of("id", "e", "user.name", "user.age", "user.a", "user.b");
        afterDegreeSelect(index, indexName, -5, true, fields);

        fields = List.of("id", "user.name", "user.age", "user.a");
        afterDegreeSelect(index, indexName, 0, false, fields);
        fields = List.of("id", "user.name", "user.age");
        afterDegreeSelect(index, indexName, 9, false, fields);
        fields = List.of("id", "user.name");
        afterDegreeSelect(index, indexName, -15, false, fields);
    }

    void afterDegreeSelect(Index index, String indexName, long afterDegree, boolean asc, List<String> fields) throws IOException {
        Select select = new Select();
        select.setSize(10);
        Hit hit = new Hit();
        hit.setSort(new Sort(indexName, "user.age", afterDegree, asc));
        hit.setFields(fields);
        select.setHit(hit);
        select.setSelectFilter(this::doFilter);
        List<byte[]> bytesList = index.select(select);
        System.out.println("list size = " + bytesList.size() + " | afterDegree = " + afterDegree + " | asc = " + asc);
        for (byte[] bytes : bytesList) {
//            System.out.println(JsonTools.toObj(new String(bytes), Role.class));
            System.out.println(new String(bytes));
        }
    }

    /// 过滤文档
    ///
    /// @param bytesList 文档字节数组列表
    /// @param hit       命中策略
    ///
    /// @return 过滤后的文档字节数组列表
    private List<byte[]> doFilter(List<byte[]> bytesList, Hit hit) {
        if (CollectionUtils.isEmpty(hit.getConditions())) {
            return bytesList;
        }
        return bytesList.stream().filter(bytes -> {
            for (Condition condition : hit.getConditions()) {
                Object obj;
                try {
                    obj = JsonTools.getValueByPath(JsonTools.toJson(JsonTools.toObj(ByteTools.toString(bytes), Role.class)), condition.getParam());
                } catch (Exception ignore) {
                    return false;
                }
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
                        default -> false;
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

    /// 比较两个 Number 大小
    ///
    /// @param n1 第一个 Number
    /// @param n2 第二个 Number
    ///
    /// @return 负数：n1 < n2
    /// 0：n1 == n2
    /// 正数：n1 > n2
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

}
