package com.iflytek.rpa.market.entity.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import lombok.Data;

/**
 * @author mjren
 * @date 2025-07-02 9:44
 * @copyright Copyright (c) 2025 mjren
 */
@Data
public class AuditApplicationDto {

    private String id;

    @NotBlank(message = "봇id비워 둘 수 없습니다")
    private String robotId;

    @NotNull(message = "봇버전비워 둘 수 없습니다")
    private Long robotVersion;

    /**
     * 신청유형: release(위)/use(사용)
     */
    @NotBlank(message = "신청유형비워 둘 수 없습니다")
    private String applicationType;

    /**
     * 상태: 대기검토pending, 완료통신경과approved, 통과하지 못했습니다rejected, 완료판매canceled
     */
    @NotBlank(message = "검토결과비워 둘 수 없습니다")
    private String status;

    /**
     * 검토의비밀단계red,green,yellow
     */
    private String securityLevel;

    /**
     * 허용사용의모듈ID목록
     */
    private String allowedDept;

    /**
     * 사용제한(week,month,quarter)
     */
    private String expireTime;

    /**
     * 검토
     */
    private String auditOpinion;

    /**
     * 선택색상비밀단계시, 후업데이트발송버전여부통신경과
     */
    private Short defaultPass;
}