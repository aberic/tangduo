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

import static cn.aberic.tangduo.search.cm.DocGenerator.generateDoc;

public class DocGeneratorTests {

    @Test
    void generate() {
        for (int i = 0; i < 10; i++) {
            System.out.println("【文档 " + (i + 1) + "】");
            System.out.println(generateDoc());
            System.out.println("字数：" + generateDoc().length());
            System.out.println("---------------------------------------");
        }
    }

}
