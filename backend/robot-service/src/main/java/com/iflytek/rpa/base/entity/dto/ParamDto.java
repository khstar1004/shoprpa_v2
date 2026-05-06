package com.iflytek.rpa.base.entity.dto;

import javax.validation.constraints.NotNull;
import lombok.Data;

/**
 * @author mjren
 * @date 2025-04-03 11:01
 * @copyright Copyright (c) 2025 mjren
 */
@Data
public class ParamDto {

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
     * 프로세스id
     */
    @NotNull(message = "processId할 수 없음로null")
    private String processId;
}