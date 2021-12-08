package com.eolinker.sql2dsl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.eolinker.sql2dsl.dto.ScoreDTO;
import com.eolinker.sql2dsl.dto.UserDTO;
import com.eolinker.sql2dsl.service.DSLConvert;
import com.eolinker.sql2dsl.service.DSLSqlHelper;
import com.eolinker.sql2dsl.service.impl.ESSelectHandler;
import com.eolinker.sql2dsl.service.impl.MySqlSelectHandler;
import org.apache.commons.lang3.tuple.ImmutablePair;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

public class TestDemo {

    ESSelectHandler esSelectHandler = new ESSelectHandler();
    MySqlSelectHandler mySqlSelectHandler = new MySqlSelectHandler();

    @Test
    public void normalTest() {
        //https://github.com/alibaba/druid/wiki/SQL-Parser
        DSLConvert dslConvert = new DSLConvert();

        String sql = "select * from user where id = 1";

        // normal sql
        sql = "select * from user where sex = 1 and age >= 18";
        sql = "select * from user where sex = 1 or age < 18";
        sql = "select * from user where dept in ('A','B','C')";
        sql = "select * from user where dept not in ('A')";
        sql = "select * from user where (sex = 1 and age > 18) or (sex = 0 and age > 18)";
        sql = "select * from user where name like 'lucy'";// 不需要带通配
        sql = "select * from user where name not like 'lucy'"; // 1
        sql = "select * from user order by id desc, name asc limit 0,10";
        sql = "select * from user where mobile is null";
        sql = "select * from user where mobile is not null";
        // group by sql
        sql = "select * from user group by dept,class,level";
        sql = "select * from user group by dept,class having sex = 1 and age >18";
        sql = "select count(*) userTotal from user group by class";
        sql = "select min(math) min_math,min(chinese) min_ch,max(english) max_eng, avg(total_score) avg_score from score group by class";
        sql = "select extended_stats(total_score) stat_score,percentiles(total_score) pc_score from score group by class";
        sql = "select filter(sex=1 and age<14) boy_total, filter(sex=2 and age<14) girl_total, filter(sex=1 and age>=14) man_total, filter(sex=2 and age>=14) women_total" +
                " from user group by dept";
        try {
            ImmutablePair<String, String> immutablePair = dslConvert.convertSelect(sql, esSelectHandler);
//            ImmutablePair<String, String> immutablePair = dslConvert.convert(sql);
            System.out.println("table: " + immutablePair.getRight());
            System.out.println("dsl: " + immutablePair.getLeft());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testJavaBean2normalSQL() {
        String table = "user";  // or es_index
        UserDTO userDTO = new UserDTO();
        userDTO.setSex(1);
        userDTO.setNameQuery("chen");
        userDTO.setDepts(Arrays.asList("A", "B", "C"));
        userDTO.setOffset(0);
        userDTO.setLimit(10);

        String whereJson = JSON.toJSONString(userDTO);
        System.out.println("where: " + whereJson);

        String sql = DSLSqlHelper.json2sql(whereJson, table);
        System.out.println("sql: " + sql);
    }

    @Test
    public void testJavaBean2GroupBySQL() {
        String table = "score";  // or es_index
        ScoreDTO scoreDTO = new ScoreDTO();
        scoreDTO.setChineseStart(60f);
        scoreDTO.setMathStart(60f);
        scoreDTO.setEnglishStart(60f);
        scoreDTO.setTotalScoreStart(180f);

        List<String> selectFieldList = Arrays.asList("count(*)", "max(chinese)", "max(math)", "max(english)");
        List<String> groupByList = Arrays.asList("dept");

        String whereJson = JSONObject.toJSONString(scoreDTO);
        System.out.println("where: " + whereJson);

        String sql = DSLSqlHelper.json2sql(whereJson, selectFieldList, table, groupByList);
        System.out.println("sql: " +sql);

    }
}
