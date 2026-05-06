package com.iflytek.rpa.auth.blacklist.dto;

import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 이름단일조회 DTO
 *
 * @author system
 * @date 2025-12-16
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BlacklistQueryDto implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 사용자ID(가능선택)
     */
    private String userId;

    /**
     * 사용자명(가능선택)
     */
    private String username;

    /**
     * 상태(가능선택)1:중, 0:완료해제
     */
    private Integer status;

    /**
     * 코드
     */
    private Integer pageNum = 1;

    /**
     * 매수
     */
    private Integer pageSize = 10;
}