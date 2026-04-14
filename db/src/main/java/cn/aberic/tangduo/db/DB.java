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
import cn.aberic.tangduo.common.file.Channel;
import cn.aberic.tangduo.common.file.Filer;
import cn.aberic.tangduo.common.file.Writer;
import cn.aberic.tangduo.db.common.*;
import cn.aberic.tangduo.db.entity.*;
import cn.aberic.tangduo.index.Index;
import cn.aberic.tangduo.index.engine.Common;
import cn.aberic.tangduo.index.engine.IEngine;
import cn.aberic.tangduo.index.engine.Transaction;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;

import javax.management.InstanceAlreadyExistsException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
public class DB {

    /** 库集合，库名+索引对象 */
    private final Map<String, SegIndex> dbMap = new ConcurrentHashMap<>();

    /** 数据库根路径 */
    String rootPath;
    /** 数据文件大小阈值，单位byte */
    long dataFileMaxSize;
    /** 单批次最大数量 */
    int batchMaxSize = INDEX_BATCH_MAX_SIZE;
    /** 每条索引检索的最大数据量，默认10000条 */
    int searchMaxCount = SEARCH_MAX_COUNT;

    private static final String blockSkip = "@@##@@";
    private static final String dbSkip = "#@#";
    /** 受保护的默认索引名称 */
    public static final String INDEX_NAME_DEFAULT = "default";
    /** 受保护的默认库名称 */
    public static final String DATABASE_NAME_DEFAULT = "default";
    public static final int SEARCH_MAX_COUNT = 10000;
    public static final int INDEX_BATCH_MAX_SIZE = 5000;

    private static final Lock lock = new ReentrantLock();
    private static DB instance;

    /**
     *
     * @param rootPath        数据根路径
     * @param dataFileMaxSize 数据文件大小阈值，单位byte
     */
    public static DB getInstance(String rootPath, long dataFileMaxSize) throws IOException, NoSuchFieldException {
        return getInstance(rootPath, dataFileMaxSize, SEARCH_MAX_COUNT, 5000);
    }

    /**
     *
     * @param rootPath        数据根路径
     * @param dataFileMaxSize 数据文件大小阈值，单位byte
     */
    public static DB getInstance(String rootPath, long dataFileMaxSize, int searchMaxCount, int batchMaxSize) throws IOException, NoSuchFieldException {
        if (instance == null) {
            lock.lock(); // 加锁
            try {
                if (instance == null) {
                    instance = new DB(rootPath, dataFileMaxSize, searchMaxCount, batchMaxSize);
                }
            } finally {
                lock.unlock(); // 释放锁
            }
        }
        return instance;
    }

    private DB() {}

    private DB(String rootPath, long dataFileMaxSize, int searchMaxCount, int batchMaxSize) throws IOException, NoSuchFieldException {
        this();
        this.rootPath = rootPath;
        this.dataFileMaxSize = dataFileMaxSize;
        this.searchMaxCount = searchMaxCount;
        this.batchMaxSize = batchMaxSize;
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
            dbMap.put(dbName, new SegIndex(seg, Index.getInstance(indexRootPath, dataFileMaxSize)));
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
        Filer.createDirectory(indexRootPath);
        dbMap.put(dbName, new SegIndex(seg, Index.getInstance(indexRootPath, dataFileMaxSize)));
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
                    dbMap.put(databaseName, new SegIndex(dbArr[1], Index.getInstance(indexRootPath, dataFileMaxSize)));
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
        Filer.deleteDirectory(Path.of(rootPath, databaseName));
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
    public DocPutResponseVO put(Object value) throws IOException {
        return put(null, value);
    }

    /**
     * Node插入数据data，主流方法
     *
     * @param value 数据
     */
    public DocPutResponseVO put(@Nullable String dbName, Object value) throws IOException {
        return put(dbName, null, value);
    }

    /**
     * Node插入数据data，主流方法
     *
     * @param key   原始key
     * @param value 数据
     */
    public DocPutResponseVO put(@Nullable String dbName, @Nullable String key, Object value) throws IOException {
        return put(dbName, null, key, value);
    }

    /**
     * Node插入数据data，此方法将进行分词插入处理
     *
     * @param indexName 索引名（全名组合确保唯一性，如：库名+表名+索引名）
     * @param key       原始key
     * @param value     数据
     */
    public DocPutResponseVO put(@Nullable String dbName, @Nullable String indexName, @Nullable String key, Object value) throws IOException {
        return put(dbName, indexName, key, true, value);
    }

    /**
     * Node插入数据data，此方法将进行分词插入处理
     *
     * @param indexName 索引名（全名组合确保唯一性，如：库名+表名+索引名）
     * @param key       原始key
     * @param value     数据
     */
    public DocPutResponseVO put(@Nullable String dbName, @Nullable String indexName, @Nullable String key, boolean seg, @Nonnull Object value) throws IOException {
        return put(new DocPutRequestVO(dbName, indexName, null, key, seg, value));
    }

    /** Node插入数据data */
    public DocPutResponseVO put(@Nonnull DocPutRequestVO batchVO) throws IOException {
        String dbName = StringUtils.isEmpty(batchVO.getDatabase()) ? DATABASE_NAME_DEFAULT : batchVO.getDatabase();
        String indexName = StringUtils.isEmpty(batchVO.getIndex()) ? INDEX_NAME_DEFAULT : batchVO.getIndex();
        String key = StringUtils.isEmpty(batchVO.getKey()) ? UUID.randomUUID().toString() : batchVO.getKey();
        long degree = Objects.isNull(batchVO.getDegree()) ? KeyHashTools.toLongKey(key) : batchVO.getDegree();
        Doc doc = new Doc(dbName, indexName, key, degree, batchVO.getValue());
        IEngine.Content content;
        if (!batchVO.isSeg()) {
            content = new IEngine.Content(new Transaction(), indexName, degree, key, doc.toBytes());
            Index index = getIndex(dbName);
            if (index == null) {
                throw new NoSuchFileException("数据库实例不存在");
            }
            index.put(content);
            return new DocPutResponseVO(doc, content);
        }
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
        List<IndexName4KeyAndDegree> list;
        String valueStr;
        switch (batchVO.getValue()) {
            case String _, List<?> _, Map<?, ?> _ -> {
                String str = String.valueOf(batchVO.getValue());
                if (JsonTools.isJson(str)) {
                    list = parseJsonStr2IndexName4KeyAndDegree(str);
                    valueStr = str;
                    for (IndexName4KeyAndDegree indexName4KeyAndDegree : list) {
                        if (indexName4KeyAndDegree.hash) {
                            valueStr = valueStr.replace(indexName4KeyAndDegree.key, "");
                        }
                    }
                } else {
                    list = new ArrayList<>();
                    if (HASH_PATTERN.matcher(str).matches()) {
                        valueStr = "";
                    } else {
                        valueStr = str;
                    }
                }
            }
            case null, default -> {
                list = new ArrayList<>();
                valueStr = "";
            }
        }
        List<String> indexNameList;
        if (segIndex.seg.equals("ik")) {
            indexNameList = IkTokenizerTools.tokenize(valueStr);
        } else {
            indexNameList = HanlpTools.segFilter(valueStr);
        }
        indexNameList.forEach(idxName -> {
            String indexName4datetime = CommonTools.indexName4datetime(idxName);
            if (list.stream().noneMatch(in4kd -> in4kd.indexName().equals(indexName4datetime))) {
                list.add(new IndexName4KeyAndDegree(indexName4datetime, key4datetime, degree4datetime, false));
            }
        });
        Index index = segIndex.index;
        doc.setSegList(indexNameList);
        content = new IEngine.Content(new Transaction(), indexName, degree, key, doc.toBytes());
        list.forEach(indexName4KeyAndDegree ->
                content.addItem(indexName4KeyAndDegree.indexName(), indexName4KeyAndDegree.degree(), indexName4KeyAndDegree.key()));
        index.put(content);
        return new DocPutResponseVO(doc, content);
    }

    /** Node批量插入数据data */
    public String put(String database, List<DocPutBatchRequestVO> batchRequestVOS) throws IOException {
        List<IEngine.Content> contentList = new ArrayList<>();
        Index baseIndex = null;
        for (DocPutBatchRequestVO batchRequestVO : batchRequestVOS) {
            String dbName = StringUtils.isEmpty(database) ? DATABASE_NAME_DEFAULT : database;
            String indexName = StringUtils.isEmpty(batchRequestVO.getIndex()) ? INDEX_NAME_DEFAULT : batchRequestVO.getIndex();
            String key = StringUtils.isEmpty(batchRequestVO.getKey()) ? UUID.randomUUID().toString() : batchRequestVO.getKey();
            long degree = Objects.isNull(batchRequestVO.getDegree()) ? KeyHashTools.toLongKey(key) : batchRequestVO.getDegree();
            Doc doc = new Doc(dbName, indexName, key, degree, batchRequestVO.getValue());
            IEngine.Content content;
            if (!batchRequestVO.isSeg()) {
                content = new IEngine.Content(indexName, degree, key, doc.toBytes());
                Index index = getIndex(dbName);
                if (index == null) {
                    throw new NoSuchFileException("数据库实例不存在");
                }
                if (Objects.isNull(baseIndex)) {
                    baseIndex = index;
                }
                contentList.add(content);
                continue;
            }
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
            List<IndexName4KeyAndDegree> list;
            String valueStr;
            switch (batchRequestVO.getValue()) {
                case String _, List<?> _, Map<?, ?> _ -> {
                    String str = String.valueOf(batchRequestVO.getValue());
                    if (JsonTools.isJson(str)) {
                        list = parseJsonStr2IndexName4KeyAndDegree(str);
                        valueStr = str;
                        for (IndexName4KeyAndDegree indexName4KeyAndDegree : list) {
                            if (indexName4KeyAndDegree.hash) {
                                valueStr = valueStr.replace(indexName4KeyAndDegree.key, "");
                            }
                        }
                    } else {
                        list = new ArrayList<>();
                        if (HASH_PATTERN.matcher(str).matches()) {
                            valueStr = "";
                        } else {
                            valueStr = str;
                        }
                    }
                }
                case null, default -> {
                    list = new ArrayList<>();
                    valueStr = "";
                }
            }
            List<String> indexNameList;
            if (segIndex.seg.equals("ik")) {
                indexNameList = IkTokenizerTools.tokenize(valueStr);
            } else {
                indexNameList = HanlpTools.segFilter(valueStr);
            }
            indexNameList.forEach(idxName -> {
                String indexName4datetime = CommonTools.indexName4datetime(idxName);
                if (list.stream().noneMatch(in4kd -> in4kd.indexName().equals(indexName4datetime))) {
                    list.add(new IndexName4KeyAndDegree(indexName4datetime, key4datetime, degree4datetime, false));
                }
            });
            if (Objects.isNull(baseIndex)) {
                baseIndex = segIndex.index;
            }
            doc.setSegList(indexNameList);
            content = new IEngine.Content(new Transaction(), indexName, degree, key, doc.toBytes());
            list.forEach(indexName4KeyAndDegree ->
                    content.addItem(indexName4KeyAndDegree.indexName(), indexName4KeyAndDegree.degree(), indexName4KeyAndDegree.key()));
            contentList.add(content);
        }
        if (Objects.isNull(baseIndex)) {
            throw new NoSuchFileException("无可用数据库实例");
        }
        baseIndex.put(contentList);
        return database;
    }

    /** 匹配 MD5 / SHA */
    private static final Pattern HASH_PATTERN = Pattern.compile("^[a-f0-9]{32}$|^[a-f0-9]{40}$|^[a-f0-9]{64}$|^[a-f0-9]{128}$|^[a-f0-9]{256}$|^[a-f0-9]{512}$|^[a-f0-9]{1024}$", Pattern.CASE_INSENSITIVE);

    /** 索引名+键信息+度信息+该条记录的唯一标记 */
    public record IndexName4KeyAndDegree(String indexName, String key, Long degree, boolean hash) {}

    /**
     * 将json中的key都建立数字索引、时间索引、HashKey索引。
     * 当json中某一key的value为number时，会根据key建立数字索引，同时根据当前时间戳建立时间索引。
     * 当json中某一key的value不为number时，建立HashKey索引和时间索引，如果该value长度和内容满足摘要条件时，额外建立摘要索引。
     * 已确认为摘要索引的value，在后续分词时，会被屏蔽掉，避免分词干扰。
     *
     * @param jsonStr json 字符串，如果不是，则返回空集合
     */
    public static List<IndexName4KeyAndDegree> parseJsonStr2IndexName4KeyAndDegree(String jsonStr) throws JsonProcessingException {
        List<IndexName4KeyAndDegree> list = new ArrayList<>();
        long degree = System.currentTimeMillis();
        String key = String.valueOf(degree);
        // 解析字符串
        JsonNode node = JsonTools.OBJECT_MAPPER.readTree(jsonStr);
        analysis(list, node, key, degree);
        return list;
    }

    /**
     * 将json中的key都建立数字索引、时间索引、HashKey索引。
     * 当json中某一key的value为number时，会根据key建立数字索引，同时根据当前时间戳建立时间索引。
     * 当json中某一key的value不为number时，建立HashKey索引和时间索引，如果该value长度和内容满足摘要条件时，额外建立摘要索引。
     * 已确认为摘要索引的value，在后续分词时，会被屏蔽掉，避免分词干扰。
     *
     * @param list   索引名+键信息+度信息+该条记录的唯一标记 集合，是本方法需要填充内容的对象
     * @param node   json 节点
     * @param key    键信息
     * @param degree 度信息
     */
    private static void analysis(List<IndexName4KeyAndDegree> list, JsonNode node, String key, long degree) {
        if (node.isArray()) {
            node.forEach(jsonNode -> analysis(list, jsonNode, key, degree));
        } else if (node.isObject()) {
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
                } else if (jsonNode.isObject()) {
                    analysis(list, jsonNode, key, degree);
                } else {
                    list.add(new IndexName4KeyAndDegree(CommonTools.indexName4datetime(indexName.toLowerCase()), key, degree, false));
                }
            });
        }
    }

    /**
     * Node获取数据data
     *
     * @param indexName 索引名（全名组合确保唯一性，如：库名+表名+索引名）
     * @param key       原始key
     */
    public DocGetResponseVO getFirst(@Nullable String dbName, @Nullable String indexName, @Nonnull String key) throws IOException {
        return getFirst(dbName, indexName, null, key);
    }

    /**
     * Node获取数据data
     *
     * @param indexName 索引名（全名组合确保唯一性，如：库名+表名+索引名）
     * @param degree    主键
     * @param key       原始key
     */
    public DocGetResponseVO getFirst(@Nullable String dbName, @Nullable String indexName, @Nullable Long degree, @Nonnull String key) throws IOException {
        List<DocGetResponseVO> docGetResponseVOList = get(dbName, indexName, degree, key);
        if (Objects.nonNull(docGetResponseVOList) && !docGetResponseVOList.isEmpty()) {
            return docGetResponseVOList.getFirst();
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
    public List<DocGetResponseVO> get(@Nullable String dbName, @Nullable String indexName, @Nullable Long degree, @Nullable String key) throws IOException {
        dbName = StringUtils.isEmpty(dbName) ? DATABASE_NAME_DEFAULT : dbName;
        indexName = StringUtils.isEmpty(indexName) ? INDEX_NAME_DEFAULT : indexName;
        List<DocGetResponseVO> docGetResponseVOList = new ArrayList<>();
        if (StringUtils.isEmpty(key)) {
            IEngine.Search search = new IEngine.Search(indexName, 10);
            List<byte[]> bytesList = select(dbName, search);
            bytesList.forEach(bytes -> {
                if (Objects.nonNull(bytes) && bytes.length > 0) {
                    try {
                        Doc doc = new Doc(bytes);
                        docGetResponseVOList.add(new DocGetResponseVO(doc));
                    } catch (JsonParseException e) {
                        log.warn("db get JsonParseException: {}", e.getMessage());
                    }
                }
            });
            return docGetResponseVOList;
        }
        degree = Objects.isNull(degree) ? KeyHashTools.toLongKey(key) : degree;
        Index index = getIndex(dbName);
        if (index == null) {
            throw new NoSuchFileException("数据库实例不存在");
        }
        List<byte[]> bytesList = index.get(indexName, degree, key);
        if (CollectionUtils.isEmpty(bytesList)) {
            return new ArrayList<>();
        }
        bytesList.forEach(bytes -> {
            if (Objects.nonNull(bytes) && bytes.length > 0) {
                try {
                    Doc doc = new Doc(bytes);
                    docGetResponseVOList.add(new DocGetResponseVO(doc));
                } catch (JsonParseException e) {
                    log.warn("db get JsonParseException: {}", e.getMessage());
                }
            }
        });
        return docGetResponseVOList;
    }

    /**
     * Node获取数据data
     *
     * @param query 检索字符串
     */
    public List<DocSearchResponseVO> search(String dbName, String query) throws IOException {
        return search(dbName, null, query, 10);
    }

    /**
     * Node获取数据data
     *
     * @param query 检索字符串
     */
    public List<DocSearchResponseVO> search(String dbName, String query, int callbackCount) throws IOException {
        return search(dbName, null, query, callbackCount);
    }

    /**
     * Node获取数据data
     *
     * @param query 检索字符串
     */
    public List<DocSearchResponseVO> search(String dbName, String indexName, String query, int callbackCount) throws IOException {
        return search(dbName, query, new IEngine.Search(indexName, callbackCount));
    }

    /** 查询集合 */
    public List<DocSearchResponseVO> search(String dbName, String query, IEngine.Search search) throws IOException {
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
        Map<String, DocSearchResponseVO> valueWithSegMap = new ConcurrentHashMap<>();
        if (StringUtils.isEmpty(search.getIndexName())) {
            // 外层循环并行虚拟线程
            try (ExecutorService parentExecutor = Executors.newVirtualThreadPerTaskExecutor()) {
                List<Future<?>> parentFutures = new ArrayList<>();
                for (String idxName : indexNameList) {
                    Future<?> parentFuture = parentExecutor.submit(() -> {
                        try {
                            List<byte[]> bytesList = segIndex.index.select(new IEngine.Search(idxName, search.getDegreeMin(), search.getDegreeMax(),
                                    search.isIncludeMin(), search.isIncludeMax(), searchMaxCount, false, this::doFilter));
                            if (!CollectionUtils.isEmpty(bytesList)) {
                                bytesList.forEach(bytes -> {
                                    try {
                                        Doc doc = new Doc(bytes);
                                        DocSearchResponseVO valueWithSeg = new DocSearchResponseVO(doc);
                                        valueWithSegMap.put(valueWithSeg.getDigests(), valueWithSeg);
                                    } catch (JsonParseException ignore) {}
                                });
                            }
                        } catch (IOException ignored) {}
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
        } else {
            List<byte[]> bytesList = segIndex.index.select(new IEngine.Search(search.getIndexName(), search.getDegreeMin(), search.getDegreeMax(),
                    search.isIncludeMin(), search.isIncludeMax(), searchMaxCount, false, this::doFilter));
            if (!CollectionUtils.isEmpty(bytesList)) {
                bytesList.forEach(bytes -> {
                    try {
                        Doc doc = new Doc(bytes);
                        DocSearchResponseVO valueWithSeg = new DocSearchResponseVO(doc);
                        valueWithSegMap.put(valueWithSeg.getDigests(), valueWithSeg);
                    } catch (JsonParseException ignore) {}
                });
            }
        }
        List<DocSearchResponseVO> docItems = Bm25Tools.rank(valueWithSegMap.values().stream().toList(), query, segIndex.seg);
        return docItems.subList(0, Math.min(search.getLimit(), docItems.size()));
    }

    private List<byte[]> doFilter(List<byte[]> bytesList, IEngine.Conditions conditions) {
        if (Objects.isNull(conditions)) {
            return bytesList;
        }
        if (CollectionUtils.isEmpty(conditions.getConditions())) {
            return bytesList;
        }
        return bytesList.stream().filter(bytes -> {
            DocSearchResponseVO docItem;
            try {
                Doc doc = new Doc(bytes);
                docItem = new DocSearchResponseVO(doc);
            } catch (JsonParseException ignore) {
                return false;
            }
            switch (docItem.getValue()) {
                case String s -> {
                    if (!JsonTools.isJson(s)) {
                        return false;
                    }
                }
                case List<?> _, Map<?, ?> _ -> {}
                case null, default -> {
                    return false;
                }
            }
            for (IEngine.Conditions.Condition condition : conditions.getConditions()) {
                Object obj;
                try {
                    obj = JsonTools.getValueByPath(JsonTools.toJson(docItem.getValue()), condition.getParam());
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

    /** 查询集合 */
    public List<DocSearchResponseVO> select1(String dbName, IEngine.Search search) throws IOException {
        Map<String, DocSearchResponseVO> valueWithSegMap = new ConcurrentHashMap<>();
        List<byte[]> bytesList = select(dbName, search);
        if (!CollectionUtils.isEmpty(bytesList)) {
            bytesList.forEach(bytes -> {
                try {
                    Doc doc = new Doc(bytes);
                    DocSearchResponseVO valueWithSeg = new DocSearchResponseVO(doc);
                    valueWithSegMap.put(valueWithSeg.getDigests(), valueWithSeg);
                } catch (JsonParseException ignore) {}
            });
        }
        List<DocSearchResponseVO> docItems = valueWithSegMap.values().stream().toList();
        return docItems.subList(0, Math.min(search.getLimit(), docItems.size()));
    }

    /** 查询集合 */
    public List<byte[]> select(String dbName, IEngine.Search search) throws IOException {
        Index index = getIndex(dbName);
        if (index == null) {
            throw new NoSuchFileException("数据库实例不存在");
        }
        if (Objects.isNull(search.getSearchFilter())) {
            search.setSearchFilter(this::doFilter);
        }
        return index.select(search);
    }

    /**
     * Node获取数据data
     *
     * @param indexName 索引名（全名组合确保唯一性，如：库名+表名+索引名）
     * @param key       原始key
     */
    public void remove(String dbName, String indexName, String key) throws IOException {
        remove(dbName, indexName, null, key);
    }

    /**
     * Node获取数据data
     *
     * @param indexName 索引名（全名组合确保唯一性，如：库名+表名+索引名）
     * @param degree    主键
     * @param key       原始key
     */
    public void remove(String dbName, String indexName, @Nullable Long degree, String key) throws IOException {
        degree = Objects.isNull(degree) ? KeyHashTools.toLongKey(key) : degree;
        Index index = getIndex(dbName);
        if (index == null) {
            throw new NoSuchFileException("索引实例不存在");
        }
        index.remove(indexName, degree, key);
    }

    /** 删除集合 */
    public List<byte[]> delete(String dbName, IEngine.Search search) throws IOException {
        Index index = getIndex(dbName);
        if (index == null) {
            throw new NoSuchFileException("索引实例不存在");
        }
        return index.delete(search);
    }

}
