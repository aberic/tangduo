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

package cn.aberic.tangduo.search.controller;

import cn.aberic.tangduo.common.http.Response;
import cn.aberic.tangduo.db.DB;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import javax.management.InstanceAlreadyExistsException;
import java.io.IOException;

@Slf4j
@RestController
@RequestMapping("db")
public class DBController {

    /// 数据库根路径
    @Value("${custom.db.DB_ROOT_PATH}")
    String rootpath;
    /// 数据文件大小阈值，单位byte
    @Value("${custom.db.DB_DATA_FILE_MAX_SIZE}")
    long dataFileMaxSize;
    /// 单批次最大数量
    @Value("${custom.index.INDEX_BATCH_MAX_SIZE}")
    int batchMaxSize;
    /// 每条索引检索的最大数据量
    @Value("${custom.db.DB_SEARCH_MAX_COUNT}")
    int searchMaxCount;

    @PutMapping({"{dbName}"})
    public Response create(@PathVariable("dbName") String dbName) {
        log.trace("PUT db/{} 建库，库名：{}", dbName, dbName);
        try {
            DB.getInstance(rootpath, dataFileMaxSize, searchMaxCount, batchMaxSize).createDB(dbName);
            return Response.success();
        } catch (IOException | NoSuchFieldException | InstanceAlreadyExistsException e) {
            return Response.failed(e);
        }
    }

    @DeleteMapping({"{dbName}"})
    public Response delete(@PathVariable("dbName") String dbName) {
        log.trace("DELETE db/{} 删库，库名：{}", dbName, dbName);
        try {
            DB.getInstance(rootpath, dataFileMaxSize, searchMaxCount, batchMaxSize).removeDB(dbName);
            return Response.success();
        } catch (IOException | NoSuchFieldException e) {
            return Response.failed(e);
        }
    }

}
