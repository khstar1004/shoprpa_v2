package com.iflytek.rpa.monitor.entity;

import java.math.BigDecimal;
import java.util.Date;
import javax.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

@Data
public class RobotMonitorDto {
    @NotBlank(message = "봇ID비워 둘 수 없습니다")
    private String robotId;

    @NotBlank(message = "중지시간비워 둘 수 없습니다")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private Date deadline;

    private Integer version;

    private Long executeTotal = 0L;

    private Long executeRunning = 0L;

    private BigDecimal executeRunningRate = new BigDecimal(0);
    /**
     * 실행성공데이터
     */
    private Long executeSuccess = 0L;

    private BigDecimal executeSuccessRate = new BigDecimal(0);

    /**
     * 실행실패데이터
     */
    private Long executeFail = 0L;

    private BigDecimal executeFailRate = new BigDecimal(0);

    /**
     * 실행중중지데이터
     */
    private Long executeAbort = 0L;

    private BigDecimal executeAbortRate = new BigDecimal(0);
}