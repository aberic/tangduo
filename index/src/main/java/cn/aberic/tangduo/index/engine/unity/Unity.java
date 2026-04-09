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
import cn.aberic.tangduo.common.DateTools;
import cn.aberic.tangduo.common.file.Channel;
import cn.aberic.tangduo.common.file.Filer;
import cn.aberic.tangduo.common.file.Reader;
import cn.aberic.tangduo.index.engine.Common;
import cn.aberic.tangduo.index.engine.IEngine;
import cn.aberic.tangduo.index.engine.unity.entity.Leaf;
import cn.aberic.tangduo.index.engine.unity.entity.Node;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.rmi.UnexpectedException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.Stream;

/**
 * Btree联合索引，所有使用此引擎的索引共用数据结构<p>
 * 即一个索引纳管所有使用该引擎的数据<p>
 * 包含【2字节节点默认声明、4个字节版本号、1个字节是否主键、1个字节是否唯一索引、1个字节是否允许为空、8个字节创建时间、1005个字节冗余、2字节节点默认收尾】，后面为正式数据的首个数据<p>
 */
@Slf4j
public class Unity extends IEngine {
    /** 默认度 */
    private static final long DEGREE = 4294967296L;
    /** 索引根节点偏移量 */
    private static final long ROOT_NODE_SEEK = 1024;

    /** 节点默认声明，值非默认即异常 */
    public static byte[] startBytes = {0x00, 0x7A};
    /** 索引详情：4个字节版本号、1个字节是否主键、1个字节是否唯一索引、1个字节是否允许为空、8个字节创建时间，总计15个字节，总计15个字节 */
    byte[] childIndexBytes;
    /** 1005个字节冗余 */
    byte[] bakBytes = new byte[1005];
    /** 节点默认收尾，值非默认即异常 */
    public static byte[] endBytes = {0x00, 0x7A};

    /** 数据根路径 */
    String rootPath;
    /** 索引详情：4个字节版本号、1个字节是否主键、1个字节是否唯一索引、1个字节是否允许为空、8个字节创建时间，总计15个字节 */
    ChildIndex childIndex;
    /** 数据文件版本号，如 1，与索引版本号结合使用，如1.1，区分相同索引下的不同数据文件 */
    int dataFileVersion = 1;
    /** 数据文件大小阈值，单位byte */
    long dataFileMaxSize;

    // 无锁队列，线程安全
    private final LinkedBlockingQueue<Content> queue;

    /** 新建索引文件内容 */
    public Unity(String rootPath, long degree, int dataFileVersion, long dataFileMaxSize, int version, String indexName, boolean primary, boolean unique, boolean nullable) throws IOException {
        this.rootPath = rootPath;
        this.dataFileVersion = dataFileVersion;
        this.dataFileMaxSize = dataFileMaxSize;
        childIndex = new ChildIndex(version, indexName, primary, unique, nullable);
        childIndexBytes = childIndex.toBytes();
        String degreeInterval = getDegreeInterval(degree);
        String indexFilepath = Common.unityIndexFilepath(rootPath, indexName, degreeInterval).toString();
        Filer.createFile(indexFilepath); // tmp/setAndGetBatch/unity/test/1_4294967296.1.idx
        // 初始化元数据 + 创世节点
        // 2字节节点默认声明、4个字节版本号、1个字节是否主键、1个字节是否唯一索引、1个字节是否允许为空、8个字节创建时间、1005个字节冗余、2字节节点默认收尾
        byte[] dataBytes = ByteTools.join(startBytes, childIndexBytes, bakBytes, endBytes, new Node().toBytes());
        Channel.append(indexFilepath, dataBytes);
        queue = new LinkedBlockingQueue<>();
        startSetThread(indexName);
    }

    /**
     * 根据索引文件解析初始索引信息
     *
     * @param rootPath        数据根路径
     * @param dataFileMaxSize 数据文件大小阈值，单位byte
     */
    public Unity(String rootPath, String indexName, long dataFileMaxSize) throws IOException {
        this.rootPath = rootPath;
        this.dataFileMaxSize = dataFileMaxSize;
        String degreeInterval = getDegreeInterval(1);
        String indexFilepath = Common.unityIndexFilepath(rootPath, indexName, degreeInterval).toString();
        // 4个字节版本号、1个字节是否主键、1个字节是否唯一索引、1个字节是否允许为空、8个字节创建时间、1005个字节冗余、2字节节点默认收尾
        childIndexBytes = Reader.read(indexFilepath, 2, 15);
        childIndex = new ChildIndex(childIndexBytes);
        queue = new LinkedBlockingQueue<>();
        startSetThread(indexName);
    }

    // 单线程真正写入磁盘（无竞争，最快）
    private void startSetThread(String indexName) {
        new Thread(() -> {
            try {
                while (true) {
                    Content content = queue.take(); // 阻塞取数据
                    Path indexFilepath = getIndexFilepath(content.getDegree(indexName), indexName);
                    put(content, indexName, indexFilepath.toString());
                    content.getLock(indexName).lock();
                    try {
                        content.getIsNotified(indexName).set(true);
                        content.getCondition(indexName).signal();
                    } finally {
                        content.getLock(indexName).unlock();
                    }
                }
            } catch (InterruptedException e) {
                log.error("Set Thread {} InterruptedException, {}", indexName, e.getMessage(), e);
            } catch (IOException e) {
                log.error("Set Thread {} IOException, {}", indexName, e.getMessage(), e);
            } catch (Exception e) {
                log.error("Set Thread {} RuntimeException, {}", indexName, e.getMessage(), e);
            }
        }, indexName).start();
    }

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

    /** 获取度文件区间，如：0_4294967296 或 neg_9223371968135299072_9223371972430266367.idx */
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

    /** 如：tmp/unity/testIndex/1_4294967296.idx */
    private Path getIndexFilepath(long degree, String indexName) {
        return Common.unityIndexFilepath(rootPath, indexName, getDegreeInterval(degree));
    }

    /** 刷盘 */
    @Override
    public void force(long degree, String indexName) throws IOException {
        Path indexFilepath = getIndexFilepath(degree, indexName);
        Channel.force(indexFilepath.toString());
    }

    /**
     * Node插入数据data<p>
     */
    @Override
    public void put(Content content) {
        queue.offer(content);
    }

    /**
     * Node插入数据data<p>
     * -------------------------------------------------------------------------------------------------------------------------------------<p>
     * | 1…256(16777216)                         | 1层：1个节点，每个节点含256个节点和16777216个数据坐标<p>
     * -------------------------------------------------------------------------------------------------------------------------------------<p>
     * | 1…256 | … | 65280…65536(65536)          | 2层：256个节点，每个节点含256个节点和65536个数据坐标<p>
     * -------------------------------------------------------------------------------------------------------------------------------------<p>
     * | 1…256 | … | 16776960…16777216(256)      | 3层：65536个节点，每个节点含256个节点和256个数据坐标<p>
     * -------------------------------------------------------------------------------------------------------------------------------------<p>
     * | 1…256 | … | 4294966940…4294967296       | 4层：16777216个节点，每个节点含256个数据坐标，总计4294967296个数据坐标<p>
     * -------------------------------------------------------------------------------------------------------------------------------------<p>
     * <p>
     */
    public void put(IEngine.Content content, String indexName, String indexFilepath) throws Exception {
        long degree = reDegree(content.getDegree(indexName));
        // 获取2层节点
        long nextPosition = Math.divideExact(degree, 16777216); // 度位置; degree=4294967056; nextPosition=255; 4294967296-4294967056=240
        long nextNodeMateSeek = ROOT_NODE_SEEK + 2 + nextPosition * 8; // 向下一节点的数据坐标值在索引文件中的起始偏移量
        if (!new File(indexFilepath).exists()) {
            log.trace("transactionId = {}, indexFilepath {} not found!", content.getTransaction().getNumber(), indexFilepath);
            fillNodeLeaf(content, indexName, indexFilepath, degree, nextPosition, 4, nextNodeMateSeek);
            return;
        }
        // 获取根节点
        Node node = new Node(indexFilepath, ROOT_NODE_SEEK, false);
        // 获取2层节点
        long nextNodeSeek = ByteTools.toLong(ByteTools.read(node.getData(), nextPosition * 8, 8));
        if (nextNodeSeek <= 0) {
            fillNodeLeaf(content, indexName, indexFilepath, degree, nextPosition, 3, nextNodeMateSeek);
            return;
        }
        // 2层节点在索引文件中的起始偏移量
        Node nextNode = new Node(indexFilepath, nextNodeMateSeek, nextNodeSeek); // 2层节点
        // 获取3层节点
        degree = degree - nextPosition * 16777216; // degree=4294967056-255*16777216=4294967056-4278190080=16776976
        nextPosition = Math.divideExact(degree, 65536); // degree=16776976; nextPosition=255; 16777216-16776976=240
        nextNodeMateSeek = nextNode.getSeek() + 2 + nextPosition * 8; // 向下一节点的数据坐标值在索引文件中的起始偏移量
        nextNodeSeek = ByteTools.toLong(ByteTools.read(nextNode.getData(), nextPosition * 8, 8));
        if (nextNodeSeek <= 0) {
            fillNodeLeaf(content, indexName, indexFilepath, degree, nextPosition, 2, nextNodeMateSeek);
            return;
        }
        // // 3层节点在索引文件中的起始偏移量
        nextNode = new Node(indexFilepath, nextNodeMateSeek, nextNodeSeek); // 3层节点
        // 获取4层节点
        degree = degree - nextPosition * 65536; // degree=16776976-255*65536=16776976-16711680=65296
        nextPosition = Math.divideExact(degree, 256); // degree=65296; nextPosition=255; 65536-65296=240
        nextNodeMateSeek = nextNode.getSeek() + 2 + nextPosition * 8; // 向下一节点的数据坐标值在索引文件中的起始偏移量
        nextNodeSeek = ByteTools.toLong(ByteTools.read(nextNode.getData(), nextPosition * 8, 8));
        if (nextNodeSeek <= 0) {
            fillNodeLeaf(content, indexName, indexFilepath, degree, nextPosition, 1, nextNodeMateSeek);
            return;
        }
        // 4层节点在索引文件中的起始偏移量
        nextNode = new Node(indexFilepath, nextNodeMateSeek, ByteTools.toLong(ByteTools.read(nextNode.getData(), nextPosition * 8, 8))); // 4层节点
        // 获取Leaf信息
        degree = degree - nextPosition * 256; // degree=65296-255*256=65296-65280=16
        nextPosition = Math.divideExact(degree, 1); // degree=16; nextPosition=16; 256-16=240
        long leafMateSeek = nextNode.getSeek() + 2 + nextPosition * 8; // 向下一节点的数据坐标值在索引文件中的起始偏移量
        nextNode.put(content, childIndex, rootPath, indexName, nextPosition, leafMateSeek, dataFileVersion, dataFileMaxSize);
    }

    private void fillNodeLeaf(IEngine.Content content, String indexName, String indexFilepath, long degree, long nextPosition, int nodeCount, long nodeMateSeek) throws Exception {
        byte[] data; // 待写入字节数组
        Node node = new Node(); // 单个节点
        byte[] nodeBytes = node.toBytes(); // 单个节点的字节数组
        Leaf leaf = new Leaf(childIndex, indexFilepath, -1); // 叶子节点
        switch (nodeCount) {
            case 1 -> {
                data = ByteTools.join(nodeBytes);
                long nodeSeek = Channel.append(indexFilepath, data);
                Channel.write(indexFilepath, nodeMateSeek, ByteTools.fromLong(nodeSeek)); // 更新下一节点在本节点的持久化数据
                long leafMateSeek = getLeafMateSeek(indexFilepath, degree, nextPosition, 256, nodeSeek);
                leaf.put(content, rootPath, indexName, leafMateSeek, dataFileVersion, dataFileMaxSize);
            }
            case 2 -> {
                data = ByteTools.join(nodeBytes, nodeBytes);
                long nodeSeek = Channel.append(indexFilepath, data);
                Channel.write(indexFilepath, nodeMateSeek, ByteTools.fromLong(nodeSeek)); // 更新下一节点在本节点的持久化数据
                long leafMateSeek = getLeafMateSeek(indexFilepath, degree, nextPosition, 65536, nodeSeek);
                leaf.put(content, rootPath, indexName, leafMateSeek, dataFileVersion, dataFileMaxSize);
            }
            case 3 -> {
                data = ByteTools.join(nodeBytes, nodeBytes, nodeBytes);
                long nodeSeek = Channel.append(indexFilepath, data);
                Channel.write(indexFilepath, nodeMateSeek, ByteTools.fromLong(nodeSeek)); // 更新下一节点在本节点的持久化数据
                long leafMateSeek = getLeafMateSeek(indexFilepath, degree, nextPosition, 16777216, nodeSeek);
                leaf.put(content, rootPath, indexName, leafMateSeek, dataFileVersion, dataFileMaxSize);
            }
            case 4 -> {
                Filer.createFile(indexFilepath); // tmp/setAndGetBatch/unity/test/1_4294967296.1.idx
                // 初始化元数据 + 创世节点 + 2层节点 + 3层节点 + 叶子节点
                // 2字节节点默认声明、4个字节版本号、1个字节是否主键、1个字节是否唯一索引、1个字节是否允许为空、8个字节创建时间、1005个字节冗余、2字节节点默认收尾
                data = ByteTools.join(startBytes, childIndexBytes, bakBytes, endBytes, nodeBytes, nodeBytes, nodeBytes, nodeBytes);
                Channel.append(indexFilepath, data);
                long nodeSeek = ROOT_NODE_SEEK + Node.NODE_LENGTH;
                Channel.write(indexFilepath, nodeMateSeek, ByteTools.fromLong(nodeSeek)); // 更新下一节点在本节点的持久化数据
                long leafMateSeek = getLeafMateSeek(indexFilepath, degree, nextPosition, 16777216, nodeSeek);
                leaf.put(content, rootPath, indexName, leafMateSeek, dataFileVersion, dataFileMaxSize);
            }
            default -> throw new UnexpectedException("nodeCount超出预期");
        }
    }

    private long getLeafMateSeek(String indexFilepath, long degree, long nextPosition, long nodeCount, long nodeSeek) throws IOException {
        degree = degree - nextPosition * nodeCount; // degree=4294967056-255*16777216=4294967056-4278190080=16776976
        long nextNodeCount = nodeCount / 256;
        nextPosition = Math.divideExact(degree, nextNodeCount); // degree=16776976; nextPosition=255; 16777216-16776976=240
        long nodeMateSeek = nodeSeek + 2 + nextPosition * 8; // 向下一节点的数据坐标值在索引文件中的起始偏移量
        if (nodeCount != 256) {
            nodeSeek = nodeSeek + Node.NODE_LENGTH;
            Channel.write(indexFilepath, nodeMateSeek, ByteTools.fromLong(nodeSeek)); // 更新下一节点在本节点的持久化数据
            return getLeafMateSeek(indexFilepath, degree, nextPosition, nextNodeCount, nodeSeek);
        }
        return nodeMateSeek;
    }

    /**
     * 从Node中获取数据<p>
     *
     * @param degree    主键（-9223372036854775807 —— 9223372036854775808）
     * @param indexName 索引名（全名组合确保唯一性，如：库名+表名+索引名）
     * @param key       原始key
     *
     * @return 数据
     */
    @Override
    public List<byte[]> get(String indexName, long degree, String key) throws IOException {
        return getOrDelete(getIndexFilepath(degree, indexName).toString(), reDegree(degree), key, false);
    }

    @Override
    public void remove(String indexName, long degree, String key) throws IOException {
        getOrDelete(getIndexFilepath(degree, indexName).toString(), reDegree(degree), key, true);
    }

    /**
     * 从Node中获取数据<p>
     * -------------------------------------------------------------------------------------------------------------------------------------<p>
     * | 1…65536(281474976710656)                                 | 1层：1个节点，每个节点含65536个节点和281474976710656个数据坐标<p>
     * -------------------------------------------------------------------------------------------------------------------------------------<p>
     * | 1…65536 | … | 4294901760…4294967296(4294967296)          | 2层：65536个节点，每个节点含65536个节点和4294967296个数据坐标<p>
     * -------------------------------------------------------------------------------------------------------------------------------------<p>
     * | 1…65536 | … | 281474976645120…281474976710656(65536)     | 3层：4294967296个节点，每个节点含65536个节点和65536个数据坐标<p>
     * -------------------------------------------------------------------------------------------------------------------------------------<p>
     * | 1…65536 | … | 18446744073709486080…18446744073709551616  | 4层：281474976710656个节点，每个节点含65536个数据坐标，总计18446744073709551616个数据坐标<p>
     * -------------------------------------------------------------------------------------------------------------------------------------<p>
     *
     * @param degree 主键（-9223372036854775807 —— 9223372036854775808）
     * @param key    原始key
     *
     * @return 数据
     */
    public List<byte[]> getOrDelete(String indexFilepath, long degree, String key, boolean delete) throws IOException {
        // 获取根节点
        Node node = new Node(indexFilepath, ROOT_NODE_SEEK, true);
        // 获取2层节点
        long nextPosition = Math.divideExact(degree, 16777216); // 度位置; degree=4294967056; nextPosition=255; 4294967296-4294967056=240
        long node2seek = ByteTools.toLong(ByteTools.read(node.getData(), nextPosition * 8, 8));
        Node nextNode = getNode(indexFilepath, node2seek); // 2层节点
        if (Objects.isNull(nextNode)) {
            return null;
        }
        // 获取3层节点
        degree = degree - nextPosition * 16777216; // degree=4294967056-255*16777216=4294967056-4278190080=16776976
        nextPosition = Math.divideExact(degree, 65536); // degree=16776976; nextPosition=255; 16777216-16776976=240
        long node3seek = ByteTools.toLong(ByteTools.read(nextNode.getData(), nextPosition * 8, 8));
        nextNode = getNode(indexFilepath, node3seek); // 3层节点
        if (Objects.isNull(nextNode)) {
            return null;
        }
        // 获取4层节点
        degree = degree - nextPosition * 65536; // degree=16776976-255*65536=16776976-16711680=65296
        nextPosition = Math.divideExact(degree, 256); // degree=65296; nextPosition=255; 65536-65296=240
        long node4seek = ByteTools.toLong(ByteTools.read(nextNode.getData(), nextPosition * 8, 8));
        nextNode = getNode(indexFilepath, node4seek); // 4层节点
        if (Objects.isNull(nextNode)) {
            return null;
        }
        // 获取Leaf信息
        degree = degree - nextPosition * 256; // degree=65296-255*256=65296-65280=16
        nextPosition = Math.divideExact(degree, 1); // degree=16; nextPosition=16; 256-16=240
        if (delete) {
            nextNode.delete(rootPath, nextPosition, key);
            return null;
        }
        return nextNode.get(rootPath, nextPosition, key);
    }

    @Override
    public List<byte[]> select(Search search) throws IOException {
        search.setDelete(false);
        if (Objects.isNull(search.getLimit())) {
            search.setLimit(10);
        }
        return selectOrDelete(search);
    }

    @Override
    public List<byte[]> delete(Search search) throws IOException {
        search.setDelete(true);
        if (Objects.isNull(search.getLimit())) {
            search.setLimit(Integer.MAX_VALUE);
        }
        return selectOrDelete(search);
    }

    public List<byte[]> selectOrDelete(Search search) throws IOException {
        if (search.getDegreeMin() > search.getDegreeMax()) {
            return new ArrayList<>();
        }
        Path indexParentPath = Common.unityIndexFileParentPath(rootPath, search.getIndexName());
        List<Path> pathList;
        try (Stream<Path> pathStream = Files.list(indexParentPath)) {
            pathList = pathStream
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".idx"))
                    .sorted(Comparator.comparingLong(path -> {
                        String[] nameSplitArr = path.getFileName().toString().split("_");
                        if (nameSplitArr[0].equals("neg")) {
                            return Long.parseLong(nameSplitArr[1]) - Long.MAX_VALUE;
                        } else {
                            return Long.parseLong(nameSplitArr[0]);
                        }
                    }))
                    .toList();
        } catch (IOException e) {
            log.error("list File {} IOException, {}", search.getIndexName(), e.getMessage(), e);
            return new ArrayList<>();
        }
        List<byte[]> bytesList = new ArrayList<>();
        if (search.isAsc()) { // 升序
            for (Path path : pathList) {
                // 提取文件名（不含后缀）
                String fileName = path.getFileName().toString();
                // 0_4294967296 或 neg_9223371968135299072_9223371972430266367.idx
                String baseName = fileName.substring(0, fileName.lastIndexOf("."));
                String[] baseNameArr = baseName.split("_");
                boolean baseNeg = baseNameArr[0].equals("neg");
                long baseMin = baseNeg ? Long.parseLong(baseNameArr[1]) : Long.parseLong(baseNameArr[0]);
                long baseMax = baseNeg ? Long.parseLong(baseNameArr[2]) : Long.parseLong(baseNameArr[1]);
                List<byte[]> bytesListFromNode;
                if (baseNeg) { // 负数文件
                    bytesListFromNode = listNegative(search, path, baseMin, baseMax, true);
                } else { // 正数文件
                    bytesListFromNode = listPositive(search, path, baseMin, baseMax, true);
                }
                if (bytesList.size() + bytesListFromNode.size() < search.getLimit()) {
                    bytesList.addAll(bytesListFromNode);
                } else if (bytesList.size() + bytesListFromNode.size() == search.getLimit()) {
                    bytesList.addAll(bytesListFromNode);
                    return bytesList;
                } else {
                    bytesList.addAll(bytesListFromNode.subList(0, search.getLimit() - bytesList.size()));
                    return bytesList;
                }
            }
        } else { // 倒序
            for (int i = pathList.size() - 1; i >= 0; i--) {
                Path path = pathList.get(i);
                // 提取文件名（不含后缀）
                String fileName = path.getFileName().toString();
                // 0_4294967296 或 neg_9223371968135299072_9223371972430266367.idx
                String baseName = fileName.substring(0, fileName.lastIndexOf("."));
                String[] baseNameArr = baseName.split("_");
                boolean baseNeg = baseNameArr[0].equals("neg");
                long baseMin = baseNeg ? Long.parseLong(baseNameArr[1]) : Long.parseLong(baseNameArr[0]);
                long baseMax = baseNeg ? Long.parseLong(baseNameArr[2]) : Long.parseLong(baseNameArr[1]);
                List<byte[]> bytesListFromNode;
                if (baseNeg) { // 负数文件
                    bytesListFromNode = listNegative(search, path, baseMin, baseMax, false);
                } else { // 正数文件
                    bytesListFromNode = listPositive(search, path, baseMin, baseMax, false);
                }
                if (bytesList.size() + bytesListFromNode.size() < search.getLimit()) {
                    bytesList.addAll(bytesListFromNode);
                } else if (bytesList.size() + bytesListFromNode.size() == search.getLimit()) {
                    bytesList.addAll(bytesListFromNode);
                    return bytesList;
                } else {
                    int size = bytesList.size();
                    for (int l = bytesListFromNode.size() - 1; l >= search.getLimit() - size; l--) {
                        bytesList.add(bytesListFromNode.get(l));
                    }
                    return bytesList;
                }
            }
        }
        return bytesList;
    }

    /** 负数文件 */
    private List<byte[]> listNegative(Search search, Path path, long baseMin, long baseMax, boolean asc) throws IOException {
        long degreeMin;
        long degreeMax;
        boolean includeMin = true;
        boolean includeMax = true;
        // 优先从左开始判断
        if (search.getDegreeMax() < 0) {
            long compareDegreeMax = search.getDegreeMax() + Long.MAX_VALUE;
            long compareDegreeMin = search.getDegreeMin() + Long.MAX_VALUE;
            if (compareDegreeMax < baseMin || compareDegreeMin > baseMax) {
                return new ArrayList<>();
            } else if (compareDegreeMax <= baseMax) {
                degreeMax = 4294967295L - (baseMax - compareDegreeMax);
                includeMax = search.isIncludeMax();
                if (compareDegreeMin <= baseMin) {
                    degreeMin = 0;
                } else {
                    degreeMin = compareDegreeMin - baseMin;
                    includeMin = search.isIncludeMin();
                }
            } else { // compareDegreeMax > baseMax
                degreeMax = 4294967295L;
                if (compareDegreeMin <= baseMin) {
                    degreeMin = 0;
                } else {
                    degreeMin = compareDegreeMin - baseMin;
                    includeMin = search.isIncludeMin();
                }
            }
        } else {
            degreeMax = 4294967295L;
            if (search.getDegreeMin() >= 0) {
                return new ArrayList<>();
            }
            long compareDegreeMin = search.getDegreeMin() + Long.MAX_VALUE;
            if (compareDegreeMin <= baseMin) {
                degreeMin = 0;
            } else if (compareDegreeMin <= baseMax) {
                degreeMin = compareDegreeMin - baseMin;
                includeMin = search.isIncludeMin();
            } else {
                return new ArrayList<>();
            }
        }
        Node node = new Node(path.toString(), ROOT_NODE_SEEK, true);
        if (asc) {
            return listAsc(search, path.toString(), degreeMin, degreeMax, includeMin, includeMax, node, 16777216);
        }
        return listDesc(search, path.toString(), degreeMin, degreeMax, includeMin, includeMax, node, 16777216);
    }

    /** 正数文件 */
    private List<byte[]> listPositive(Search search, Path path, long baseMin, long baseMax, boolean asc) throws IOException {
        long degreeMin;
        long degreeMax;
        boolean includeMin = true;
        boolean includeMax = true;
        // 优先从左开始判断
        if (search.getDegreeMax() < 0) {
            return new ArrayList<>();
        } else if (search.getDegreeMin() < 0) {
            degreeMin = 0;
            long compareDegreeMax = search.getDegreeMax();
            if (compareDegreeMax < baseMin) {
                return new ArrayList<>();
            } else if (compareDegreeMax <= baseMax) {
                degreeMax = compareDegreeMax - baseMin;
                includeMax = search.isIncludeMax();
            } else { // compareDegreeMax > baseMax
                degreeMax = 4294967295L;
            }
        } else { // search.getDegreeMin() >= 0
            long compareDegreeMax = search.getDegreeMax();
            long compareDegreeMin = search.getDegreeMin();
            if (compareDegreeMax < baseMin || compareDegreeMin > baseMax) {
                return new ArrayList<>();
            } else if (compareDegreeMin <= baseMin) {
                degreeMin = 0;
                if (compareDegreeMax <= baseMax) {
                    degreeMax = compareDegreeMax - baseMin;
                    includeMax = search.isIncludeMax();
                } else { // compareDegreeMax > baseMax
                    degreeMax = 4294967295L;
                }
            } else {
                degreeMin = compareDegreeMin - baseMin;
                includeMin = search.isIncludeMin();
                if (compareDegreeMax <= baseMax) {
                    degreeMax = compareDegreeMax - baseMin;
                    includeMax = search.isIncludeMax();
                } else { // compareDegreeMax > baseMax
                    degreeMax = 4294967295L;
                }
            }
        }
        Node node = new Node(path.toString(), ROOT_NODE_SEEK, true);
        if (asc) {
            return listAsc(search, path.toString(), degreeMin, degreeMax, includeMin, includeMax, node, 16777216);
        }
        return listDesc(search, path.toString(), degreeMin, degreeMax, includeMin, includeMax, node, 16777216);
    }

    /**
     * 从Node中右遍历升序<p>
     *
     * @param degreeMin  主键（-9223372036854775807 —— 9223372036854775808）
     * @param includeMin 是否包含degree
     *
     * @return 数据
     */
    public List<byte[]> listAsc(Search search, String indexFilepath, long degreeMin, long degreeMax, boolean includeMin, boolean includeMax, Node node, long nodeCount) throws IOException {
        List<byte[]> bytesList = new ArrayList<>();
        long nextPositionMin = Math.divideExact(degreeMin, nodeCount);
        long nextPositionMax = Math.divideExact(degreeMax, nodeCount);
        if (nodeCount == 1) {
            nextPositionMin = includeMin ? nextPositionMin : (nextPositionMin + 1);
            nextPositionMax = includeMax ? (nextPositionMax + 1) : nextPositionMax;
            while (nextPositionMin < nextPositionMax) {
                long position = nextPositionMin;
                List<byte[]> bytesListFromNode = node.select(rootPath, position);
                nextPositionMin += 1;
                if (bytesListFromNode.isEmpty()) {
                    continue;
                }
                if (Objects.nonNull(search.getSearchFilter())) {
                    bytesListFromNode = search.getSearchFilter().filter(bytesListFromNode, search.getConditions());
                }
                if (search.isDelete() && !bytesListFromNode.isEmpty()) {
                    long leafMateSeek = node.getSeek() + 2 + position * 8;
                    Channel.write(indexFilepath, leafMateSeek, new byte[8]);
                }
                if (bytesList.size() + bytesListFromNode.size() < search.getLimit()) {
                    bytesList.addAll(bytesListFromNode);
                } else if (bytesList.size() + bytesListFromNode.size() == search.getLimit()) {
                    bytesList.addAll(bytesListFromNode);
                    break;
                } else {
                    bytesList.addAll(bytesListFromNode.subList(0, search.getLimit() - bytesList.size()));
                }
            }
        } else {
            while (nextPositionMin <= nextPositionMax) {
                long nodeSeek = ByteTools.toLong(ByteTools.read(node.getData(), nextPositionMin * 8, 8));
                Node nextNode = getNode(indexFilepath, nodeSeek);
                if (Objects.nonNull(nextNode)) {
                    long degreeMinTmp = Math.max(degreeMin - nextPositionMin * nodeCount, 0);
                    long degreeMaxTmp = Math.min(degreeMax - nextPositionMax * nodeCount, nodeCount);
                    List<byte[]> bytesListTmp = listAsc(search, indexFilepath, degreeMinTmp, degreeMaxTmp, includeMin, includeMax, nextNode, nodeCount / 256);
                    if (!bytesListTmp.isEmpty()) {
                        if (Objects.nonNull(search.getSearchFilter())) {
                            bytesListTmp = search.getSearchFilter().filter(bytesListTmp, search.getConditions());
                        }
                        if (bytesList.size() + bytesListTmp.size() < search.getLimit()) {
                            bytesList.addAll(bytesListTmp);
                        } else if (bytesList.size() + bytesListTmp.size() == search.getLimit()) {
                            bytesList.addAll(bytesListTmp);
                            break;
                        } else {
                            bytesList.addAll(bytesListTmp.subList(0, search.getLimit() - bytesList.size()));
                        }
                    }
                }
                nextPositionMin += 1;
            }
        }
        return bytesList;
    }

    /**
     * 从Node中右遍历降序<p>
     *
     * @param degreeMin  主键（-9223372036854775807 —— 9223372036854775808）
     * @param includeMin 是否包含degree
     *
     * @return 数据
     */
    public List<byte[]> listDesc(Search search, String indexFilepath, long degreeMin, long degreeMax, boolean includeMin, boolean includeMax, Node node, long nodeCount) throws IOException {
        List<byte[]> bytesList = new ArrayList<>();
        long nextPositionMin = Math.divideExact(degreeMin, nodeCount);
        long nextPositionMax = Math.divideExact(degreeMax, nodeCount);
        if (nodeCount == 1) {
            nextPositionMin = includeMin ? nextPositionMin : (nextPositionMin + 1);
            nextPositionMax = includeMax ? nextPositionMax : (nextPositionMax - 1);
            while (nextPositionMin <= nextPositionMax) {
                long position = nextPositionMax;
                List<byte[]> bytesListFromNode = node.select(rootPath, position);
                nextPositionMax -= 1;
                if (bytesListFromNode.isEmpty()) {
                    continue;
                }
                if (Objects.nonNull(search.getSearchFilter())) {
                    bytesListFromNode = search.getSearchFilter().filter(bytesListFromNode, search.getConditions());
                }
                if (search.isDelete() && !bytesListFromNode.isEmpty()) {
                    long leafMateSeek = node.getSeek() + 2 + position * 8;
                    Channel.write(indexFilepath, leafMateSeek, new byte[8]);
                }
                if (bytesList.size() + bytesListFromNode.size() < search.getLimit()) {
                    bytesList.addAll(bytesListFromNode);
                } else if (bytesList.size() + bytesListFromNode.size() == search.getLimit()) {
                    bytesList.addAll(bytesListFromNode);
                    break;
                } else {
                    int size = bytesList.size();
                    for (int i = bytesListFromNode.size() - 1; i >= search.getLimit() - size; i--) {
                        bytesList.add(bytesListFromNode.get(i));
                    }
                }
            }
        } else {
            while (nextPositionMin <= nextPositionMax) {
                long nodeSeek = ByteTools.toLong(ByteTools.read(node.getData(), nextPositionMax * 8, 8));
                Node nextNode = getNode(indexFilepath, nodeSeek);
                if (Objects.nonNull(nextNode)) {
                    long degreeMinTmp = Math.max(degreeMin - nextPositionMin * nodeCount, 0);
                    long degreeMaxTmp = Math.min(degreeMax - nextPositionMax * nodeCount, nodeCount);

                    List<byte[]> bytesListTmp = listDesc(search, indexFilepath, degreeMinTmp, degreeMaxTmp, includeMin, includeMax, nextNode, nodeCount / 256);
                    if (!bytesListTmp.isEmpty()) {
                        if (Objects.nonNull(search.getSearchFilter())) {
                            bytesListTmp = search.getSearchFilter().filter(bytesListTmp, search.getConditions());
                        }
                        if (bytesList.size() + bytesListTmp.size() < search.getLimit()) {
                            bytesList.addAll(bytesListTmp);
                        } else if (bytesList.size() + bytesListTmp.size() == search.getLimit()) {
                            bytesList.addAll(bytesListTmp);
                            break;
                        } else {
                            bytesList.addAll(bytesListTmp.subList(0, search.getLimit() - bytesList.size()));
                        }
                    }
                }
                nextPositionMax -= 1;
            }
        }
        return bytesList;
    }

    /**
     * 查找节点<p>
     *
     * @param nodeSeek 节点数据在索引文件中的起始偏移量
     */
    private Node getNode(String indexFilepath, long nodeSeek) throws IOException {
        Node nextNode;
        if (nodeSeek <= 0) { // 下一节点不存在，需要新建下一节点
            return null;
        } else {
            nextNode = new Node(indexFilepath, nodeSeek, true); // 下层节点
        }
        return nextNode;
    }

    @Override
    public int intValue() {
        return IEngine.UNITY;
    }

    /** 索引详情：4个字节版本号、1个字节是否主键、1个字节是否唯一索引、1个字节是否允许为空、8个字节创建时间，总计15个字节 */
    @Data
    public static class ChildIndex {

        // 写入文件信息开始
        /** 版本号，4个字节 */
        byte[] versionBytes;
        /** 是否主键，主键也是唯一索引，1个字节 */
        byte primaryByte;
        /** 是否唯一索引，1个字节 */
        byte uniqueByte;
        /** 是否允许为空，1个字节 */
        byte nullableByte;
        /** 创建时间，8个字节 */
        byte[] localDateTimeBytes;
        // 写入文件信息结束

        /** 版本号，4个字节 */
        int version;
        /** 索引名称 */
        String name;
        /** 是否主键，主键也是唯一索引，1个字节 */
        boolean primary;
        /** 是否唯一索引，1个字节 */
        boolean unique;
        /** 是否允许为空，1个字节 */
        boolean nullable;
        /** 创建时间，8个字节 */
        LocalDateTime localDateTime;

        public ChildIndex(byte[] dataBytes) {
            versionBytes = ByteTools.read(dataBytes, 0, 4);
            primaryByte = ByteTools.read(dataBytes, 4, 1)[0];
            uniqueByte =  ByteTools.read(dataBytes, 5, 1)[0];
            nullableByte =  ByteTools.read(dataBytes, 6, 1)[0];
            localDateTimeBytes = ByteTools.read(dataBytes, 7, 8);

            version = ByteTools.toInt(versionBytes);
            primary = ByteTools.toBool(primaryByte);
            unique = ByteTools.toBool(uniqueByte);
            nullable = ByteTools.toBool(nullableByte);
            localDateTime = DateTools.timestamp2localDateTime(ByteTools.toLong(localDateTimeBytes));
        }

        public ChildIndex(int version, String name, boolean primary, boolean unique, boolean nullable) {
            this.version = version;
            this.name = name;
            this.primary = primary;
            this.unique = unique;
            this.nullable = nullable;

            versionBytes = ByteTools.fromInt(version);
            primaryByte = ByteTools.fromBool(primary);
            uniqueByte = ByteTools.fromBool(unique);
            nullableByte = ByteTools.fromBool(nullable);
            localDateTimeBytes = ByteTools.fromLong(DateTools.localDateTime2timestamp(LocalDateTime.now()));
        }

        /** 4字节版本号+8字节索引名称偏移量+4字节索引名称长度+1字节是否主键+1字节是否唯一索引+1字节是否允许为空+8字节创建时间 */
        public byte[] toBytes() throws IOException {
            return ByteTools.join(versionBytes, new byte[]{primaryByte, uniqueByte, nullableByte}, localDateTimeBytes);
        }
    }

}
