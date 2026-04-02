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

package cn.aberic.tangduo.db.common;

import org.junit.jupiter.api.Test;

public class SegToolsTests {

    @Test
    void ik1() {
        String str = "我在SpringBoot中使用IK分词器做中英文分词";
        System.out.println("ik tokenize = " + IkTokenizerTools.tokenize(str));
        System.out.println("ik tokenize true = " + IkTokenizerTools.tokenize(str, true));
        System.out.println("ik tokenize false = " + IkTokenizerTools.tokenize(str, false));
        System.out.println("ik seg = " + IkTokenizerTools.seg(str));
        System.out.println("hanlp segNormal = " + HanlpTools.segNormal(str));
        System.out.println("hanlp segFilter = " + HanlpTools.segFilter(str));
        System.out.println("hanlp segFilter4datetimeKey = " + HanlpTools.segFilter4datetimeKey(str));
        System.out.println("hanlp seg = " + HanlpTools.seg(str));
    }

    final String jsonStr = """
                {
                  "name": "City School",
                  "description": "ICSE",
                  "street": "West End",
                  "city": "Meerut",
                  "state": "UP",
                  "zip": "250002",
                  "location": [
                    28.9926174,
                    77.692485
                  ],
                  "fees": 3500,
                  "tags": [
                    "fully computerized"
                  ],
                  "rating": 4.5,
                  "rating1": 4.6,
                  "brief": "我在SpringBoot中使用IK分词器做中英文分词",
                  "love": "i love u"
                }""";

    @Test
    void hanlp0lk() {
        System.out.println("ik tokenize = " + IkTokenizerTools.tokenize(jsonStr));
        System.out.println("ik tokenize true = " + IkTokenizerTools.tokenize(jsonStr, true));
        System.out.println("ik seg = " + IkTokenizerTools.seg(jsonStr));
        System.out.println("hanlp segNormal = " + HanlpTools.segNormal(jsonStr));
        System.out.println("hanlp segFilter = " + HanlpTools.segFilter(jsonStr));
        System.out.println("hanlp segFilter4datetimeKey = " + HanlpTools.segFilter4datetimeKey(jsonStr));
        System.out.println("hanlp seg = " + HanlpTools.seg(jsonStr));
    }

    final static String text = """
            读书能够丰富我们的知识储备，让我们在面对问题时更加从容。
            通过阅读，我们可以接触不同的思想，拓宽眼界，提升认知水平。
            长期坚持学习，能让内心更加沉稳，也能在生活和工作中获得更多机会。
            不断学习，是提升自我最稳妥的方式，也能让人生拥有更多可能。
            """;
    @Test
    void hanlp1lk() {
        System.out.println(IkTokenizerTools.tokenize(text));
        System.out.println(IkTokenizerTools.tokenize(text, true));
        System.out.println(HanlpTools.segNormal(text));
        System.out.println(HanlpTools.segFilter(text));
        System.out.println(HanlpTools.segFilter4datetimeKey(text));
    }

    @Test
    void hanlpWithPos() {
        System.out.println(HanlpTools.segWithPos(text));
    }

    @Test
    void hanlpKeywords() {
        System.out.println(HanlpTools.keywords(text));
    }

    @Test
    void hanlpSummary() {
        System.out.println(HanlpTools.summary(text));
    }

}
