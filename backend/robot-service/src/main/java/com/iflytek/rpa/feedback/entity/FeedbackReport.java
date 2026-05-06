package com.iflytek.rpa.feedback.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * 반대테이블유형
 *
 * @author system
 * @since 2024-12-15
 */
@Data
@TableName("feedback_report")
public class FeedbackReport implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 기본 키ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 일번호
     */
    private String reportNo;

    /**
     * 사용자로그인이름
     */
    private String username;

    /**
     * 제목분유형목록(JSON형식)
     * 형식: {"내용설치전체유형":["완료법/정보","내용"],"공가능유형":["완료프로세스코드오류, 불가실행"]}
     */
    private String categories;

    /**
     * 제목설명
     */
    private String description;

    /**
     * 이미지파일ID목록(분)
     */
    private String imageIds;

    /**
     * 생성 시간
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createTime;

    /**
     * 수정 시간
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date updateTime;

    /**
     * 삭제로그 0:삭제되지 않음 1:삭제됨
     */
    private Integer deleted;

    /**
     * 여부완료관리 0:미완료관리 1:완료관리
     */
    private Integer processed;
}