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

package cn.aberic.tangduo.index.engine;

import cn.aberic.tangduo.common.file.Filer;
import cn.aberic.tangduo.common.file.Writer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/// 公共类
/// 用于存储公共方法
public class Common {

    public Common() {
        throw new IllegalStateException("Common class");
    }

    /// 联合索引的索引文件和数据文件名默认前缀
    public static final String UNITY_PATH = "unity";

    /**
     * 根据根路径获取记录索引和索引对应文件的文件路径，如"tmp/record.rd"
     *
     * @param rootPath 根路径
     *
     * @return 记录索引和索引对应文件的文件路径
     */
    public static Path recordFilepath(String rootPath) {
        return Path.of(rootPath, "record.rd");
    }

    /**
     * 根据索引版本号获取聚合索引 Path，如"tmp/unity/testIndex"
     *
     * @param rootPath 文件根路径
     *
     * @return 聚合索引 Path，如"tmp/unity/testIndex"
     */
    public static Path unityIndexFileParentPath(String rootPath, String indexName) {
        return Path.of(rootPath, UNITY_PATH, indexName);
    }

    /**
     * 根据索引版本号获取聚合索引 Path，如"tmp/unity/testIndex/1_4294967296.idx"
     *
     * @param rootPath 文件根路径
     *
     * @return 聚合索引 Path，如"tmp/unity/testIndex/1_4294967296.idx"
     */
    public static Path unityIndexFilepath(String rootPath, String indexName, String degreeInterval) {
        return Path.of(rootPath, UNITY_PATH, indexName, degreeInterval + ".idx");
    }

    /**
     * 根据数据文件版本号查找数据文件名
     *
     * @param rootPath        数据根路径
     * @param dataFileVersion 数据文件版本号，如 1，与索引版本号结合使用，如1.1，区分相同索引下的不同数据文件，如"tmp/unity/testIndex/data.1.td"
     *
     * @return 数据文件名
     */
    public static Path dataFilepath(String rootPath, int dataFileVersion) {
        return Path.of(rootPath, "data." + dataFileVersion + ".td");
    }

    /**
     * 根据数据文件版本号查找数据文件名，如当前传入为1，经查验文件大小已超过阈值，若更新，则版本号++
     *
     * @param rootPath        数据根路径
     * @param dataFileVersion 数据文件版本号，如 1，与索引版本号结合使用，如1.1，区分相同索引下的不同数据文件，如"tmp/unity.1.1.td"
     * @param fileMaxSize     数据文件大小阈值，单位byte
     *
     * @return 数据文件名
     */
    public static int dataFileVersion(String rootPath, int dataFileVersion, long fileMaxSize) throws IOException {
        Path dataFilepath = dataFilepath(rootPath, dataFileVersion);
        long fileSize;
        if (!Files.exists(dataFilepath)) {
            Filer.createFile(dataFilepath);
            Writer.append(dataFilepath.toString(), Datum.startBytes);
        } else {
            fileSize = Files.size(dataFilepath);
            if (fileSize >= fileMaxSize) {
                return dataFileVersion(rootPath, ++dataFileVersion, fileMaxSize);
            }
        }
        return dataFileVersion;
    }
}
