package com.iflytek.rpa.auth.core.entity;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;

/**
 * 의모듈DTO - 패키지필요필드으로가능
 * @author AI Assistant
 * @date 2025-09-24
 */
@Data
public class SimpleDeptTreeNodeDto {

    /**
     * 모듈ID
     */
    private String id;

    /**
     * 모듈이름
     */
    private String name;

    /**
     * 모듈ID
     */
    private String pid;

    /**
     * 모듈사람데이터
     */
    private Long userNum;

    /**
     * 모듈사람이름
     */
    private String userName;

    /**
     * 조직ID(대기id필드, 로완료내용프론트엔드)
     */
    private String orgId;

    /**
     * 모듈목록
     */
    private List<SimpleDeptTreeNodeDto> nodes = new ArrayList<>();

    public SimpleDeptTreeNodeDto() {}

    public SimpleDeptTreeNodeDto(String id, String name, String pid, Long userNum, String userName) {
        this.id = id;
        this.orgId = id; // orgId및id보관일
        this.name = name;
        this.pid = pid;
        this.userNum = userNum;
        this.userName = userName;
    }
}