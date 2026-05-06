package com.iflytek.rpa.astronAgent.entity.dto;

import com.alibaba.fastjson.annotation.JSONField;
import com.iflytek.rpa.base.entity.dto.ParamDto;
import java.util.List;
import lombok.Data;

/**
 * 복사봇DTO
 */
@Data
public class CopyRobotResponseDto {

    /**
     * 복사후의봇id
     */
    @JSONField(name = "robotId")
    private String robotId;

    /**
     * 봇이름
     */
    private String name;

    /**
     * 영어이름
     */
    @JSONField(name = "english_name")
    private String englishName;

    /**
     * 설명
     */
    private String description;

    /**
     * 버전
     */
    private String version;

    /**
     * 상태
     */
    private Integer status;

    /**
     * 매개변수목록
     */
    private List<ParamDto> parameters;
}