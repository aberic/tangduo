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

import com.hankcs.hanlp.HanLP;
import com.hankcs.hanlp.seg.common.Term;

import java.math.BigDecimal;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/// 汉语分词工具类
public final class HanlpTools {

    private HanlpTools() {
        throw new AssertionError("工具类禁止实例化");
    }

    /// 无意义英文正则：连续重复字母（aa, bb, aaa, abab）、纯2字母以内乱码英文
    private static final Pattern MEANINGLESS_EN = Pattern.compile("^([a-z])\\1{1,}$|^[a-z]{1,2}$");
    /// 特殊符号正则（全覆盖 - * # 等）
    private static final Pattern SPECIAL_SYMBOL = Pattern.compile("[^a-zA-Z0-9\\u4e00-\\u9fa5]");


    /// 标准分词，过滤数字、空格、空串、标点，并去重
    ///
    /// @param text 待分词文本
    ///
    /// @return 分词结果
    ///
    /// @see #segFilterWithNature(String)
    public static List<String> segNormal(String text) {
        List<Term> termList = HanLP.segment(text);
        return termList.stream()
                .filter(term -> !term.nature.startsWith("w"))
                .map(term -> term.word.trim())
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

    /// 标准分词，过滤无意义词（标点 + 助词 + 介词 + 连词 + 副词），并去重
    ///
    /// @param text 待分词文本
    ///
    /// @return 分词结果
    ///
    /// @see #segNormal(String)
    public static List<Term> segFilterWithNature(String text) {
        // ====================== 【第一步：全局过滤特殊符号】 ======================
        text = SPECIAL_SYMBOL.matcher(text).replaceAll(" "); // 把符号全部换成空格
        text = text.replaceAll("\\s+", " ").trim(); // 多空格变单空格
        List<Term> termList = HanLP.segment(text);
        return termList.stream()
                .filter(term -> {
                    // 过滤：w, u, p, c, d, r, m, q, f, s, t, b, e, y, o
                    return !(term.nature.startsWith("w") ||
                            term.nature.startsWith("u") ||
                            term.nature.startsWith("p") ||
                            term.nature.startsWith("c") ||
                            term.nature.startsWith("d") ||
                            term.nature.startsWith("r") ||
                            term.nature.startsWith("m") ||
                            term.nature.startsWith("q") ||
                            "f".equals(term.nature.toString()) ||
                            "s".equals(term.nature.toString()) ||
                            "t".equals(term.nature.toString()) ||
                            "b".equals(term.nature.toString()) ||
                            "e".equals(term.nature.toString()) ||
                            "y".equals(term.nature.toString()) ||
                            "o".equals(term.nature.toString()));
                })
                .filter(term -> CommonTools.unSingleChinese(term.word))
                .filter(term -> CommonTools.unSingleLetter(term.word))
                .filter(term -> (!MEANINGLESS_EN.matcher(term.word.toLowerCase()).matches()))
                .filter(term -> {
                    if (term.word.isBlank()) return false;
                    try {
                        new BigDecimal(term.word.trim());
                        return false;
                    } catch (NumberFormatException e) {
                        return true;
                    }
                })
                .distinct()                       // 去重
                .collect(Collectors.toList());
    }

    /// 标准分词，过滤无意义词（标点 + 助词 + 介词 + 连词 + 副词），并去重
    ///
    /// @param text 待分词文本
    ///
    /// @return 分词结果
    ///
    /// @see #segFilterWithNature(String)
    public static List<String> segFilter(String text) {
        // ====================== 【第一步：全局过滤特殊符号】 ======================
        text = SPECIAL_SYMBOL.matcher(text).replaceAll(" "); // 把符号全部换成空格
        text = text.replaceAll("\\s+", " ").trim(); // 多空格变单空格
        List<Term> termList = HanLP.segment(text);
        return termList.stream()
                .filter(term -> {
                    // 过滤：w, u, p, c, d, r, m, q, f, s, t, b, e, y, o
                    return !(term.nature.startsWith("w") ||
                            term.nature.startsWith("u") ||
                            term.nature.startsWith("p") ||
                            term.nature.startsWith("c") ||
                            term.nature.startsWith("d") ||
                            term.nature.startsWith("r") ||
                            term.nature.startsWith("m") ||
                            term.nature.startsWith("q") ||
                            "f".equals(term.nature.toString()) ||
                            "s".equals(term.nature.toString()) ||
                            "t".equals(term.nature.toString()) ||
                            "b".equals(term.nature.toString()) ||
                            "e".equals(term.nature.toString()) ||
                            "y".equals(term.nature.toString()) ||
                            "o".equals(term.nature.toString()));
                })
                .map(term -> term.word.trim())
                .filter(CommonTools::unSingleChinese)
                .filter(CommonTools::unSingleLetter)
                .filter(s -> (!MEANINGLESS_EN.matcher(s.toLowerCase()).matches()))
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
                .map(String::toLowerCase)
                .filter(s -> !s.isBlank())  // 去掉空串
                .distinct()                       // 去重
                .collect(Collectors.toList());
    }

    /// 标准分词，过滤无意义词（标点 + 助词 + 介词 + 连词 + 副词），并去重
    ///
    /// @param text 待分词文本
    ///
    /// @return 分词结果
    ///
    /// @see #segFilter(String)
    public static List<String> seg(String text) {
        return segFilter4datetimeKey(text.toLowerCase());
    }

    /// 标准分词，过滤无意义词（标点 + 助词 + 介词 + 连词 + 副词），并去重
    ///
    /// @param text 待分词文本
    ///
    /// @return 分词结果
    ///
    /// @see #segFilter(String)
    /// @see #segNormal(String)
    public static List<String> segFilter4datetimeKey(String text) {
        // ====================== 【第一步：全局过滤特殊符号】 ======================
        text = SPECIAL_SYMBOL.matcher(text).replaceAll(" "); // 把符号全部换成空格
        text = text.replaceAll("\\s+", " ").trim(); // 多空格变单空格
        List<Term> termList = HanLP.segment(text);
        return termList.stream()
                .filter(term -> {
                    // 过滤：w, u, p, c, d, r, m, q, f, s, t, b, e, y, o
                    return !(term.nature.startsWith("w") ||
                            term.nature.startsWith("u") ||
                            term.nature.startsWith("p") ||
                            term.nature.startsWith("c") ||
                            term.nature.startsWith("d") ||
                            term.nature.startsWith("r") ||
                            term.nature.startsWith("m") ||
                            term.nature.startsWith("q") ||
                            "f".equals(term.nature.toString()) ||
                            "s".equals(term.nature.toString()) ||
                            "t".equals(term.nature.toString()) ||
                            "b".equals(term.nature.toString()) ||
                            "e".equals(term.nature.toString()) ||
                            "y".equals(term.nature.toString()) ||
                            "o".equals(term.nature.toString()));
                })
                .map(term -> term.word.trim())
                .filter(CommonTools::unSingleChinese)
                .filter(CommonTools::unSingleLetter)
                .filter(s -> (!MEANINGLESS_EN.matcher(s.toLowerCase()).matches()))
                .map(s -> CommonTools.indexName4datetime(s.trim())) // 去空格同时转时间戳key
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

    /// 分词 + 词性标注
    ///
    /// @param text 待分词文本
    ///
    /// @return 分词结果
    ///
    /// @see #segNormal(String)
    public static List<Term> segWithPos(String text) {
        return HanLP.segment(text);
    }

    /// 提取关键词
    ///
    /// @param text 待提取关键词文本
    ///
    /// @return 关键词结果
    public static List<String> keywords(String text) {
        int size;
        if (text.length() <= 100) {
            size = 3;
        } else if (text.length() <= 150) {
            size = 4;
        } else if (text.length() <= 200) {
            size = 5;
        } else if (text.length() <= 300) {
            size = 6;
        } else if (text.length() <= 400) {
            size = 7;
        } else if (text.length() <= 500) {
            size = 8;
        } else {
            size = 12;
        }
        return keywords(text, size);
    }

    /// 提取关键词
    ///
    /// @param text 待提取关键词文本
    ///
    /// @return 关键词结果
    /// size=1：太少，代表性差
    /// size=2：勉强可用
    /// size=3~5：最适合短文本，效果最佳
    /// size>10：短文本会出现无关、噪音词
    ///
    /// 短文本（100 字左右，你之前的测试文本）
    /// size = 3 ~ 5
    /// 3 个：最核心
    /// 5 个：完整覆盖主题
    /// 2）中长文本（300~500 字）
    /// size = 5 ~ 8
    /// 3）长文本（800~2000 字）
    /// size = 8 ~ 12
    public static List<String> keywords(String text, int size) {
        return HanLP.extractKeyword(text, size);
    }

    /// 提取摘要
    ///
    /// @param text 待提取摘要文本
    ///
    /// @return 摘要结果
    public static List<String> summary(String text) {
        int size;
        if (text.length() <= 100) {
            size = 1;
        } else if (text.length() <= 350) {
            size = 2;
        } else if (text.length() <= 500) {
            size = 3;
        } else {
            size = 5;
        }
        return summary(text, size);
    }

    /// 提取摘要
    ///
    /// @param text 待提取摘要文本
    ///
    /// @return 摘要结果
    /// size = 1～2
    /// 短文本身就 1～3 句，抽 2 句基本等于全文，1 句最像摘要。
    /// 300～500 字段落
    /// size = 2～3
    /// 800～2000 字文章
    /// size = 3～5
    public static List<String> summary(String text, int size) {
        return HanLP.extractSummary(text, size);
    }

}
