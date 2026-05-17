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

package cn.aberic.tangduo.index.engine.entity;

import org.junit.jupiter.api.Test;

import java.rmi.UnexpectedException;
import java.util.Arrays;
import java.util.List;

public class ConditionTests {

    @Test
    void simplifyConditions() throws UnexpectedException {
        // 测试用例1：age的GT(10) + GE(10) → 保留GT(10)
        Condition c1 = new Condition("age", Condition.Compare.GT, 10);
        Condition c2 = new Condition("age", Condition.Compare.GE, 10);
        // 测试用例2：age的GT(10) + GE(12) → 保留GE(12)
        Condition c3 = new Condition("age", Condition.Compare.GE, 12);
        // 测试用例3：score的LT(20) + LE(18) → 保留LE(18)
        Condition c4 = new Condition("score", Condition.Compare.LT, 20);
        Condition c5 = new Condition("score", Condition.Compare.LE, 18);
        // 测试用例4：name的EQ + NE → 均保留
        Condition c6 = new Condition("name", Condition.Compare.EQ, "张三");
        Condition c7 = new Condition("name", Condition.Compare.NE, "李四");

        List<Condition> conditions = Arrays.asList(c1, c2, c3, c4, c5, c6, c7);
        List<Condition> simplified = Condition.Utils.simplifyConditions(conditions);

        // 输出结果验证
        System.out.println("精简后的条件列表：");
        for (Condition condition : simplified) {
            System.out.printf("param: %s, compare: %s, value: %s%n",
                    condition.getParam(), condition.getCompare().name(), condition.getCompareValue());
        }
        // 预期输出：
        // param: age, compare: GE, value: 12
        // param: score, compare: LE, value: 18
        // param: name, compare: EQ, value: 张三
        // param: name, compare: NE, value: 李四
    }

    @Test
    void simplifyConditions2() throws UnexpectedException {
        // 测试用例1：同一param下多个EQ值不同 → 抛异常
        Condition eq1 = new Condition("name", Condition.Compare.EQ, "张三");
        Condition eq2 = new Condition("name", Condition.Compare.EQ, "李四");
        // 测试用例2：同一param下多个EQ值相同 → 保留一个
        Condition eq3 = new Condition("age", Condition.Compare.EQ, 18);
        Condition eq4 = new Condition("age", Condition.Compare.EQ, 18);
        // 测试用例3：age的GT(10) + GE(12) → 保留GE(12)
        Condition c1 = new Condition("age", Condition.Compare.GT, 10);
        Condition c2 = new Condition("age", Condition.Compare.GE, 12);

        // 测试用例1（异常场景）
        try {
            List<Condition> errorConditions = Arrays.asList(eq1, eq2);
            Condition.Utils.simplifyConditions(errorConditions);
        } catch (UnexpectedException e) {
            System.out.println("预期异常：" + e.getMessage()); // 输出：同一param下存在多个EQ条件且compareValue不同...
        }

        // 测试用例2+3（正常场景）
        List<Condition> normalConditions = Arrays.asList(eq3, eq4, c1, c2);
        List<Condition> simplified = Condition.Utils.simplifyConditions(normalConditions);
        System.out.println("\n精简后的条件列表：");
        for (Condition condition : simplified) {
            System.out.printf("param: %s, compare: %s, value: %s%n",
                    condition.getParam(), condition.getCompare().name(), condition.getCompareValue());
        }
        // 预期输出：
        // param: age, compare: GE, value: 12
        // param: age, compare: EQ, value: 18
    }

    @Test
    void simplifyConditions3() throws UnexpectedException {
        // 测试用例1：EQ在区间内 → 仅保留EQ
        Condition eq1 = new Condition("age", Condition.Compare.EQ, 20);
        Condition ge1 = new Condition("age", Condition.Compare.GE, 18);
        Condition le1 = new Condition("age", Condition.Compare.LE, 30);
        List<Condition> case1 = Arrays.asList(eq1, ge1, le1);
        System.out.println("测试用例1（EQ在区间内）：");
        Condition.Utils.simplifyConditions(case1).forEach(c ->
                System.out.printf("param=%s, compare=%s, value=%s%n", c.getParam(), c.getCompare(), c.getCompareValue())
        ); // 仅输出EQ(20)

        // 测试用例2：EQ超出区间 → 抛异常
        Condition eq2 = new Condition("age", Condition.Compare.EQ, 17);
        List<Condition> case2 = Arrays.asList(eq2, ge1, le1);
        try {
            Condition.Utils.simplifyConditions(case2);
        } catch (UnexpectedException e) {
            System.out.println("\n测试用例2（EQ超出区间）：" + e.getMessage());
            // 输出：param=age的EQ值=17 小于区间下限[GE:18]，条件矛盾
        }

        // 测试用例3：多个EQ值不同 → 抛异常
        Condition eq3 = new Condition("name", Condition.Compare.EQ, "张三");
        Condition eq4 = new Condition("name", Condition.Compare.EQ, "李四");
        List<Condition> case3 = Arrays.asList(eq3, eq4);
        try {
            Condition.Utils.simplifyConditions(case3);
        } catch (UnexpectedException e) {
            System.out.println("\n测试用例3（多个EQ值不同）：" + e.getMessage());
            // 输出：param=name存在多个EQ条件且compareValue不同，values=[张三, 李四]
        }
    }

    @Test
    void simplifyConditions4() throws UnexpectedException {
        // 测试用例1：NE在区间内 → 保留NE
        Condition ne1 = new Condition("age", Condition.Compare.NE, 20);
        Condition ge1 = new Condition("age", Condition.Compare.GE, 18);
        Condition le1 = new Condition("age", Condition.Compare.LE, 30);
        List<Condition> case1 = Arrays.asList(ne1, ge1, le1);
        System.out.println("测试用例1（NE在区间内）：");
        Condition.Utils.simplifyConditions(case1).forEach(c ->
                System.out.printf("param=%s, compare=%s, value=%s%n", c.getParam(), c.getCompare(), c.getCompareValue())
        ); // 输出：GE(18)、LE(30)、NE(20)

        // 测试用例2：NE不在区间内 → 移除NE
        Condition ne2 = new Condition("age", Condition.Compare.NE, 17);
        List<Condition> case2 = Arrays.asList(ne2, ge1, le1);
        System.out.println("\n测试用例2（NE不在区间内）：");
        Condition.Utils.simplifyConditions(case2).forEach(c ->
                System.out.printf("param=%s, compare=%s, value=%s%n", c.getParam(), c.getCompare(), c.getCompareValue())
        ); // 输出：GE(18)、LE(30)（NE被移除）

        // 测试用例3：EQ合法 + NE + 区间 → 仅保留EQ
        Condition eq1 = new Condition("age", Condition.Compare.EQ, 20);
        List<Condition> case3 = Arrays.asList(eq1, ne1, ge1, le1);
        System.out.println("\n测试用例3（EQ合法+NE+区间）：");
        Condition.Utils.simplifyConditions(case3).forEach(c ->
                System.out.printf("param=%s, compare=%s, value=%s%n", c.getParam(), c.getCompare(), c.getCompareValue())
        ); // 仅输出：EQ(20)

        // 测试用例4：NE非数值类型 → 保留NE
        Condition ne3 = new Condition("name", Condition.Compare.NE, "张三");
        Condition ge2 = new Condition("name", Condition.Compare.GE, "李四"); // 非数值区间，无法校验
        List<Condition> case4 = Arrays.asList(ne3, ge2);
        System.out.println("\n测试用例4（NE非数值类型）：");
        Condition.Utils.simplifyConditions(case4).forEach(c ->
                System.out.printf("param=%s, compare=%s, value=%s%n", c.getParam(), c.getCompare(), c.getCompareValue())
        ); // 输出：GE(李四)、NE(张三)
    }

}
