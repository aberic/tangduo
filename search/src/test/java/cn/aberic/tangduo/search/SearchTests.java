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

package cn.aberic.tangduo.search;

import cn.aberic.tangduo.common.ByteTools;
import cn.aberic.tangduo.db.DB;
import cn.aberic.tangduo.db.common.CommonTools;
import cn.aberic.tangduo.index.engine.entity.Search;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

public class SearchTests {

    @Test
    @Order(2)
    void get() throws IOException, NoSuchFieldException {
        String rootpath = "tmp/data";
        String dbName = "test";
        String indexName = "include";
        DB db = DB.getInstance(rootpath, 10737418240L);
        indexName = CommonTools.indexName4datetime(indexName);
        List<byte[]> bytesList = db.select(dbName, new Search(indexName, 100, true));
        if (Objects.isNull(bytesList)) {
            System.out.println("bytesList is null");
            return;
        }
        bytesList.forEach(bytes -> System.out.println(ByteTools.toString(bytes)));
    }

}
