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

package cn.aberic.tangduo.sdk.log;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class LogBatchSender {

    // 批量入库 + 失败重试
    public static boolean sendBatch(String serverUrl, LinkedBlockingQueue<LogEntity> queue, int batchSize) {
        if (queue.isEmpty()) return false;

        List<LogEntity> list = new ArrayList<>(batchSize);
        queue.drainTo(list, batchSize);

        if (list.isEmpty()) return false;

        int retry = 0;
        boolean success = false;

        while (retry < 2 && !success) {
            try {
                // mongoTemplate.insertAll(list);
                // todo 真实批量写入
                System.out.println("模拟批量写入开始：serverUrl" + serverUrl);
                System.out.println(list);
                System.out.println("模拟批量写入结束：serverUrl" + serverUrl);
                success = true;
            } catch (Exception e) {
                retry++;
                try {
                    TimeUnit.MILLISECONDS.sleep(200);
                } catch (InterruptedException ie) {
                    break;
                }
            }
        }
        return true;
    }
}