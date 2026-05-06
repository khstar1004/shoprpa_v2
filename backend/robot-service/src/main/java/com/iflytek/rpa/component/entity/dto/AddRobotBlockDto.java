package com.iflytek.rpa.component.entity.dto;

import javax.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 추가봇기록데이터입출력객체
 *
 * @author makejava
 * @since 2024-12-19
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class AddRobotBlockDto {

    /**
     * 봇id
     */
    @NotBlank(message = "봇ID비워 둘 수 없습니다")
    private String robotId;

    /**
     * 발송요청 의위치
     */
    @NotBlank(message = "mode비워 둘 수 없습니다")
    private String mode;

    /**
     * 봇버전
     */
    private Integer robotVersion;

    /**
     * 컴포넌트id
     */
    @NotBlank(message = "컴포넌트ID비워 둘 수 없습니다")
    private String componentId;
}