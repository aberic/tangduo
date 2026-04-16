# TangDuo Search
这是 **TangDuo 嵌入式文件型检索数据库** 的标准 HTTP 接口层，基于 Spring Boot 提供 RESTful API，对外暴露**数据库管理、索引管理、数据增删改查、全文检索**等全部能力，上层业务可直接通过 HTTP 请求使用数据库功能。

---

## 一、服务说明
1. **底层依赖**：`cn.aberic.tangduo.db.DB` 核心数据库类
2. **服务形式**：Spring Boot RESTful API
3. **配置方式**：`application.yml` 配置数据库路径、文件大小、检索阈值等
4. **返回格式**：统一封装 `Response` 对象，包含成功/失败状态、数据、异常信息

---

## 二、通用配置（application.yml）
所有 Controller 共用以下配置，必须添加：
```yaml
custom:
  db:
    # 数据库根目录（数据持久化路径）
    DB_ROOT_PATH: ./tangduo-data
    # 单个数据文件最大大小（byte），示例：100MB
    DB_DATA_FILE_MAX_SIZE: 104857600
    # 单次检索最大数据量
    DB_SEARCH_MAX_COUNT: 10000
  index:
    # 批量插入最大条数
    INDEX_BATCH_MAX_SIZE: 5000
```

---

## 三、三大接口模块总览
| 控制器类            | 根路径     | 功能                     |
|--------------------|-----------|--------------------------|
| `DBController`     | `/db`     | 数据库创建、删除         |
| `IndexController`  | `/index`  | 索引创建、删除           |
| `DataController`   | `/data`   | 数据增删改查、全文检索   |

---

# 三、接口详细说明

## 1. DBController — 数据库管理
**路径：`/db`**
负责创建/删除数据库（文件系统级目录）。

### 1.1 创建数据库
- **请求**：`PUT /db/{dbName}`
- **参数**：`dbName` 路径变量
- **成功**：返回 `Response.success()`
- **异常**：已存在返回 `InstanceAlreadyExistsException`

### 1.2 删除数据库
- **请求**：`DELETE /db/{dbName}`
- **作用**：删除整个数据库目录及所有数据
- **危险操作**：不可恢复！

---

## 2. IndexController — 索引管理
**路径：`/index`**
在指定数据库内创建/删除索引。

### 2.1 创建索引
- **请求**：`PUT /index`
- **请求体**：`ReqCreateIndexVO`
```json
{
  "database": "testdb",
  "index": "userindex",
  "version": 1,
  "name": "userindex",
  "primary": false,
  "unique": false,
  "nullable": false
}
```
- **引擎固定**：`IEngine.UNITY`
- **作用**：创建检索索引，后续数据必须写入索引

### 2.2 删除索引
- **请求**：`DELETE /index/{dbName}/{indexName}`
- **作用**：删除指定库下的索引及索引数据

---

## 3. DataController — 数据操作（核心）
**路径：`/data`**
提供**单条插入、批量插入、精确查询、全文检索、条件检索、单条删除、条件删除**。

### 3.1 单条插入数据
- **请求**：`PUT /data`
- **请求体**：`ReqPutDataVO`
```json
{
  "database": "testdb",
  "index": "userindex",
  "key": "user001",
  "seg": true,
  "value": "{"name":"张三","age":25}"
}
```
- `seg=true`：开启分词检索
- `key` 可选，不填自动生成 UUID

### 3.2 批量插入数据
- **请求**：`PUT /data/batch`
- **请求体**：`ReqPutDataBatchVO`
- **作用**：批量写入，提升性能

### 3.3 精确查询（按 key）
- **请求**：`GET /data`
- **请求体**：`ReqGetDataVO`
- **作用**：根据 `database + index + key` 精确查询原始数据

### 3.4 全文检索（分词搜索）
- **请求**：`GET /data/search`
- **请求体**：`ReqSearchDataVO`
```json
{
  "database": "testdb",
  "index": "userindex",
  "query": "张三",
  "limit": 10
}
```
- **自动分词**：HanLP / IK
- **排序算法**：BM25 相关性排序
- **返回**：匹配度最高的结果集

### 3.5 条件检索（范围/等值查询）
- **请求**：`GET /data/select`
- **请求体**：`ReqSelectDataVO`
- **支持**
    - 数值范围 `degreeMin / degreeMax`
    - 包含边界 `includeMin/includeMax`
    - 排序 `asc`
    - 条件过滤 `conditions`（= ≠ > < ≥ ≤）
    - 分页 `limit`

### 3.6 单条删除（按 key）
- **请求**：`DELETE /data`
- **请求体**：`ReqRemoveDataVO`
- **作用**：精确删除一条数据

### 3.7 条件删除（按检索条件删除）
- **请求**：`DELETE /data/delete`
- **请求体**：`ReqDeleteDataVO`
- **作用**：根据检索条件批量删除
- **返回**：删除的结果列表

---

# 四、完整请求/返回结构
## 统一返回体 Response
```json
{
  "success": true,
  "code": 200,
  "data": {},
  "msg": "success"
}
```

## 常用 VO 结构说明
### ReqPutDataVO（插入单条）
```java
private String database;
private String index;
private String key;
private boolean seg;    // 是否分词
private Object value;   // 数据体（字符串/JSON/对象）
```

### ReqSearchDataVO（全文检索）
```java
private String database;
private String index;
private String query;   // 搜索关键词
private int limit;      // 返回条数
```

### ReqSelectDataVO（条件检索）
```java
private String database;
private String index;
private Long degreeMin;
private Long degreeMax;
private boolean includeMin;
private boolean includeMax;
private int limit;
private boolean asc;
private List<Condition> conditions;
```

---

# 五、使用流程（最佳实践）
```
1. 创建数据库 → PUT /db/testdb
2. 创建索引 → PUT /index
3. 插入数据 → PUT /data
4. 全文检索 → GET /data/search
5. 条件查询 → GET /data/select
6. 删除数据 → DELETE /data
7. 删除索引 → DELETE /index/...
8. 删除库 → DELETE /db/...
```

---

# 六、异常说明
| 异常类型 | 含义 |
|---------|------|
| `InstanceAlreadyExistsException` | 库/索引已存在 |
| `NoSuchFileException` | 库/索引不存在 |
| `IOException` | 文件读写异常 |
| `JsonParseException` | JSON 解析失败 |
| `NoSuchFieldException` | 配置读取失败 |

---

# 七、适用场景
- 嵌入式检索服务
- 无需部署 MySQL/ES 的轻量级搜索服务
- 单服务内置全文检索
- 小体量结构化 + 非结构化数据存储与检索
- 快速搭建日志检索、内容检索、文档检索服务

---

## 总结
这三个 Controller 完整封装了 **TangDuo 文件检索数据库** 的全部能力：
- **DBController**：管理数据库（目录）
- **IndexController**：管理检索索引
- **DataController**：提供全功能数据 HTTP 接口（增删改查/分词检索/条件查询）

你可以直接将这三个类集成到 Spring Boot 项目中，开箱即用一套**本地化、持久化、带分词检索的文件数据库**，也可以独立部署一个TangDuoSearch服务，通过服务暴露的API使用上述功能。