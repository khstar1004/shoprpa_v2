package com.iflytek.rpa.auth.blacklist.dto;

import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 사용자 차단 목록 조회 DTO
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
     * 사용자ID(선택)
     */
    private String userId;

    /**
     * 사용자명(선택)
     */
    private String username;

    /**
     * 상태(선택): 1=차단 중, 0=해제됨
     */
    private Integer status;

    /**
     * 페이지 번호
     */
    private Integer pageNum = 1;

    /**
     * 페이지 크기
     */
    private Integer pageSize = 10;
}
