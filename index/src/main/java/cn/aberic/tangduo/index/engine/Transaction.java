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

import cn.aberic.tangduo.common.file.Channel;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** 事务，用于存储每条完整命令的写入计划，待验证完成后一次执行，或全部失败，或全部成功 */
@Slf4j
@Data
public class Transaction {

    /** 事务号，长整型自增 */
    long number;
    /** 待执行任务集合 */
    Map<String, List<Task>> taskListMap = new HashMap<>();

    public Transaction(long number) {
        this.number = number;
    }

    public void addTask(Task task) {
        if (taskListMap.containsKey(task.filepath)) {
            taskListMap.get(task.filepath).add(task);
        } else {
            List<Task> tasks = new ArrayList<>();
            tasks.add(task);
            taskListMap.put(task.filepath, tasks);
        }
    }

    public void addTask(String filepath, long seek, byte[] dataNew, byte[] dataOrigin) {
        addTask(new Task(filepath, seek, dataNew, dataOrigin));
    }

    /**
     * 执行事务。
     * 事务的执行必须全部成功或全部失败。
     * 若即将写入成功事务时发生磁盘不足、宕机等不可控情况时，需保证事务在执行前已经做了日志记录，待服务恢复后根据日志记录重新执行事务
     */
    public void execute() {
        boolean rollback = false;
        // 默认分配1MB的缓冲区，即 1024*1024 byte
        Map<String, List<Task>> successTaskListMap = new HashMap<>();
        for (Map.Entry<String, List<Task>> filepathListEntry : taskListMap.entrySet()) {
            List<Task> successTaskList = new ArrayList<>();
            successTaskListMap.put(filepathListEntry.getKey(), successTaskList);
            for (Task task : filepathListEntry.getValue()) {
                try {
                    Channel.write(filepathListEntry.getKey(), task.seek, task.dataNew);
                    successTaskList.add(task);
                } catch (IOException e) {
                    log.warn("transaction-{} execute write error, rollback all task!", number);
                    rollback = true;
                    break;
                }
            }
        }
        if (rollback) {
            for (Map.Entry<String, List<Task>> filepathListEntry : successTaskListMap.entrySet()) {
                for (Task task : filepathListEntry.getValue()) {
                    try {
                        Channel.write(filepathListEntry.getKey(), task.seek, task.dataOrigin);
                    } catch (IOException e) {
                        log.warn("transaction-{} execute rollback write error!", number);
                        break;
                    }
                }
            }
        }
    }

    /** 任务 */
    public static class Task {

        // 入参开始
        /** 待写入内容的文件 */
        String filepath;
        /** 待写入内容在文件中的偏移量，当为-1时，表示追加写入 */
        long seek;
        /** 待写入的内容 */
        byte[] dataNew;
        /** 原写入的内容，可为空，即表示原先内容都是0x00 */
        byte[] dataOrigin;
        // 入参结束

        /** 写入数据后，数据在文件中的起始偏移量 */
        long writeStartSeek;

        public Task(String filepath, long seek, byte[] dataNew, byte[] dataOrigin) {
            this.filepath = filepath;
            this.seek = seek;
            this.dataNew = dataNew;
            if (dataOrigin.length == 0) {
                this.dataOrigin = new byte[dataNew.length];
            } else {
                this.dataOrigin = dataOrigin;
            }
            if (seek > 0) {
                writeStartSeek = seek;
            } else {
                writeStartSeek = -1;
            }
        }
    }

}
