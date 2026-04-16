## 模块概述
`db` 是本数据库项目的核心业务模块，提供**对象存储、文档存储、富查询**三大核心能力，并深度整合 `index` 模块的索引能力，为上层 `search` 模块（类 Elasticsearch 搜索服务）提供底层数据存储与检索支撑。
该模块屏蔽了底层索引操作、存储介质的复杂度，对外提供统一、易用的结构化数据存取接口，同时支持灵活的富查询语法，兼顾数据存储的可靠性与检索的高性能。

## 模块依赖
- 核心依赖：`index` 模块（提供索引创建、读写、事务、异步刷盘等基础能力）
- 基础依赖：`common` 模块（通用工具类、序列化/反序列化、异常处理、配置解析等）
- 上层关联：`search` 模块基于本模块封装搜索服务，依赖本模块的富查询能力

## 核心功能
### 1. 多类型存储支持
#### （1）对象存储
- 支持结构化对象的持久化存储：基于自定义序列化/反序列化机制，实现 Java 对象的一键存储与读取；
- 对象版本管理：可选开启对象版本控制，支持版本回溯、覆盖保护；
- 批量对象操作：支持批量插入、更新、删除对象，兼顾性能与原子性。

#### （2）文档存储
- 类 JSON 文档存储：支持非结构化/半结构化文档的存储，兼容 JSON 格式的灵活字段；
- 文档字段索引：自动为文档核心字段创建索引（基于 `index` 模块），支撑后续富查询；
- 文档分片存储（规划）：支持大文档拆分存储，解决单文档过大导致的性能问题。

#### （3）存储一致性保障
- 事务封装：整合 `index` 模块的事务能力，保证“数据存储 + 索引更新”的原子性；
- 写前校验：对存储数据做合法性校验（字段类型、长度、约束），避免脏数据写入；
- 数据恢复：支持基于日志的崩溃恢复，保证异常宕机后数据不丢失。

### 2. 富查询能力
- 基础条件查询：支持等值、范围、模糊匹配等基础查询语法；
- 组合条件查询：支持多条件 `AND/OR/NOT` 组合，满足复杂业务查询场景；
- 排序：查询结果支持按指定字段排序（升序/降序）；
- 分页（规划）：提供分页接口；
- 索引优化查询：自动路由查询条件到对应索引，避免全量扫描，提升查询效率；

### 3. 核心辅助能力
- 存储元数据管理：维护存储对象/文档的元信息（创建时间、修改时间、大小、索引映射等）；
- 配置化管理：支持通过配置文件调整存储路径、索引刷盘频率、查询超时时间等参数；
- 监控与统计（规划）：提供存储容量、读写 QPS、查询耗时等基础监控指标，便于问题排查。


## DB概述
`cn.aberic.tangduo.db.DB` 是本地文件型索引数据库的核心入口类，采用**单例模式**设计，提供数据库（文件目录维度）、索引的创建/删除，以及数据的插入、查询、删除、检索（分词搜索）等全量操作能力。底层基于文件系统存储数据，结合分词引擎（HanLP/IK）实现高效文本检索，支持批量操作、条件过滤、结果排序等核心特性。

### 核心特性
- 单例化管理：保证数据库实例全局唯一，避免资源冲突；
- 多数据库/索引支持：按名称隔离多个数据库，每个数据库下支持多索引；
- 分词检索：集成 HanLP/IK 分词引擎，支持文本智能拆分与检索；
- 批量操作：支持批量插入数据，提升写入效率；
- 条件过滤：支持数值、字符串等多类型条件筛选；
- 结果排序：基于 BM25 算法实现检索结果相关性排序；
- 数据安全：通过文件锁、线程安全容器保证并发操作安全。

## 核心依赖
- 分词引擎：HanLP（默认）、IK 分词器；
- JSON 处理：Jackson；
- 并发工具：JDK 虚拟线程、ConcurrentHashMap、ReentrantLock；
- 文件操作：Java NIO、自定义文件工具类（`Filer`/`Channel`/`Writer`）；
- 其他：Lombok（日志）、Apache Commons Lang3（字符串处理）。

## 初始化与单例获取
### 1. 核心参数说明
| 参数名 | 类型 | 说明 |
|--------|------|------|
| `rootPath` | String | 数据库根目录路径（文件系统路径，如 `tmp/data`），所有数据库文件均存储在此目录下 |
| `dataFileMaxSize` | long | 单个数据文件的大小阈值（单位：byte），超过阈值自动分片 |
| `searchMaxCount` | int | 单次检索的最大数据量（默认 10000 条） |
| `batchMaxSize` | int | 单批次插入的最大数据量（默认 5000 条） |

### 2. 初始化方法
#### 最简初始化（使用默认检索/批次阈值）
```java
import cn.aberic.tangduo.db.DB;
import java.io.IOException;

public class Demo {
    public static void main(String[] args) throws IOException, NoSuchFieldException {
        // 根目录 + 单个文件最大size（示例：100MB）
        DB db = DB.getInstance("/tmp/tangduo/db", 1024 * 1024 * 100);
    }
}
```

#### 自定义阈值初始化
```java
// 根目录 + 文件阈值 + 最大检索数 + 批次最大数
DB db = DB.getInstance("/tmp/tangduo/db", 1024 * 1024 * 100, 20000, 10000);
```

## 核心操作
### 1. 数据库管理
#### 创建数据库
支持指定分词引擎（HanLP/IK），默认使用 HanLP。
```java
try {
    // 创建默认分词（hanlp）的数据库
    db.createDB("my_db");
    // 创建指定IK分词的数据库
    db.createDB("my_ik_db", "ik");
} catch (IOException | InstanceAlreadyExistsException | NoSuchFieldException e) {
    e.printStackTrace(); // 捕获“数据库已存在”等异常
}
```

#### 检查数据库是否存在
```java
boolean exists = db.dbExist("my_db");
System.out.println("数据库my_db是否存在：" + exists);
```

#### 删除数据库
删除数据库会同时删除对应的文件目录及所有索引/数据。
```java
try {
    db.removeDB("my_db");
} catch (IOException e) {
    e.printStackTrace();
}
```

### 2. 索引管理
#### 创建索引
需指定引擎类型（如 `IEngine.UNITY`）和索引信息（`Index.Info`）。
```java
import cn.aberic.tangduo.db.entity.Index;
import cn.aberic.tangduo.index.engine.IEngine;

try {
    // 索引信息：分片数1、索引名、是否压缩、是否加密、是否分片
    Index.Info info = new Index.Info(1, "user_index", false, false, false);
    db.createIndex("my_db", IEngine.UNITY, info);
} catch (Exception e) {
    e.printStackTrace(); // 捕获“数据库不存在/索引已存在”等异常
}
```

#### 删除索引
```java
try {
    db.removeIndex("my_db", "user_index");
} catch (Exception e) {
    e.printStackTrace();
}
```

### 3. 数据操作
#### 插入数据
支持单条插入、指定数据库/索引/主键插入，默认自动生成 UUID 主键，支持分词/不分词插入。

##### 最简插入（默认数据库+默认索引）
```java
try {
    // 插入字符串数据
    DocPutResponseVO response = db.put("hello tangduo db");
    // 插入JSON对象（自动解析JSON字段建立索引）
    String json = "{\"name\":\"张三\",\"age\":25,\"email\":\"zhangsan@test.com\"}";
    DocPutResponseVO jsonResponse = db.put(json);
} catch (IOException e) {
    e.printStackTrace();
}
```

##### 指定数据库/索引/主键插入
```java
try {
    // 指定数据库、索引、主键，开启分词
    DocPutResponseVO response = db.put("my_db", "user_index", "user_001", true, "{\"name\":\"李四\",\"age\":30}");
} catch (IOException e) {
    e.printStackTrace();
}
```

##### 批量插入
```java
import cn.aberic.tangduo.db.entity.DocPutBatchRequestVO;
import java.util.ArrayList;
import java.util.List;

try {
    List<DocPutBatchRequestVO> batchList = new ArrayList<>();
    // 构造批量插入数据
    DocPutBatchRequestVO vo1 = new DocPutBatchRequestVO();
    vo1.setIndex("user_index");
    vo1.setKey("user_002");
    vo1.setValue("{\"name\":\"王五\",\"age\":28}");
    vo1.setSeg(true); // 开启分词
    batchList.add(vo1);

    DocPutBatchRequestVO vo2 = new DocPutBatchRequestVO();
    vo2.setIndex("user_index");
    vo2.setKey("user_003");
    vo2.setValue("{\"name\":\"赵六\",\"age\":35}");
    vo2.setSeg(true);
    batchList.add(vo2);

    // 批量插入到my_db数据库
    db.put("my_db", batchList);
} catch (IOException e) {
    e.printStackTrace();
}
```

#### 查询数据
支持按主键/索引/度（哈希值）查询，返回单条或多条结果。

##### 查询单条数据（按主键）
```java
try {
    // 指定数据库、索引、主键查询
    DocGetResponseVO response = db.getFirst("my_db", "user_index", "user_001");
    if (response != null) {
        System.out.println("查询结果：" + response.getDoc().getValue());
    }
} catch (IOException e) {
    e.printStackTrace();
}
```

##### 查询多条数据（按索引/条件）
```java
try {
    // 指定数据库、索引、度（哈希值）、主键查询（主键为空则查索引下所有数据）
    List<DocGetResponseVO> list = db.get("my_db", "user_index", null, "");
    list.forEach(item -> System.out.println(item.getDoc().getValue()));
} catch (IOException e) {
    e.printStackTrace();
}
```

#### 检索数据（分词搜索）
支持按关键词分词检索，基于 BM25 算法排序，支持指定返回条数。

```java
try {
    // 检索my_db中包含“张三”的内容，返回前10条
    List<DocSearchResponseVO> result = db.search("my_db", "张三", 10);
    // 按相关性排序后输出
    result.forEach(item -> System.out.println("检索结果：" + item.getValue()));
} catch (IOException e) {
    e.printStackTrace();
}
```

#### 删除数据
支持按主键删除、按条件批量删除。

##### 按主键删除
```java
try {
    // 指定数据库、索引、主键删除
    db.remove("my_db", "user_index", "user_001");
} catch (IOException e) {
    e.printStackTrace();
}
```

##### 批量删除（按条件）
```java
import cn.aberic.tangduo.index.engine.entity.Search;

try {
    // 构造搜索条件，删除my_db中索引为user_index、包含“李四”的内容
    Search search = new Search("user_index", 10);
    List<DocSearchResponseVO> deletedList = db.delete("my_db", search);
    System.out.println("删除条数：" + deletedList.size());
} catch (IOException e) {
    e.printStackTrace();
}
```

## 核心常量说明
| 常量名 | 类型 | 说明 |
|--------|------|------|
| `INDEX_NAME_DEFAULT` | String | 默认索引名（`default`） |
| `DATABASE_NAME_DEFAULT` | String | 默认数据库名（`default`） |
| `SEARCH_MAX_COUNT` | int | 默认单次检索最大数据量（10000） |
| `INDEX_BATCH_MAX_SIZE` | int | 默认单批次插入最大数据量（5000） |

## 异常处理
| 异常类型 | 触发场景 |
|----------|----------|
| `InstanceAlreadyExistsException` | 创建已存在的数据库/索引 |
| `NoSuchFileException` | 操作不存在的数据库/索引 |
| `JsonParseException` | 解析JSON格式数据失败 |
| `IOException` | 文件读写失败（如权限不足、路径不存在） |
| `InterruptedException` | 并发操作时线程中断 |

## 注意事项
1. **单例唯一性**：初始化时需保证 `rootPath` 唯一，避免多实例冲突；
2. **文件权限**：确保 `rootPath` 目录有读写权限，否则会触发 `IOException`；
3. **分词引擎**：切换 HanLP/IK 需确保对应依赖包已引入，否则分词功能异常；
4. **数据分片**：`dataFileMaxSize` 建议根据磁盘性能设置（如 100MB~1GB），过小会导致文件数量过多，过大影响读写效率；
5. **并发安全**：类内已通过 `ConcurrentHashMap`、`ReentrantLock` 保证并发安全，无需额外加锁；
6. **JSON 解析**：插入 JSON 格式数据时，会自动解析字段建立索引（数值字段建立数值索引，字符串字段建立哈希/时间索引），非 JSON 数据仅做文本分词。

## 八、扩展说明
### 1. 自定义分词规则
可通过扩展 `IkTokenizerTools`/`HanlpTools` 类，修改分词过滤逻辑（如屏蔽停用词、自定义词典）。

### 2. 自定义条件过滤
`doFilter` 方法支持扩展条件过滤规则（如新增日期范围、模糊匹配等）。

### 3. 数据持久化
所有数据均存储在 `rootPath` 对应的文件目录中，数据库/索引对应子目录，数据文件按分片规则命名，可直接备份目录实现数据备份。