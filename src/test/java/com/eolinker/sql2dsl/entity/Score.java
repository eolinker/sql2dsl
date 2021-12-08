package com.eolinker.sql2dsl.entity;

import lombok.Data;

@Data
public class Score {

    private Float chinese;
    private Float math;
    private Float english;
    private Float totalScore;

    private Integer userId;

    private String dept;
    private String className;

}
