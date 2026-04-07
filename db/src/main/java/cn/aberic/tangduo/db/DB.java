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
import cn.aberic.tangduo.common.SHA256Tools;
import cn.aberic.tangduo.common.file.Channel;
import cn.aberic.tangduo.common.file.Filer;
import cn.aberic.tangduo.common.file.Writer;
import cn.aberic.tangduo.db.common.*;
import cn.aberic.tangduo.common.JsonTools;
import cn.aberic.tangduo.index.Index;
import cn.aberic.tangduo.index.engine.Common;
import cn.aberic.tangduo.index.engine.IEngine;
import cn.aberic.tangduo.index.engine.Transaction;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import javax.management.InstanceAlreadyExistsException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.regex.Pattern;

@Slf4j
public class DB {

    /** 库集合，库名+索引对象 */
    private final Map<String, SegIndex> dbMap = new ConcurrentHashMap<>();

    /** 数据库根路径 */
    String rootPath;
    /** 数据文件大小阈值，单位byte */
    long dataFileMaxSize;
    /** 每条索引检索的最大数据量，默认10000条 */
    int searchMaxCount = SEARCH_MAX_COUNT;

    private static final String blockSkip = "@@##@@";
    private static final String dbSkip = "#@#";
    /** 受保护的默认索引名称 */
    public static final String INDEX_NAME_DEFAULT = "default";
    /** 受保护的默认库名称 */
    public static final String DATABASE_NAME_DEFAULT = "default";
    public static final int SEARCH_MAX_COUNT = 10000;

    private static final Lock lock = new ReentrantLock();
    private static DB instance;

    /**
     *
     * @param rootPath        数据根路径
     * @param dataFileMaxSize 数据文件大小阈值，单位byte
     */
    public static DB getInstance(String rootPath, long dataFileMaxSize) throws IOException, NoSuchFieldException {
        return getInstance(rootPath, dataFileMaxSize, SEARCH_MAX_COUNT);
    }

    /**
     *
     * @param rootPath        数据根路径
     * @param dataFileMaxSize 数据文件大小阈值，单位byte
     */
    public static DB getInstance(String rootPath, long dataFileMaxSize, int searchMaxCount) throws IOException, NoSuchFieldException {
        if (instance == null) {
            lock.lock(); // 加锁
            try {
                if (instance == null) {
                    instance = new DB(rootPath, dataFileMaxSize, searchMaxCount);
                }
            } finally {
                lock.unlock(); // 释放锁
            }
        }
        return instance;
    }

    private DB() {}

    private DB(String rootPath, long dataFileMaxSize, int searchMaxCount) throws IOException, NoSuchFieldException {
        this();
        this.rootPath = rootPath;
        this.dataFileMaxSize = dataFileMaxSize;
        this.searchMaxCount = searchMaxCount;
        init();
    }

    public record SegIndex(String seg, Index index) {}

    /**
     * 构造Master后最先执行的方法
     * 基于数据根路径获取索引相关信息<p>
     * 从指定文件中读取数据库和文件等关联信息，如不存在，则新建文件<p>
     * 文件内容：
     * 数据库名称@@##@@数据库名称
     */
    private void init() throws IOException, NoSuchFieldException {
        Path recordPath = Common.recordFilepath(rootPath); // 如"tmp/data/record.rd"
        if (!Files.exists(recordPath)) {
            Filer.createFile(recordPath);
        }
        String res = Files.readString(recordPath);
        if (StringUtils.isEmpty(res)) {
            return;
        }
        String[] blockArr = res.split(blockSkip);
        for (String block : blockArr) {
            String[] dbArr = block.split(dbSkip);
            String dbName = dbArr[0];
            String seg = dbArr[1];
            String indexRootPath = Path.of(rootPath, dbName).normalize().toString(); // 如"tmp/data/manage"
            dbMap.put(dbName, new SegIndex(seg, new Index(indexRootPath, dataFileMaxSize)));
        }
    }

    /**
     * 创建数据库
     *
     * @param dbName 数据库名称
     */
    public void createDB(String dbName) throws IOException, InstanceAlreadyExistsException, NoSuchFieldException {
        createDB(dbName, "hanlp");
    }

    /**
     * 创建数据库
     *
     * @param dbName 数据库名称
     * @param seg    分词方法，如：hanlp、ik
     */
    public void createDB(String dbName, String seg) throws IOException, InstanceAlreadyExistsException, NoSuchFieldException {
        // 遍历确认没有重复名称的索引
        if (dbMap.entrySet().stream().anyMatch(entry -> entry.getKey().equals(dbName))) {
            throw new InstanceAlreadyExistsException("数据库实例已存在");
        }
        Path recordPath = Common.recordFilepath(rootPath); // 如"tmp/data/record.rd"
        String format;
        if (Files.size(recordPath) == 0) {
            format = String.format("%s%s%s", dbName, dbSkip, seg);
        } else {
            format = String.format("%s%s%s%s", blockSkip, dbName, dbSkip, seg);
        }
        Channel.append(recordPath.toString(), format.getBytes(StandardCharsets.UTF_8));
        String indexRootPath = Path.of(rootPath, dbName).normalize().toString(); // 如"tmp/data/manage"
        dbMap.put(dbName, new SegIndex(seg, new Index(indexRootPath, dataFileMaxSize)));
    }

    /**
     * 删除数据库
     *
     * @param databaseName 数据库名称
     */
    public boolean dbExist(String databaseName) throws IOException, NoSuchFieldException {
        // 遍历确认没有重复名称的索引
        if (dbMap.entrySet().stream().noneMatch(entry -> entry.getKey().equals(databaseName))) {
            Path recordPath = Common.recordFilepath(rootPath); // 如"tmp/data/record.rd"
            String res = Files.readString(recordPath);
            if (StringUtils.isEmpty(res)) {
                return false;
            }
            String[] blockArr = res.split(blockSkip);
            for (String dbNameAndSeg : blockArr) {
                String[] dbArr = dbNameAndSeg.split(dbSkip);
                if (dbArr[0].equals(databaseName)) {
                    String indexRootPath = Path.of(rootPath, databaseName).normalize().toString(); // 如"tmp/data/manage"
                    dbMap.put(databaseName, new SegIndex(dbArr[1], new Index(indexRootPath, dataFileMaxSize)));
                    return true;
                }
            }
            return false;
        }
        return true;
    }

    /**
     * 删除数据库
     *
     * @param databaseName 数据库名称
     */
    public void removeDB(String databaseName) throws IOException {
        // 遍历确认没有重复名称的索引
        if (dbMap.entrySet().stream().noneMatch(entry -> entry.getKey().equals(databaseName))) {
            return;
        }
        Path recordPath = Common.recordFilepath(rootPath); // 如"tmp/data/record.rd"
        String res = Files.readString(recordPath);
        if (StringUtils.isEmpty(res)) {
            return;
        }
        StringBuilder sb = new StringBuilder();
        String[] blockArr = res.split(blockSkip);
        for (String dbNameAndSeg : blockArr) {
            String[] dbArr = dbNameAndSeg.split(dbSkip);
            if (dbArr[0].equals(databaseName)) {
                continue;
            }
            if (!sb.isEmpty()) {
                sb.append(blockSkip).append(dbNameAndSeg);
            } else {
                sb.append(dbNameAndSeg);
            }
        }
        Writer.write(recordPath.toString(), sb.toString().getBytes(StandardCharsets.UTF_8));
        dbMap.remove(databaseName);
    }

    private Index getIndex(String dbName) {
        SegIndex segIndex = dbMap.get(dbName);
        if (segIndex == null) {
            return null;
        }
        return segIndex.index;
    }

    /**
     * 创建索引
     *
     * @param engine 引擎，在 Engine.UNITY、Engine.SKIP 等中选值，或传入对应的整型值
     * @param info   索引信息
     */
    public void createIndex(String dbName, int engine, Index.Info info) throws IOException, InstanceAlreadyExistsException, NoSuchFieldException, NoSuchMethodException {
        if (!dbMap.containsKey(dbName)) {
            throw new NoSuchElementException("数据库实例不存在");
        }
        Index index = dbMap.get(dbName).index;
        info.setName(CommonTools.indexName(info.getName()));
        index.createIndex(engine, info);
    }

    /**
     * Node插入数据data，主流方法
     *
     * @param value 数据
     */
    public IEngine.Content put(String value) throws IOException {
        return put(DATABASE_NAME_DEFAULT, value);
    }

    /**
     * Node插入数据data，主流方法
     *
     * @param value 数据
     */
    public IEngine.Content put(String dbName, String value) throws IOException {
        String key = SHA256Tools.sha256(value);
        return put(dbName, key, value);
    }

    /**
     * Node插入数据data，主流方法
     *
     * @param key   原始key
     * @param value 数据
     */
    public IEngine.Content put(String dbName, String key, String value) throws IOException {
        return put(dbName, INDEX_NAME_DEFAULT, key, value);
    }

    /**
     * Node插入数据data，此方法将进行分词插入处理
     *
     * @param indexName 索引名（全名组合确保唯一性，如：库名+表名+索引名）
     * @param key       原始key
     * @param value     数据
     */
    public IEngine.Content put(String dbName, String indexName, String key, String value) throws IOException {
        SegIndex segIndex = dbMap.get(dbName);
        if (segIndex == null) {
            if (dbName.equals(DATABASE_NAME_DEFAULT)) {
                try {
                    createDB(DATABASE_NAME_DEFAULT);
                    createIndex(DATABASE_NAME_DEFAULT, IEngine.UNITY, new Index.Info(1, INDEX_NAME_DEFAULT, false, false, false));
                    segIndex = dbMap.get(dbName);
                } catch (InstanceAlreadyExistsException | NoSuchFieldException | NoSuchMethodException e) {
                    segIndex = dbMap.get(dbName);
                    if (segIndex == null) {
                        throw new NoSuchFileException("数据库实例不存在");
                    }
                }
            } else {
                throw new NoSuchFileException("数据库实例不存在");
            }
        }
        long degree4datetime = System.currentTimeMillis();
        String key4datetime = String.valueOf(degree4datetime);
        List<IndexName4KeyAndDegree> list = parseIndexName4KeyAndDegree(value);
        String valueTmp = value;
        for (IndexName4KeyAndDegree indexName4KeyAndDegree : list) {
            if (indexName4KeyAndDegree.hash) {
                valueTmp = valueTmp.replace(indexName4KeyAndDegree.key, "");
            }
        }
        List<String> indexNameList;
        if (segIndex.seg.equals("ik")) {
            indexNameList = IkTokenizerTools.tokenize(valueTmp);
        } else {
            indexNameList = HanlpTools.segFilter(valueTmp);
        }
        indexNameList.forEach(idxName -> {
            String indexName4datetime = CommonTools.indexName4datetime(idxName);
            if (list.stream().noneMatch(in4kd -> in4kd.indexName().equals(indexName4datetime))) {
                list.add(new IndexName4KeyAndDegree(indexName4datetime, key4datetime, degree4datetime, false));
            }
        });
        Index index = segIndex.index;
        long degree = KeyHashTools.toLongKey(key);
        IEngine.Content content = new IEngine.Content(new Transaction(), CommonTools.indexName(indexName), degree, key, valueToBytes(value, indexNameList));
        list.forEach(indexName4KeyAndDegree ->
                content.addItem(indexName4KeyAndDegree.indexName(), indexName4KeyAndDegree.degree(), indexName4KeyAndDegree.key()));
        index.put(content);
        return content;
    }

    // 匹配 MD5 / SHA
    private static final Pattern HASH_PATTERN = Pattern.compile("^[a-f0-9]{32}$|^[a-f0-9]{40}$|^[a-f0-9]{64}$|^[a-f0-9]{128}$|^[a-f0-9]{256}$|^[a-f0-9]{512}$|^[a-f0-9]{1024}$", Pattern.CASE_INSENSITIVE);
    public record IndexName4KeyAndDegree(String indexName, String key, Long degree, boolean hash) {}
    public static List<IndexName4KeyAndDegree> parseIndexName4KeyAndDegree(String str) throws JsonProcessingException {
        List<IndexName4KeyAndDegree> list = new ArrayList<>();
        if (Objects.isNull(str) || !JsonTools.isJson(str)) {
            return list;
        }
        long degree = System.currentTimeMillis();
        String key = String.valueOf(degree);
        // 解析字符串
        JsonNode node = JsonTools.OBJECT_MAPPER.readTree(str);
        analysis(list, node, key, degree);
        return list;
    }
    private static void analysis(List<IndexName4KeyAndDegree> list, JsonNode node, String key, long degree) {
        if (node.isArray()) {
            node.forEach(jsonNode -> analysis(list, jsonNode, key, degree));
        }  else if (node.isObject()) {
            node.forEachEntry((indexName, jsonNode) -> {
                if (jsonNode.isTextual()) {
                    if (HASH_PATTERN.matcher(jsonNode.asText()).matches()) {
                        list.add(new IndexName4KeyAndDegree(CommonTools.indexName(indexName.toLowerCase()), jsonNode.asText(), KeyHashTools.toLongKey(jsonNode.asText()), false));
                        list.add(new IndexName4KeyAndDegree(CommonTools.indexName4dhash(jsonNode.asText()), jsonNode.asText(), degree, true));
                    }
                    list.add(new IndexName4KeyAndDegree(CommonTools.indexName4datetime(indexName.toLowerCase()), key, degree, false));
                } else if (jsonNode.isLong() || jsonNode.isInt()) {
                    list.add(new IndexName4KeyAndDegree(CommonTools.indexName(indexName.toLowerCase()), jsonNode.asText(), jsonNode.asLong(), false));
                    list.add(new IndexName4KeyAndDegree(CommonTools.indexName4datetime(indexName.toLowerCase()), key, degree, false));
                } else if (jsonNode.isDouble() || jsonNode.isFloat()) {
                    list.add(new IndexName4KeyAndDegree(CommonTools.indexName(indexName.toLowerCase()), jsonNode.asText(), KeyHashTools.toLongKey(jsonNode.asDouble()), false));
                    list.add(new IndexName4KeyAndDegree(CommonTools.indexName4datetime(indexName.toLowerCase()), key, degree, false));
                } else if (jsonNode.isArray()) {
                    node.forEach(jn -> analysis(list, jn, key, degree));
                }  else if (jsonNode.isObject()) {
                    analysis(list, jsonNode, key, degree);
                } else {
                    list.add(new IndexName4KeyAndDegree(CommonTools.indexName4datetime(indexName.toLowerCase()), key, degree, false));
                }
            });
        }
    }

    private byte[] valueToBytes(String value, List<String> segList) {
        return ByteTools.fromString(SHA256Tools.sha256(value) + "###@@@###" + value + "###@@@###" + ListTools.toString(segList));
    }

    private Bm25Tools.DocItem bytesToDocItem(byte[] value) {
        String valueStr = ByteTools.toString(value);
        String[] segList = valueStr.split("###@@@###");
        return new Bm25Tools.DocItem(segList[0], segList[1], ListTools.fromString(segList[2]));
    }

    /** Node插入数据data，此方法无分词插入处理 */
    public void put(String dbName, IEngine.Content content) throws IOException {
        Index index = getIndex(dbName);
        if (index == null) {
            throw new NoSuchFileException("数据库实例不存在");
        }
        index.put(content);
    }

    /**
     * Node获取数据data
     *
     * @param indexName 索引名（全名组合确保唯一性，如：库名+表名+索引名）
     * @param degree    主键
     * @param key       原始key
     */
    public byte[] getFirst(String dbName, String indexName, long degree, String key) throws IOException {
        List<byte[]> bytesList = get(dbName, indexName, degree, key);
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
    public List<byte[]> get(String dbName, String indexName, long degree, String key) throws IOException {
        Index index = getIndex(dbName);
        if (index == null) {
            throw new NoSuchFileException("数据库实例不存在");
        }
        return index.get(indexName, degree, key);
    }

    /**
     * Node获取数据data
     *
     * @param query 检索字符串
     */
    public List<Bm25Tools.DocItem> get(String dbName, String query) throws IOException {
        return get(dbName, null, query, 10);
    }

    /**
     * Node获取数据data
     *
     * @param query 检索字符串
     */
    public List<Bm25Tools.DocItem> get(String dbName, String query, int callbackCount) throws IOException {
        return get(dbName, null, query, callbackCount);
    }

    /**
     * Node获取数据data
     *
     * @param query 检索字符串
     */
    public List<Bm25Tools.DocItem> get(String dbName, String indexName, String query, int callbackCount) throws IOException {
        SegIndex segIndex = dbMap.get(dbName);
        if (segIndex == null) {
            throw new NoSuchFileException("数据库实例不存在");
        }
        List<String> indexNameList;
        if (segIndex.seg.equals("ik")) {
            indexNameList = IkTokenizerTools.seg(query);
        } else {
            indexNameList = HanlpTools.seg(query);
        }
        if (HASH_PATTERN.matcher(query).matches()) {
            indexNameList.add(CommonTools.indexName4dhash(query));
        }
        Map<String, Bm25Tools.DocItem> valueWithSegMap = new ConcurrentHashMap<>();
        if (StringUtils.isEmpty(indexName)) {
            CountDownLatch latch = new CountDownLatch(indexNameList.size()); // 计数3
            indexNameList.forEach(idxName -> Thread.startVirtualThread(() -> {
                try {
                    List<byte[]> bytesList = segIndex.index.select(new IEngine.Search(idxName, searchMaxCount, false));
                    if (Objects.nonNull(bytesList) && !bytesList.isEmpty()) {
                        bytesList.forEach(bytes -> {
                            Bm25Tools.DocItem valueWithSeg = bytesToDocItem(bytes);
                            valueWithSegMap.put(valueWithSeg.getId(), valueWithSeg);
                        });
                    }
                } catch (IOException ignored) {
                } finally {
                    latch.countDown();
                }
            }));
            try {
                latch.await();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        } else {
            List<byte[]> bytesList = segIndex.index.select(new IEngine.Search(indexName, callbackCount, false));
            if (Objects.nonNull(bytesList) && !bytesList.isEmpty()) {
                bytesList.forEach(bytes -> {
                    Bm25Tools.DocItem valueWithSeg = bytesToDocItem(bytes);
                    valueWithSegMap.put(valueWithSeg.getId(), valueWithSeg);
                });
            }
        }
        List<Bm25Tools.DocItem> docItems = Bm25Tools.rank(valueWithSegMap.values().stream().toList(), query, segIndex.seg);
        return docItems.subList(0, Math.min(callbackCount, docItems.size()));
    }

    /**
     * Node获取数据data
     *
     * @param indexName 索引名（全名组合确保唯一性，如：库名+表名+索引名）
     * @param degree    主键
     * @param key       原始key
     */
    public void remove(String dbName, String indexName, long degree, String key) throws IOException {
        Index index = getIndex(dbName);
        if (index == null) {
            throw new NoSuchFileException("数据库实例不存在");
        }
        index.remove(indexName, degree, key);
    }

    /** 查询集合 */
    public List<byte[]> select(String dbName, IEngine.Search search) throws IOException {
        Index index = getIndex(dbName);
        if (index == null) {
            throw new NoSuchFileException("数据库实例不存在");
        }
        return index.select(search);
    }

    /** 删除集合 */
    public List<byte[]> delete(String dbName, IEngine.Search search) throws IOException {
        Index index = getIndex(dbName);
        if (index == null) {
            throw new NoSuchFileException("数据库实例不存在");
        }
        return index.delete(search);
    }

}
