package com.eolinker.sql2dsl.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.eolinker.sql2dsl.service.DSLSelectHandler;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.ImmutableTriple;

import java.util.*;

/**
 * ES DSL 构造
 */
public class ESSelectHandler implements DSLSelectHandler {

    @Override
    public String andExpr(String left, String right) {
        return andorExpr(left, right);
    }

    @Override
    public String orExpr(String left, String right) {
        return andorExpr(left, right);
    }

    private String andorExpr(String left, String right){
        String resultStr;
        if (left == "" || right == "") {
            resultStr = left + right;
        } else {
            resultStr = left + "," + right;
        }
        return resultStr;
    }

    @Override
    public String betweenExpr(String field, String beginValue, String endValue) {
        return String.format("{\"range\" : {\"%s\" : {\"from\" : \"%s\", \"to\" : \"%s\"}}}", field, beginValue, endValue);
    }

    @Override
    public String inExpr(String field, List<String> values) {
        return String.format("{\"terms\" : {\"%s\" : [%s]}}", field, formatListValues(values));
    }

    @Override
    public String notInExpr(String field, List<String> values) {
        return String.format("{\"bool\" : {\"must_not\" : {\"terms\" : {\"%s\" : [%s]}}}}", field, formatListValues(values));
    }

    private String formatListValues(List<String> values){
        StringBuilder sb = new StringBuilder();
        values.forEach(v->{
            sb.append("\"").append(v).append("\"").append(",");
        });
        String value = sb.toString();
        //rm ","
        value = sb.substring(0, value.length() - 1);
        return value;
    }

    @Override
    public String greaterThanOrEqual(String field, String value) {
        return String.format("{\"range\" : {\"%s\" : {\"from\" : \"%s\"}}}", field, value);
    }

    @Override
    public String equality(String field, String value) {
        return String.format("{\"match_phrase\" : {\"%s\" : \"%s\"}}", field, value);
    }

    @Override
    public String lessThanOrEqual(String field, String value) {
        return String.format("{\"range\" : {\"%s\" : {\"to\" : \"%s\"}}}", field, value);
    }

    @Override
    public String greaterThan(String field, String value) {
        return String.format("{\"range\" : {\"%s\" : {\"gt\" : \"%s\"}}}", field, value);
    }

    @Override
    public String lessThan(String field, String value) {
        return String.format("{\"range\" : {\"%s\" : {\"lt\" : \"%s\"}}}", field, value);
    }

    @Override
    public String notEqual(String field, String value) {
        return String.format("{\"bool\" : {\"must_not\" : [{\"match_phrase\" : {\"%s\" : {\"query\" : \"%s\"}}}]}}", field, value);
    }

    @Override
    public String like(String field, String value) {
        return String.format("{\"query_string\":{\"default_field\": \"%s\",\"query\":\"*%s*\"}}", field, value);
    }

    @Override
    public String notLike(String field, String value) {
        return String.format("{\"bool\" : {\"must_not\" : {\"match_phrase\" : {\"%s\" : {\"query\" : \"%s\"}}}}}", field, value);
    }

    @Override
    public String is(String field, String value) {
        return String.format("{\"bool\": { \"must_not\": { \"exists\": { \"field\": \"%s\" }}}}",field);
    }

    @Override
    public String isNot(String field, String value) {
        return String.format("{\"bool\": { \"must\": { \"exists\": { \"field\": \"%s\" }}}}",field);
    }

    @Override
    public String root(String dsl, String groupBy, String orderBy, String offset, String limit) {
        StringBuilder result = new StringBuilder(String.format("{\"query\" : %s ", dsl));
        if (!StringUtils.isEmpty(offset)) {
            result.append(String.format(" ,\"from\" : %s ",offset));
        }
        if (!StringUtils.isEmpty(limit)) {
            result.append(String.format(" ,\"size\" : %s ", limit));
        }
        if (!StringUtils.isEmpty(orderBy)) {
            result.append(String.format(" ,\"sort\" : %s", orderBy));
        }
        if (!StringUtils.isEmpty(groupBy)) {
            result.append(String.format(" ,\"aggregations\" : %s", groupBy));
        }
        result.append("}");
        return result.toString();
//        return String.format("{\"query\" : %s ,\"from\" : %s ,\"size\" : %s ,\"sort\" : %s}", dsl, offset, limit, orderBy);
    }

    @Override
    public String orRoot(String dsl) {
        return String.format("{\"bool\" : {\"should\" : [%s]}}", dsl);
    }

    @Override
    public String andRoot(String dsl) {
        return must(dsl);
    }

    @Override
    public String betweenRoot(String dsl) {
        return must(dsl);
    }

    @Override
    public String inRoot(String dsl) {
        return must(dsl);
    }

    @Override
    public String comparisonRoot(String dsl){
        return must(dsl);
    }

    private String must(String dsl){
        return String.format("{\"bool\" : {\"must\" : [%s]}}", dsl);
    }

    @Override
    public String strQuotes(){
        return "";
    }

    @Override
    public String orderBy(List<ImmutablePair<String, String>> orderByList) {
        if (orderByList == null || orderByList.isEmpty()) {
            return null;
        } else {
            return JSON.toJSONString(orderByList);
        }
    }

    @Override
    public String groupBy(List<ImmutableTriple<String, String, String>> selectExprs, List<String> groupByFields, String havingStr) {
        Queue<String> gbs = new LinkedList<>(groupByFields);
        Map<String, Object> aggsMap = parseAggs(gbs, selectExprs, havingStr);
        return JSON.toJSONString(aggsMap);
    }

    private Map<String, Object> parseAggs(Queue<String> fields, List<ImmutableTriple<String, String, String>> selectFields, String havingStr) {
        if (fields.isEmpty()) {
            // 没有子聚合，解析函数项
            return parseStatAggs(selectFields, havingStr);
        }
        String field = fields.poll();
        Map<String, Object> aggsMap = new HashMap<>();
        Map<String, Object> fieldMap = new HashMap<>();
        Map<String, Object> termsMap = new HashMap<>();
        termsMap.put("field", field);
        termsMap.put("size", 500);  // size can be change
        fieldMap.put("terms", termsMap);
        fieldMap.put("aggregations", parseAggs(fields, selectFields, havingStr));

        aggsMap.put(field, fieldMap);
        return aggsMap;
    }

    private Map<String, Object> parseStatAggs(List<ImmutableTriple<String, String, String>> fields, String havingStr) {
        if (fields == null || fields.isEmpty()) {
            return null;
        }
        Map<String, Object> resultAggsMap = new HashMap<>();
        for (ImmutableTriple<String, String, String> field : fields) {
            String columnName = field.getLeft();
            String alias = field.getMiddle();
            String method = field.getRight();
            if (StringUtils.isEmpty(method)) {
                // 无函数跳过
                continue;
            }
            switch (method) {
                // 扩充方法，独立于sql和dsl语法，以filter(EXPR)来做dsl的聚合
                case "filter":
                    String filterStr = filter(columnName, null);
                    JSONObject filterJson = JSON.parseObject(filterStr);
                    resultAggsMap.put(alias, filterJson);
                    break;
                case "count":
                    if (columnName.equals("*")) {
                        columnName = "_index";
                    }
                    method = "value_count";
                    // 流入default做统一封装
                default:
                    // to map = {"$alias":{"$method":{"field":"%field"}}}
                    Map<String, Object> fieldMap = new HashMap<>();
                    fieldMap.put("field", columnName);
                    Map<String, Object> methodMap = new HashMap<>();
                    methodMap.put(method, fieldMap);
                    resultAggsMap.put(alias, methodMap);
                    break;
            }
        }
        // handle having str
        if (!StringUtils.isEmpty(havingStr)) {
            String bucketName = "HAVING_RESULT"; // bucket name can be change
            String filterStr = filter(havingStr, null);
            JSONObject filterJson = JSON.parseObject(filterStr);
            resultAggsMap.put(bucketName, filterJson);
        }
        return resultAggsMap;
    }

    private String filter(String queryStr, String resultName) {
        String filterFormat = "{\"filters\":{\"filters\":{\"%s\":%s}}}";
        if (StringUtils.isEmpty(resultName)) {
            resultName = "COUNT";   // default bucket name
        }
        return String.format(filterFormat, resultName, queryStr);
    }

    @Override
    public String queryAll() {
        return "{\"match_all\": {}}";
    }

}
