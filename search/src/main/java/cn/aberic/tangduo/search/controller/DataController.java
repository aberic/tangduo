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

import cn.aberic.tangduo.common.SHA256Tools;
import cn.aberic.tangduo.common.http.Response;
import cn.aberic.tangduo.db.DB;
import cn.aberic.tangduo.index.engine.IEngine;
import cn.aberic.tangduo.search.entity.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("data")
public class DataController {

    /// 数据库根路径
    @Value("${custom.db.DB_ROOT_PATH}")
    String rootpath;
    /// 数据文件大小阈值，单位byte
    @Value("${custom.db.DB_DATA_FILE_MAX_SIZE}")
    long dataFileMaxSize;
    /// 每条索引检索的最大数据量
    @Value("${custom.db.DB_SEARCH_MAX_COUNT}")
    int searchMaxCount;

    @PutMapping()
    public Response putData(@RequestBody ReqPutDataVO reqPutDataVO) {
        log.debug("PUT data 向 {}/{} 中插入数据，数据摘要 {}", reqPutDataVO.getDatabase(), reqPutDataVO.getIndex(), SHA256Tools.sha256(String.valueOf(reqPutDataVO.getValue())));
        try {
            DB.getInstance(rootpath, dataFileMaxSize, searchMaxCount).put(reqPutDataVO.getDatabase(), reqPutDataVO.getIndex(), reqPutDataVO.getKey(), reqPutDataVO.isSeg(), String.valueOf(reqPutDataVO.getValue()));
            return Response.success();
        } catch (IOException | NoSuchFieldException e) {
            return Response.failed(e);
        }
    }

    @PutMapping({"batch"})
    public Response putDataBatch(@RequestBody ReqPutDataBatchVO reqPutDataBatchVO) {
        log.debug("PUT data 向 {}/{} 中批量插入数据，数据摘要 {}", reqPutDataBatchVO.getDatabase(), reqPutDataBatchVO.getIndex(), SHA256Tools.sha256(String.valueOf(reqPutDataBatchVO.getValues())));
        Map<String, String> map = reqPutDataBatchVO.getValues().stream().collect(Collectors.toMap(
                        ReqPutDataBatchVO.Value::getKey,
                        value -> String.valueOf(value.getValue()),
                        (oldName, newName) -> oldName
                )
        );
        try {
            DB db = DB.getInstance(rootpath, dataFileMaxSize, searchMaxCount);
            CountDownLatch latch = new CountDownLatch(map.size()); // 计数3
            for (Map.Entry<String, String> keyValueEntry : map.entrySet()) {
                Thread.startVirtualThread(() -> {
                    try {
                        db.put(reqPutDataBatchVO.getDatabase(), reqPutDataBatchVO.getIndex(), keyValueEntry.getKey(), reqPutDataBatchVO.isSeg(), keyValueEntry.getValue());
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

    @GetMapping()
    public Response getData(@RequestBody ReqGetDataVO data) {
        log.debug("GET data 从 {}/{} 中读取数据", data.getDatabase(), data.getIndex());
        try {
            DB.getInstance(rootpath, dataFileMaxSize, searchMaxCount).get(data.getDatabase(), data.getIndex(), null, data.getKey());
            return Response.success();
        } catch (IOException | NoSuchFieldException e) {
            return Response.failed(e);
        }
    }

    @GetMapping("search")
    public Response search(@RequestBody ReqSearchDataVO data) {
        log.debug("GET data 从 {}/{} 中读取数据", data.getDatabase(), data.getIndex());
        try {
            DB.getInstance(rootpath, dataFileMaxSize, searchMaxCount).search(data.getDatabase(), data.getQuery(), fromReq(data));
            return Response.success();
        } catch (IOException | NoSuchFieldException e) {
            return Response.failed(e);
        }
    }

    @GetMapping("select")
    public Response select(@RequestBody ReqSelectDataVO data) {
        log.debug("GET data 从 {}/{} 中读取数据", data.getDatabase(), data.getIndex());
        try {
            DB.getInstance(rootpath, dataFileMaxSize, searchMaxCount).select(data.getDatabase(), fromReq(data));
            return Response.success();
        } catch (IOException | NoSuchFieldException e) {
            return Response.failed(e);
        }
    }

    private IEngine.Search fromReq(ReqSelectDataVO data) {
        IEngine.Search search = new IEngine.Search();
        search.setIndexName(data.getIndex());
        search.setDegreeMin(data.getDegreeMin());
        search.setDegreeMax(data.getDegreeMax());
        search.setIncludeMin(data.isIncludeMin());
        search.setIncludeMax(data.isIncludeMax());
        search.setLimit(data.getLimit());
        search.setAsc(data.isAsc());
        return search;
    }

}
