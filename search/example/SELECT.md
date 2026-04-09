读取单条数据，database、index和key可选，建议必传，避免无法精确定位查询

PUT data
```json
{
  "database": "national",
  "index": "schools",
  "query": "读书学习有什么好处",
  "degreeMin": "0",
  "degreeMax": "10000",
  "includeMin": true,
  "includeMax": false,
  "limit": 100,
  "asc": true,
  "delete": true
}
```

RETURN
```json
{
  "code": 200
}
```