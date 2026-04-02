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

import cn.aberic.tangduo.common.Bytes;
import cn.aberic.tangduo.common.file.Filer;
import cn.aberic.tangduo.index.Index;
import cn.aberic.tangduo.index.engine.IEngine;
import cn.aberic.tangduo.index.engine.Transaction;
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
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Slf4j
public class IndexTests {

    @Test
    @Order(1)
    void init() throws IOException {
        try (Stream<Path> stream = Files.walk(Paths.get("tmp"))) {
            stream.forEach(f -> {
                try {
                    Files.delete(f);
                } catch (IOException e) {}
            });
        }
    }

    @Test
    @Order(2)
    void create() throws IOException, NoSuchFieldException, NoSuchMethodException {
        String rootPath = "tmp/create";
        Filer.deleteDirectory(rootPath);
        String indexName = "index";
        Index index = new Index(rootPath);
        try {
            index.createIndex(IEngine.UNITY, new Index.Info(1, indexName, true, true, false));
        } catch (InstanceAlreadyExistsException e) {
            System.out.println(e.getMessage());
        }
    }

    @Test
    @Order(2)
    void putAndGetFirstOne() throws IOException, NoSuchFieldException, NoSuchMethodException {
        String rootPath = "tmp/putAndGetOne";
        Filer.deleteDirectory(rootPath);
        String indexName = "index";
        Index index = new Index(rootPath);
        try {
            index.createIndex(IEngine.UNITY, new Index.Info(1, indexName, true, true, false));
        } catch (InstanceAlreadyExistsException e) {
            System.out.println(e.getMessage());
        }
        index.put(new IEngine.Content(new Transaction(1), indexName, 1, "1", Bytes.fromInt(1)));
        assert 1 == Bytes.toInt(index.getFirst(indexName, 1, "1")) : Bytes.toInt(index.getFirst(indexName, 1, "1"));
    }

    @Test
    @Order(2)
    void putAndGetFirst() throws IOException, NoSuchFieldException, NoSuchMethodException {
        String rootPath = "tmp/setAndGet";
        Filer.deleteDirectory(rootPath);
        String indexName = "index";
        Index index = new Index(rootPath);
        try {
            index.createIndex(IEngine.UNITY, new Index.Info(1, indexName, true, true, false));
        } catch (InstanceAlreadyExistsException e) {
            System.out.println(e.getMessage());
        }
        index.put(new IEngine.Content(new Transaction(1), indexName, -64424581328L, "1", Bytes.fromInt(1)));
        assert 1 == Bytes.toInt(index.getFirst(indexName, -64424581328L, "1")) : Bytes.toInt(index.getFirst(indexName, -64424581328L, "1"));
        index.put(new IEngine.Content(new Transaction(2), indexName, 0, "1", Bytes.fromInt(1)));
        assert 1 == Bytes.toInt(index.getFirst(indexName, 0, "1")) : Bytes.toInt(index.getFirst(indexName, 0, "1"));
        index.put(new IEngine.Content(new Transaction(3), indexName, 1, "1", Bytes.fromInt(1)));
        assert 1 == Bytes.toInt(index.getFirst(indexName, 1, "1")) : Bytes.toInt(index.getFirst(indexName, 1, "1"));
        index.put(new IEngine.Content(new Transaction(4), indexName, 9223372036854775807L, "1", Bytes.fromLong(9223372036854775807L)));
        assert 9223372036854775807L == Bytes.toLong(index.getFirst(indexName, 9223372036854775807L, "1")) : Bytes.toLong(index.getFirst(indexName, 9223372036854775807L, "1"));
    }

    @Test
    @Order(2)
    void putAndGetFirstAndRemove() throws IOException, NoSuchFieldException, NoSuchMethodException {
        String rootPath = "tmp/setAndGetAndDelete";
        Filer.deleteDirectory(rootPath);
        String indexName = "index";
        Index index = new Index(rootPath);
        try {
            index.createIndex(IEngine.UNITY, new Index.Info(1, indexName, true, true, false));
        } catch (InstanceAlreadyExistsException e) {
            System.out.println(e.getMessage());
        }
        index.put(new IEngine.Content(new Transaction(1), indexName, -64424581328L, "1", Bytes.fromInt(1)));
        index.put(new IEngine.Content(new Transaction(2), indexName, 0, "1", Bytes.fromInt(1)));
        index.put(new IEngine.Content(new Transaction(3), indexName, 1, "1", Bytes.fromInt(1)));
        index.put(new IEngine.Content(new Transaction(4), indexName, 9223372036854775807L, "1", Bytes.fromLong(9223372036854775807L)));

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
        Path path = Paths.get("tmp/setAndGet/unity/index/0_4294967295.idx");
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
    @Order(2)
    void resetAndGetFirst() throws IOException, NoSuchFieldException, NoSuchMethodException {
        String rootPath = "tmp/resetAndGet";
        Filer.deleteDirectory(rootPath);
        String indexName = "index";
        Index index = new Index(rootPath);
        try {
            index.createIndex(IEngine.UNITY, new Index.Info(1, indexName, true, true, false));
        } catch (InstanceAlreadyExistsException e) {
            System.out.println(e.getMessage());
        }
        index.put(new IEngine.Content(new Transaction(1), indexName, 1, "1", Bytes.fromInt(1)));
        assert 1 == Bytes.toInt(index.getFirst(indexName, 1, "1")) : "1 !=" + Bytes.toInt(index.getFirst(indexName, 1, "1"));
        index.put(new IEngine.Content(new Transaction(1), indexName, 1, "1", Bytes.fromInt(2)));
        assert 2 == Bytes.toInt(index.getFirst(indexName, 1, "1")) : "2 !=" + Bytes.toInt(index.getFirst(indexName, 1, "1"));
        Filer.deleteDirectory(rootPath);
    }

    @Test
    @Order(2)
    void putAndGetFirstTimes() throws IOException, NoSuchFieldException, NoSuchMethodException {
        String rootPath = "tmp/setAndGetTimes";
        Filer.deleteDirectory(rootPath);
        String indexName = "index";
        Index index = new Index(rootPath);
        try {
            index.createIndex(IEngine.UNITY, new Index.Info(1, indexName, true, true, false));
        } catch (InstanceAlreadyExistsException e) {
            System.out.println(e.getMessage());
        }
        int count = 100000;
        for (int i = 0; i < count; i++) {
            index.put(new IEngine.Content(new Transaction(i), indexName, i, String.valueOf(i), Bytes.fromInt(i)));
        }
        log.debug("setAndGetTimes set success!");
        for (int i = 0; i < count; i++) {
            assert i == Bytes.toInt(index.getFirst(indexName, i, String.valueOf(i))) : i;
        }
        log.debug("setAndGetTimes check success!");
    }

    @Test
    @Order(2)
    void putAndGetFirstTimesAsync() throws IOException, NoSuchFieldException, NoSuchMethodException, InterruptedException {
        String rootPath = "tmp/putAndGetFirstTimesAsync";
        Filer.deleteDirectory(rootPath);
        String indexName = "index";
        Index index = new Index(rootPath);
        try {
            index.createIndex(IEngine.UNITY, new Index.Info(1, indexName, true, true, false));
        } catch (InstanceAlreadyExistsException e) {
            System.out.println(e.getMessage());
        }
        int threadCount = 100000;
        int startIndex = threadCount / 2 - threadCount;
        CountDownLatch latch = new CountDownLatch(threadCount); // 计数3
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
                        index.put(new IEngine.Content(new Transaction(finalI), indexName, finalI, String.valueOf(finalI), Bytes.fromInt(finalI)));
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
        index.force(1, indexName);
        log.debug("setAndGetTimes set success!");

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
                        if (finalI != Bytes.toInt(Objects.isNull(bytes) ? new byte[4] : bytes)) {
                            log.debug("i = {}, | read = {}", finalI, Bytes.toInt(Objects.isNull(bytes) ? new byte[4] : bytes));
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
        log.debug("setAndGetTimes check over! wrongCount = {}", wrongCount.get());
        assert wrongCount.get() == 0 : wrongCount;
    }

    @Test
    @Order(3)
    void select() throws IOException, NoSuchFieldException {
        String rootPath = "tmp/putAndGetFirstTimesAsync";
        String indexName = "index";
        Index index = new Index(rootPath);
        long wrongCount = 0;
        for (int i = -500; i < 500; i++) {
            byte[] bytes = index.getFirst(indexName, i, String.valueOf(i));
            if (i != Bytes.toInt(Objects.isNull(bytes) ? new byte[4] : bytes)) {
                log.debug("i = {}, | read = {}", i, Bytes.toInt(Objects.isNull(bytes) ? new byte[4] : bytes));
                wrongCount++;
            }
        }
        log.debug("setAndGetTimes check over! wrongCount =  {}", wrongCount);

        IEngine.Search search = new IEngine.Search(indexName, -500, 500, true, true, 100, true);
        List<byte[]> bytesList = index.select(search);
        System.out.println("list size = " + bytesList.size());
        for (int i = 0; i < bytesList.size(); i++) {
            assert Bytes.toInt(bytesList.get(i)) == -500 + i : Bytes.toInt(bytesList.get(i)) + " != " + (-500 + i);
        }

        search = new IEngine.Search(indexName, -500, 500, false, false, 100, true);
        bytesList = index.select(search);
        System.out.println("list size = " + bytesList.size());
        for (int i = 0; i < bytesList.size(); i++) {
            assert Bytes.toInt(bytesList.get(i)) == -499 + i : Bytes.toInt(bytesList.get(i)) + " != " + (-499 + i);
        }

        search = new IEngine.Search(indexName, -500, 500, true, true, 100, false);
        bytesList = index.select(search);
        System.out.println("list size = " + bytesList.size());
        for (int i = 0; i < bytesList.size(); i++) {
            assert Bytes.toInt(bytesList.get(i)) == 500 - i : Bytes.toInt(bytesList.get(i)) + " != " + (500 - i);
        }

        search = new IEngine.Search(indexName, -500, 500, false, false, 100, false);
        bytesList = index.select(search);
        System.out.println("list size = " + bytesList.size());
        for (int i = 0; i < bytesList.size(); i++) {
            assert Bytes.toInt(bytesList.get(i)) == 499 - i : Bytes.toInt(bytesList.get(i)) + " != " + (499 - i);
        }

        search = new IEngine.Search(indexName, -50, 50, true, true, 100, true);
        bytesList = index.select(search);
        System.out.println("list size = " + bytesList.size());
        for (int i = 0; i < bytesList.size(); i++) {
            assert Bytes.toInt(bytesList.get(i)) == -50 + i : Bytes.toInt(bytesList.get(i)) + " != " + (-50 + i);
        }

        search = new IEngine.Search(indexName, -50, 50, false, false, 100, true);
        bytesList = index.select(search);
        System.out.println("list size = " + bytesList.size());
        for (int i = 0; i < bytesList.size(); i++) {
            System.out.print(Bytes.toInt(bytesList.get(i)) + " ");
            assert Bytes.toInt(bytesList.get(i)) == -49 + i : Bytes.toInt(bytesList.get(i)) + " != " + (-49 + i);
        }
        System.out.println();

        search = new IEngine.Search(indexName, -50, 50, false, false, 100, true, bsList -> {
            List<byte[]> bl = new ArrayList<>();
            for (byte[] bytes : bsList) {
                if (0 != Bytes.toInt(bytes)) {
                    bl.add(bytes);
                }
            }
            return bl;
        });
        bytesList = index.select(search);
        System.out.println("list size = " + bytesList.size());
        for (int i = 0; i < bytesList.size(); i++) {
            if (i < 49) {
                System.out.print(Bytes.toInt(bytesList.get(i)) + " ");
                assert Bytes.toInt(bytesList.get(i)) == -49 + i : Bytes.toInt(bytesList.get(i)) + " != " + (-49 + i);
            } else {
                System.out.print(Bytes.toInt(bytesList.get(i)) + " ");
                assert Bytes.toInt(bytesList.get(i)) == -48 + i : Bytes.toInt(bytesList.get(i)) + " != " + (-48 + i);
            }
        }
        System.out.println();
    }

    @Test
    @Order(2)
    void deleteList() throws IOException, NoSuchFieldException {
        String rootPath = "tmp/deleteList";
        Filer.deleteDirectory(rootPath);
        String indexName = "index";
        Index index = new Index(rootPath);
        try {
            index.createIndex(IEngine.UNITY, new Index.Info(1, indexName, true, true, false));
        } catch (InstanceAlreadyExistsException | NoSuchMethodException e) {
            System.out.println(e.getMessage());
        }

        int count = 10000;
        for (int i = -5000; i < count; i++) {
            index.put(new IEngine.Content(new Transaction(i), indexName, i, String.valueOf(i), Bytes.fromInt(i)));
        }
        log.debug("setAndGetTimes set success!");
        for (int i = -5000; i < count; i++) {
            assert i == Bytes.toInt(index.getFirst(indexName, i, String.valueOf(i))) : i;
        }
        log.debug("setAndGetTimes check success!");

        IEngine.Search search = new IEngine.Search(indexName, -100, 100, false, false, true);
        List<byte[]> bytesList = index.delete(search);
        assert 199 == bytesList.size() : "199 != " + bytesList.size(); // (-99 —— 0) + (1 —— 99) = 199
        for (int i = 0; i < bytesList.size(); i++) {
            assert (i - 99) == Bytes.toInt(bytesList.get(i)) : (i - 99) + " != " + Bytes.toInt(bytesList.get(i)); // (-99 —— 0) + (1 —— 99) = 199
        }
        search = new IEngine.Search(indexName, -120, 150, false, false, 100, true);
        bytesList = index.select(search); // -99 —— 99 上一轮已删
        assert 70 == bytesList.size() : "70 != " + bytesList.size(); // -120——150总计271个数字，减去上一轮的199，还剩70个数字
        for (int i = 0; i < bytesList.size(); i++) {
            // (-99 —— 0) + (1 —— 99) 因获取不到，被过滤掉
            if (i < 20) { // 即 -120 —— -100 是可查到数字，但不包含 -120
                assert (i - 119) == Bytes.toInt(bytesList.get(i)) : (i - 119) + " != " + Bytes.toInt(bytesList.get(i));
            } else { // 100 —— 150 是可查到数字，但不包含 150，20以后从100开始计数
                assert (i + 80) == Bytes.toInt(bytesList.get(i)) : (i + 80) + " != " + Bytes.toInt(bytesList.get(i));
            }
        }
    }

    @Test
    @Order(2)
    void mutilPutAndGetFirst() throws IOException, NoSuchFieldException, NoSuchMethodException {
        String rootPath = "tmp/mutilPutAndGet";
        Filer.deleteDirectory(rootPath);
        String indexName1 = "index1";
        String indexName2 = "index2";
        String indexName3 = "index3";
        String indexName4 = "index4";
        Index index = new Index(rootPath);
        try {
            index.createIndex(IEngine.UNITY, new Index.Info(1, indexName1, true, true, false));
        } catch (InstanceAlreadyExistsException e) {
            System.out.println(e.getMessage());
        }
        IEngine.Content content1 = new IEngine.Content(new Transaction(1), indexName1, -64424581328L, "1", Bytes.fromInt(1));
        index.put(content1);
        IEngine.Content content2 = new IEngine.Content(new Transaction(2), indexName2, 0, "1", Bytes.fromInt(1));
        content2.setDataFileVersionBytes(content1.getDataFileVersionBytes());
        content2.setDataSeekBytes(content1.getDataSeekBytes());
        index.put(content2);
        IEngine.Content content3 = new IEngine.Content(new Transaction(3), indexName3, 1, "1", Bytes.fromInt(1));
        content3.setDataFileVersionBytes(content1.getDataFileVersionBytes());
        content3.setDataSeekBytes(content1.getDataSeekBytes());
        index.put(content3);
        IEngine.Content content4 = new IEngine.Content(new Transaction(4), indexName4, 9223372036854775807L, "1", Bytes.fromInt(1));
        content4.setDataFileVersionBytes(content1.getDataFileVersionBytes());
        content4.setDataSeekBytes(content1.getDataSeekBytes());
        index.put(content4);
        assert 1 == Bytes.toInt(index.getFirst(indexName1, -64424581328L, "1")) : Bytes.toInt(index.getFirst(indexName1, -64424581328L, "1"));
        assert 1 == Bytes.toInt(index.getFirst(indexName2, 0, "1")) : Bytes.toInt(index.getFirst(indexName2, 0, "1"));
        assert 1 == Bytes.toInt(index.getFirst(indexName3, 1, "1")) : Bytes.toInt(index.getFirst(indexName3, 1, "1"));
        assert 1 == Bytes.toInt(index.getFirst(indexName4, 9223372036854775807L, "1")) : Bytes.toInt(index.getFirst(indexName4, 9223372036854775807L, "1"));
    }

    @Test
    @Order(2)
    void mutilPutAndGetFirstAuto() throws IOException, NoSuchFieldException, NoSuchMethodException {
        String rootPath = "tmp/mutilPutAndGetAuto";
        Filer.deleteDirectory(rootPath);
        String indexName1 = "index1";
        String indexName2 = "index2";
        String indexName3 = "index3";
        String indexName4 = "index4";
        Index index = new Index(rootPath);
        try {
            index.createIndex(IEngine.UNITY, new Index.Info(1, indexName1, true, true, false));
        } catch (InstanceAlreadyExistsException e) {
            System.out.println(e.getMessage());
        }
        IEngine.Content content = new IEngine.Content(new Transaction(1), indexName1, -64424581328L, "1", Bytes.fromInt(1));
        content.addItem(indexName2, 0, "2");
        content.addItem(indexName3, 1, "3");
        content.addItem(indexName4, 9223372036854775807L, "4");
        index.put(content);
        assert 1 == Bytes.toInt(index.getFirst(indexName1, -64424581328L, "1")) : Bytes.toInt(index.getFirst(indexName1, -64424581328L, "1"));
        assert 1 == Bytes.toInt(index.getFirst(indexName2, 0, "2")) : Bytes.toInt(index.getFirst(indexName2, 0, "2"));
        assert 1 == Bytes.toInt(index.getFirst(indexName3, 1, "3")) : Bytes.toInt(index.getFirst(indexName3, 1, "3"));
        assert 1 == Bytes.toInt(index.getFirst(indexName4, 9223372036854775807L, "4")) : Bytes.toInt(index.getFirst(indexName4, 9223372036854775807L, "4"));
    }

}
