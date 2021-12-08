package com.eolinker.sql2dsl.service;

import com.alibaba.druid.sql.ast.SQLStatement;
import com.alibaba.druid.sql.ast.statement.SQLDeleteStatement;
import com.alibaba.druid.sql.ast.statement.SQLInsertStatement;
import com.alibaba.druid.sql.ast.statement.SQLSelectStatement;
import com.alibaba.druid.sql.ast.statement.SQLUpdateStatement;
import com.alibaba.druid.sql.dialect.mysql.parser.MySqlStatementParser;
import com.alibaba.druid.sql.parser.SQLStatementParser;
import com.eolinker.sql2dsl.service.impl.ESSelectHandler;
import com.eolinker.sql2dsl.service.impl.MySqlSelectHandler;
import org.apache.commons.lang3.tuple.ImmutablePair;


public class DSLConvert {

    public ImmutablePair<String, String> convertSelect(String sql, DSLSelectHandler dslSelectHandler) throws Exception {
        // 新建 MySQL Parser
        SQLStatementParser parser = new MySqlStatementParser(sql);
        // 使用Parser解析生成AST，这里SQLStatement就是AST
        SQLStatement statement = parser.parseStatement();
        SelectHandler handler = new SelectHandler();
        handler.setDslSelectHandler(dslSelectHandler);
        return handler.handleSelect((SQLSelectStatement) statement);
    }


    public ImmutablePair<String, String> convert(String sql) throws Exception {
        // 新建 MySQL Parser
        SQLStatementParser parser = new MySqlStatementParser(sql);
        // 使用Parser解析生成AST，这里SQLStatement就是AST
        SQLStatement statement = parser.parseStatement();

        if (statement instanceof SQLSelectStatement) {
            SelectHandler handler = new SelectHandler();
            ESSelectHandler esSelectDSLHandler = new ESSelectHandler();
            handler.setDslSelectHandler(esSelectDSLHandler);
            return handler.handleSelect((SQLSelectStatement) statement);
        } else if (statement instanceof SQLInsertStatement) {
            //todo
        } else if (statement instanceof SQLUpdateStatement) {
            //todo
        } else if (statement instanceof SQLDeleteStatement) {
            //todo
        }
        return null;
    }
}
