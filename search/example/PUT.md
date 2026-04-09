插入数据，数据插入后会被分词建立索引，database、index、key和degree可选，建议必传，避免无法精确定位查询

PUT data 
```json
{
  "database": "national",
  "index": "schools",
  "key": "key",
  "value": {
    "name": "City School",
    "description": "ICSE",
    "street": "West End",
    "city": "Meerut",
    "state": "UP",
    "zip": "250002",
    "location": [
      28.9926174,
      77.692485
    ],
    "fees": 3500,
    "tags": [
      "fully computerized"
    ],
    "rating": "4.5"
  }
}
```
RETURN
```json
{
  "code": 200
}
```

插入数据，数据插入后不会被分词建立索引，database、index、key和degree可选，建议必传，避免无法精确定位查询

PUT data
```json
{
  "database": "national",
  "index": "schools",
  "key": "key",
  "seg": false,
  "value": {
    "name": "City School",
    "description": "ICSE",
    "street": "West End",
    "city": "Meerut",
    "state": "UP",
    "zip": "250002",
    "location": [
      28.9926174,
      77.692485
    ],
    "fees": 3500,
    "tags": [
      "fully computerized"
    ],
    "rating": "4.5"
  }
}
```
RETURN
```json
{
  "code": 200
}
```

批量插入数据，数据插入后会被分词建立索引，database、index、key和degree可选，建议必传，避免无法精确定位查询

PUT data
```json
{
  "database": "national",
  "index": "schools",
  "values": [
    {
      "key": "key",
      "value": {
        "name": "City School",
        "description": "ICSE",
        "street": "West End",
        "city": "Meerut",
        "state": "UP",
        "zip": "250002",
        "location": [
          28.9926174,
          77.692485
        ],
        "fees": 3500,
        "tags": [
          "fully computerized"
        ],
        "rating": "4.5"
      }
    },
    {
      "key": "key",
      "value": {
        "name": "City School",
        "description": "ICSE",
        "street": "West End",
        "city": "Meerut",
        "state": "UP",
        "zip": "250002",
        "location": [
          28.9926174,
          77.692485
        ],
        "fees": 3500,
        "tags": [
          "fully computerized"
        ],
        "rating": "4.5"
      }
    }
  ]
}
```
RETURN
```json
{
  "code": 200
}
```

批量插入数据，数据插入后不会被分词建立索引，database、index、key和degree可选，建议必传，避免无法精确定位查询

PUT data
```json
{
  "database": "national",
  "index": "schools",
  "key": "key",
  "seg": false,
  "values": [
    {
      "key": "key",
      "value": {
        "name": "City School",
        "description": "ICSE",
        "street": "West End",
        "city": "Meerut",
        "state": "UP",
        "zip": "250002",
        "location": [
          28.9926174,
          77.692485
        ],
        "fees": 3500,
        "tags": [
          "fully computerized"
        ],
        "rating": "4.5"
      }
    },
    {
      "key": "key",
      "value": {
        "name": "City School",
        "description": "ICSE",
        "street": "West End",
        "city": "Meerut",
        "state": "UP",
        "zip": "250002",
        "location": [
          28.9926174,
          77.692485
        ],
        "fees": 3500,
        "tags": [
          "fully computerized"
        ],
        "rating": "4.5"
      }
    }
  ]
}
```
RETURN
```json
{
  "code": 200
}
```