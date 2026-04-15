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

package cn.aberic.tangduo.index.engine.skip;

import cn.aberic.tangduo.common.file.Reader;
import cn.aberic.tangduo.common.file.Writer;
import cn.aberic.tangduo.index.engine.IEngine;
import cn.aberic.tangduo.index.engine.Transaction;
import cn.aberic.tangduo.index.engine.entity.Content;
import cn.aberic.tangduo.index.engine.entity.Search;
import cn.aberic.tangduo.index.engine.skip.entity.Node;
import lombok.NonNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.rmi.UnexpectedException;
import java.util.List;

public class Skip extends IEngine {

    String indexFilepath;
    Node node;
    long nodeSeek;
    String datapath;

    public Skip() {
    }

    @Override
    public void force(long degree, String indexName) throws IOException {

    }

    /**
     * Node插入数据data
     */
    @Override
    public void put(Content content) throws IOException {
        set(content.getTransaction(), node, nodeSeek, content.getDegree(), content.getIndexName(), content.getKey(), content.getValue());
    }

    /**
     * Node插入数据data
     *
     * @param node   当前node
     * @param seek   当前node坐标
     * @param degree 主键
     * @param data   数据
     */
    public void set(Transaction transaction, Node node, long seek, long degree, String indexName, String key, byte[] data) throws IOException {
        long difference = degree - node.getKey(); // 200309595 - 0
        if (difference == 0) { // 找到节点
            // todo 写入新数据并根据数据状态及事务规则决定是否更新数据坐标，考虑事务一致性原则，或支持事务操作
        } else { // 继续找节点
            int len = (int) (Math.log10(difference) + 1); // 计算主键差的长度，如200309595的长度为9
            long base = (long) Math.pow(10, len - 1); // 若主键为200309595或300309595，均得到100000000
            long matchNodePosition; // 匹配上的后续节点坐标
            long matchNodePositionInThisNodePositionWithFile; // 匹配上的后续节点坐标信息在文件中的坐标
            switch (len) {
                case 1 -> {
                    matchNodePosition = node.getNextNodePosition();
                    matchNodePositionInThisNodePositionWithFile = seek + 19; // start(2)+status(1)+key(8)+dataPosition(8)
                }
                case 2 -> {
                    matchNodePosition = node.getNextShiNodePosition();
                    matchNodePositionInThisNodePositionWithFile = seek + 27; // getNextNodePosition + 8
                }
                case 3 -> {
                    matchNodePosition = node.getNextBaiNodePosition();
                    matchNodePositionInThisNodePositionWithFile = seek + 35; // getNextShiNodePosition + 8
                }
                case 4 -> {
                    matchNodePosition = node.getNextQianNodePosition();
                    matchNodePositionInThisNodePositionWithFile = seek + 43; // getNextBaiNodePosition + 8
                }
                case 5 -> {
                    matchNodePosition = node.getNextWanNodePosition();
                    matchNodePositionInThisNodePositionWithFile = seek + 51; // getNextQianNodePosition + 8
                }
                case 6 -> {
                    matchNodePosition = node.getNextShiWanNodePosition();
                    matchNodePositionInThisNodePositionWithFile = seek + 59; // getNextWanNodePosition + 8
                }
                case 7 -> {
                    matchNodePosition = node.getNextBaiWanNodePosition();
                    matchNodePositionInThisNodePositionWithFile = seek + 67; // getNextShiWanNodePosition + 8
                }
                case 8 -> {
                    matchNodePosition = node.getNextQianWanNodePosition();
                    matchNodePositionInThisNodePositionWithFile = seek + 75; // getNextBaiWanNodePosition + 8
                }
                case 9 -> {
                    matchNodePosition = node.getNextYiNodePosition();
                    matchNodePositionInThisNodePositionWithFile = seek + 83; // getNextQianWanNodePosition + 8
                }
                case 10 -> {
                    matchNodePosition = node.getNextShiYiNodePosition();
                    matchNodePositionInThisNodePositionWithFile = seek + 91; // getNextYiNodePosition + 8
                }
                case 11 -> {
                    matchNodePosition = node.getNextBaiYiNodePosition();
                    matchNodePositionInThisNodePositionWithFile = seek + 99; // getNextShiYiNodePosition + 8
                }
                case 12 -> {
                    matchNodePosition = node.getNextQianYiNodePosition();
                    matchNodePositionInThisNodePositionWithFile = seek + 107; // getNextBaiYiNodePosition + 9
                }
                case 13 -> {
                    matchNodePosition = node.getNextZhaoNodePosition();
                    matchNodePositionInThisNodePositionWithFile = seek + 115; // getNextQianYiNodePosition + 8
                }
                default -> throw new UnexpectedException("计算主键差的长度不在[1-13]范围内，len=" + len);
            }
            Node matchNode;
            long matchNodePositionInFile; // 匹配上的节点在文件中的起始坐标
            if (matchNodePosition == 0) { // 如果下一节点坐标为0，则表示不存在，新建该节点
                matchNode = new Node(node.getKey() + base, 0);
                matchNodePositionInFile = appendIndex(indexFilepath, matchNode.toBytes()); // 匹配上的节点在文件中写入后的起始坐标
                Writer.write(indexFilepath, matchNodePositionInThisNodePositionWithFile, matchNodePositionInFile); // 更新节点后续节点数据
            } else {
                matchNode = new Node(Reader.read(indexFilepath, matchNodePosition, node.getLength()));
                matchNodePositionInFile = matchNodePosition;
            }
            set(transaction, matchNode, matchNodePositionInFile, degree - base, indexName, key, data);
        }
    }

    @Override
    public List<byte[]> get(String indexName, long degree, String key) throws IOException {
        return null;
    }

    @Override
    public void remove(String indexName, long degree, String key) throws IOException {

    }

    @Override
    public List<byte[]> select(Search search) throws IOException {
        return List.of();
    }

    @Override
    public List<byte[]> delete(Search search) throws IOException {
        return List.of();
    }

    /**
     * 同步方法，向指定文件中追加指定字节数组
     *
     * @param filepath 指定文件
     * @param bytes    指定字节数组
     */
    private synchronized long appendIndex(@NonNull String filepath, byte[] bytes) throws IOException {
        long position = Files.size(Path.of(filepath));
        Writer.append(filepath, bytes);
        return position;
    }

    @Override
    public int intValue() {
        return IEngine.SKIP;
    }
}
