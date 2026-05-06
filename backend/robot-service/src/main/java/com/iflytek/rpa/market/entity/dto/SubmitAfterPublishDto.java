package com.iflytek.rpa.market.entity.dto;

import javax.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 클라이언트발송버전후제출위신청DTO
 */
@Data
public class SubmitAfterPublishDto extends ReleaseApplicationDto {
    /**
     * 봇이름, 비워 둘 수 없습니다
     */
    @NotBlank(message = "봇이름비워 둘 수 없습니다")
    private String name;

    private String creatorId;

    private String tenantId;
}