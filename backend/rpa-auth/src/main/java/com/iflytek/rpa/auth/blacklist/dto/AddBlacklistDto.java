package com.iflytek.rpa.auth.blacklist.dto;

import java.io.Serializable;
import javax.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 추가이름단일 DTO
 *
 * @author system
 * @date 2025-12-16
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AddBlacklistDto implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 사용자ID
     */
    @NotBlank(message = "사용자 ID는 비워 둘 수 없습니다")
    private String userId;

    /**
     * 사용자명
     */
    @NotBlank(message = "사용자명은 비워 둘 수 없습니다")
    private String username;

    /**
     * 원인
     */
    @NotBlank(message = "원인비워 둘 수 없습니다")
    private String reason;
}