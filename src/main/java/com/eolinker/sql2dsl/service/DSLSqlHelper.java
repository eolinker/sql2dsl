package com.eolinker.sql2dsl.service;

import com.alibaba.fastjson.JSONObject;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;


/**
 * 一个简单的json转sql工具，使用json构建where语句，selectField和groupByList选择传入
 * 目前不支持having
 */
public class DSLSqlHelper {

    public static String json2sql(String jsonStr, String index) {
        return json2sql(jsonStr, null, index, null);
    }

    public static String json2sql(String jsonStr, List<String> selectFields,String index) {
        return json2sql(jsonStr, selectFields, index, null);
    }

    public static String json2sql(String jsonStr, String index, List<String> groupByList) {
        return json2sql(jsonStr, null, index, groupByList);
    }

    public static String json2sql(String jsonStr, List<String> selectFields, String index, List<String> groupByList) {
        JSONObject obj = JSONObject.parseObject(jsonStr);
        StringBuilder sb = new StringBuilder("SELECT");
        if (selectFields != null && !selectFields.isEmpty()) {
            String fieldStr = selectFields.stream().collect(Collectors.joining(","));
            sb.append(" ").append(fieldStr);
        } else {
            sb.append(" *");    // for all
        }
        sb.append(" FROM " + index);
        Set<String> set = obj.keySet();
        if (set.size() > 0) {
            String offset = "offset";
            String limit = "limit";
            sb.append(" WHERE ");
            int init = 0;
            for (String key : set) {
                Object value = obj.get(key);
                if (key.equals(offset) || key.equals(limit) || value.equals("")) {
                    continue;
                }

                if (init > 0) {
                    sb.append(" and ");
                }

                sb.append(key + " " + decorate(value));
                init++;
            }

            // handle group by
            if (groupByList!=null && !groupByList.isEmpty()) {
                String groupByStr = groupByList.stream().collect(Collectors.joining(","));
                sb.append(" GROUP BY ").append(groupByStr);
            }

            // handle limit
            Object offsetObj = obj.get(offset);
            Object limitObj = obj.get(limit);

            if (limitObj != null) {
                if (offsetObj == null) {
                    sb.append(" LIMIT " + limitObj);
                } else {
                    sb.append(" LIMIT " + offsetObj + "," + limitObj);
                }
            }
        }
        return sb.toString();
    }

    private static Object decorate(Object obj) {
        if (obj instanceof String) {
            return "\"" + obj.toString() + "\"";
        } else if (obj instanceof List) {
            List list = (List) obj;
            String listStr = list.toString();
            return " IN (" + listStr.substring(1, listStr.length() - 1) + ")";
        }
        return obj;
    }
}
