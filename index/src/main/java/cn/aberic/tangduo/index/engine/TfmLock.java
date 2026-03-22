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


import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/** [filepath + MateSeekLock] */
@Slf4j
public class TfmLock {

    /** [filepath + MateSeekLock] */
    private static final Map<String, FileMate> tfm = new ConcurrentHashMap<>();

    final static ReadWriteLock rwLock = new ReentrantReadWriteLock();
    final static Lock readLock = rwLock.readLock();  // 多线程可同时加锁
    final static Lock writeLock = rwLock.writeLock();// 独占锁

    public static void lock(long transactionId, String filepath, long mateSeek) {
        FileMate fileMate = getFileMate(filepath);
        if (Objects.isNull(fileMate)) {
            put(transactionId, filepath, mateSeek);
        }
        getFileMate(filepath).lock(transactionId, mateSeek);
    }

    public static void unlock(long transactionId, String filepath, long mateSeek) {
        FileMate fileMate = getFileMate(filepath);
        if (Objects.nonNull(fileMate)) {
            fileMate.unlock(transactionId, mateSeek);
        } else {
            log.warn("TfmLock unlock transactionId = {}, mateSeek = {}", transactionId, mateSeek);
        }
    }

    private static void put(long transactionId, String filepath, long mateSeek) {
        writeLock.lock();
        try {
            if (!tfm.containsKey(filepath)) {
                tfm.put(filepath, new FileMate(transactionId, mateSeek));
            }
        } finally {
            writeLock.unlock();
        }
    }

    private static FileMate getFileMate(String filepath) {
        readLock.lock();
        try {
            return tfm.get(filepath);
        } finally {
            readLock.unlock();
        }
    }

    static class FileMate {

        private static final Map<Long, ReentrantLock> fm = new ConcurrentHashMap<>();

        final ReadWriteLock rwLock = new ReentrantReadWriteLock();
        final Lock readLock = rwLock.readLock();  // 多线程可同时加锁
        final Lock writeLock = rwLock.writeLock();// 独占锁

        List<Long> transactionIdList = new CopyOnWriteArrayList<>();

        public FileMate(long transactionId, long mateSeek) {
            transactionIdList.add(transactionId);
            put(mateSeek);
        }

        private void put(Long mateSeek) {
            writeLock.lock();
            try {
                if (!fm.containsKey(mateSeek)) {
                    fm.put(mateSeek, new ReentrantLock(true));
                }
            } finally {
                writeLock.unlock();
            }
        }

        private void put(Long mateSeek, ReentrantLock lock) {
            writeLock.lock();
            try {
                if (!fm.containsKey(mateSeek)) {
                    fm.put(mateSeek, lock);
                }
            } finally {
                writeLock.unlock();
            }
        }

        private ReentrantLock getReentrantLock(Long mateSeek) {
            readLock.lock();
            try {
                return fm.get(mateSeek);
            } finally {
                readLock.unlock();
            }
        }

        public void lock(long transactionId, long mateSeek) {
            ReentrantLock reentrantLock = getReentrantLock(mateSeek);
            if (Objects.isNull(reentrantLock)) {
                if (mateSeek == 1026) {
                    log.debug("lock transactionId = {}, mateSeek == 1026, null", transactionId);
                }
                reentrantLock = new ReentrantLock(true);
                put(mateSeek, reentrantLock);
            } else {
                if (mateSeek == 1026) {
                    log.debug("lock transactionId = {}, mateSeek == 1026, not null", transactionId);
                }
            }
            reentrantLock.lock();
            if (mateSeek == 1026) {
                log.debug("lock transactionId = {}, mateSeek == 1026", transactionId);
                log.debug("fm = {}, ptr = {}", fm.keySet().toArray(), reentrantLock);
            }
        }

        public void unlock(long transactionId, long mateSeek) {
            ReentrantLock reentrantLock = getReentrantLock(mateSeek);
            if (Objects.nonNull(reentrantLock) && reentrantLock.isHeldByCurrentThread()) {
                log.debug("unlock transactionId = {}, mateSeek == {}, not null", mateSeek, transactionId);
                reentrantLock.unlock();
            } else {
                log.warn("TfmLock FileMate unlock transactionId = {}, mateSeek = {}", transactionId, mateSeek);
            }
        }
    }
}
