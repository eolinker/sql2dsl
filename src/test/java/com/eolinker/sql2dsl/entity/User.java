package com.eolinker.sql2dsl.entity;

import lombok.Data;

@Data
public class User {
    private Integer id;
    private String name;
    private Integer sex;
    private Integer age;
    private String dept;
    private String className;
}
