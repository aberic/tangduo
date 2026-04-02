PUT db/数据库名/表(默认索引)名/索引值
PUT db/national/schools/5 
```json
{
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
```
RETURN
```json
{
  "_database": "national",
  "_index": "schools",
  "_id": "5",
  "_version": 1,
  "result": "created",
  "_shards": {
    "total": 2,
    "successful": 1,
    "failed": 0
  },
  "_seq_no": 2,
  "_primary_term": 1
}
```