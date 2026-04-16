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

package cn.aberic.tangduo.db.entity;

import org.junit.jupiter.api.Test;

public class DocTests {

    record User(String name, int age) {}

    record Role(int id, User user) {}

    Role role(int i) {
        User user = new User("name" + i, i);
        return new Role(i, user);
    }

    @Test
    void doc() {
        Doc doc = new Doc("db", "idx", "key", 1, role(100));
        System.out.println("doc = " + doc);
        byte[] bytes = doc.toBytes();
        Doc docFromBytes = new Doc(bytes);
        System.out.println("docFromBytes = " + docFromBytes);
    }
}
