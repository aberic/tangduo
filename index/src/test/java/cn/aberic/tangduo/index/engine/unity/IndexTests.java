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
import org.junit.jupiter.api.Test;

import javax.management.InstanceAlreadyExistsException;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Slf4j
public class IndexTests {

    @Test
    void setAndGet() throws IOException, NoSuchFieldException, NoSuchMethodException {
        String rootPath = "tmp/setAndGet";
        Filer.deleteDirectory(rootPath);
        String indexName = "index";
        Index index = Index.getInstance(rootPath);
        try {
            index.createIndex(IEngine.UNITY, 1, indexName, true, true, false, 200);
        } catch (InstanceAlreadyExistsException e) {
            System.out.println(e.getMessage());
        }
        index.set(new IEngine.Content(new Transaction(1), indexName, -64424581328L, "1", Bytes.fromInt(1)));
        assert 1 == Bytes.toInt(index.get(indexName, -64424581328L, "1")) : Bytes.toInt(index.get(indexName, -64424581328L, "1"));
        index.set(new IEngine.Content(new Transaction(2), indexName, 0, "1", Bytes.fromInt(1)));
        assert 1 == Bytes.toInt(index.get(indexName, 0, "1")) : Bytes.toInt(index.get(indexName, 0, "1"));
        index.set(new IEngine.Content(new Transaction(3), indexName, 1, "1", Bytes.fromInt(1)));
        assert 1 == Bytes.toInt(index.get(indexName, 1, "1")) : Bytes.toInt(index.get(indexName, 1, "1"));
        index.set(new IEngine.Content(new Transaction(4), indexName, 9223372036854775807L, "1", Bytes.fromLong(9223372036854775807L)));
        assert 9223372036854775807L == Bytes.toLong(index.get(indexName, 9223372036854775807L, "1")) : Bytes.toLong(index.get(indexName, 9223372036854775807L, "1"));
    }

    @Test
    void setAndGetAndRemove() throws IOException, NoSuchFieldException, NoSuchMethodException {
        String rootPath = "tmp/setAndGetAndDelete";
        Filer.deleteDirectory(rootPath);
        String indexName = "index";
        Index index = Index.getInstance(rootPath);
        try {
            index.createIndex(IEngine.UNITY, 1, indexName, true, true, false, 200);
        } catch (InstanceAlreadyExistsException e) {
            System.out.println(e.getMessage());
        }
        index.set(new IEngine.Content(new Transaction(1), indexName, -64424581328L, "1", Bytes.fromInt(1)));
        index.set(new IEngine.Content(new Transaction(2), indexName, 0, "1", Bytes.fromInt(1)));
        index.set(new IEngine.Content(new Transaction(3), indexName, 1, "1", Bytes.fromInt(1)));
        index.set(new IEngine.Content(new Transaction(4), indexName, 9223372036854775807L, "1", Bytes.fromLong(9223372036854775807L)));

        index.remove(indexName, -64424581328L, "1");
        assert null == index.get(indexName, -64424581328L, "1") : Bytes.toInt(index.get(indexName, -64424581328L, "1"));
        index.remove(indexName, 0, "1");
        assert null == index.get(indexName, 0, "1") : Bytes.toInt(index.get(indexName, 0, "1"));
        index.remove(indexName, 1, "1");
        assert null == index.get(indexName, 1, "1") : Bytes.toInt(index.get(indexName, 1, "1"));
        index.remove(indexName, 9223372036854775807L, "1");
        assert null == index.get(indexName, 9223372036854775807L, "1") : Bytes.toLong(index.get(indexName, 9223372036854775807L, "1"));
    }

//    @Test
//    void setAndGet2() throws IOException, NoSuchFieldException, NoSuchMethodException {
//        String rootPath = "tmp/setAndGet2";
//        String indexName = "index";
//        Index index = Index.getInstance(rootPath);
//        try {
//            index.createIndex(IEngine.UNITY, 1, indexName, true, true, false);
//        } catch (InstanceAlreadyExistsException e) {
//            System.out.println(e.getMessage());
//        }
//        index.set(new Transaction(1), -64424581328L, indexName, "1", Bytes.fromInt(1));
//        index.set(new Transaction(2), 0, indexName, "1", Bytes.fromInt(1));
//        index.set(new Transaction(3), 1, indexName, "1", Bytes.fromInt(1));
//        index.set(new Transaction(4), 9223372036854775807L, indexName, "1", Bytes.fromLong(9223372036854775807L));
//    }
//
//    @Test
//    void setAndGet2_1() throws IOException, NoSuchFieldException {
//        String rootPath = "tmp/setAndGet2";
//        String indexName = "index";
//        Index index1 = Index.getInstance(rootPath);
//        assert 1 == Bytes.toInt(index1.get(-64424581328L, indexName, "1")) : Bytes.toInt(index1.get(-64424581328L, indexName, "1"));
//        assert 1 == Bytes.toInt(index1.get(1, indexName, "1")) : Bytes.toInt(index1.get(1, indexName, "1"));
//        assert 1 == Bytes.toInt(index1.get(0, indexName, "1")) : Bytes.toInt(index1.get(0, indexName, "1"));
//        assert 9223372036854775807L == Bytes.toLong(index1.get(9223372036854775807L, indexName, "1")) : Bytes.toLong(index1.get(9223372036854775807L, indexName, "1"));
//        Filer.deleteDirectory(rootPath);
//    }

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

    private String getDegreeInterval(long degree) {
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
    void getDegreeIntervalTest() {
        System.out.println(getDegreeInterval(4294967296L)); // 4294967296_8589934591
        System.out.println(getDegreeInterval(64424581328L)); // 64424509440_68719476735
        System.out.println(getDegreeInterval(-429496729)); // neg_9223372032559808512_9223372036854775807
        System.out.println(getDegreeInterval(-64424581328L)); // neg_9223371968135299072_9223371972430266367
    }

    @Test
    void resetAndGet() throws IOException, NoSuchFieldException, NoSuchMethodException {
        String rootPath = "tmp/resetAndGet";
        Filer.deleteDirectory(rootPath);
        String indexName = "index";
        Index index = Index.getInstance(rootPath);
        try {
            index.createIndex(IEngine.UNITY, 1, indexName, true, true, false, 200);
        } catch (InstanceAlreadyExistsException e) {
            System.out.println(e.getMessage());
        }
        index.set(new IEngine.Content(new Transaction(1), indexName, 1, "1", Bytes.fromInt(1)));
        assert 1 == Bytes.toInt(index.get(indexName, 1, "1")) : Bytes.toInt(index.get(indexName, 1, "1"));
        index.set(new IEngine.Content(new Transaction(1), indexName, 1, "1", Bytes.fromInt(2)));
        assert 2 == Bytes.toInt(index.get(indexName, 1, "1")) : Bytes.toInt(index.get(indexName, 1, "1"));
        Filer.deleteDirectory(rootPath);
    }

    @Test
    void setAndGetTimes() throws IOException, NoSuchFieldException, NoSuchMethodException {
        String rootPath = "tmp/setAndGetTimes";
        Filer.deleteDirectory(rootPath);
        String indexName = "index";
        Index index = Index.getInstance(rootPath);
        try {
            index.createIndex(IEngine.UNITY, 1, indexName, true, true, false, 200);
        } catch (InstanceAlreadyExistsException e) {
            System.out.println(e.getMessage());
        }
        int count = 100000;
        for (int i = 0; i < count; i++) {
            index.set(new IEngine.Content(new Transaction(i), indexName, i, String.valueOf(i), Bytes.fromInt(i)));
        }
        log.debug("setAndGetTimes set success!");
        for (int i = 0; i < count; i++) {
            assert i == Bytes.toInt(index.get(indexName, i, String.valueOf(i))) : i;
        }
        log.debug("setAndGetTimes check success!");
        Filer.deleteDirectory(rootPath);
    }

    @Test
    void setAndGetTimesAsync() throws IOException, NoSuchFieldException, NoSuchMethodException, InterruptedException {
        String rootPath = "tmp/setAndGetTimesAsync";
        Filer.deleteDirectory(rootPath);
        String indexName = "index";
        Index index = Index.getInstance(rootPath);
        try {
            index.createIndex(IEngine.UNITY, 1, indexName, true, true, false, 200);
        } catch (InstanceAlreadyExistsException e) {
            System.out.println(e.getMessage());
        }
        int threadCount = 10000;
        CountDownLatch latch = new CountDownLatch(threadCount); // 计数3
        try (ThreadPoolExecutor executor = new ThreadPoolExecutor(
                10,                  // 核心线程
                50,                  // 最大线程（关键！限制线程总数）
                60L, TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(200),  // 有界队列！！绝对不用无界 LinkedBlockingQueue
                new ThreadPoolExecutor.CallerRunsPolicy()  // 拒绝策略
        )) {
            for (int i = -5000; i < threadCount; i++) {
                int finalI = i;
                executor.execute(() -> {
                    try {
                        index.set(new IEngine.Content(new Transaction(finalI), indexName, finalI, String.valueOf(finalI), Bytes.fromInt(finalI)));
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
        long wrongCount = 0;
        for (int i = 0; i < threadCount; i++) {
            byte[] bytes = index.get(indexName, i, String.valueOf(i));
            if (i != Bytes.toInt(Objects.isNull(bytes) ? new byte[4] : bytes)) {
                log.debug("i = {}, | read = {}", i, Bytes.toInt(Objects.isNull(bytes) ? new byte[4] : bytes));
                wrongCount++;
            }
        }
        log.debug("setAndGetTimes check over! wrongCount =  {}", wrongCount);
        assert wrongCount == 0 : wrongCount;
    }

    @Test
    void select() throws IOException, NoSuchFieldException {
        String rootPath = "tmp/setAndGetTimesAsync";
        String indexName = "index";
        Index index = Index.getInstance(rootPath);
        long wrongCount = 0;
        for (int i = -500; i < 500; i++) {
            byte[] bytes = index.get(indexName, i, String.valueOf(i));
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
    void deleteList() throws IOException, NoSuchFieldException {
        String rootPath = "tmp/deleteList";
        Filer.deleteDirectory(rootPath);
        String indexName = "index";
        Index index = Index.getInstance(rootPath);
        try {
            index.createIndex(IEngine.UNITY, 1, indexName, true, true, false, 200);
        } catch (InstanceAlreadyExistsException | NoSuchMethodException e) {
            System.out.println(e.getMessage());
        }

        int count = 10000;
        for (int i = -5000; i < count; i++) {
            index.set(new IEngine.Content(new Transaction(i), indexName, i, String.valueOf(i), Bytes.fromInt(i)));
        }
        log.debug("setAndGetTimes set success!");
        for (int i = -5000; i < count; i++) {
            assert i == Bytes.toInt(index.get(indexName, i, String.valueOf(i))) : i;
        }
        log.debug("setAndGetTimes check success!");

        IEngine.Search search = new IEngine.Search(indexName, -100, 100, false, false, true);
        List<byte[]> bytesList = index.delete(search);
        System.out.println("list size = " + bytesList.size());
        bytesList.forEach(bytes -> System.out.print(Bytes.toInt(bytes) + " "));
        System.out.println();
        search = new IEngine.Search(indexName, -120, 150, false, false, 100, true);
        bytesList = index.select(search);
        System.out.println("list size = " + bytesList.size());
        for (byte[] bytes : bytesList) {
            System.out.print(Bytes.toInt(bytes) + " ");
//            if (i < 49) {
//                System.out.print(Bytes.toInt(bytesList.get(i)) + " ");
//                assert Bytes.toInt(bytesList.get(i)) == -49 + i : Bytes.toInt(bytesList.get(i)) + " != " + (-49 + i);
//            } else {
//                System.out.print(Bytes.toInt(bytesList.get(i)) + " ");
//                assert Bytes.toInt(bytesList.get(i)) == -48 + i : Bytes.toInt(bytesList.get(i)) + " != " + (-48 + i);
//            }
        }
        System.out.println();
    }

}
