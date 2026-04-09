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

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Bm25Tools {

    // ===================== Lucene 官方默认参数 =====================
    private static final double K1 = 1.2;
    private static final double B = 0.75;
    // ===============================================================

    /**
     * 纯Java BM25 排序（结果 = Lucene原生BM25）
     *
     * @param docs  文档列表
     * @param query 搜索词
     *
     * @return 按BM25分数降序排列
     */
    public static List<DocItem> rank(List<DocItem> docs, String query, String seg) {
        if (docs == null || docs.isEmpty() || query == null || query.isBlank()) {
            return docs;
        }

        // ========== 1. 分词（你可替换成IK/HanLP，这里用空格/通用分词保持通用）==========
        List<String> queryTerms;
        if (seg.equals("ik")) {
            queryTerms = IkTokenizerTools.tokenize(query.toLowerCase());
        } else {
            queryTerms = HanlpTools.segFilter(query.toLowerCase());
        }
        Map<String, List<String>> docTermMap = new HashMap<>();
        Map<String, Integer> docLengthMap = new HashMap<>();

        for (DocItem doc : docs) {
            docTermMap.put(doc.id, doc.segList);
            docLengthMap.put(doc.id, doc.segList.size());
        }

        // ========== 2. 全局统计（预计算，性能关键）==========
        int totalDocs = docs.size();
        double avgDocLength = docLengthMap.values().stream().mapToInt(Integer::intValue).average().orElse(1);

        // 统计每个词出现在多少篇文档中 (DF)
        Map<String, Integer> dfMap = new HashMap<>();
        for (String term : queryTerms) {
            int count = 0;
            for (List<String> terms : docTermMap.values()) {
                if (terms.contains(term)) count++;
            }
            dfMap.put(term, count);
        }

        // ========== 3. 逐文档计算 BM25 分数（Lucene 同款公式）==========
        Map<String, Double> scoreMap = new HashMap<>();
        for (DocItem doc : docs) {
            String docId = doc.id;
            List<String> docTerms = docTermMap.get(docId);
            int docLen = docLengthMap.get(docId);
            double score = 0.0;

            for (String term : queryTerms) {
                int df = dfMap.getOrDefault(term, 0);
                if (df == 0) continue;
                // ==================== Lucene IDF 公式 ====================
                double idf = Math.log(1 + (totalDocs - df + 0.5) / (df + 0.5));
                // ==================== 词频 TF ====================
                long tf = Collections.frequency(docTerms, term);
                // ==================== Lucene BM25 核心公式 ====================
                double norm = tf + K1 * (1 - B + B * docLen / avgDocLength);
                double tfComponent = tf * (K1 + 1) / norm;

                score += idf * tfComponent;
            }

            // 标题加权（可选，和Lucene boosting效果一致）
            if (doc.content.toLowerCase().contains(query.toLowerCase())) {
                score *= 2.0;
            }
            doc.score = score;
            scoreMap.put(docId, score);
        }

        // ========== 4. 按分数排序 ==========
        return docs.stream()
                .sorted((a, b) -> Double.compare(scoreMap.get(b.id), scoreMap.get(a.id)))
                .collect(Collectors.toList());
    }

    @Data
    @AllArgsConstructor
    public static class DocItem {
        private String id;
        private String content;
        double score = 0.0;
        /// 预计算好的分词（关键：分词只做一次，不重复做）
        private List<String> segList;

        public DocItem(String id, String content, List<String> segList) {
            this.id = id;
            this.content = content;
            this.segList = segList;
        }

        @Override
        public String toString() {
            return "score = " + score + ", content = " + content;
        }
    }

}
