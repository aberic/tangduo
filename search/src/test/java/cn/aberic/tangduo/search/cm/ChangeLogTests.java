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

package cn.aberic.tangduo.search.cm;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class ChangeLogTests {

    record Log(String title, int length, Object value) {}

    record Item(String title, int length) {}

    @Test
    void append() throws IOException {
        Path path = Path.of("tmp/changeLog");
        if (Files.notExists(path)) {
            Files.createDirectories(path);
        }
        ChangeLog.startWriteThread();
        Item item = new Item("1", 1);
        Log log = new Log("t1", 1, item);
        ChangeLog.append("put", "tmp/changeLog", log.toString());
    }

}
