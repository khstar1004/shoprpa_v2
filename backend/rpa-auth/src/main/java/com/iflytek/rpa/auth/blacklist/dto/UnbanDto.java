package com.iflytek.rpa.auth.blacklist.dto;

import java.io.Serializable;
import javax.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 해제 DTO
 *
 * @author system
 * @date 2025-12-16
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UnbanDto implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 사용자ID
     */
    @NotBlank(message = "사용자 ID는 비워 둘 수 없습니다")
    private String userId;
}