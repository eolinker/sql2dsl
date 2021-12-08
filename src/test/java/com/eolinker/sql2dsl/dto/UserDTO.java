package com.eolinker.sql2dsl.dto;

import com.alibaba.fastjson.annotation.JSONField;
import lombok.Data;

import java.util.List;

@Data
public class UserDTO extends BaseDTO{

    @JSONField(name = "sex=")
    private Integer sex;
    @JSONField(name = "age>=")
    private Integer startAge;
    @JSONField(name = "age<=")
    private Integer endAge;
    @JSONField(name = "dept=")
    private String dept;

    // for in query
    @JSONField(name = "dept")
    private List<String> depts;

    // for like query
    @JSONField(name = "name like ")
    private String nameQuery;
}
