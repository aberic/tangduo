读取单条数据，database、index和key可选，建议必传，避免无法精确定位查询

PUT data
```json
{
  "database": "national",
  "index": "schools",
  "key": "key"
}
```
RETURN
```json
{
  "code": 200
}
```