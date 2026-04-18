# TangDuo

## 项目简介
**tangduo（汤朵）** 提供轻量、易用的技术解决方案，适用于中小型项目快速开发与集成。

## 核心特性
1. **轻量简洁**：代码结构清晰，无冗余依赖，易于理解和二次开发
2. **持续迭代**：基于 `develop` 分支持续更新，不断优化功能与修复问题
3. **开源免费**：遵循开源协议，可自由使用、修改与分发
4. **易用性强**：提供简洁的使用方式，降低开发者接入成本

## 适用场景
- 中小型项目快速搭建
- 技术学习与源码研究
- 个人开发者工具集成
- 轻量级业务系统开发

## 核心模块结构
仓库采用多模块拆分的方式组织代码，各模块职责边界清晰，具体模块如下：

| 模块名    | 核心定位                                                                 |
|---------|--------------------------------------------------------------------------|
| [`sdk4j21/`](/sdk4j21) | 适配 Java 21 版本的 SDK 模块，封装通用的工具类、基础接口或适配 Java 21 新特性的核心能力 |
| [`sdk4j8/`](/sdk4j8)    | 适配 Java 8 版本的 SDK 模块，保障项目在低版本 Java 环境下的兼容性，提供与 `sdk4j21` 功能对齐的基础能力 |
| [`db/`](/db)        | 数据库操作核心模块，负责封装数据持久化相关逻辑，是整个项目的数据存储层 |
| [`index/`](/index)     | 索引相关模块，推测用于处理数据索引构建、检索优化等能力 |
| [`search/`](/search)    | 检索功能模块，包含 `http/` 子目录，负责对外提供检索相关的 HTTP 接口，或处理检索请求的网络交互、参数解析等 |
| [`common/`](/common)    | 通用公共模块，封装全项目复用的工具类、常量定义、异常处理、通用配置等基础能力，为其他模块提供底层支撑 |

## 使用说明
1. 克隆项目到本地
```bash
git clone https://gitee.com/aberic/tangduo.git
```
2. 切换至对应版本分支，如开发分支
```bash
git checkout develop
```
3. 按照项目内具体模块说明完成配置与启动

## 核心能力
✅ **嵌入式部署**：无外部依赖，直接集成到 Java 项目，开箱即用  
✅ **多类型存储**：支持结构化对象、JSON 文档存储，兼顾灵活与规范  
✅ **高性能检索**：集成分词引擎 + BM25 相关性排序，支持全文检索/条件检索  
✅ **异步写入优化**：基于异步 Channel 模型，批量写入性能优异  
✅ **事务一致性**：支持索引操作原子性，保证数据存储与索引更新一致性  
✅ **轻量化运维**：文件级存储，备份/迁移仅需拷贝目录，无复杂配置

### 整体架构
TangDuo 采用模块化设计，核心分为三层：
```
┌─────────────────┐
│  search 模块    │ 对外暴露 RESTful API，封装 DB 能力（类 Elasticsearch 接口）
└────────┬────────┘
         │
┌────────▼────────┐
│    db 模块      │ 核心业务层，提供对象/文档存储、富查询能力，整合索引能力
└────────┬────────┘
         │
┌────────▼────────┐
│   index 模块    │ 底层索引层，提供索引读写、异步刷盘、事务一致性保障
└─────────────────┘
```

#### 模块职责
| 模块    | 核心能力                                                                 | 对外形态                          |
|---------|--------------------------------------------------------------------------|-------------------------------|
| `index` | 索引读写、异步刷盘、事务封装、文件 IO 优化                               | 底层 API（供 db 模块调用）             |
| `db`    | 对象/文档存储、富查询（条件/分词）、批量操作、数据一致性保障             | Java 核心 API（单例入口）             |
| `search`| 基于 Spring Boot 封装 RESTful API，暴露数据库/索引/数据全量操作能力      | [HTTP 接口（开箱即用）](/search/http) |

#### 核心接口调用示例
| 功能         | 请求方式 | 接口路径               | 示例请求体                                                                 |
|--------------|----------|------------------------|----------------------------------------------------------------------------|
| 创建数据库   | PUT      | /db/my_test_db         | 无（路径参数）                                                             |
| 创建索引     | PUT      | /index                 | `{"database":"my_test_db","index":"user_index","version":1,"name":"user_index"}` |
| 插入数据     | PUT      | /data                  | `{"database":"my_test_db","index":"user_index","seg":true,"value":"{\"name\":\"张三\",\"age\":25}"}` |
| 全文检索     | GET      | /data/search           | `{"database":"my_test_db","index":"user_index","query":"张三","limit":10}`  |
| 条件检索     | GET      | /data/select           | `{"database":"my_test_db","index":"user_index","conditions":[{"field":"age","op":">","value":20}]}` |
| 删除数据     | DELETE   | /data                  | `{"database":"my_test_db","index":"user_index","key":"user_001"}`          |

### 核心特性详解
#### 1. 多类型存储
| 存储类型   | 适用场景                | 核心能力                                                                 |
|------------|-------------------------|--------------------------------------------------------------------------|
| 对象存储   | 结构化 Java 对象        | 自定义序列化、版本管理、批量操作、原子性保障                             |
| 文档存储   | JSON 非结构化/半结构化   | 自动字段索引、分词检索、（规划）大文档分片                               |

#### 2. 检索能力
| 检索类型   | 特性                                                                 |
|------------|----------------------------------------------------------------------|
| 全文检索   | HanLP/IK 分词、BM25 相关性排序、支持批量检索                         |
| 条件检索   | 等值/范围/模糊匹配、多条件 AND/OR/NOT、排序、（规划）分页             |
| 精确查询   | 按主键快速定位，基于文件索引直接读取，性能无损耗                     |

#### 3. 性能优化
- **异步写入**：Index 模块基于 Channel 异步写线程模型，避免磁盘 IO 阻塞业务线程
- **批量操作**：支持批量插入/删除，减少文件 IO 次数
- **索引优化**：自动路由查询条件到对应索引，避免全量扫描
- **文件分片**：单个数据文件达到阈值自动分片，平衡读写效率

#### 4. 数据安全
- **事务原子性**：保证“数据存储 + 索引更新”原子性，避免数据不一致
- **写前校验**：校验数据合法性（字段类型/长度/约束），杜绝脏数据
- **崩溃恢复**：基于日志实现异常宕机后数据恢复
- **并发安全**：ConcurrentHashMap + ReentrantLock 保证多线程安全

### 适用场景
- 嵌入式检索服务（无需部署外部中间件）
- 轻量级全文检索（日志检索、文档检索、内容检索）
- 小体量结构化/非结构化数据存储 + 检索
- 快速搭建本地测试/演示环境的检索能力
- 对部署复杂度敏感、追求轻量化的业务场景

### 注意事项
1. **单例唯一性**：DB 类为单例设计，初始化时保证 rootPath 全局唯一，避免资源冲突
2. **文件权限**：确保数据库根目录有读写权限，否则会触发 IOException
3. **分词引擎**：切换 HanLP/IK 需确保对应依赖包已引入
4. **异步写入**：批量写入后立即查询可能因异步刷盘未完成导致数据未落地，可调用 `force()` 等待刷盘
5. **数据备份**：直接备份数据库根目录即可实现全量数据备份
6. **分片大小**：dataFileMaxSize 建议设置为 100MB~1GB，过小会导致文件过多，过大影响读写效率

### 扩展能力
- **自定义分词规则**：扩展 `IkTokenizerTools`/`HanlpTools` 实现停用词屏蔽、自定义词典
- **自定义条件过滤**：扩展 `doFilter` 方法新增过滤规则（如日期范围、模糊匹配）
- **索引分片（规划）**：支持分布式分片索引，支撑海量数据检索
- **索引缓存（规划）**：增加内存缓存层，提升热点数据读取性能
- **监控统计（规划）**：提供存储容量、读写 QPS、查询耗时等监控指标

## 作为嵌入式文件型检索数据库使用
TangDuo 是一款轻量级**嵌入式文件型检索数据库**，基于本地文件系统实现结构化/非结构化数据的持久化存储，结合分词引擎（HanLP/IK）提供高性能全文检索能力，兼具易用性、高性能与轻量化特性。
### 快速开始
#### 1. 环境依赖
- JDK 11+
- 依赖：HanLP/IK 分词引擎、Jackson、Lombok、Apache Commons Lang3
- 框架（search 模块）：Spring Boot 2.x+

#### 2. 核心依赖引入（Maven）
```xml
<!-- 核心 DB 模块 -->
<dependency>
    <groupId>cn.aberic</groupId>
    <artifactId>tangduo-db</artifactId>
    <version>1.0.0</version>
</dependency>
<!-- 索引模块（DB 已依赖，无需重复引入） -->
<dependency>
    <groupId>cn.aberic</groupId>
    <artifactId>tangduo-index</artifactId>
    <version>1.0.0</version>
</dependency>
<!-- HTTP 接口模块（可选） -->
<dependency>
    <groupId>cn.aberic</groupId>
    <artifactId>tangduo-search</artifactId>
    <version>1.0.0</version>
</dependency>
```

#### 3. 核心 API 使用（DB 模块）
#### 初始化数据库（单例）
```java
import cn.aberic.tangduo.db.DB;
import java.io.IOException;

public class TangDuoDemo {
    public static void main(String[] args) throws IOException, NoSuchFieldException {
        // 初始化：根目录 + 单个数据文件最大size（100MB）
        DB db = DB.getInstance("/tmp/tangduo/db", 1024 * 1024 * 100);
    }
}
```

#### 基础操作流程
```java
// 1. 创建数据库（默认 HanLP 分词）
db.createDB("my_test_db");

// 2. 创建索引
import cn.aberic.tangduo.db.entity.Index;
import cn.aberic.tangduo.index.engine.IEngine;
Index.Info indexInfo = new Index.Info(1, "user_index", false, false, false);
db.createIndex("my_test_db", IEngine.UNITY, indexInfo);

// 3. 插入数据（JSON 格式，开启分词）
String jsonData = "{\"name\":\"张三\",\"age\":25,\"email\":\"zhangsan@test.com\"}";
db.put("my_test_db", "user_index", "user_001", true, jsonData);

// 4. 全文检索（关键词“张三”，返回前10条）
db.search("my_test_db", "张三", 10).forEach(result -> 
    System.out.println("检索结果：" + result.getValue())
);

// 5. 按主键查询
System.out.println("主键查询：" + db.getFirst("my_test_db", "user_index", "user_001").getDoc().getValue());

// 6. 删除数据
db.remove("my_test_db", "user_index", "user_001");
```

#### 4. HTTP 接口使用（search 模块）
#### 配置 [application.yml](/search/src/main/resources/application.yml)
```yaml
custom:
  db:
    DB_ROOT_PATH: ./tangduo-data  # 数据存储根目录
    DB_DATA_FILE_MAX_SIZE: 104857600  # 单个文件最大100MB
    DB_SEARCH_MAX_COUNT: 10000  # 单次检索最大条数
  index:
    INDEX_BATCH_MAX_SIZE: 5000  # 批量插入最大条数
```

### 异常说明
| 异常类型                          | 触发场景                     | 解决方案                                   |
|-----------------------------------|------------------------------|--------------------------------------------|
| InstanceAlreadyExistsException    | 创建已存在的数据库/索引      | 操作前先调用 `dbExist()`/`indexExist()` 检查 |
| NoSuchFileException               | 操作不存在的数据库/索引      | 确保库/索引已创建                           |
| JsonParseException                | JSON 格式数据解析失败        | 检查数据格式合法性                         |
| IOException                       | 文件读写失败（权限/路径问题） | 检查目录权限、路径是否存在                 |
| InterruptedException              | 并发操作线程中断             | 捕获异常并处理线程中断逻辑                 |

### 整体使用流程（最佳实践）
```
1. 初始化 DB 实例（配置根目录、文件分片大小等）
2. 创建数据库（可选指定分词引擎）
3. 创建索引（配置分片、压缩、加密等属性）
4. 插入数据（单条/批量，按需开启分词）
5. 检索数据（全文检索/条件检索/精确查询）
6. 删除数据（按主键/按条件）
7. （可选）删除索引/数据库（危险操作，谨慎执行）
```

## 作为独立部署与接口服务
tangduo/search 模块支持独立部署模式，可作为独立的服务进程对外提供标准化的搜索接口能力，以下是核心部署与接口服务相关信息补充：

### 一、独立部署特性
1. **部署形态**：基于 Spring Boot（可通过 pom.xml 确认依赖）实现可执行 Jar 包独立部署，无需依赖其他模块的进程，仅需对接底层存储/搜索引擎（如数据库、ES 等）；
2. **环境隔离**：通过 [`http-client.private.env.json`](search/http/http-client.private.env.json) 配置私有环境变量（如服务端口、数据库地址、搜索引擎连接信息），支持开发/测试/生产环境差异化部署；
3. **端口与服务暴露**：可通过配置文件指定独立端口，对外暴露 HTTP 接口，支持单机/集群部署模式。

### 二、对外接口能力（基于 HTTP 接口定义文件）
模块内的 `.http` 接口定义文件（[`example`](search/http/example) 目录下）对应对外提供的核心接口，覆盖不同搜索场景：

| 接口文件                | 接口能力说明                     | 适用场景                     |
|-------------------------|----------------------------------|------------------------------|
| [`data_no_seg.http`](search/http/example/data_no_seg.http)      | 非分词搜索接口                   | 精确匹配、无需分词的搜索场景 |
| [`data_no_seg_batch.http`](search/http/example/data_no_seg_batch.http)| 批量非分词搜索接口               | 批量精确查询、批量数据校验   |
| [`data_seg.http`](search/http/example/data_seg.http)         | 分词搜索接口                     | 全文检索、模糊匹配场景       |
| [`data_seg_batch_search.http`](search/http/example/data_seg_batch_search.http) | 批量分词搜索接口             | 批量全文检索、批量模糊查询   |
| [`data_seg_batch_select.http`](search/http/example/data_seg_batch_select.http) | 批量分词筛选接口             | 带条件的批量分词结果过滤     |
| [`db.http`](search/http/example/db.http)               | 基础数据库交互接口（兜底/调试）  | 直接数据库查询、数据兜底     |

### 三、部署与调用流程
1. **打包**：执行 [`build.sh`](build.sh) 在[build](build)目录下生成可执行 Jar 包；
2. **部署启动**：`java -jar search-${version}.jar --spring.profiles.active=prod`；
3. **接口调用**：基于 HTTP 协议调用，示例（分词搜索）：
   ```http
   POST /api/search/seg HTTP/1.1
   Host: {部署机器IP}:{端口}
   Content-Type: application/json

   {
     "keywords": ["关键词1", "关键词2"],
     "pageNum": 1,
     "pageSize": 10
   }
   ```

该模块通过独立部署，可灵活对接前端应用、其他微服务，提供标准化的搜索能力，且通过环境配置、批量接口设计，兼顾了部署灵活性与高并发场景适配。

## 使用 Docker 部署（推荐）

为简化部署流程、保证环境一致性，TangDuo 提供 Docker 镜像，支持一键部署，无需手动配置依赖、编译打包，适用于生产环境快速落地。

#### 一、核心镜像信息
- 镜像地址：`registry.cn-hangzhou.aliyuncs.com/aberic/tangduosearch:latest`
- 基础环境：基于 OpenJDK 21，已内置 search 模块可执行 Jar 包
- 默认端口：19219（业务接口）、19220（健康检查/监控接口）
- 数据目录：容器内 `/data`（用于存储数据库数据，已在 Dockerfile 中预先指定）

#### 二、一键部署命令
执行以下命令，快速启动 TangDuo 容器（支持开机自启、数据持久化、时区同步）：
```shell
docker run --name tangduosearch --restart=always \
-p 19219:19219 \
-p 19220:19220 \
-v /etc/localtime:/etc/localtime \
-v /etc/timezone:/etc/timezone \
-v /data/vol/tangduosearch:/data \
-itd registry.cn-hangzhou.aliyuncs.com/aberic/tangduosearch:latest
```

#### 三、部署命令参数说明

|参数|说明|
|---|---|
|`-name tangduosearch`|指定容器名称为 tangduosearch，便于后续管理（如停止、重启）|
|`-p 19219:19219`|映射业务接口端口（宿主机端口:容器内端口），外部通过宿主机 19219 端口访问业务接口|
|`-p 19220:19220`|映射健康检查/监控端口，用于容器健康检测、服务监控|
|`-v /data/vol/tangduosearch:/data`|数据持久化：将容器内 /data 目录（数据库数据存储目录）映射到宿主机 /data/vol/tangduosearch 目录，避免容器删除后数据丢失|

#### 四、Dockerfile 详情（镜像构建逻辑）
镜像的 [dockerfile](build/tangduosearch/dockerfile) 内容，清晰呈现镜像构建流程，便于自定义修改或验证。

#### 五、可自定义环境变量
部署时可通过 `\-e` 参数覆盖默认环境变量，适配不同场景需求，支持的环境变量如下（对应 [application.yml](search/src/main/resources/application.yml) 配置）：

## 贡献指南
欢迎提交 Issue 反馈问题，或通过 Pull Request 参与代码贡献，共同完善项目。

## 许可证
本项目采用开源协议，具体许可信息请参考项目内 LICENSE 文件。


