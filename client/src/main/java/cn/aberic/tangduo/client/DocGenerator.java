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

package cn.aberic.tangduo.client;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** 定时生成 100 字左右随机文档（高词库 + 低相似度） */
public class DocGenerator {
    private static final Random RANDOM = new Random();

    /// 连接词 & 逻辑句式
    private static final List<String> LOGIC_PREFIX = Arrays.asList(
            "在实际构建中，", "从整体设计来看，", "围绕核心目标，", "为实现更好体验，",
            "立足于场景需求，", "从多角度出发，", "综合各类要素，", "通过系统化设计，"
    );

    private static final List<String> CONJUNCTION = Arrays.asList(
            "同时", "并且", "此外", "另一方面", "更重要的是", "除此之外", "进而"
    );

    /// 科技类
    private static final Map<String, List<String>> TECH = Stream.of(
            new AbstractMap.SimpleEntry<>("arch", Arrays.asList("微服务架构", "分布式体系", "云原生平台", "服务网格")),
            new AbstractMap.SimpleEntry<>("perf", Arrays.asList("并发处理能力", "延迟优化策略", "吞吐量提升", "资源调度效率")),
            new AbstractMap.SimpleEntry<>("safe", Arrays.asList("权限精细化管控", "数据脱敏加密", "访问安全审计", "风险智能识别")),
            new AbstractMap.SimpleEntry<>("ops", Arrays.asList("自动化运维", "监控告警体系", "弹性扩缩容", "故障自愈能力")),
            new AbstractMap.SimpleEntry<>("data", Arrays.asList("分库分表", "读写分离", "增量同步", "冷热数据分层"))
    ).collect(Collectors.toMap(
            Map.Entry::getKey,
            Map.Entry::getValue,
            // 解决key重复冲突（旧值覆盖/新值覆盖）
            (oldValue, newValue) -> newValue
    ));

    /// 人文地理
    private static final Map<String, List<String>> GEO = Stream.of(
            new AbstractMap.SimpleEntry<>("scene", Arrays.asList("山川河谷", "古城街巷", "滨海湾岸", "林海梯田")),
            new AbstractMap.SimpleEntry<>("culture", Arrays.asList("民俗传承", "历史文脉", "乡土记忆", "非遗技艺")),
            new AbstractMap.SimpleEntry<>("season", Arrays.asList("春日花开", "夏日蝉鸣", "秋染层林", "冬雪覆山")),
            new AbstractMap.SimpleEntry<>("human", Arrays.asList("炊烟村落", "古桥流水", "老街烟火", "庭院深深")),
            new AbstractMap.SimpleEntry<>("spirit", Arrays.asList("岁月沉淀", "山河壮阔", "人文温润", "烟火温情"))
    ).collect(Collectors.toMap(
            Map.Entry::getKey,
            Map.Entry::getValue,
            // 解决key重复冲突（旧值覆盖/新值覆盖）
            (oldValue, newValue) -> newValue
    ));

    /// 生活日常
    private static final Map<String, List<String>> LIFE = Stream.of(
            new AbstractMap.SimpleEntry<>("moment", Arrays.asList("清晨微光", "午后暖阳", "晚风轻拂", "静夜星空")),
            new AbstractMap.SimpleEntry<>("feel", Arrays.asList("平和安宁", "温暖治愈", "松弛自在", "踏实心安")),
            new AbstractMap.SimpleEntry<>("act", Arrays.asList("读书品茶", "散步闲谈", "静思观云", "下厨烟火")),
            new AbstractMap.SimpleEntry<>("people", Arrays.asList("家人闲坐", "好友相聚", "独处清欢", "邻里温情")),
            new AbstractMap.SimpleEntry<>("mood", Arrays.asList("小确幸", "满足感", "温柔力量", "内心丰盈"))
    ).collect(Collectors.toMap(
            Map.Entry::getKey,
            Map.Entry::getValue,
            // 解决key重复冲突（旧值覆盖/新值覆盖）
            (oldValue, newValue) -> newValue
    ));

    /// 动漫随笔
    private static final Map<String, List<String>> ANIME = Stream.of(
            new AbstractMap.SimpleEntry<>("theme", Arrays.asList("热血成长", "青春日常", "奇幻冒险", "温柔治愈")),
            new AbstractMap.SimpleEntry<>("spirit", Arrays.asList("勇气信念", "伙伴羁绊", "初心坚守", "梦想追逐")),
            new AbstractMap.SimpleEntry<>("scene", Arrays.asList("经典名场面", "细腻画面", "动人配乐", "高光时刻")),
            new AbstractMap.SimpleEntry<>("role", Arrays.asList("主角成长线", "配角温柔弧光", "群像鲜活立体", "反派复杂人性")),
            new AbstractMap.SimpleEntry<>("feeling", Arrays.asList("深刻共鸣", "热血沸腾", "治愈安心", "热泪盈眶"))
    ).collect(Collectors.toMap(
            Map.Entry::getKey,
            Map.Entry::getValue,
            // 解决key重复冲突（旧值覆盖/新值覆盖）
            (oldValue, newValue) -> newValue
    ));

    /// 自然风景
    private static final Map<String, List<String>> NATURE = Stream.of(
            new AbstractMap.SimpleEntry<>("sky", Arrays.asList("云海日出", "落日余晖", "星河璀璨", "霞光漫天")),
            new AbstractMap.SimpleEntry<>("land", Arrays.asList("青山叠翠", "草原辽阔", "溪流潺潺", "海浪轻拍")),
            new AbstractMap.SimpleEntry<>("sound", Arrays.asList("林间鸟鸣", "清风入耳", "溪水叮咚", "松涛阵阵")),
            new AbstractMap.SimpleEntry<>("atm", Arrays.asList("清新湿润", "宁和悠远", "壮阔苍茫", "温柔静谧")),
            new AbstractMap.SimpleEntry<>("time", Arrays.asList("清晨薄雾", "正午光影", "黄昏暮色", "静夜微凉"))
    ).collect(Collectors.toMap(
            Map.Entry::getKey,
            Map.Entry::getValue,
            // 解决key重复冲突（旧值覆盖/新值覆盖）
            (oldValue, newValue) -> newValue
    ));

    /// 动态句式库（真正无重复感）
    private static final List<String> PATTERNS = Arrays.asList(
            "%s 依托 %s，有效提升 %s；%s 强化 %s，让整体更加稳健可靠。",
            "%s 以 %s 为核心，通过 %s 优化体验，%s 进一步增强质感，兼顾 %s。",
            "%s 围绕 %s 展开设计，结合 %s 提升效率，%s 保障稳定，最终实现 %s。",
            "%s 注重 %s 的表达，借助 %s 营造氛围，搭配 %s 增强感染力，传递 %s。",
            "%s 从 %s 出发，利用 %s 实现突破，%s 提供支撑，%s 带来更完整体验。",
            "%s 融合 %s 与 %s，通过 %s 完善细节，依托 %s 达成整体目标。"
    );

    /// 终极生成入口
    public static String generateDoc() {
        switch (RANDOM.nextInt(5)) {
            case 0:
                return build(TECH);
            case 1:
                return build(GEO);
            case 2:
                return build(LIFE);
            case 3:
                return build(ANIME);
            default:
                return build(NATURE);
        }
    }

    /// 智能构建逻辑
    private static String build(Map<String, List<String>> domain) {
        String prefix = LOGIC_PREFIX.get(RANDOM.nextInt(LOGIC_PREFIX.size()));
        String pattern = PATTERNS.get(RANDOM.nextInt(PATTERNS.size()));

        // 随机抽取维度词汇
        String a = random(domain, 0);
        String b = random(domain, 1);
        String c = random(domain, 2);
        String d = random(domain, 3);
        String e = random(domain, 4);

        // 随机插入连接词，进一步提升自然度
        if (RANDOM.nextBoolean()) {
            String conj = CONJUNCTION.get(RANDOM.nextInt(CONJUNCTION.size()));
            pattern = conj + "，" + pattern;
        }

        String content = prefix + String.format(pattern, a, b, c, d, e);
        return content.replaceAll("\\s", "");
    }

    /// 安全随机取值
    private static String random(Map<String, List<String>> map, int index) {
        List<List<String>> groups = new ArrayList<>(map.values());
        List<String> list = groups.get(Math.min(index, groups.size() - 1));
        return list.get(RANDOM.nextInt(list.size()));
    }
}