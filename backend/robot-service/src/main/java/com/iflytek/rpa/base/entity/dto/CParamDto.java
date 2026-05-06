package com.iflytek.rpa.base.entity.dto;

import com.iflytek.rpa.utils.StringUtils;
import javax.validation.constraints.AssertTrue;
import javax.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CParamDto {

    private String id;

    /**
     * 입력출력
     */
    @NotNull(message = "varDirection할 수 없음로null")
    private int varDirection;

    /**
     * 매개변수이름
     */
    @NotNull(message = "varName할 수 없음로null")
    private String varName;

    /**
     * 매개변수유형
     */
    @NotNull(message = "varType할 수 없음로null")
    private String varType;

    /**
     * 값
     */
    private String varValue;

    /**
     * 매개변수설명
     */
    private String varDescribe;

    /**
     * 봇id
     */
    @NotNull(message = "robotId할 수 없음로null")
    private String robotId;

    /**
     * 봇버전
     */
    // 봇버전가능으로로null, 원인로추가발송에서상태
    private Integer robotVersion;

    /**
     * 프로세스id
     */
    private String processId;

    /**
     * python모듈id
     */
    private String moduleId;

    @AssertTrue(message = "processId및moduleId할 수 없음시로null")
    public boolean isProcessIdOrModuleIdNotBothNull() {
        return !StringUtils.isEmpty(this.processId) || !StringUtils.isEmpty(this.moduleId);
    }
}