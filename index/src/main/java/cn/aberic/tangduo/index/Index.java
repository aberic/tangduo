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
import cn.aberic.tangduo.index.engine.Common;
import cn.aberic.tangduo.index.engine.IEngine;
import cn.aberic.tangduo.index.engine.skip.Skip;
import cn.aberic.tangduo.index.engine.unity.Unity;
import org.apache.commons.lang3.StringUtils;

import javax.management.InstanceAlreadyExistsException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.rmi.UnexpectedException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 索引接口<p>
 * 暴露索引相关使用接口
 */
public class Index {

    private final Map<String, IEngine> engineMap = new HashMap<>();

    private static final String blockSkip = "##@@##";
    private static final String indexSkip = "@#@";

    /** 数据文件大小阈值，单位byte，默认10GB */
    private static final long DATA_FILE_MAX_SIZE = 10737418240L;

    /** 数据根路径 */
    String rootPath;
    /** 数据文件大小阈值，单位byte */
    long dataFileMaxSize;

    private static final Lock lock = new ReentrantLock();
    private static Index instance;

    public static Index getInstance(String rootPath) throws IOException, NoSuchFieldException {
        return getInstance(rootPath, DATA_FILE_MAX_SIZE);
    }

    public static Index getInstance(String rootPath, long dataFileMaxSize) throws IOException, NoSuchFieldException {
        if (instance == null) {
            lock.lock(); // 加锁
            try {
                if (instance == null) {
                    instance = new Index(rootPath, dataFileMaxSize);
                }
            } finally {
                lock.unlock(); // 释放锁
            }
        }
        return instance;
    }

    private Index() {}

    private Index(String rootPath, long dataFileMaxSize) throws IOException, NoSuchFieldException {
        this();
        this.rootPath = rootPath;
        this.dataFileMaxSize = Math.max(dataFileMaxSize, DATA_FILE_MAX_SIZE);
        init();
    }

    /**
     * 构造Master后最先执行的方法
     * 基于数据根路径获取索引相关信息<p>
     * 从指定文件中读取索引和文件等关联信息，如不存在，则新建文件<p>
     * 文件内容：
     * 索引名称@#@索引文件地址@#@索引引擎@#@索引文件版本号##@@##索引名称@#@索引文件地址@#@索引引擎@#@索引文件版本号
     */
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
            engineMap.put(indexName, engine(indexEngine, indexName));
        }
    }

    private IEngine engine(int indexEngine, String indexName) throws NoSuchFieldException {
        switch (indexEngine) {
            case IEngine.UNITY -> {
                return new Unity(rootPath, indexName, dataFileMaxSize, 200);
            }
            case IEngine.SKIP -> {
                return new Skip();
            }
            default -> throw new NoSuchFieldException("入参引擎不存在");
        }
    }

    /**
     * 创建索引
     *
     * @param engine    引擎，在 Engine.UNITY、Engine.SKIP 等中选值，或传入对应的整型值
     * @param version   版本号
     * @param indexName 索引名称
     * @param primary   是否主键，主键也是唯一索引
     * @param unique    是否唯一索引
     * @param nullable  是否允许为空
     */
    public void createIndex(int engine, int version, String indexName, boolean primary, boolean unique, boolean nullable, int capacity) throws NoSuchMethodException, IOException, InstanceAlreadyExistsException {
        switch (engine) {
            case IEngine.UNITY -> createUnityIndex(version, indexName, primary, unique, nullable, capacity);
            case IEngine.SKIP -> throw new NoSuchMethodException("方法未实现");
            default -> throw new UnexpectedException("传参未匹配");
        }
    }

    /**
     * 创建索引和文件等关联信息<p>
     * 文件内容类似：
     * 索引引擎@#@索引名称@#@索引文件地址@#@索引文件版本号##@@##索引引擎@#@索引名称@#@索引文件地址@#@索引文件版本号
     *
     * @param version   版本号
     * @param indexName 索引名称
     * @param primary   是否主键，主键也是唯一索引
     * @param unique    是否唯一索引
     * @param nullable  是否允许为空
     */
    private synchronized void createUnityIndex(int version, String indexName, boolean primary, boolean unique, boolean nullable, int capacity) throws InstanceAlreadyExistsException, IOException {
        // 遍历确认没有重复名称的索引
        for (Map.Entry<String, IEngine> engineEntry : engineMap.entrySet()) {
            if (indexName.equals(engineEntry.getKey())) {
                throw new InstanceAlreadyExistsException("索引实例已存在");
            }
        }
        Unity unity = new Unity(rootPath, 1, 1, dataFileMaxSize, version, indexName, primary, unique, nullable, capacity);
        engineMap.put(indexName, unity);
        Path recordPath = Common.recordFilepath(rootPath); // 如"tmp/record.rd"
        String format;
        if (Files.size(recordPath) == 0) {
            format = String.format("%s@#@%s", IEngine.UNITY, indexName);
        } else {
            format = String.format("##@@##%s@#@%s", IEngine.UNITY, indexName);
        }
        Channel.append(recordPath.toString(), format.getBytes(StandardCharsets.UTF_8));
    }

    /** 刷盘 */
    public void force(long degree, String indexName) throws IOException {
        engineMap.get(indexName).force(degree, indexName);
    }

    /** Node插入数据data */
    public void set(IEngine.Content content) throws IOException {
        engineMap.get(content.getIndexName()).set(content);
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
        content.getTransaction().execute();
    }

    /**
     * Node获取数据data
     *
     * @param indexName 索引名（全名组合确保唯一性，如：库名+表名+索引名）
     * @param degree    主键
     * @param key       原始key
     */
    public byte[] get(String indexName, long degree, String key) throws IOException {
        return engineMap.get(indexName).get(indexName, degree, key);
    }

    /**
     * Node获取数据data
     *
     * @param indexName 索引名（全名组合确保唯一性，如：库名+表名+索引名）
     * @param degree    主键
     * @param key       原始key
     */
    public void remove(String indexName, long degree, String key) throws IOException {
        engineMap.get(indexName).remove(indexName, degree, key);
    }

    /** 查询集合 */
    public List<byte[]> select(IEngine.Search search) throws IOException {
        return engineMap.get(search.getIndexName()).select(search);
    }

    /** 删除集合 */
    public List<byte[]> delete(IEngine.Search search) throws IOException {
        return engineMap.get(search.getIndexName()).delete(search);
    }

}
