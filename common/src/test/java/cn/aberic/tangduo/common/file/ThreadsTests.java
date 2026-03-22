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

package cn.aberic.tangduo.common.file;

import org.junit.jupiter.api.Test;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class ThreadsTests {

    private final Object object = new Object();
    // 关键：状态标志，记录是否已发出通知
    private boolean isNotified = false;

    public void lockWaitNotify() throws InterruptedException {
        // 消费者
        new Thread(() -> {
            synchronized (object) {
                // 关键：循环判断，而不是 if
                while (!isNotified) {
                    try {
                        System.out.println("消费者 wait 等待通知");
                        object.wait();
                        System.out.println("消费者被唤醒，检查标志：" + isNotified);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
                System.out.println("消费者正常执行完毕");
            }
        }).start();

        // 保证消费者先启动并进入锁（模拟真实场景，非必须）
        Thread.sleep(10);

        // 生产者
        new Thread(() -> {
            synchronized (object) {
                System.out.println("生产者执行 notify");
                // 关键：先修改状态，再 notify
                isNotified = true;
                object.notify();
            }
        }).start();
    }

    @Test
    void demo() throws InterruptedException {
        lockWaitNotify();
    }




    private final Lock lock = new ReentrantLock();
    private final Condition condition = lock.newCondition();

    public void lockCondition() {
        // 消费者
        new Thread(() -> {
            lock.lock();
            try {
                while (!isNotified) {
                    condition.await();
                }
                System.out.println("消费者执行完毕");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                lock.unlock();
            }
        }).start();

        // 生产者
        new Thread(() -> {
            lock.lock();
            try {
                isNotified = true;
                condition.signal();
            } finally {
                lock.unlock();
            }
        }).start();
    }

    @Test
    void demo1() {
        lockCondition();
    }

}
