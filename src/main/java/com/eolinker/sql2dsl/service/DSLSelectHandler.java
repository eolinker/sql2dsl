package com.eolinker.sql2dsl.service;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.ImmutableTriple;

import java.util.List;

/**
 * select语法转换构造接口
 */
public interface DSLSelectHandler {

    //比较运算符（叶子节点（条件内容）逻辑处理）
    // e.g. mysql:  a > 1  ---->  es {"range" : {"a" : {"gt" : "1"}}}
    // a > 1 转义成  {"range" : {"a" : {"gt" : "1"}}}
    /**
     * >= 运算符处理
     * @returng
     */
    String greaterThanOrEqual(String field, String value);

    /**
     * = 运算符处理
     * @returng
     */
    String equality(String field, String value);

    /**
     * <= 运算符处理
     * @returng
     */
    String lessThanOrEqual(String field, String value);

    /**
     * > 运算符处理
     * @returng
     */
    String greaterThan(String field, String value);

    /**
     * < 运算符处理
     * @returng
     */
    String lessThan(String field, String value);

    /**
     * != 运算符处理
     * @returng
     */
    String notEqual(String field, String value);

    /**
     * like 运算符处理
     * @returng
     */
    String like(String field, String value);

    /**
     * not like 运算符处理
     * @returng
     */
    String notLike(String field, String value);

    /**
     * is 运算符处理
     * 某个字段为空值
     */
    String is(String field, String value);

    /**
     * is not 运算符处理
     * 查询字段不为空值
     */
    String isNot(String field, String value);

    //逻辑运算符（逻辑运算符条件拼接处理）
    // e.g. mysql:  a > 1 and b < 10  ----- > es {"range" : {"a" : {"gt" : "1"}}},{"range" : {"b" : {"lt" : "10"}}}
    // and 转成 ","
    /**
     * and 逻辑表达式处理
     * @return
     */
    String andExpr(String left, String right);

    /**
     * or 逻辑表达式处理
     */
    String orExpr(String left, String right);


    //逻辑运算符叶子节点（条件内容）处理
    //与比较运算符节点内容处理相似
    /**
     * Between 表达式处理
     */
    String betweenExpr(String field, String beginValue, String endValue);

    /**
     * in 表达式处理
     */
    String inExpr(String field, List<String> values);

    /**
     * not in 表达式处理
     * @param field
     * @param values
     * @return
     */
    String notInExpr(String field, List<String> values);

    String orderBy(List<ImmutablePair<String, String>> orderByList);

    String groupBy(List<ImmutableTriple<String, String, String>> selectExprs, List<String> groupByFields, String havingStr);

    String queryAll();

    //根节点、子节点拼接处理
    //es在条件外需要增加{"query":}
    //其他dsl如果不需要直接return
    /**
     * 根节点处理
     * 暂时只处理limit
     * @return
     */
    String root(String dsl, String groupBy, String orderBy, String offset, String limit);

    /**
     * or节点为根节点 dsl拼接逻辑
     * @param dsl
     * @return
     */
    default String orRoot(String dsl){
        return dsl;
    }

    /**
     * and节点为根节点 dsl拼接逻辑
     * @param dsl
     * @return
     */
    default String andRoot(String dsl){
        return dsl;
    }

    /**
     * between（只有一个条件） 为根节点时 dsl拼接逻辑
     * @param dsl
     * @return
     */
    default String betweenRoot(String dsl){
        return dsl;
    }

    /**
     * in（只有一个条件） 为根节点时 dsl拼接逻辑
     * @param dsl
     * @return
     */
    default String inRoot(String dsl){
        return dsl;
    }

    /**
     * 只有一个条件（比较运算符条件）时根节点 dsl拼接逻辑
     * @param dsl
     * @return
     */
    default String comparisonRoot(String dsl){
        return dsl;
    }

    /**
     * 字符串表示符号
     * @return
     */
    default String strQuotes(){
        return "'";
    }

}
