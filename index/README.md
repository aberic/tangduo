## 模块概述
`index` 是数据库项目的核心索引模块，为上层 `db` 模块（对象存储、文档存储、富查询）和 `search` 模块（类 Elasticsearch 搜索服务）提供高性能、可靠的索引管理能力，是整个系统实现快速检索、数据定位的核心依赖。
该模块封装了底层存储的索引读写、异步刷盘、事务一致性等核心逻辑，对外提供简洁的索引操作接口，支撑上层模块的富查询、全文检索等核心能力。

## 模块依赖
- 底层依赖：`common` 模块（通用工具、线程、IO、集合等基础能力）
- 上层依赖：`db` 模块直接依赖本模块，`search` 模块通过 `db` 模块间接依赖

## 核心功能
### 1. 索引基础操作
- 索引写入（Put）：支持单条/批量索引数据写入，基于异步 Channel 机制实现高性能写入，避免同步磁盘 IO 阻塞业务线程。
- 索引读取（Get）：支持按索引键快速查询对应数据，保证读取的是已落地的有效数据。
- 索引删除（Delete）：支持索引的标记删除与物理删除，兼顾性能与数据一致性。
- 事务支持：支持索引操作的事务封装，保证批量索引操作的原子性。

### 2. 异步存储优化
- 基于 Channel 异步写线程模型：将索引写入操作封装为 Buffer 放入无锁队列，由独立异步线程处理磁盘 IO，提升写入吞吐量。
- 刷盘机制：支持手动/自动刷盘（可配置），保证索引数据最终落地磁盘，兼顾性能与数据可靠性。
- 队列消费管控：通过队列缓冲削峰填谷，避免高并发写入时的磁盘 IO 瓶颈。

### 3. 一致性保障
- 写入通知机制：通过锁/条件变量（Lock/Condition）保证索引层操作的有序性与通知机制。
- 事务执行：封装事务逻辑，确保批量索引操作的执行一致性，插入失败时可回滚（按需配置）。

## 核心设计
### 1. 核心类说明
| 类名          | 核心职责                                                                 |
|---------------|--------------------------------------------------------------------------|
| `Channel`     | 底层存储通道，管理文件级别的异步读写、队列缓冲、刷盘，封装 FileChannel 操作 |
| `Channel.Intent` | Channel 内部的文件操作意图封装，管理单文件的异步写线程、Buffer 队列        |
| `Index`       | 索引核心类，对外提供 Put/Get/Delete 等索引操作，封装事务与一致性逻辑       |
| `Transaction` | 索引事务封装类，管理批量索引操作的原子执行                               |

### 2. 异步写入流程
1. 调用 `Index.put()` 触发索引写入；
2. 事务层封装写入操作，调用 `Channel.write()` 将写入请求封装为 Buffer 放入无锁队列；
3. Channel 异步写线程（`startWriteThread`）从队列中消费 Buffer，执行 FileChannel 磁盘写入；
4. 写入完成后可通过刷盘机制（`Channel.force()`）确保数据落地。

### 3. 关键注意点
- 异步写入特性：`Channel.write()` 仅将请求入队后立即返回，并非同步完成磁盘写入；
- 线程生命周期：异步写线程默认为非守护线程，会持续消费队列，需注意测试/生产环境的线程管控；
- 批量操作建议：大批量写入时建议分批次执行，或主动调用“等待写入完成”接口，避免查询时数据未落地。

## 快速使用
### 1. 基础索引写入与查询
```java
// 1. 初始化索引
String indexName = "putAndGetFirst";
Index index = Index.getInstance(rootpath, DATA_FILE_DEFAULT_SIZE);

// 2. 单条索引写入
String key = "user_1001";
byte[] value = "{\"name\":\"test\",\"age\":20}".getBytes(StandardCharsets.UTF_8);
index.put(new Content(new Transaction(1), indexName, key, value));

// 3. 等待异步写入完成（单元测试/关键场景使用）
index.force(degree);

// 4. 索引查询
byte[] result = index.get(key);
if (result != null) {
    System.out.println(ByteTools.toString(result));
}
```

### 2. 批量事务写入
```java
String indexName = "putAndGetFirstBatch";
Index index = Index.getInstance(rootpath, DATA_FILE_DEFAULT_SIZE);

int threadCount = 300000;
int startIndex = threadCount / 2 - threadCount;
int endIndex = threadCount / 2;

List<Content> contentList = new ArrayList<>();
for (int i = startIndex; i < endIndex; i++) {
    contentList.add(new Content(indexName, i, String.valueOf(i), ByteTools.fromInt(i)));
}
// 2. 批量添加写入操作
index.put(contentList);


// 5. 批量查询
// 参考单元测试 putAndGetFirstBatch 和 putAndGetFirstBatchTmp
```

## 单元测试注意事项
1. 异步写入导致的查询异常：
    - 问题：批量写入后立即查询可能因异步线程未完成刷盘，导致查询不到数据；
    - 解决方案：查询前调用 `Channel.waitForWriteComplete(filePath)`，等待队列消费完成并刷盘。
2. 分批次执行建议：
    - 大批量测试数据写入时，建议分批次执行并增加短暂休眠，避免队列堆积导致的测试不稳定。
3. 线程管控：
    - 测试结束后需确保异步写线程正常关闭，避免测试进程残留。

## 性能优化建议
1. 批量写入：优先使用事务批量写入，减少单次 IO 开销；
2. 刷盘策略：非核心数据可降低刷盘频率（减少 `force()` 调用），核心数据建议写入后主动刷盘；
3. 队列配置：根据业务并发量调整 Channel 队列大小，避免队列溢出或过度占用内存。

## 常见问题
| 问题现象                     | 根因分析                                     | 解决方案                                     |
|------------------------------|----------------------------------------------|----------------------------------------------|
| 写入无报错但查询不到数据     | 异步写线程未消费完队列，数据未落地           | 调用 `Channel.waitForWriteComplete()` 等待落地 |
| 大批量写入性能下降           | 磁盘 IO 瓶颈/队列堆积                        | 分批次写入 + 调整异步线程数 + 优化磁盘性能   |
| 测试进程退出导致数据丢失     | 异步写线程为守护线程，进程退出时写入中断     | 确保写线程为非守护线程，测试前等待写入完成   |

## 扩展能力
- 支持自定义索引存储路径：通过配置调整索引文件的存储目录，适配不同部署环境；
- 支持索引分片（规划中）：可扩展为分片索引，支撑海量数据的分布式检索；
- 支持索引缓存（规划中）：增加内存缓存层，进一步提升热点索引的读取性能。