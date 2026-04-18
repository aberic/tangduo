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

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.*;
import java.util.Comparator;
import java.util.stream.Stream;

/// 文件工具类
@Slf4j
public final class Filer {

    private Filer() {
        throw new AssertionError("工具类禁止实例化");
    }

    /// 创建文件
    ///
    /// @param filepath 指定文件路径
    ///
    /// @throws IOException 异常
    public static synchronized void createFile(String filepath) throws IOException {
        createFile(Path.of(filepath));
    }

    /// 创建文件
    ///
    /// @param filepath 指定文件路径
    ///
    /// @throws IOException 异常
    public static synchronized void createFile(Path filepath) throws IOException {
        createDirectory(filepath.getParent());
        try {
            Files.createFile(filepath);
        } catch (FileAlreadyExistsException ignored) {}
    }

    /// 创建目录
    ///
    /// @param filepath 指定目录路径
    ///
    /// @throws IOException 异常
    public static synchronized void createDirectory(String filepath) throws IOException {
        createDirectory(Path.of(filepath));
    }

    /// 创建目录
    ///
    /// @throws IOException 异常
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

    /// 删除目录
    ///
    /// @param dirPath 指定目录路径
    public static void deleteDirectory(String dirPath) {
        deleteDirectory(Paths.get(dirPath));
    }

    /// 删除目录
    ///
    /// @param path 指定目录路径
    public static void deleteDirectory(Path path) {
        Path normalizedPath = path.toAbsolutePath().normalize();
        if (!Files.exists(normalizedPath)) {
            return;
        }
        try (Stream<Path> stream = Files.walk(path)) {
            stream.sorted(Comparator.reverseOrder())
                    .forEach(p -> {
                        try {
                            Files.delete(p);
                        } catch (IOException ignored) {}
                    });
        } catch (SecurityException | IOException e) {
            log.error("deleteDirectory error! e: {}", e.getMessage());
        }
    }

}
