package com.eolinker.sql2dsl.service.impl;

import com.eolinker.sql2dsl.service.DSLSelectHandler;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.ImmutableTriple;

import java.util.List;

/**
 * 通易理解demo
 */
public class MySqlSelectHandler implements DSLSelectHandler {
    @Override
    public String greaterThanOrEqual(String field, String value) {
        return field + " >= " + value;
    }

    @Override
    public String equality(String field, String value) {
        return field + " = " + value;
    }

    @Override
    public String lessThanOrEqual(String field, String value) {
        return field + " <= " + value;
    }

    @Override
    public String greaterThan(String field, String value) {
        return field + " > " + value;
    }

    @Override
    public String lessThan(String field, String value) {
        return field + " < " + value;
    }

    @Override
    public String notEqual(String field, String value) {
        return field + " != " + value;
    }

    @Override
    public String like(String field, String value) {
        return field + " like " + value;
    }

    @Override
    public String notLike(String field, String value) {
        return field + " not like " + value;
    }

    @Override
    public String is(String field, String value) {
        return field + " is " + value;
    }

    @Override
    public String isNot(String field, String value) {
        return field + " is not " + value;
    }

    @Override
    public String andExpr(String left, String right) {
        return hasAndOrLogic(left, " or ") + " and " + hasAndOrLogic(right, " or ");
    }

    @Override
    public String orExpr(String left, String right) {
        return hasAndOrLogic(left, " and ") + " or " + hasAndOrLogic(right, " and ");
    }

    private String hasAndOrLogic(String condition, String logic){
        if(condition != null && condition.indexOf(logic) != -1){
            return "(" + condition + ")";
        }
        return condition;
    }

    @Override
    public String betweenExpr(String field, String beginValue, String endValue) {
        return field + " between " + beginValue + " and " + endValue;
    }

    @Override
    public String inExpr(String field, List<String> values) {
        String value = formatListValues(values);
        return field + " in (" + value + ")";
    }

    private String formatListValues(List<String> values){
        StringBuilder sb = new StringBuilder();
        values.forEach(v->{
            sb.append(v).append(",");
        });
        String value = sb.toString();
        //rm ","
        value = sb.substring(0, value.length() - 1);
        return value;
    }

    @Override
    public String notInExpr(String field, List<String> values) {
        String value = formatListValues(values);
        return field + " not in (" + value + ")";
    }

    @Override
    public String root(String dsl, String groupBy, String orderBy, String offset, String limit) {
        StringBuilder result = new StringBuilder("SELECT ").append(dsl);
        if (!StringUtils.isEmpty(groupBy)) {
            result.append(" GROUP BY " ).append(groupBy);
        }
        if (!StringUtils.isEmpty(orderBy)) {
            result.append(" ORDER BY ").append(orderBy);
        }
        if (!StringUtils.isEmpty(limit)) {
            result.append(" LIMIT ").append(StringUtils.isEmpty(offset)?"0":offset).append(", ").append(limit);
        }
        return result.toString();
    }

    @Override
    public String orderBy(List<ImmutablePair<String, String>> orderByList) {
        if (orderByList == null || orderByList.isEmpty()) {
            return null;
        }
        StringBuilder result = new StringBuilder();
        for (ImmutablePair<String, String> orderBy : orderByList) {
            if (result.length() > 0) {
                result.append(",");
            }
            result.append(orderBy.getLeft()).append(" ").append(orderBy.getRight());
        }
        return result.toString();
    }

    @Override
    public String groupBy(List<ImmutableTriple<String, String, String>> selectExprs, List<String> groupByFields, String havingStr) {
        // sql 不对fields处理
        if (groupByFields == null || groupByFields.isEmpty()) {
            return null;
        }
        StringBuilder result = new StringBuilder();
        for (String gField : groupByFields) {
            if (result.length() > 0) {
                result.append(",");
            }
            result.append(gField);
        }
        result.append(" HAVING ").append(havingStr);
        return result.toString();
    }

    @Override
    public String queryAll() {
        return "*";
    }
}
