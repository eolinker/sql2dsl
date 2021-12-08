package com.eolinker.sql2dsl.dto;

import com.alibaba.fastjson.annotation.JSONField;
import lombok.Data;

import java.util.List;

@Data
public class ScoreDTO extends BaseDTO{

    @JSONField(name = "chinese>=")
    private Float chineseStart;
    @JSONField(name = "chinese<=")
    private Float chineseEnd;
    @JSONField(name = "math>=")
    private Float mathStart;
    @JSONField(name = "math<=")
    private Float mathEnd;
    @JSONField(name = "english>=")
    private Float englishStart;
    @JSONField(name = "english<=")
    private Float englishEnd;
    @JSONField(name = "total_score>=")
    private Float totalScoreStart;
    @JSONField(name = "total_score<=")
    private Float totalScoreEnd;

    @JSONField(name = "user_id=")
    private Integer userId;

    // for in query
    @JSONField(name = "dept")
    private List<String> depts;
    @JSONField(name = "class_name")
    private List<String> classNames;
}
