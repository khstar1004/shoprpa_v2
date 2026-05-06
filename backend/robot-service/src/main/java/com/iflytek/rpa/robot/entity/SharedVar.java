package com.iflytek.rpa.robot.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * 공유 변수정보(SharedVar)유형
 *
 * @author jqfang3
 * @since 2025-07-21
 */
@Data
public class SharedVar implements Serializable {
    private static final long serialVersionUID = 221473413657231317L;

    /**
     * 기본 키id
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 테넌트id
     */
    private String tenantId;

    /**
     * 공유 변수이름
     */
    private String sharedVarName;
    /**
     * 공유 변수유형: text/password/array/group
     */
    private String sharedVarType;
    /**
     * 사용상태: 1사용, 0사용 안 함
     */
    private Integer status;

    /**
     * 변수설명
     */
    private String remark;

    /**
     * 모듈ID
     */
    private String deptId;

    /**
     * 가능사용계정유형(all/dept/select): 모든사람: all, 모듈모든사람: dept, 지정사람: select
     */
    private String usageType;

    /**
     * 생성자id
     */
    private String creatorId;

    /**
     * 생성 시간
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createTime;

    /**
     * 수정자id
     */
    private String updaterId;

    /**
     * 수정 시간
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date updateTime;

    /**
     * 삭제 여부 0: 삭제되지 않음, 1: 삭제됨
     */
    private Integer deleted;
}