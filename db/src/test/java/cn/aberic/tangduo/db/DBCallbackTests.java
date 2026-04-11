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

package cn.aberic.tangduo.db;

import cn.aberic.tangduo.common.file.Filer;
import cn.aberic.tangduo.db.entity.DocSearchResponseVO;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import javax.management.InstanceAlreadyExistsException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Stream;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Slf4j
public class DBCallbackTests {
    final static String text1 = """
            读书能够丰富我们的知识储备，让我们在面对问题时更加从容。
            通过阅读，我们可以接触不同的思想，拓宽眼界，提升认知水平。
            长期坚持学习，能让内心更加沉稳，也能在生活和工作中获得更多机会。
            不断学习，是提升自我最稳妥的方式，也能让人生拥有更多可能。
            """;
    final static String text2 = """
            学习是伴随一生的修行，坚持读书可以不断提升个人能力与素养。
            书本中蕴含前人的经验与智慧，能帮助我们少走弯路，更好地应对生活挑战。
            每天抽出时间阅读，不仅能充实精神世界，还能培养思考能力，让我们在成长路上持续进步，变得更加优秀。
            """;
    final static String text3 = """
            阅读是成本最低的自我提升方式，通过读书我们可以获取大量知识与见解。
            在学习过程中，我们的思维会更加清晰，表达更加流畅，处理问题也更有条理。
            坚持学习能让人保持积极向上的状态，不断突破自我，让生活与工作都朝着更好的方向发展。
            """;
    final static String text4 = """
            坚持读书学习，能够不断充实自己的精神世界，提升内在修养。
            书本带给我们的不仅是知识，还有开阔的视野和理性的思考方式。
            在快节奏的生活中，静下心来阅读，能让人远离浮躁，保持平和心态，同时也能持续提升能力，为未来积累更多底气。
            """;
    final static String text5 = """
            学习可以让人不断进步，而读书是最直接有效的学习方式。
            通过阅读各类书籍，我们能学到专业知识、生活常识与处世智慧。
            长期积累下来，个人气质与能力都会明显提升。
            保持学习的习惯，能让我们始终保持竞争力，在人生道路上稳步前行。
            """;
    final static String text6 = """
            读书不仅能增长知识，还能培养独立思考与判断能力，让人更加理性成熟。
            在学习过程中，我们可以吸收优秀思想，完善自身价值观，提升综合素养。
            每天坚持阅读一点，日积月累就会有巨大收获，让自己不断成长，变得更加强大。
            """;
    final static String text7 = """
            持续学习与阅读，是提升自我、改变人生的重要途径。
            书本中包含丰富的知识与经验，能帮助我们解决疑惑，开阔思路。
            坚持读书可以让内心更加强大，面对困难更有勇气，同时也能提升工作能力与生活品质，让未来拥有更多选择。
            """;
    final static String text8 = """
            阅读能够滋养心灵，学习能够成就自我。
            通过读书，我们可以接触不同领域的内容，丰富知识结构，提升逻辑思维。
            在不断学习中，我们会变得更加沉稳、自信，处理事务也更加高效。
            保持学习习惯，能让人生不断向上，收获更多成长与惊喜。
            """;
    final static String text9 = """
            读书是一场无声的成长，坚持学习可以让我们不断更新认知，提升综合能力。
            书本带给我们知识、智慧与力量，帮助我们更好地理解世界、面对生活。
            长期阅读能让人气质温润，思想深刻，在学习中不断完善自己，走向更好的自己。
            """;
    final static String text10 = """
            学习永无止境，读书则是最好的学习方式。
            通过阅读，我们可以积累知识、拓宽视野、提升修养，让自己不断进步。
            在阅读中沉淀内心，在学习中提升能力，能让我们在生活中更从容，在工作中更出色，实现持续的自我成长。
            """;
    final static String text11 = """
            坚持读书能够丰富知识储备，提升个人修养与思维能力。
            学习让我们不断接触新事物，理解不同观点，变得更加包容与理性。
            每天阅读一点点，长期坚持就能明显提升自己，让精神世界更加充实，人生道路也更加开阔。
            """;
    final static String text12 = """
            阅读可以带来知识与力量，学习能够帮助我们不断突破局限。
            在书本中，我们能学到处世方法、专业技能与人生智慧。
            坚持学习能让人保持进步的状态，提升竞争力，同时也能让内心更加丰盈，在平凡生活中收获不凡的成长。
            """;
    final static String text13 = """
            读书学习能够提升个人能力，丰富精神世界，让人终身受益。
            通过阅读，我们可以汲取前人智慧，提高思考与表达能力，更好地适应社会。
            保持学习习惯，能让人不断成长，不断进步，在面对挑战时更有底气，生活也更加充实。
            """;
    final static String text14 = """
            学习是一种长期投资，而读书是最有效的投资方式。
            通过阅读，我们可以积累知识、提升素养、锻炼思维，让自己越来越优秀。
            坚持学习能让人保持积极心态，不断提升自我价值，在生活和工作中都能获得更多进步与收获。
            """;
    final static String text15 = """
            阅读能够开阔视野，增长见识，学习能够提升能力，成就更好的自己。
            在书本中，我们可以获得知识、启发与力量，让内心更加充实强大。
            坚持每天读书学习，能让人持续成长，不断完善自我，让人生拥有更多可能与希望。
            """;

    final static String noiseText1 = """
            清晨的早餐摊总是热气腾腾，豆浆醇厚，油条酥脆。
            摊主熟练地招呼客人，香气在空气中弥漫。
            简单的食物却能带来满满的幸福感，开启一天的好心情。市井烟火最是动人，平凡滋味里藏着生活最真实的温暖与慰藉。
            """;
    final static String noiseText2 = """
            小狗总喜欢跟在主人身后，摇着尾巴十分乖巧。
            闲暇时它会趴在脚边安静陪伴，偶尔抬头用湿漉漉的眼睛望着人。
            简单的陪伴格外治愈，生活因这些可爱的小生命多了许多欢乐与温柔，让人内心变得柔软。
            """;
    final static String noiseText3 = """
            天刚亮广场上就热闹起来，人们慢跑、打拳、跳操。
            微风轻拂，空气清新，每个人都充满活力。
            坚持晨练能增强体质，让人一整天精神饱满。
            在自然中舒展身体，是迎接新一天最好的方式。
            """;
    final static String noiseText4 = """
            阳台上种满绿植与鲜花，浇水修剪成日常乐趣。
            叶片舒展，花朵次第开放，生机盎然。
            照料花草能让人静下心来，感受生命成长的美好。
            小小的一方天地，成为繁忙生活里宁静治愈的角落。
            """;
    final static String noiseText5 = """
            周末骑行在郊外小路，沿途风景不断变换。
            绿树成荫，鸟鸣阵阵，风吹过格外舒畅。
            自由穿梭在自然间，烦恼仿佛都被吹散。
            既能锻炼身体，又能放松心情，是非常舒适惬意的休闲方式。
            """;
    final static String noiseText6 = """
            拿起相机记录身边美好瞬间，日出晚霞、街头烟火都值得留存。
            每一张照片都是时光印记，定格平凡生活中的小确幸。
            慢慢发现美、记录美，内心也会变得细腻丰盈，生活更有仪式感。
            """;
    final static String noiseText7 = """
            河边静坐垂钓，水波轻漾，四周安静祥和。
            不必执着收获，享受这份悠然自在即可。
            远离喧嚣，放空思绪，在慢时光里平复浮躁内心。
            简单的爱好，带来难得的平静与放松。
            """;
    final static String noiseText8 = """
            闲暇时做做手工，编织、折纸、陶艺都很有趣。
            专注手中物件，时间静静流淌，压力慢慢消散。
            完成作品时满是成就感，动手的过程治愈又充实，给生活增添许多小乐趣。
            """;
    final static String noiseText9 = """
            夜晚仰望星空，繁星点点闪烁，银河清晰可见。
            宇宙浩瀚辽阔，让人忘却琐碎烦恼。
            静静感受夜空的宁静与神秘，内心变得平和开阔。
            简单的仰望，就能带来治愈与力量。
            """;
    final static String noiseText10 = """
            周末在家研究菜谱，洗菜切菜精心烹饪。
            食材在锅中翻滚，香气慢慢散开。
            做出美味菜肴与家人分享，满是幸福感。
            用心对待每一餐，平凡日子也能过得有滋有味。
            """;

    final static String search1 = "读书学习有什么好处？";
    final static String search2 = "坚持阅读能带来什么？";
    final static String search3 = "学习对人有哪些帮助？";

    final static String rootpath = "tmp/callback";

    @Test
    @Order(1)
    void init() {
        try (Stream<Path> stream = Files.walk(Paths.get(rootpath))) {
            stream.forEach(f -> {
                try {
                    Files.delete(f);
                } catch (IOException ignore) {}
            });
        } catch (IOException e) {
            log.warn(e.getMessage());
        }
    }

    @Test
    @Order(2)
    void putText() throws IOException, NoSuchFieldException {
        String dbName = "putTextDB";
        Filer.deleteDirectory(Path.of(rootpath, dbName).toAbsolutePath().toString());
        DB db = DB.getInstance(rootpath, 10737418240L);
        db.removeDB(dbName);
        try {
            db.createDB(dbName);
        } catch (InstanceAlreadyExistsException e) {
            System.out.println(e.getMessage());
        }
        // ik 分词 564个
        // hanlp 分词 359个
        db.put(dbName, text1);
        db.put(dbName, text2);
        db.put(dbName, text3);
        db.put(dbName, text4);
        db.put(dbName, text5);
        db.put(dbName, text6);
        db.put(dbName, text7);
        db.put(dbName, text8);
        db.put(dbName, text9);
        db.put(dbName, text10);
        db.put(dbName, text11);
        db.put(dbName, text12);
        db.put(dbName, text13);
        db.put(dbName, text14);
        db.put(dbName, text15);
        db.put(dbName, noiseText1);
        db.put(dbName, noiseText2);
        db.put(dbName, noiseText3);
        db.put(dbName, noiseText4);
        db.put(dbName, noiseText5);
        db.put(dbName, noiseText6);
        db.put(dbName, noiseText7);
        db.put(dbName, noiseText8);
        db.put(dbName, noiseText9);
        db.put(dbName, noiseText10);
    }

    @Test
    @Order(3)
    void putTextAgain() throws IOException, NoSuchFieldException {
        String dbName = "putTextDB";
        DB db = DB.getInstance(rootpath, 10737418240L);
        db.put(dbName, "text1text1text1text1text1text1text1text1text1text1text1text1text1text1text1text1text1text1text1text1text1text1text1text1text1");
    }

    @Test
    @Order(3)
    void searchText() throws IOException, NoSuchFieldException {
        String dbName = "putTextDB";
        DB db = DB.getInstance(rootpath, 10737418240L);
        List<DocSearchResponseVO> docItems = db.search(dbName, search1, 100);
        System.out.println("size = " + docItems.size());
        docItems.forEach(System.out::println);
    }

    @Test
    @Order(3)
    void searchText1() throws IOException, NoSuchFieldException {
        String dbName = "putTextDB";
        DB db = DB.getInstance(rootpath, 10737418240L);
        List<DocSearchResponseVO> docItems = db.search(dbName, search2);
        System.out.println("size = " + docItems.size());
        docItems.forEach(System.out::println);
    }

    @Test
    @Order(3)
    void searchText2() throws IOException, NoSuchFieldException {
        String dbName = "putTextDB";
        DB db = DB.getInstance(rootpath, 10737418240L);
        List<DocSearchResponseVO> docItems = db.search(dbName, search3);
        System.out.println("size = " + docItems.size());
        docItems.forEach(System.out::println);
    }

}
