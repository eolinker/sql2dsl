package com.eolinker.sql2dsl.service;

import com.alibaba.druid.sql.ast.*;
import com.alibaba.druid.sql.ast.expr.*;
import com.alibaba.druid.sql.ast.statement.*;
import com.alibaba.druid.sql.dialect.mysql.ast.statement.MySqlSelectQueryBlock;
import com.eolinker.sql2dsl.util.StringUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.ImmutableTriple;

import java.util.ArrayList;
import java.util.List;

import static com.alibaba.druid.sql.ast.expr.SQLBinaryOperator.BooleanAnd;
import static com.alibaba.druid.sql.ast.expr.SQLBinaryOperator.BooleanOr;

/**
 * @author kohri
 * mysql select 语法处理器
 */
public class SelectHandler {

    private DSLSelectHandler dslSelectHandler;

    public void setDslSelectHandler(DSLSelectHandler dslSelectHandler) {
        this.dslSelectHandler = dslSelectHandler;
    }

    public ImmutablePair<String, String> handleSelect(SQLSelectStatement statement) throws Exception {
        // Handle where
        // top level node pass in an empty interface
        // to tell the children this is root
        String queryMapStr;
        String esType = "";
        String queryFrom = "0";
        String querySize = "10";
        String orderByStr = null;
        String groupByStr = null;
        String result = "";

        try {
            if (statement.getSelect() != null) {
                SQLSelectQuery sqlSelectQuery = statement.getSelect().getQuery();
                MySqlSelectQueryBlock mySqlSelectQueryBlock = (MySqlSelectQueryBlock) sqlSelectQuery;

                //get select fields
                // 目前group by 才需要对fields进行解析
                List<SQLSelectItem> selectItems = mySqlSelectQueryBlock.getSelectList();
                List<ImmutableTriple<String, String, String>> fields = getSelectFields(selectItems);

                queryMapStr = handleSelectWhere(mySqlSelectQueryBlock.getWhere(), true);

                //Handle from
                SQLExprTableSource sqlTableSource = (SQLExprTableSource) mySqlSelectQueryBlock.getFrom();
                SQLIdentifierExpr fromSqlIdentifierExpr = (SQLIdentifierExpr) sqlTableSource.getExpr();
                esType = fromSqlIdentifierExpr.getName();

                //Handle group by
                List<String> groupByList = new ArrayList<>();
                SQLSelectGroupByClause sqlSelectGroupByClause = mySqlSelectQueryBlock.getGroupBy();
                if (sqlSelectGroupByClause != null) {
                    String havingStr = null;
                    SQLExpr havingSQL = sqlSelectGroupByClause.getHaving();
                    if (havingSQL != null) {
                        havingStr = handleSelectWhere(havingSQL, true);
                    }
                    List<SQLExpr> groupByExpr = sqlSelectGroupByClause.getItems();
                    for (int i = 0; i < groupByExpr.size(); i++) {
                        SQLIdentifierExpr groupBySqlIdentifierExpr = (SQLIdentifierExpr) groupByExpr.get(i);
                        groupByList.add(groupBySqlIdentifierExpr.getName());
                    }
                    groupByStr = dslSelectHandler.groupBy(fields, groupByList, havingStr);
                }

                //Handle limit
                SQLLimit sqlLimit = mySqlSelectQueryBlock.getLimit();
                if (sqlLimit != null) {
                    SQLIntegerExpr sqlLimitOffset = (SQLIntegerExpr) sqlLimit.getOffset();
                    SQLIntegerExpr sqlLimitRowCount = (SQLIntegerExpr) sqlLimit.getRowCount();

                    if (sqlLimitOffset == null) {
                        queryFrom = "0";
                    } else {
                        queryFrom = sqlLimitOffset.getNumber().toString();
                    }
                    querySize = sqlLimitRowCount.getNumber().toString();
                }

                // Handle order
                SQLOrderBy sqlOrderBy = mySqlSelectQueryBlock.getOrderBy();
                if (sqlOrderBy != null) {
                    List<ImmutablePair<String, String>> orderByList = new ArrayList<>();
                    List<SQLSelectOrderByItem> sqlSelectOrderByItems = sqlOrderBy.getItems();
                    for (int i = 0; i < sqlSelectOrderByItems.size(); i++) {
                        SQLSelectOrderByItem sqlSelectOrderByItem = sqlSelectOrderByItems.get(i);
                        SQLIdentifierExpr orderBySqlIdentifierExpr = (SQLIdentifierExpr) sqlSelectOrderByItem.getExpr();
                        SQLOrderingSpecification sqlOrderingSpecification = sqlSelectOrderByItem.getType();
                        orderByList.add(new ImmutablePair<>(orderBySqlIdentifierExpr.getName(), sqlOrderingSpecification.name));
                    }
                    orderByStr = dslSelectHandler.orderBy(orderByList);
                }
                result = dslSelectHandler.root(queryMapStr, groupByStr, orderByStr, queryFrom, querySize);

            }
        } catch (Exception e) {
            throw new RuntimeException("SQL convert DSL failed. Please check the SQL", e);
        }
        return new ImmutablePair<>(result, esType);
    }


    public String handleSelectWhere(SQLExpr sqlExpr, boolean topLevel) throws Exception {
        if (sqlExpr instanceof SQLBinaryOpExpr) {
            SQLBinaryOpExpr binaryOpExpr = (SQLBinaryOpExpr) sqlExpr;
            SQLBinaryOperator sqlBinaryOperator = binaryOpExpr.getOperator();

            switch (sqlBinaryOperator) {
                case BooleanOr:
                    return handleSelectWhereOrExpr(binaryOpExpr);
                case BooleanAnd:
                    return handleSelectWhereAndExpr(binaryOpExpr);
                case IsNot:
                case Is:
                default:
                    return handleSelectWhereComparisonExpr(binaryOpExpr, topLevel);
            }
        } else if (sqlExpr instanceof SQLInListExpr) {
            SQLInListExpr inaryOpExpr = (SQLInListExpr) sqlExpr;
            return handleSelectWhereComparisonInExpr(inaryOpExpr, topLevel);
        } else if (sqlExpr instanceof SQLBetweenExpr) {
            SQLBetweenExpr betweenExpr = (SQLBetweenExpr) sqlExpr;
            return handleSelectWhereComparisonBetweenExpr(betweenExpr, topLevel);
        }
        return dslSelectHandler.queryAll(); // for null
    }

    public String handleSelectWhereAndExpr(SQLBinaryOpExpr sqlBinaryOpExpr) throws Exception {

        ImmutablePair<String, String> immutablePair = handleLeftAndRight(sqlBinaryOpExpr);

        String resultStr = dslSelectHandler.andExpr(immutablePair.getLeft(), immutablePair.getRight());

        if (sqlBinaryOpExpr.getParent() instanceof SQLBinaryOpExpr) {
            SQLBinaryOpExpr sqlBinaryOpExpr1 = (SQLBinaryOpExpr) sqlBinaryOpExpr.getParent();
            if (sqlBinaryOpExpr1.getOperator() == BooleanAnd) {
                return resultStr;
            }
        }

        resultStr = dslSelectHandler.andRoot(resultStr);
        return resultStr;
    }

    public String handleSelectWhereOrExpr(SQLBinaryOpExpr sqlBinaryOpExpr) throws Exception {

        ImmutablePair<String, String> immutablePair = handleLeftAndRight(sqlBinaryOpExpr);

        String resultStr = dslSelectHandler.orExpr(immutablePair.getLeft(), immutablePair.getRight());

        if (sqlBinaryOpExpr.getParent() instanceof SQLBinaryOpExpr) {
            SQLBinaryOpExpr sqlBinaryOpExpr1 = (SQLBinaryOpExpr) sqlBinaryOpExpr.getParent();
            if (sqlBinaryOpExpr1.getOperator() == BooleanOr) {
                return resultStr;
            }
        }

        resultStr = dslSelectHandler.orRoot(resultStr);
        return resultStr;
    }

    private ImmutablePair<String, String> handleLeftAndRight(SQLBinaryOpExpr sqlBinaryOpExpr) throws Exception {
        SQLExpr leftExpr = sqlBinaryOpExpr.getLeft();
        SQLExpr rightExpr = sqlBinaryOpExpr.getRight();

        String leftStr = handleSelectWhere(leftExpr, false);
        String rightStr = handleSelectWhere(rightExpr, false);
        return new ImmutablePair<>(leftStr, rightStr);
    }

    public String handleSelectWhereComparisonBetweenExpr(SQLBetweenExpr sqlBetweenExpr, boolean topLevel) {
        String resultStr;

        SQLIdentifierExpr sqlIdentifierExpr = (SQLIdentifierExpr) sqlBetweenExpr.getTestExpr();
        String field = sqlIdentifierExpr.getName();

        String beginValue = getValue(sqlBetweenExpr.getBeginExpr());
        String endValue = getValue(sqlBetweenExpr.getEndExpr());

        resultStr = dslSelectHandler.betweenExpr(field, beginValue, endValue);

        if (topLevel) {
            resultStr = dslSelectHandler.betweenRoot(resultStr);
        }
        return resultStr;
    }

    private String getValue(SQLExpr sqlExpr) {
        String value = "";
        if (sqlExpr instanceof SQLIntegerExpr) {
            SQLIntegerExpr sqlIntegerExpr = (SQLIntegerExpr) sqlExpr;
            value = String.valueOf(sqlIntegerExpr.getNumber());
        } else if (sqlExpr instanceof SQLCharExpr) {
            SQLCharExpr sqlCharExpr = (SQLCharExpr) sqlExpr;
            value = dslSelectHandler.strQuotes() + sqlCharExpr.getText() + dslSelectHandler.strQuotes();
        } else if (sqlExpr instanceof SQLNumberExpr) {
            SQLNumberExpr sqlNumberExpr = (SQLNumberExpr) sqlExpr;
            value = String.valueOf(sqlNumberExpr.getNumber());
        } else if (sqlExpr instanceof SQLNullExpr) {
            value = null;
        }
        return value;
    }

    public String handleSelectWhereComparisonInExpr(SQLInListExpr sqlInListExpr, boolean topLevel) {

        String resultStr;

        SQLIdentifierExpr sqlIdentifierExpr = (SQLIdentifierExpr) sqlInListExpr.getExpr();
        String field = sqlIdentifierExpr.getName();
        List<SQLExpr> list = sqlInListExpr.getTargetList();
        List<String> values = new ArrayList<>();

        list.forEach(v -> {
            values.add(getValue(v));
        });

        if (sqlInListExpr.isNot()) {
            resultStr = dslSelectHandler.notInExpr(field, values);
        } else {
            resultStr = dslSelectHandler.inExpr(field, values);
        }

        if (topLevel) {
            resultStr = dslSelectHandler.inRoot(resultStr);
        }
        return resultStr;
    }

    public String handleSelectWhereComparisonExpr(SQLBinaryOpExpr sqlExpr, boolean topLevel) {

        String resultStr = "";

        SQLIdentifierExpr sqlIdentifierExpr = (SQLIdentifierExpr) sqlExpr.getLeft();
        String field = sqlIdentifierExpr.getName();
        String value = getValue(sqlExpr.getRight());

        switch (sqlExpr.getOperator()) {
            // >=
            case GreaterThanOrEqual:
                resultStr = dslSelectHandler.greaterThanOrEqual(field, value);
                break;
            // <=
            case LessThanOrEqual:
                resultStr = dslSelectHandler.lessThanOrEqual(field, value);
                break;
            // =
            case Equality:
                resultStr = dslSelectHandler.equality(field, value);
                break;
            // >
            case GreaterThan:
                resultStr = dslSelectHandler.greaterThan(field, value);
                break;
            // <
            case LessThan:
                resultStr = dslSelectHandler.lessThan(field, value);
                break;
            // !=
            case NotEqual:
                resultStr = dslSelectHandler.notEqual(field, value);
                break;
            // like
            case Like:
                value = StringUtil.escapeQueryChars(value);
                resultStr = dslSelectHandler.like(field, value);
                break;
            // not like
            case NotLike:
                resultStr = dslSelectHandler.notLike(field, value);
                break;
            case Is:
                resultStr = dslSelectHandler.is(field, value);
                break;
            case IsNot:
                resultStr = dslSelectHandler.isNot(field, value);
                break;
        }

        if (topLevel) {
            resultStr = dslSelectHandler.comparisonRoot(resultStr);
        }

        return resultStr;
    }

    private List<ImmutableTriple<String, String, String>> getSelectFields(List<SQLSelectItem> selectItems) throws Exception {
        List<ImmutableTriple<String, String, String>> fields = new ArrayList<>(); // <column， alias, method>
        for (int i = 0; i < selectItems.size(); i++) {
            SQLSelectItem sqlSelectItem = selectItems.get(i);
            String alias = sqlSelectItem.getAlias();
            SQLExpr sqlExpr = sqlSelectItem.getExpr();
            if (sqlExpr instanceof SQLAggregateExpr) {
                SQLAggregateExpr aggregateExpr = (SQLAggregateExpr) sqlExpr;
                String methodName = aggregateExpr.getMethodName();
                List<SQLExpr> childSQLExprs = aggregateExpr.getArguments();
                // 自取第一个，且默认字段处理，不处理 函数
                if (childSQLExprs == null || childSQLExprs.isEmpty()) {
                    throw new RuntimeException("aggregateExpr's argument is null");
                }
                SQLExpr childSQLExpr = childSQLExprs.get(0);
                if (!(childSQLExpr instanceof SQLIdentifierExpr) && !(childSQLExpr instanceof SQLAllColumnExpr)) {
                    throw new RuntimeException("aggregateExpr's argument is not a SQLIdentifierExpr");
                }
                if (childSQLExpr instanceof SQLAllColumnExpr) {
                    fields.add(new ImmutableTriple<>("*", alias, methodName));
                } else {
                    SQLIdentifierExpr childIdExpr = (SQLIdentifierExpr) childSQLExpr;
                    String name = childIdExpr.getName();
                    fields.add(new ImmutableTriple<>(name, alias, methodName));
                }
            } else if (sqlExpr instanceof SQLMethodInvokeExpr) {
                if (StringUtils.isEmpty(alias)) {
                    throw new RuntimeException("method must have a alias");
                }
                // sql 为定义的func
                // 此处处理 stats、 entendedStats、 percentiles 等
                // 添加 filter 函数
                SQLMethodInvokeExpr methodInvokeExpr = (SQLMethodInvokeExpr) sqlExpr;
                String methodName = methodInvokeExpr.getMethodName();
                SQLExpr argSqlExpr = methodInvokeExpr.getArguments().get(0);
                // toString 取出method里面的值， 此处可能是expr可能是column
                // 处理内部逻辑
                String argStr = null;
                if (argSqlExpr instanceof SQLIdentifierExpr) {
                    argStr = ((SQLIdentifierExpr) argSqlExpr).getName();
                } else if (argSqlExpr instanceof SQLBinaryOpExpr) {
                    argStr = handleSelectWhere(argSqlExpr, true);
                } else {
                    // defalut toString
                    argStr = argSqlExpr.toString();
                }
                fields.add(new ImmutableTriple<>(argStr, alias, methodName));
            } else if (sqlExpr instanceof SQLAllColumnExpr) {
                fields.add(new ImmutableTriple<>("*", alias, ""));
            } else {
                SQLIdentifierExpr sqlIdentifierExpr = (SQLIdentifierExpr) sqlExpr;
                fields.add(new ImmutableTriple<>(sqlIdentifierExpr.getName(), alias, ""));
            }
        }
        return fields;
    }


}
