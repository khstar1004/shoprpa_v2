package com.iflytek.rpa.common.feign.entity;

import com.alibaba.fastjson.annotation.JSONField;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * 단말봇테이블(RobotExecute)유형 - 사용Feign입출력
 *
 * @author system
 */
@Data
public class RobotExecute implements Serializable {
    private static final long serialVersionUID = -49733269650418210L;
    /**
     * 기본 키id
     */
    private Long id;
    /**
     * 봇일id, 가져오기의사용id
     */
    @JSONField(name = "robot_id")
    private String robotId;
    /**
     * 현재이름문자, 사용목록 
     */
    private String name;
    /**
     * 생성자id
     */
    @JSONField(name = "creator_id")
    private String creatorId;
    /**
     * 생성 시간
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createTime;
    /**
     * 수정자id
     */
    @JSONField(name = "updater_id")
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

    @JSONField(name = "tenant_id")
    private String tenantId;
    /**
     * appmarketResource중의사용id
     */
    @JSONField(name = "app_id")
    private String appId;
    /**
     * 가져오기의사용: 앱 마켓버전
     */
    @JSONField(name = "app_version")
    private Integer appVersion;
    /**
     * 가져오기의사용: 마켓id
     */
    @JSONField(name = "market_id")
    private String marketId;
    /**
     * 상태: toObtain, obtaining, obtained, toUpdate, updating
     */
    @JSONField(name = "resource_status")
    private String resourceStatus;
    /**
     * : create 생성 ; market 마켓가져오기
     */
    @JSONField(name = "data_source")
    private String dataSource;

    @JSONField(name = "param_detail")
    private String paramDetail;

    /**
     * 모듈id경로, 사용근거모듈시스템계획봇수
     */
    @JSONField(name = "dept_id_path")
    private String deptIdPath;

    private Boolean isCreator;

    /**
     * 새버전봇의유형, web, other
     */
    private String type;

    /**
     * 새버전 발송버전시간
     */
    @JSONField(name = "latest_release_time")
    private Date latestReleaseTime;

    @JSONField(name = "robot_version")
    private Integer robotVersion;

    private String introduction;
}