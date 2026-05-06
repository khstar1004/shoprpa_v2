package com.iflytek.rpa.agent.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import java.io.Serializable;
import java.util.Date;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * RPA Agent매칭테이블유형
 */
@Data
@Accessors(chain = true)
public class Agent implements Serializable {

    /**
     * 증가기본 키
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * RPA Agent ID
     */
    private String agentId;

    /**
     * Agent매칭정보(초과길이텍스트)
     */
    private String content;

    /**
     * 삭제식별자: 0-삭제되지 않음, 1-삭제됨
     */
    private Integer deleted;

    /**
     * 생성사람ID
     */
    private String creatorId;

    /**
     * 생성 시간, 삽입시완료
     */
    private Date createTime;

    /**
     * 업데이트사람ID
     */
    private String updaterId;

    /**
     * 수정 시간, 업데이트시업데이트
     */
    private Date updateTime;
}