package com.iflytek.rpa.terminal.entity.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import lombok.Data;

/**
 * @author mjren
 * @date 2025-06-16 15:10
 * @copyright Copyright (c) 2025 mjren
 */
@Data
public class BeatDto {

    /**
     * 단말일식별자, 예준비mac주소
     */
    @NotBlank(message = "단말id비워 둘 수 없습니다")
    private String terminalId;

    /**
     * 현재상태, 사용계획종료상태, 있음상태, 실행중busy, 빈free
     */
    @NotBlank(message = "준비상태비워 둘 수 없습니다")
    private String status;

    /**
     * 여부스케줄링방식 (0: 아니요, 1: 예)
     */
    @NotNull(message = "단말방식비워 둘 수 없습니다")
    private Integer isDispatch;

    /**
     * CPU사용(분)
     */
    private Integer cpu;

    /**
     * 메모리사용(분)
     */
    private Integer memory;

    /**
     * 하드사용(분)
     */
    private Integer disk;
}