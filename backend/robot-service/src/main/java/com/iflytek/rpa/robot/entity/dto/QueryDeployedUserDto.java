package com.iflytek.rpa.robot.entity.dto;

import javax.validation.constraints.NotBlank;
import lombok.Data;

/**
 * @author mjren
 * @date 2025-07-15 10:12
 * @copyright Copyright (c) 2025 mjren
 */
@Data
public class QueryDeployedUserDto {

    @NotBlank(message = "휴대폰 번호또는이름비워 둘 수 없습니다")
    private String keyword;

    @NotBlank(message = "봇id비워 둘 수 없습니다")
    private String robotId;

    private Integer pageNo;

    private Integer pageSize;
}