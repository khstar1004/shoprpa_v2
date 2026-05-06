package com.iflytek.rpa.component.entity.dto;

import java.util.List;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 봇컴포넌트데이터입출력객체
 *
 * @author makejava
 * @since 2024-12-19
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ComponentRobotBlockDto {

    /**
     * 봇id
     */
    @NotBlank(message = "봇ID비워 둘 수 없습니다")
    private String robotId;

    /**
     * 봇버전
     */
    @NotNull(message = "봇버전비워 둘 수 없습니다")
    private Integer robotVersion;

    /**
     * 컴포넌트id목록
     */
    @NotNull(message = "컴포넌트ID목록은 비워 둘 수 없습니다")
    private List<String> componentIds;
}