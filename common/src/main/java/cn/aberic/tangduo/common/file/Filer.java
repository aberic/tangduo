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

import java.io.IOException;
import java.nio.file.*;
import java.util.Comparator;
import java.util.stream.Stream;

public class Filer {

    public Filer() {
        throw new IllegalStateException("Filer class");
    }

    public static synchronized void createFile(String filepath) throws IOException {
        createFile(Path.of(filepath));
    }

    public static synchronized void createFile(Path filepath) throws IOException {
        createDirectory(filepath.getParent());
        try {
            Files.createFile(filepath);
        } catch (FileAlreadyExistsException ignored) {}
    }

    public static synchronized void createDirectory(String filepath) throws IOException {
        createDirectory(Path.of(filepath));
    }

    public static synchronized void createDirectory(Path path) throws IOException {
        if (!Files.exists(path)) {
            try {
                Files.createDirectory(path);
            } catch (NoSuchFileException e) {
                createDirectory(path.getParent());
                createDirectory(path);
            }
        }
    }

    public static void deleteDirectory(String dirPath) {
        deleteDirectory(Paths.get(dirPath));
    }

    public static void deleteDirectory(Path path) {
        if (!Files.exists(path)) {
            return;
        }
        try (Stream<Path> stream = Files.walk(path)) {
            stream.sorted(Comparator.reverseOrder())
                    .forEach(p -> {
                        try {
                            Files.delete(p);
                        } catch (IOException ignored) {}
                    });
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
