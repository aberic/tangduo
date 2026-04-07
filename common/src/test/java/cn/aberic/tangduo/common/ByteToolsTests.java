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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.zip.GZIPOutputStream;

public class ByteToolsTests {

    @Test
    void wr() throws IOException {
        long a = 1;
        long b = 999999999;
        long c = 99999999999999999L;
        byte[] bs = ByteTools.join(ByteTools.fromLong(a), ByteTools.fromLong(b), ByteTools.fromLong(c));
        System.out.println(ByteTools.toLong(ByteTools.read(bs, 0, 8)));
        System.out.println(ByteTools.toLong(ByteTools.read(bs, 8, 8)));
        System.out.println(ByteTools.toLong(ByteTools.read(bs, 16, 8)));
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
        byte[] bytes = ByteTools.fromInt(a);
        System.out.println(Arrays.toString(bytes));
        System.out.println(ByteTools.toInt(bytes));
    }

    @Test
    void string() {
        String a = "hello world!你好，世界！";
        byte[] bytes = ByteTools.fromString(a);
        System.out.println(Arrays.toString(bytes));
        System.out.println(ByteTools.toString(bytes));
    }

    @Test
    void newNull() {
        System.out.println(Arrays.toString(new byte[10]));
    }

    @Test
    void bool2bytes() {
        boolean a = false;
        byte b = ByteTools.fromBool(a);
        System.out.println(ByteTools.toBool(b));
        a = true;
        b = ByteTools.fromBool(a);
        System.out.println(ByteTools.toBool(b));
    }

    @Test
    void double2bytes() {
        double d = 2.33;
        byte[] bytes = ByteTools.fromDouble(d);
        System.out.println(ByteTools.toDouble(bytes));

        d = 2.340;
        bytes = ByteTools.fromDouble(d);
        System.out.println(ByteTools.toDouble(bytes));

        d = -50;
        bytes = ByteTools.fromDouble(d);
        System.out.println(ByteTools.toDouble(bytes));
    }

    @Test
    void string2bytes8gzip() throws Exception {
        String text = """
                package cn.aberic.log.entity;
                import java.util.Date;
                public class LogEntity {
                    private String  appName;
                    private String  traceId;
                    private String  level;
                    private String  message;
                    private String  threadName;
                    private String  loggerName;
                    private long    timestamp;
                    private Date    createTime;
                    private String  serverIp;
                    private String  exception;
                
                    // getter/setter 省略，自行生成
                    public String getAppName() {return appName;}
                    public void setAppName(String appName) {this.appName = appName;}
                    public String getTraceId() {return traceId;}
                    public void setTraceId(String traceId) {this.traceId = traceId;}
                    public String getLevel() {return level;}
                    public void setLevel(String level) {this.level = level;}
                    public String getMessage() {return message;}
                    public void setMessage(String message) {this.message = message;}
                    public String getThreadName() {return threadName;}
                    public void setThreadName(String threadName) {this.threadName = threadName;}
                    public String getLoggerName() {return loggerName;}
                    public void setLoggerName(String loggerName) {this.loggerName = loggerName;}
                    public long getTimestamp() {return timestamp;}
                    public void setTimestamp(long timestamp) {this.timestamp = timestamp;}
                    public Date getCreateTime() {return createTime;}
                    public void setCreateTime(Date createTime) {this.createTime = createTime;}
                    public String getServerIp() {return serverIp;}
                    public void setServerIp(String serverIp) {this.serverIp = serverIp;}
                    public String getException() {return exception;}
                    public void setException(String exception) {this.exception = exception;}
                }
                """;
        byte[] bytes = ByteTools.fromString(text);
        System.out.println(bytes.length);
        byte[] gzipBytes = GzipTools.compress(bytes);
        System.out.println(gzipBytes.length);
        byte[] bytes1 = GzipTools.decompress(gzipBytes);
        System.out.println(bytes1.length);
        System.out.println(ByteTools.toString(bytes1));
    }

    public static byte[] gzip(byte[] data) throws Exception {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             GZIPOutputStream gzip = new GZIPOutputStream(baos)) {
            gzip.write(data);
            gzip.finish();
            return baos.toByteArray();
        }
    }

}
