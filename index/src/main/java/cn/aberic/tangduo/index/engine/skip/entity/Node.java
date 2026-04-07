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

package cn.aberic.tangduo.index.engine.skip.entity;

import cn.aberic.tangduo.common.ByteTools;
import cn.aberic.tangduo.index.engine.INode;
import lombok.Data;

import java.io.IOException;
import java.rmi.UnexpectedException;

/**
 * 跳表引擎节点<p>
 * 2^32 = 4294967296<p>
 * 个、十、百、千、万、十万、百万、千万、亿、十亿、百亿、千亿、兆、十兆、百兆、千兆、京<p>
 * 2^64 = 18446744073709551616 | 1844京6744兆737亿955万1616<p>
 * 节点包含【节点默认声明、数据状态、主键id、当前数据坐标、下一节点坐标、后面第十个节点坐标、后面第一百个节点坐标、后面第一千个节点坐标、后面第一万个节点坐标、后面第十万个节点坐标、
 * 后面第百万个节点坐标、后面第千万个节点坐标、后面第亿个节点坐标、后面第十亿个节点坐标、后面第百亿个节点坐标、后面第千亿个节点坐标、后面第兆个节点坐标、节点默认收尾】<p>
 * 一个坐标8个字节，坐标总计112个字节，数据状态、主键id、节点声明和收尾各两个字节，一个节点总计125个字节
 */
@Data
public class Node implements INode {

    /** 节点默认声明，值非默认即异常 */
    byte[] start = {0x09, 0x7F};
    /** 数据状态，0x00无数据、0x01写入、0x02删除 */
    byte status = 0x00;
    /** 主键id，索引id，序列id */
    long key;
    /** 当前数据坐标 */
    long dataPosition;
    /** 下一节点坐标 */
    long nextNodePosition = 0;
    /** 后面第十个节点坐标 */
    long nextShiNodePosition = 0;
    /** 后面第一百个节点坐标 */
    long nextBaiNodePosition = 0;
    /** 后面第一千个节点坐标 */
    long nextQianNodePosition = 0;
    /** 后面第一万个节点坐标 */
    long nextWanNodePosition = 0;
    /** 后面第十万个节点坐标 */
    long nextShiWanNodePosition = 0;
    /** 后面第百万个节点坐标 */
    long nextBaiWanNodePosition = 0;
    /** 后面第千万个节点坐标 */
    long nextQianWanNodePosition = 0;
    /** 后面第亿个节点坐标 */
    long nextYiNodePosition = 0;
    /** 后面第十亿个节点坐标 */
    long nextShiYiNodePosition = 0;
    /** 后面第百亿个节点坐标 */
    long nextBaiYiNodePosition = 0;
    /** 后面第千亿个节点坐标 */
    long nextQianYiNodePosition = 0;
    /** 后面第兆个节点坐标 */
    long nextZhaoNodePosition = 0;
    /** 节点默认收尾，值非默认即异常 */
    byte[] end = {0x09, 0x6F};

    /** 节点转字节数组后的字节长度 */
    int length = 125;

    /**
     * 根据传入的117个字节数组解析Node对象
     *
     * @param bytes 117个字节数组
     */
    public Node(byte[] bytes) throws UnexpectedException {
        if (bytes.length > 116) {
            throw new UnexpectedException("解析bytes与Node所需长度不匹配！");
        } else {
            if (bytes[0] != 0x08 || bytes[1] != 0x7F || bytes[114] != 0x09 || bytes[115] != 0x6F) {
                throw new UnexpectedException("解析bytes与Node所需首尾默认值不匹配！");
            }
            status = bytes[2];
            key = ByteTools.toLong(ByteTools.read(bytes, 3, 8));
            dataPosition = ByteTools.toLong(ByteTools.read(bytes, 11, 8));
            nextNodePosition = ByteTools.toLong(ByteTools.read(bytes, 19, 8));
            nextShiNodePosition = ByteTools.toLong(ByteTools.read(bytes, 27, 8));
            nextBaiNodePosition = ByteTools.toLong(ByteTools.read(bytes, 35, 8));
            nextQianNodePosition = ByteTools.toLong(ByteTools.read(bytes, 43, 8));
            nextWanNodePosition = ByteTools.toLong(ByteTools.read(bytes, 51, 8));
            nextShiWanNodePosition = ByteTools.toLong(ByteTools.read(bytes, 59, 8));
            nextBaiWanNodePosition = ByteTools.toLong(ByteTools.read(bytes, 67, 8));
            nextQianWanNodePosition = ByteTools.toLong(ByteTools.read(bytes, 75, 8));
            nextYiNodePosition = ByteTools.toLong(ByteTools.read(bytes, 83, 8));
            nextShiYiNodePosition = ByteTools.toLong(ByteTools.read(bytes, 91, 8));
            nextBaiYiNodePosition = ByteTools.toLong(ByteTools.read(bytes, 99, 8));
            nextQianYiNodePosition = ByteTools.toLong(ByteTools.read(bytes, 107, 8));
            nextZhaoNodePosition = ByteTools.toLong(ByteTools.read(bytes, 115, 8));
        }
    }

    public Node(long key, long dataPosition) {
        this.key = key;
        this.dataPosition = dataPosition;
    }

    public Node(long key, long dataPosition, long nextNodePosition) {
        this.key = key;
        this.dataPosition = dataPosition;
        this.nextNodePosition = nextNodePosition;
    }

    public Node(long key, long dataPosition, long nextNodePosition, long nextShiNodePosition) {
        this.key = key;
        this.dataPosition = dataPosition;
        this.nextNodePosition = nextNodePosition;
        this.nextShiNodePosition = nextShiNodePosition;
    }

    public Node(long key, long dataPosition, long nextNodePosition, long nextShiNodePosition, long nextBaiNodePosition) {
        this.key = key;
        this.dataPosition = dataPosition;
        this.nextNodePosition = nextNodePosition;
        this.nextShiNodePosition = nextShiNodePosition;
        this.nextBaiNodePosition = nextBaiNodePosition;
    }

    public Node(long key, long dataPosition, long nextNodePosition, long nextShiNodePosition, long nextBaiNodePosition, long nextQianNodePosition) {
        this.key = key;
        this.dataPosition = dataPosition;
        this.nextNodePosition = nextNodePosition;
        this.nextShiNodePosition = nextShiNodePosition;
        this.nextBaiNodePosition = nextBaiNodePosition;
        this.nextQianNodePosition = nextQianNodePosition;
    }

    public Node(long key, long dataPosition, long nextNodePosition, long nextShiNodePosition, long nextBaiNodePosition, long nextQianNodePosition,
                long nextWanNodePosition) {
        this.key = key;
        this.dataPosition = dataPosition;
        this.nextNodePosition = nextNodePosition;
        this.nextShiNodePosition = nextShiNodePosition;
        this.nextBaiNodePosition = nextBaiNodePosition;
        this.nextQianNodePosition = nextQianNodePosition;
        this.nextWanNodePosition = nextWanNodePosition;
    }

    public Node(long key, long dataPosition, long nextNodePosition, long nextShiNodePosition, long nextBaiNodePosition, long nextQianNodePosition,
                long nextWanNodePosition, long nextShiWanNodePosition) {
        this.key = key;
        this.dataPosition = dataPosition;
        this.nextNodePosition = nextNodePosition;
        this.nextShiNodePosition = nextShiNodePosition;
        this.nextBaiNodePosition = nextBaiNodePosition;
        this.nextQianNodePosition = nextQianNodePosition;
        this.nextWanNodePosition = nextWanNodePosition;
        this.nextShiWanNodePosition = nextShiWanNodePosition;
    }

    public Node(long key, long dataPosition, long nextNodePosition, long nextShiNodePosition, long nextBaiNodePosition, long nextQianNodePosition,
                long nextWanNodePosition, long nextShiWanNodePosition, long nextBaiWanNodePosition) {
        this.key = key;
        this.dataPosition = dataPosition;
        this.nextNodePosition = nextNodePosition;
        this.nextShiNodePosition = nextShiNodePosition;
        this.nextBaiNodePosition = nextBaiNodePosition;
        this.nextQianNodePosition = nextQianNodePosition;
        this.nextWanNodePosition = nextWanNodePosition;
        this.nextShiWanNodePosition = nextShiWanNodePosition;
        this.nextBaiWanNodePosition = nextBaiWanNodePosition;
    }

    public Node(long key, long dataPosition, long nextNodePosition, long nextShiNodePosition, long nextBaiNodePosition, long nextQianNodePosition,
                long nextWanNodePosition, long nextShiWanNodePosition, long nextBaiWanNodePosition, long nextQianWanNodePosition) {
        this.key = key;
        this.dataPosition = dataPosition;
        this.nextNodePosition = nextNodePosition;
        this.nextShiNodePosition = nextShiNodePosition;
        this.nextBaiNodePosition = nextBaiNodePosition;
        this.nextQianNodePosition = nextQianNodePosition;
        this.nextWanNodePosition = nextWanNodePosition;
        this.nextShiWanNodePosition = nextShiWanNodePosition;
        this.nextBaiWanNodePosition = nextBaiWanNodePosition;
        this.nextQianWanNodePosition = nextQianWanNodePosition;
    }

    public Node(long key, long dataPosition, long nextNodePosition, long nextShiNodePosition, long nextBaiNodePosition, long nextQianNodePosition,
                long nextWanNodePosition, long nextShiWanNodePosition, long nextBaiWanNodePosition, long nextQianWanNodePosition, long nextYiNodePosition) {
        this.key = key;
        this.dataPosition = dataPosition;
        this.nextNodePosition = nextNodePosition;
        this.nextShiNodePosition = nextShiNodePosition;
        this.nextBaiNodePosition = nextBaiNodePosition;
        this.nextQianNodePosition = nextQianNodePosition;
        this.nextWanNodePosition = nextWanNodePosition;
        this.nextShiWanNodePosition = nextShiWanNodePosition;
        this.nextBaiWanNodePosition = nextBaiWanNodePosition;
        this.nextQianWanNodePosition = nextQianWanNodePosition;
        this.nextYiNodePosition = nextYiNodePosition;
    }

    public Node(long key, long dataPosition, long nextNodePosition, long nextShiNodePosition, long nextBaiNodePosition, long nextQianNodePosition,
                long nextWanNodePosition, long nextShiWanNodePosition, long nextBaiWanNodePosition, long nextQianWanNodePosition, long nextYiNodePosition,
                long nextShiYiNodePosition) {
        this.key = key;
        this.dataPosition = dataPosition;
        this.nextNodePosition = nextNodePosition;
        this.nextShiNodePosition = nextShiNodePosition;
        this.nextBaiNodePosition = nextBaiNodePosition;
        this.nextQianNodePosition = nextQianNodePosition;
        this.nextWanNodePosition = nextWanNodePosition;
        this.nextShiWanNodePosition = nextShiWanNodePosition;
        this.nextBaiWanNodePosition = nextBaiWanNodePosition;
        this.nextQianWanNodePosition = nextQianWanNodePosition;
        this.nextYiNodePosition = nextYiNodePosition;
        this.nextShiYiNodePosition = nextShiYiNodePosition;
    }

    public Node(long key, long dataPosition, long nextNodePosition, long nextShiNodePosition, long nextBaiNodePosition, long nextQianNodePosition,
                long nextWanNodePosition, long nextShiWanNodePosition, long nextBaiWanNodePosition, long nextQianWanNodePosition, long nextYiNodePosition,
                long nextShiYiNodePosition, long nextBaiYiNodePosition) {
        this.key = key;
        this.dataPosition = dataPosition;
        this.nextNodePosition = nextNodePosition;
        this.nextShiNodePosition = nextShiNodePosition;
        this.nextBaiNodePosition = nextBaiNodePosition;
        this.nextQianNodePosition = nextQianNodePosition;
        this.nextWanNodePosition = nextWanNodePosition;
        this.nextShiWanNodePosition = nextShiWanNodePosition;
        this.nextBaiWanNodePosition = nextBaiWanNodePosition;
        this.nextQianWanNodePosition = nextQianWanNodePosition;
        this.nextYiNodePosition = nextYiNodePosition;
        this.nextShiYiNodePosition = nextShiYiNodePosition;
        this.nextBaiYiNodePosition = nextBaiYiNodePosition;
    }

    public Node(long key, long dataPosition, long nextNodePosition, long nextShiNodePosition, long nextBaiNodePosition, long nextQianNodePosition,
                long nextWanNodePosition, long nextShiWanNodePosition, long nextBaiWanNodePosition, long nextQianWanNodePosition, long nextYiNodePosition,
                long nextShiYiNodePosition, long nextBaiYiNodePosition, long nextQianYiNodePosition) {
        this.key = key;
        this.dataPosition = dataPosition;
        this.nextNodePosition = nextNodePosition;
        this.nextShiNodePosition = nextShiNodePosition;
        this.nextBaiNodePosition = nextBaiNodePosition;
        this.nextQianNodePosition = nextQianNodePosition;
        this.nextWanNodePosition = nextWanNodePosition;
        this.nextShiWanNodePosition = nextShiWanNodePosition;
        this.nextBaiWanNodePosition = nextBaiWanNodePosition;
        this.nextQianWanNodePosition = nextQianWanNodePosition;
        this.nextYiNodePosition = nextYiNodePosition;
        this.nextShiYiNodePosition = nextShiYiNodePosition;
        this.nextBaiYiNodePosition = nextBaiYiNodePosition;
        this.nextQianYiNodePosition = nextQianYiNodePosition;
    }

    public Node(long key, long dataPosition, long nextNodePosition, long nextShiNodePosition, long nextBaiNodePosition, long nextQianNodePosition,
                long nextWanNodePosition, long nextShiWanNodePosition, long nextBaiWanNodePosition, long nextQianWanNodePosition, long nextYiNodePosition,
                long nextShiYiNodePosition, long nextBaiYiNodePosition, long nextQianYiNodePosition, long nextZhaoNodePosition) {
        this.key = key;
        this.dataPosition = dataPosition;
        this.nextNodePosition = nextNodePosition;
        this.nextShiNodePosition = nextShiNodePosition;
        this.nextBaiNodePosition = nextBaiNodePosition;
        this.nextQianNodePosition = nextQianNodePosition;
        this.nextWanNodePosition = nextWanNodePosition;
        this.nextShiWanNodePosition = nextShiWanNodePosition;
        this.nextBaiWanNodePosition = nextBaiWanNodePosition;
        this.nextQianWanNodePosition = nextQianWanNodePosition;
        this.nextYiNodePosition = nextYiNodePosition;
        this.nextShiYiNodePosition = nextShiYiNodePosition;
        this.nextBaiYiNodePosition = nextBaiYiNodePosition;
        this.nextQianYiNodePosition = nextQianYiNodePosition;
        this.nextZhaoNodePosition = nextZhaoNodePosition;
    }

    /**
     * 转成存储字节数组
     *
     * @return 存储字节数组
     */
    public byte[] toBytes() throws IOException {
        return ByteTools.join(start, new byte[]{status}, ByteTools.fromLong(key), ByteTools.fromLong(dataPosition), ByteTools.fromLong(nextNodePosition), ByteTools.fromLong(nextShiNodePosition),
                ByteTools.fromLong(nextBaiNodePosition), ByteTools.fromLong(nextQianNodePosition), ByteTools.fromLong(nextBaiWanNodePosition),
                ByteTools.fromLong(nextShiWanNodePosition), ByteTools.fromLong(nextBaiWanNodePosition), ByteTools.fromLong(nextQianWanNodePosition),
                ByteTools.fromLong(nextYiNodePosition), ByteTools.fromLong(nextShiYiNodePosition), ByteTools.fromLong(nextBaiYiNodePosition),
                ByteTools.fromLong(nextQianYiNodePosition), ByteTools.fromLong(nextZhaoNodePosition), end);
    }

}
