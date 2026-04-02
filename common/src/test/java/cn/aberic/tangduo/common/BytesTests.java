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

package cn.aberic.tangduo.common;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.math.BigInteger;
import java.util.Arrays;

public class BytesTests {

    @Test
    void wr() throws IOException {
        long a = 1;
        long b = 999999999;
        long c = 99999999999999999L;
        byte[] bs = Bytes.join(Bytes.fromLong(a), Bytes.fromLong(b), Bytes.fromLong(c));
        System.out.println(Bytes.toLong(Bytes.read(bs, 0, 8)));
        System.out.println(Bytes.toLong(Bytes.read(bs, 8, 8)));
        System.out.println(Bytes.toLong(Bytes.read(bs, 16, 8)));
    }

    @Test
    void longMax() {
        System.out.println((long) Math.pow(2, 32));
        System.out.println((long) Math.pow(2, 64));

        BigInteger two = BigInteger.valueOf(2);
        BigInteger result = two.pow(64);
        System.out.println(result);
    }

    @Test
    void int2bytes() {
        int a = 123;
        byte[] bytes = Bytes.fromInt(a);
        System.out.println(Arrays.toString(bytes));
        System.out.println(Bytes.toInt(bytes));
    }

    @Test
    void string() {
        String a = "hello world!你好，世界！";
        byte[] bytes = Bytes.fromString(a);
        System.out.println(Arrays.toString(bytes));
        System.out.println(Bytes.toString(bytes));
    }

    @Test
    void newNull() {
        System.out.println(Arrays.toString(new byte[10]));
    }

    @Test
    void bool2bytes() {
        boolean a = false;
        byte b = Bytes.fromBool(a);
        System.out.println(Bytes.toBool(b));
        a = true;
        b = Bytes.fromBool(a);
        System.out.println(Bytes.toBool(b));
    }

    @Test
    void double2bytes() {
        double d = 2.33;
        byte[] bytes = Bytes.fromDouble(d);
        System.out.println(Bytes.toDouble(bytes));

        d = 2.340;
        bytes = Bytes.fromDouble(d);
        System.out.println(Bytes.toDouble(bytes));

        d = -50;
        bytes = Bytes.fromDouble(d);
        System.out.println(Bytes.toDouble(bytes));
    }

}
