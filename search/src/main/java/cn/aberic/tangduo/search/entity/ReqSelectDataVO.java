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

package cn.aberic.tangduo.search.entity;

import cn.aberic.tangduo.db.common.CommonTools;
import cn.aberic.tangduo.index.engine.entity.Hit;
import cn.aberic.tangduo.index.engine.entity.Sort;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;

import java.util.Objects;

/// 查询数据请求体
@Data
public class ReqSelectDataVO {

    /// 数据库名
    String database;
    /// 索引名（全名组合确保唯一性，如：库名+表名+索引名）
    String index;
    /// 跳过指定条数，默认0
    int from = 0;
    /// 返回数量
    int size = 10;
    /// 命中策略
    Hit hit;

    public String getIndex() {
        return CommonTools.indexName(index);
    }

    public Hit reHit() {
        if (Objects.isNull(hit)) {
            hit = new Hit();
            hit.setSort(new Sort(CommonTools.indexName(index)));
            return hit;
        }
        if (hit.sortNotNull()) {
            if (StringUtils.isEmpty(hit.getSort().getParam())) {
                hit.setSortIndexName(CommonTools.indexName(hit.getSort().getIndexName()));
            } else {
                hit.setSortIndexName(CommonTools.indexName(hit.getSort().getOriginIndexName()));
            }
        } else {
            hit.setSort(new Sort(CommonTools.indexName(index)));
        }
        return hit;
    }

}
