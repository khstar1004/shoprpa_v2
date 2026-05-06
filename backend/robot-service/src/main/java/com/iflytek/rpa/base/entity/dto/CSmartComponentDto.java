package com.iflytek.rpa.base.entity.dto;

import static com.iflytek.rpa.robot.constants.RobotConstant.EDIT_PAGE;

import com.alibaba.fastjson.JSONObject;
import java.util.List;
import javax.validation.constraints.NotBlank;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
public class CSmartComponentDto {

    /**
     * 가능컴포넌트Id
     */
    private String smartId;

    /**
     * 컴포넌트버전
     */
    private Integer version;

    /**
     * 컴포넌트유형 web_auto | data_process
     */
    private String smartType;

    /**
     * 컴포넌트필요저장의데이터
     */
    private SmartDetail detail;

    /**
     * 봇Id
     */
    private String robotId;

    /**
     * 봇버전
     */
    private Integer robotVersion;

    /**
     * 현재방식
     */
    @NotBlank(message = "실행위치비워 둘 수 없습니다")
    private String mode = EDIT_PAGE;

    @Data
    @Accessors(chain = true)
    public static class SmartDetail {
        /**
         * 버전목록
         */
        private List<JSONObject> versionList;
    }
}