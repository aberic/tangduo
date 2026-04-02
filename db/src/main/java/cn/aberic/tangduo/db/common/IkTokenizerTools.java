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

import lombok.extern.slf4j.Slf4j;
import org.wltea.analyzer.core.IKSegmenter;
import org.wltea.analyzer.core.Lexeme;

import java.io.StringReader;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public class IkTokenizerTools {

    public IkTokenizerTools() {
        throw new IllegalStateException("IkTokenizerUtil class");
    }

    /// 智能分词
    public static List<String> seg(String text) {
        return tokenize4datetimeKey(text.toLowerCase());
    }

    /// 智能分词
    public static List<String> tokenize4datetimeKey(String text) {
        List<String> list = tokenize(text, false);
        return list.stream()
                .filter(CommonTools::unSingleChinese)
                .filter(CommonTools::unSingleLetter)
                .filter(s -> {
                    if (s.isBlank()) return false;
                    try {
                        new BigDecimal(s.trim());
                        return false;
                    } catch (NumberFormatException e) {
                        return true;
                    }
                })                                // 去掉数字
                .map(String::trim)                // 去空格
                .map(s -> CommonTools.indexName4datetime(s.trim())) // 去空格同时转时间戳key
                .filter(s -> !s.isBlank())  // 去掉空串
                .distinct()                       // 去重
                .collect(Collectors.toList());
    }

    /// 智能分词
    public static List<String> tokenize(String text) {
        List<String> list = tokenize(text, false);
        return list.stream()
                .filter(CommonTools::unSingleChinese)
                .filter(CommonTools::unSingleLetter)
                .filter(s -> {
                    if (s.isBlank()) return false;
                    try {
                        new BigDecimal(s.trim());
                        return false;
                    } catch (NumberFormatException e) {
                        return true;
                    }
                })                                // 去掉数字
                .map(String::trim)                // 去空格
                .filter(s -> !s.isBlank())  // 去掉空串
                .distinct()                       // 去重
                .collect(Collectors.toList());
    }

    /**
     * 分词
     *
     * @param useSmart true=智能分词 false=最细粒度
     */
    public static List<String> tokenize(String text, boolean useSmart) {
        List<String> result = new ArrayList<>();
        if (text == null || text.isBlank()) return result;

        StringReader reader = new StringReader(text);

        try (reader) {
            IKSegmenter segmenter = new IKSegmenter(reader, useSmart);
            Lexeme lex;
            while ((lex = segmenter.next()) != null) {
                result.add(lex.getLexemeText().trim());
            }
        } catch (Exception e) {
            log.warn(e.getMessage());
        }
        return result;
    }
}
