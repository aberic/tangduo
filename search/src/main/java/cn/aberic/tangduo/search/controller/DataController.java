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
import cn.aberic.tangduo.db.common.CommonTools;
import cn.aberic.tangduo.db.common.KeyHashTools;
import cn.aberic.tangduo.db.entity.DocPutBatchRequestVO;
import cn.aberic.tangduo.db.entity.DocPutRequestVO;
import cn.aberic.tangduo.db.entity.DocSearchResponseVO;
import cn.aberic.tangduo.index.engine.entity.Search;
import cn.aberic.tangduo.search.entity.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

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
    /// 单批次最大数量
    @Value("${custom.index.INDEX_BATCH_MAX_SIZE}")
    int batchMaxSize;
    /// 每条索引检索的最大数据量
    @Value("${custom.db.DB_SEARCH_MAX_COUNT}")
    int searchMaxCount;

    /// 插入数据
    @PutMapping()
    public Response putData(@RequestBody ReqPutDataVO vo) {
        log.debug("PUT data 向 {}/{} 中插入数据，key={},degree={},数据摘要={}", vo.getDatabase(), vo.getIndex(),
                StringUtils.isEmpty(vo.getKey()) ? SHA256Tools.sha256(String.valueOf(vo.getValue())) : vo.getKey(),
                KeyHashTools.toLongKey(StringUtils.isEmpty(vo.getKey()) ? SHA256Tools.sha256(String.valueOf(vo.getValue())) : vo.getKey()),
                SHA256Tools.sha256(String.valueOf(vo.getValue())));
        try {
            DB db = DB.getInstance(rootpath, dataFileMaxSize, searchMaxCount, batchMaxSize);
            DocPutRequestVO requestVO = new DocPutRequestVO(vo.getDatabase(), vo.getIndex(), null, vo.getKey(), vo.isSeg(), vo.getValue());
            return Response.success(db.put(requestVO));
        } catch (Exception e) {
            return Response.failed(e);
        }
    }

    /// 批量插入数据
    @PutMapping({"batch"})
    public Response putDataBatch(@RequestBody ReqPutDataBatchVO vo) {
        log.debug("PUT data 向 {}中批量插入数据，数据量 {}", vo.getDatabase(), vo.getValues().size());
        List<DocPutBatchRequestVO> batchRequestVOS = new ArrayList<>();
        vo.getValues().forEach(value -> batchRequestVOS.add(new DocPutBatchRequestVO(value.getIndex(), value.getKey(), vo.isSeg(), value.getValue())));
        try {
            DB db = DB.getInstance(rootpath, dataFileMaxSize, searchMaxCount, batchMaxSize);
            return Response.success(db.put(vo.getDatabase(), batchRequestVOS));
        } catch (Exception e) {
            return Response.failed(e);
        }
    }

    /// 读取数据
    @GetMapping()
    public Response getData(@RequestBody ReqGetDataVO vo) {
        log.debug("GET data 从 {}/{} 中读取数据，key={},degree={}", vo.getDatabase(), vo.getIndex(), vo.getKey(), KeyHashTools.toLongKey(vo.getKey()));
        try {
            DB db = DB.getInstance(rootpath, dataFileMaxSize, searchMaxCount, batchMaxSize);
            return Response.success(db.get(vo.getDatabase(), vo.getIndex(), null, vo.getKey()));
        } catch (Exception e) {
            return Response.failed(e);
        }
    }

    /// 搜索数据
    @GetMapping("search")
    public Response search(@RequestBody ReqSearchDataVO vo) {
        log.debug("SEARCH data 从 {}/{} 中search数据", vo.getDatabase(), vo.getIndex());
        try {
            DB db = DB.getInstance(rootpath, dataFileMaxSize, searchMaxCount, batchMaxSize);
            List<DocSearchResponseVO> list = db.search(vo.getDatabase(), vo.getQuery(), createSearch(vo, false));
            return Response.success(list);
        } catch (Exception e) {
            return Response.failed(e);
        }
    }

    /// 选择数据
    @GetMapping("select")
    public Response select(@RequestBody ReqSelectDataVO vo) {
        log.debug("SELECT data 从 {}/{} 中select数据", vo.getDatabase(), vo.getIndex());
        try {
            DB db = DB.getInstance(rootpath, dataFileMaxSize, searchMaxCount, batchMaxSize);
            List<DocSearchResponseVO> list = db.select(vo.getDatabase(), createSearch(vo, false));
            return Response.success(list);
        } catch (Exception e) {
            return Response.failed(e);
        }
    }

    /// 创建搜索条件
    ///
    /// @param vo     搜索请求VO
    /// @param delete 是否删除
    ///
    /// @return 搜索条件
    private Search createSearch(ReqSelectDataVO vo, boolean delete) {
        Search search = new Search();
        search.setIndexName(StringUtils.isEmpty(vo.getIndex()) ? null : CommonTools.indexName(vo.getIndex()));
        search.setDegreeMin(vo.getDegreeMin());
        search.setDegreeMax(vo.getDegreeMax());
        search.setIncludeMin(vo.isIncludeMin());
        search.setIncludeMax(vo.isIncludeMax());
        search.setLimit(vo.getLimit());
        search.setAsc(vo.isAsc());
        search.setConditions(vo.getConditions());
        search.setDelete(delete);
        return search;
    }

    /// 删除数据
    @DeleteMapping()
    public Response removeData(@RequestBody ReqRemoveDataVO vo) {
        log.debug("DELETE data 从 {}/{} 中删除数据，key={},degree={}", vo.getDatabase(), vo.getIndex(), vo.getKey(), vo.getDegree());
        try {
            DB db = DB.getInstance(rootpath, dataFileMaxSize, searchMaxCount, batchMaxSize);
            db.remove(vo.getDatabase(), vo.getIndex(), vo.getDegree(), vo.getKey());
            return Response.success();
        } catch (Exception e) {
            return Response.failed(e);
        }
    }

    /// 删除数据
    @DeleteMapping("delete")
    public Response delete(@RequestBody ReqDeleteDataVO vo) {
        log.debug("DELETE data 从 {}/{} 中delete数据", vo.getDatabase(), vo.getIndex());
        try {
            DB db = DB.getInstance(rootpath, dataFileMaxSize, searchMaxCount, batchMaxSize);
            List<DocSearchResponseVO> list = db.delete(vo.getDatabase(), createSearch(vo, true));
            return Response.success(list);
        } catch (Exception e) {
            return Response.failed(e);
        }
    }

}
