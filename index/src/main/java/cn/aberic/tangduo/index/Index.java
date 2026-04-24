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

package cn.aberic.tangduo.index;

import cn.aberic.tangduo.common.file.Channel;
import cn.aberic.tangduo.common.file.Filer;
import cn.aberic.tangduo.common.file.Writer;
import cn.aberic.tangduo.index.engine.Common;
import cn.aberic.tangduo.index.engine.Datum;
import cn.aberic.tangduo.index.engine.IEngine;
import cn.aberic.tangduo.index.engine.Transaction;
import cn.aberic.tangduo.index.engine.entity.Content;
import cn.aberic.tangduo.index.engine.entity.Search;
import cn.aberic.tangduo.index.engine.unity.Unity;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import javax.management.InstanceAlreadyExistsException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.rmi.UnexpectedException;
import java.util.*;
import java.util.concurrent.*;

/// 索引接口<p>
/// 暴露索引相关使用接口
@Slf4j
public class Index {

    /// 索引集合，索引名+索引引擎
    private final Map<String, IEngine> indexMap = new ConcurrentHashMap<>();

    private static final String blockSkip = "##@@##";
    private static final String indexSkip = "@#@";

    /// 数据库索引所在根路径
    String rootPath;
    /// 数据文件大小阈值，单位byte
    long dataFileMaxSize;
    /// 单批次最大数量
    int batchMaxSize;

    private Index() {}

    /// 索引构造函数
    ///
    /// @param rootPath        数据根路径
    /// @param dataFileMaxSize 数据文件大小阈值，单位byte
    ///
    /// @throws IOException          异常
    /// @throws NoSuchFieldException 异常
    public Index(String rootPath, long dataFileMaxSize) throws IOException, NoSuchFieldException {
        this(rootPath, dataFileMaxSize, 5000);
    }

    /// 索引构造函数
    ///
    /// @param rootPath        数据根路径
    /// @param dataFileMaxSize 数据文件大小阈值，单位byte
    /// @param batchMaxSize    单批次最大数量
    ///
    /// @throws IOException          异常
    /// @throws NoSuchFieldException 异常
    public Index(String rootPath, long dataFileMaxSize, int batchMaxSize) throws IOException, NoSuchFieldException {
        this();
        this.rootPath = rootPath;
        this.dataFileMaxSize = dataFileMaxSize;
        this.batchMaxSize = batchMaxSize;
        Channel.startWriteThread();
        init();
    }

    /// 初始化索引
    /// 构造Master后最先执行的方法
    /// 基于数据根路径获取索引相关信息<p>
    /// 从指定文件中读取索引和文件等关联信息，如不存在，则新建文件<p>
    /// 文件内容：
    /// 索引名称@#@索引文件地址@#@索引引擎@#@索引文件版本号##@@##索引名称@#@索引文件地址@#@索引引擎@#@索引文件版本号
    ///
    /// @throws IOException          异常
    /// @throws NoSuchFieldException 异常
    private void init() throws IOException, NoSuchFieldException {
        Path recordPath = Common.recordFilepath(rootPath); // 如"tmp/record.rd"
        if (!Files.exists(recordPath)) {
            Filer.createFile(recordPath);
        }
        String res = Files.readString(recordPath);
        if (StringUtils.isEmpty(res)) {
            return;
        }
        String[] blockArr = res.split(blockSkip);
        for (String block : blockArr) {
            String[] indexArr = block.split(indexSkip);
            int indexEngine = Integer.parseInt(indexArr[0]);
            String indexName = indexArr[1];
            indexMap.put(indexName, engine(indexEngine, indexName));
        }
    }

    /// 获取所有索引名称
    /// @return 索引名称列表
    public List<String> indexList() {
        return indexMap.keySet().stream().toList();
    }

    /// 获取索引引擎
    ///
    /// @param indexEngine 索引引擎
    /// @param indexName   索引名称
    ///
    /// @return 索引引擎
    ///
    /// @throws NoSuchFieldException 异常
    /// @throws IOException          异常
    private IEngine engine(int indexEngine, String indexName) throws NoSuchFieldException, IOException {
        if (indexEngine == IEngine.UNITY) {
            return new Unity(rootPath, indexName, dataFileMaxSize);
        }
        throw new NoSuchFieldException("入参引擎不存在");
    }

    /**
     * 创建索引和文件等关联信息<p>
     * 文件内容类似：
     * 索引引擎@#@索引名称@#@索引文件地址@#@索引文件版本号@#@索引创建时间##@@##索引引擎@#@索引名称@#@索引文件地址@#@索引文件版本号@#@索引创建时间
     *
     * @param engine 引擎，在 Engine.UNITY、Engine.SKIP 等中选值，或传入对应的整型值
     * @param info   索引信息
     */
    public synchronized void createIndex(int engine, Info info) throws InstanceAlreadyExistsException, IOException, NoSuchMethodException {
        // 遍历确认没有重复名称的索引
        for (Map.Entry<String, IEngine> engineEntry : indexMap.entrySet()) {
            if (info.name.equals(engineEntry.getKey())) {
                throw new InstanceAlreadyExistsException("索引实例已存在");
            }
        }
        int dataFileVersion = 1;
        // 新建数据文件
        Path dataFilepath = Common.dataFilepath(rootPath, dataFileVersion);
        if (!Files.exists(dataFilepath)) {
            Files.createFile(dataFilepath);
            Writer.append(dataFilepath.toString(), Datum.startBytes);
        }
        switch (engine) {
            case IEngine.UNITY ->
                    indexMap.put(info.name, new Unity(rootPath, 1, dataFileVersion, dataFileMaxSize, info.version, info.name, info.primary, info.unique, info.nullable));
            case IEngine.SKIP -> throw new NoSuchMethodException("方法未实现");
            default -> throw new UnexpectedException("传参未匹配");
        }
        Path recordPath = Common.recordFilepath(rootPath); // 如"tmp/record.rd"
        String format;
        if (!Files.exists(recordPath) || Files.size(recordPath) == 0) {
            format = String.format("%s%s%s", engine, indexSkip, info.name);
        } else {
            format = String.format("%s%s%s%s", blockSkip, engine, indexSkip, info.name);
        }
        Channel.append(recordPath.toString(), format.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 删除索引
     *
     * @param indexName 索引名称
     */
    public void removeIndex(String indexName) throws IOException {
        // 遍历确认没有重复名称的索引
        if (indexMap.entrySet().stream().noneMatch(entry -> entry.getKey().equals(indexName))) {
            return;
        }
        Path recordPath = Common.recordFilepath(rootPath); // 如"tmp/record.rd"
        // 索引引擎@#@索引名称@#@索引文件地址@#@索引文件版本号@#@索引创建时间 ##@@## 索引引擎@#@索引名称@#@索引文件地址@#@索引文件版本号@#@索引创建时间
        String res = Files.readString(recordPath);
        if (StringUtils.isEmpty(res)) {
            return;
        }
        StringBuilder sb = new StringBuilder();
        String[] blockArr = res.split(blockSkip);
        for (String indexRecord : blockArr) {
            String[] dbArr = indexRecord.split(indexSkip);
            if (dbArr[1].equals(indexName)) {
                continue;
            }
            if (!sb.isEmpty()) {
                sb.append(blockSkip).append(indexRecord);
            } else {
                sb.append(indexRecord);
            }
        }
        Writer.write(recordPath.toString(), sb.toString().getBytes(StandardCharsets.UTF_8));
        indexMap.remove(indexName);
        Filer.deleteDirectory(Path.of(rootPath, indexName));
    }

    /// 刷盘
    ///
    /// @param degree    主键（-9223372036854775807 —— 9223372036854775808）
    /// @param indexName 索引名称
    ///
    /// @throws IOException 异常
    public void force(long degree, String indexName) throws IOException {
        indexMap.get(indexName).force(degree, indexName);
    }

    /// Node插入数据data
    ///
    /// @param content 数据内容
    ///
    /// @throws IOException 异常
    public void put(Content content) throws IOException {
        if (!indexMap.containsKey(content.getIndexName())) {
            if (content.isAutoCreateIndex()) {
                try {
                    createIndex(IEngine.UNITY, new Info(content.getIndexName()));
                } catch (InstanceAlreadyExistsException ignore) {} catch (NoSuchMethodException e) {
                    throw new RuntimeException(e);
                }
            } else {
                throw new NoSuchElementException("索引" + content.getIndexName() + "实例不存在");
            }
        }
        IEngine engine = indexMap.get(content.getIndexName());
        engine.put(engine, content.getIndexName(), content);
        content.getLock().lock();
        try {
            while (!content.getIsNotified().get()) {
                content.getCondition().await();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            content.getLock().unlock();
        }
        if (!content.getItems().isEmpty()) {
            try (ExecutorService childExecutor = Executors.newVirtualThreadPerTaskExecutor()) {
                List<Future<?>> childFutures = new ArrayList<>();
                for (Content.Item item : content.getItems()) {
                    Future<?> childFuture = childExecutor.submit(() -> {
                        try {
                            processSingleItem(item, content);
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    });
                    childFutures.add(childFuture);
                }
                // 等待所有item任务完成
                for (Future<?> future : childFutures) {
                    try {
                        future.get();
                    } catch (InterruptedException | ExecutionException e) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException(e);
                    }
                }
            }
        }
        content.getTransaction().execute();
    }

    /// 批量插入数据data
    ///
    /// @param contentList 数据内容列表
    ///
    /// @throws IOException 异常
    public void put(List<Content> contentList) throws IOException {
        List<List<Content>> result = splitContentList(contentList);
        for (List<Content> contents : result) {
            putBatch(contents);
        }
    }

    /**
     * 将大的contentList拆分为多个指定大小的子集合
     *
     * @param contentList 原始数据集合
     *
     * @return 拆分后的子批次集合
     */
    private List<List<Content>> splitContentList(List<Content> contentList) {
        List<List<Content>> result = new ArrayList<>();
        if (contentList == null || contentList.isEmpty()) {
            return result;
        }
        int totalSize = contentList.size();
        int startIndex = 0;
        // 循环拆分：从startIndex开始，每次取batchSize条，直到末尾
        while (startIndex < totalSize) {
            int endIndex = Math.min(startIndex + batchMaxSize, totalSize);
            List<Content> subBatch = contentList.subList(startIndex, endIndex);
            // 注意：subList是视图，需转为新ArrayList避免原集合修改影响子批次
            result.add(new ArrayList<>(subBatch));
            startIndex = endIndex;
        }
        return result;
    }

    /// 批量插入数据data
    ///
    /// @param contentList 数据内容列表
    public void putBatch(List<Content> contentList) {
        Transaction transaction = new Transaction();
        // 外层循环并行虚拟线程
        try (ExecutorService parentExecutor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<Future<?>> parentFutures = new ArrayList<>();
            for (Content content : contentList) {
                Future<?> parentFuture = parentExecutor.submit(() -> {
                    try {
                        processSingleContent(content, transaction);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });
                parentFutures.add(parentFuture);
            }
            // 统一等待所有任务，处理异常
            for (Future<?> future : parentFutures) {
                try {
                    future.get();
                } catch (InterruptedException | ExecutionException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException(e);
                }
            }
        }
        transaction.execute();
    }

    /// 处理单条数据
    ///
    /// @param content     数据内容
    /// @param transaction 事务
    ///
    /// @throws Exception 异常
    private void processSingleContent(Content content, Transaction transaction) throws Exception {
        if (!indexMap.containsKey(content.getIndexName())) {
            try {
                createIndex(IEngine.UNITY, new Info(content.getIndexName()));
            } catch (InstanceAlreadyExistsException ignore) {} catch (NoSuchMethodException e) {
                throw new RuntimeException(e);
            }
        }
        content.setTransaction(transaction);
        IEngine engine = indexMap.get(content.getIndexName());
        engine.put(engine, content.getIndexName(), content);

        content.getLock().lock();
        try {
            while (!content.getIsNotified().get()) {
                content.getCondition().await();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw e;
        } finally {
            content.getLock().unlock();
        }

        // 内层循环用虚拟线程池替代CountDownLatch
        if (!content.getItems().isEmpty()) {
            try (ExecutorService childExecutor = Executors.newVirtualThreadPerTaskExecutor()) {
                List<Future<?>> childFutures = new ArrayList<>();
                for (Content.Item item : content.getItems()) {
                    Future<?> childFuture = childExecutor.submit(() -> {
                        try {
                            processSingleItem(item, content);
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    });
                    childFutures.add(childFuture);
                }
                // 等待所有item任务完成
                for (Future<?> future : childFutures) {
                    try {
                        future.get();
                    } catch (InterruptedException | ExecutionException e) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException(e);
                    }
                }
            }
        }
    }

    /// 处理单条数据
    ///
    /// @param item    数据项
    /// @param content 数据内容
    ///
    /// @throws Exception 异常
    private void processSingleItem(Content.Item item, Content content) throws Exception {
        // 原业务逻辑完全不变
        String indexName = item.getIndexName();
        if (!indexMap.containsKey(indexName)) {
            try {
                createIndex(IEngine.UNITY, new Info(indexName));
            } catch (InstanceAlreadyExistsException | IOException ignore) {} catch (NoSuchMethodException e) {
                throw new RuntimeException(e);
            }
        }
        IEngine engine = indexMap.get(indexName);
        engine.put(engine, indexName, content);

        content.getLock(indexName).lock();
        try {
            while (!content.getIsNotified(indexName).get()) {
                content.getCondition(indexName).await();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw e;
        } finally {
            content.getLock(indexName).unlock();
        }
    }

    /**
     * Node获取数据data
     *
     * @param indexName 索引名（全名组合确保唯一性，如：库名+表名+索引名）
     * @param degree    主键
     * @param key       原始key
     */
    public byte[] getFirst(String indexName, long degree, String key) throws IOException {
        List<byte[]> bytesList = get(indexName, degree, key);
        if (Objects.nonNull(bytesList) && !bytesList.isEmpty()) {
            return bytesList.getFirst();
        }
        return null;
    }

    /**
     * Node获取数据data
     *
     * @param indexName 索引名（全名组合确保唯一性，如：库名+表名+索引名）
     * @param degree    主键
     * @param key       原始key
     */
    public byte[] getLast(String indexName, long degree, String key) throws IOException {
        List<byte[]> bytesList = get(indexName, degree, key);
        if (Objects.nonNull(bytesList) && !bytesList.isEmpty()) {
            return bytesList.getLast();
        }
        return null;
    }

    /**
     * Node获取数据data
     *
     * @param indexName 索引名（全名组合确保唯一性，如：库名+表名+索引名）
     * @param degree    主键
     * @param key       原始key
     */
    public List<byte[]> get(String indexName, long degree, String key) throws IOException {
        if (indexMap.containsKey(indexName)) {
            return indexMap.get(indexName).get(indexName, degree, key);
        }
        return null;
    }

    /**
     * Node获取数据data
     *
     * @param indexName 索引名（全名组合确保唯一性，如：库名+表名+索引名）
     * @param degree    主键
     * @param key       原始key
     */
    public void remove(String indexName, long degree, String key) throws IOException {
        indexMap.get(indexName).remove(indexName, degree, key);
    }

    /// 查询集合
    ///
    /// @param search 查询条件
    ///
    /// @throws IOException 异常
    public List<byte[]> select(Search search) throws IOException {
        String indexName = search.getIndexName();
        if (StringUtils.isEmpty(indexName)) {
            return new ArrayList<>();
        }
        if (indexMap.containsKey(indexName)) {
            return indexMap.get(indexName).select(search);
        } else {
            log.info("untrace select indexMap.get({}) is null", indexName);
            return null;
        }
    }

    /// 删除集合
    ///
    /// @param search 删除条件
    ///
    /// @throws IOException 异常
    public List<byte[]> delete(Search search) throws IOException {
        String indexName = search.getIndexName();
        if (indexMap.containsKey(indexName)) {
            return indexMap.get(indexName).delete(search);
        } else {
            log.info("untrace delete indexMap.get({}) is null", indexName);
            return null;
        }
    }

    /// 索引信息类
    @NoArgsConstructor
    @Data
    public static class Info {
        /// 版本号，4个字节
        int version = 1;
        /// 索引名称
        String name;
        /// 是否主键，主键也是唯一索引，1个字节
        boolean primary = false;
        /// 是否唯一索引，1个字节
        boolean unique = false;
        /// 是否允许为空，1个字节
        boolean nullable = true;

        /// 构造函数
        ///
        /// @param name 索引名称
        public Info(String name) {
            this.name = name;
        }

        /// 构造函数
        ///
        /// @param version  版本号，4个字节
        /// @param name     索引名称
        /// @param primary  是否主键，主键也是唯一索引，1个字节
        /// @param unique   是否唯一索引，1个字节
        /// @param nullable 是否允许为空，1个字节
        public Info(int version, String name, boolean primary, boolean unique, boolean nullable) {
            if (name.contains(".")) {
                throw new IllegalArgumentException("name cannot contain .");
            }
            this.version = version;
            this.name = name;
            this.primary = primary;
            this.unique = unique;
            this.nullable = nullable;
        }
    }

}
