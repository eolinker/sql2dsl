## 简介
convert SQL to Elasticsearch DSL in java.

将SQL转成Elasticsearch的DSL的工具，语言类型：Java。

## sql语法支持

#### 普通查询条件支持

- [x] and
- [x] or
- [x] equal(=)
- [x] not equal(!=)
- [x] gt(>)
- [x] gte(>=)
- [x] lt(<)
- [x] lte(<=)
- [x] in (如 id in (1,2,3) )
- [x] not in (如 id not in (1,2,3) )
- [x] 括号语法 (如 where (a=1 or b=1) and (c=1 or d=1))
- [x] 模糊查询 like 表达式 (目前用query_string实现)
- [x] order by
- [x] limit
- [x] not like
- [x] 空字段检查(is null, is not null)

#### 聚合功能支持

- [x] group by

> group by中多个字段用“,”隔开，生成的dsl聚合会从左到右深入，sql中存在group by才会生成聚合

- [x] having聚合（如 having a=1 and b=2）

> having条件最后会作为一个filter的聚合放入到最底层聚合中，最终放在buckets=HAVING.COUNT中

- [x] 统计类聚合函数，如 count(\*), count(field), min(field), max(field), avg(field), sum(field)

> 统计类函数会放到最底层聚合中，且只有group by时这些函数才会生效
>
> * 每个聚合必须带别名，如（select count(user) userTotal ...）

- [x] 仅es支持的统计聚合函数，如 stats(field), extended_stats(field), percentiles(field)

> 此3个方法仅在dsl聚合中支持，sql不支持，这里是对sql的扩充
>
> * 每个聚合必须带别名，如（select stats(user) userStats ...）

- [x] 其他支持，filter函数

> 此函数在dsl和sql均不支持，作为dsl聚合的扩充，用法filter($sql_expr)，如 filter(sex = 1)， filter(sex = 0 and name like 'lucy')，每一个filter会转换成一个聚合放入最底层聚合当中，最终放在buckets = $Alias.COUNT中
>
> * 每个聚合必须带别名，如（select filter(sex=1) maleTotal, filter(sex=2) femaleTotal ...）

#### 暂不支持

- [ ] join表达式
- [ ] 多表查询

## 示例代码
* com.eolinker.sql2dsl.TestDemo

## 例子展示

1. select * from user where sex = 1 and age >= 18

```json
{"query" : {"bool" : {"must" : [{"match_phrase" : {"sex" : "1"}},{"range" : {"age" : {"from" : "18"}}}]}}  ,"from" : 0  ,"size" : 0 }
```

2. select * from user where sex = 1 or age < 18

```json
{"query" : {"bool" : {"should" : [{"match_phrase" : {"sex" : "1"}},{"range" : {"age" : {"lt" : "18"}}}]}}  ,"from" : 0  ,"size" : 0 }
```

3. select * from user where dept in ('A','B','C')

```json
{"query" : {"bool" : {"must" : [{"terms" : {"dept" : ["A","B","C"]}}]}}  ,"from" : 0  ,"size" : 0 }
```

4. select * from user where dept not in ('A')

```json
{"query" : {"bool" : {"must" : [{"bool" : {"must_not" : {"terms" : {"dept" : ["A"]}}}}]}}  ,"from" : 0  ,"size" : 0 }
```

5. select * from user where (sex = 1 and age > 18) or (sex = 0 and age > 18)

```json
{"query" : {"bool" : {"should" : [{"bool" : {"must" : [{"match_phrase" : {"sex" : "1"}},{"range" : {"age" : {"gt" : "18"}}}]}},{"bool" : {"must" : [{"match_phrase" : {"sex" : "0"}},{"range" : {"age" : {"gt" : "18"}}}]}}]}}  ,"from" : 0  ,"size" : 0 }
```

6. select * from user where name like 'lucy'

```json
{"query" : {"bool" : {"must" : [{"query_string":{"default_field": "name","query":"*lucy*"}}]}}  ,"from" : 0  ,"size" : 0 }
```

7. select * from user where name not like 'lucy'

```json
{"query" : {"bool" : {"must" : [{"bool" : {"must_not" : {"match_phrase" : {"name" : {"query" : "lucy"}}}}}]}}  ,"from" : 0  ,"size" : 0 }
```

8. select * from user order by id desc, name asc limit 0,10

```json
{"query" : {"match_all": {}}  ,"from" : 0  ,"size" : 10  ,"sort" : [{"id":"DESC"},{"name":"ASC"}]}
```

9. select * from user where mobile is null

```json
{"query" : {"bool" : {"must" : [{"bool": { "must_not": { "exists": { "field": "mobile" }}}}]}}  ,"from" : 0  ,"size" : 0 }
```

10. select * from user where mobile is not null

```json
{"query" : {"bool" : {"must" : [{"bool": { "must": { "exists": { "field": "mobile" }}}}]}}  ,"from" : 0  ,"size" : 0 }
```

11. select * from user group by dept,class,level

```json
{"query" : {"match_all": {}}  ,"from" : 0  ,"size" : 0  ,"aggregations" : {"dept":{"terms":{"field":"dept","size":500},"aggregations":{"class":{"terms":{"field":"class","size":500},"aggregations":{"level":{"terms":{"field":"level","size":500},"aggregations":{}}}}}}}}
```

12. select * from user group by dept,class having sex = 1 and age >18

```json
{"query" : {"match_all": {}}  ,"from" : 0  ,"size" : 0  ,"aggregations" : {"dept":{"terms":{"field":"dept","size":500},"aggregations":{"class":{"terms":{"field":"class","size":500},"aggregations":{"HAVING_RESULT":{"filters":{"filters":{"COUNT":{"bool":{"must":[{"match_phrase":{"sex":"1"}},{"range":{"age":{"gt":"18"}}}]}}}}}}}}}}}
```

13. select count(*) userTotal from user group by class

```json
{"query" : {"match_all": {}}  ,"from" : 0  ,"size" : 0  ,"aggregations" : {"class":{"terms":{"field":"class","size":500},"aggregations":{"userTotal":{"value_count":{"field":"_index"}}}}}}
```

14. select min(math) min_math,min(chinese) min_ch,max(english) max_eng, avg(total_score) avg_score from score group by class

```json
{"query" : {"match_all": {}}  ,"from" : 0  ,"size" : 0  ,"aggregations" : {"class":{"terms":{"field":"class","size":500},"aggregations":{"min_math":{"min":{"field":"math"}},"max_eng":{"max":{"field":"english"}},"min_ch":{"min":{"field":"chinese"}},"avg_score":{"avg":{"field":"total_score"}}}}}}
```

15. select extended_stats(total_score) stat_score,percentiles(total_score) pc_score from score group by class

```json
{"query" : {"match_all": {}}  ,"from" : 0  ,"size" : 0  ,"aggregations" : {"class":{"terms":{"field":"class","size":500},"aggregations":{"stat_score":{"extended_stats":{"field":"total_score"}},"pc_score":{"percentiles":{"field":"total_score"}}}}}}
```

16. select filter(sex=1 and age<14) boy_total, filter(sex=2 and age<14) girl_total, filter(sex=1 and age>=14) man_total, filter(sex=2 and age>=14) women_total from user group by dept

```json
{"query" : {"match_all": {}}  ,"from" : 0  ,"size" : 0  ,"aggregations" : {"dept":{"terms":{"field":"dept","size":500},"aggregations":{"women_total":{"filters":{"filters":{"COUNT":{"bool":{"must":[{"match_phrase":{"sex":"2"}},{"range":{"age":{"from":"14"}}}]}}}}},"girl_total":{"filters":{"filters":{"COUNT":{"bool":{"must":[{"match_phrase":{"sex":"2"}},{"range":{"age":{"lt":"14"}}}]}}}}},"man_total":{"filters":{"filters":{"COUNT":{"bool":{"must":[{"match_phrase":{"sex":"1"}},{"range":{"age":{"from":"14"}}}]}}}}},"boy_total":{"filters":{"filters":{"COUNT":{"bool":{"must":[{"match_phrase":{"sex":"1"}},{"range":{"age":{"lt":"14"}}}]}}}}}}}}}
```