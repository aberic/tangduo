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
import java.util.List;
import java.util.concurrent.CountDownLatch;

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
    /// 每条索引检索的最大数据量
    @Value("${custom.db.DB_SEARCH_MAX_COUNT}")
    int searchMaxCount;

    @PutMapping({"create/{dbName}"})
    public Response create(@PathVariable("dbName") String dbName) {
        log.info("PUT db/create/{} 建库，库名：{}", dbName, dbName);
        try {
            DB.getInstance(rootpath, dataFileMaxSize, searchMaxCount).createDB(dbName);
            return Response.success();
        } catch (IOException | NoSuchFieldException | InstanceAlreadyExistsException e) {
            return Response.failed(e);
        }
    }

    @PutMapping({"{dbName}"})
    public Response putD(@PathVariable("dbName") String dbName, @RequestBody String data) {
        log.info("PUT db/{} 向数据库 {} 中插入数据，数据长度 {}}", dbName, dbName, data.length());
        try {
            DB.getInstance(rootpath, dataFileMaxSize, searchMaxCount).put(dbName, data);
            return Response.success();
        } catch (IOException | NoSuchFieldException e) {
            return Response.failed(e);
        }
    }

    @PutMapping({"batch/{dbName}"})
    public Response putBD(@PathVariable("dbName") String dbName, @RequestBody List<String> dataList) {
        log.info("PUT db/batch/{} 向数据库 {} 中批量插入数据，数据量 {}}", dbName, dbName, dataList.size());
        try {
            DB db = DB.getInstance(rootpath, dataFileMaxSize, searchMaxCount);
            CountDownLatch latch = new CountDownLatch(dataList.size()); // 计数3
            for (String data : dataList) {
                Thread.startVirtualThread(() -> {
                    try {
                        db.put(dbName, data);
                    } catch (IOException ignored) {
                    } finally {
                        latch.countDown();
                    }
                });
            }
            try {
                latch.await();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            return Response.success();
        } catch (IOException | NoSuchFieldException e) {
            return Response.failed(e);
        }
    }

    @PutMapping({"{dbName}/{indexName}/{key}"})
    public Response putDIK(@PathVariable("dbName") String dbName, @PathVariable("indexName") String indexName,
                           @PathVariable("key") String key, @RequestBody String data) {
        log.info("PUT db/{}/{}/{} 向指定数据库 {} 指定索引 {} 中插入key为 {} 的数据，数据长度 {}}", dbName, indexName, key, dbName, indexName, key, data.length());
        try {
            DB.getInstance(rootpath, dataFileMaxSize, searchMaxCount).put(dbName, indexName, key, data);
            return Response.success();
        } catch (IOException | NoSuchFieldException e) {
            return Response.failed(e);
        }
    }

    @GetMapping({"{dbName}/{query}"})
    public Response getD(@PathVariable("dbName") String dbName, @PathVariable("query") String query) {
        log.info("GET db/{} 从数据库 {} 中获取数据，获取依据问题：{}", dbName, dbName, query);
        try {
            return Response.success(DB.getInstance(rootpath, dataFileMaxSize, searchMaxCount).get(dbName, query));
        } catch (IOException | NoSuchFieldException e) {
            return Response.failed(e);
        }
    }

    @GetMapping({"{dbName}/{query}/{size}"})
    public Response getD(@PathVariable("dbName") String dbName, @PathVariable("query") String query, @PathVariable("size") int size) {
        log.info("GET db/{} 从数据库 {} 中获取数据，获取依据问题：{}", dbName, dbName, query);
        try {
            return Response.success(DB.getInstance(rootpath, dataFileMaxSize, searchMaxCount).get(dbName, query, size));
        } catch (IOException | NoSuchFieldException e) {
            return Response.failed(e);
        }
    }

}
