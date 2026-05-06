package com.iflytek.rpa.robot.entity;

import com.alibaba.fastjson.annotation.JSONField;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.io.Serializable;
import java.util.Date;
import javax.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 단말봇버전테이블(RobotVersion)유형
 *
 * @author makejava
 * @since 2024-09-29 15:34:14
 */
@Data
public class RobotVersion implements Serializable {
    private static final long serialVersionUID = 221473423657236377L;
    /**
     * 기본 키id
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    /**
     * 봇id
     */
    @NotBlank(message = "봇id비워 둘 수 없습니다")
    @JSONField(name = "robot_id")
    private String robotId;

    // 테이블이름해당필드아니요에서데이터베이스중
    @TableField(exist = false)
    private String name;

    //    @NotBlank(message = "봇아이콘비워 둘 수 없습니다")
    private String icon;

    /**
     * 버전
     */
    private Integer version;
    /**
     * 
     */
    private String introduction;
    /**
     * 변경 로그
     */
    @JSONField(name = "update_log")
    private String updateLog;
    /**
     * 사용설명
     */
    @JSONField(name = "use_description")
    private String useDescription;
    /**
     * 여부사용 0:사용할 수 없습니다,1:완료사용
     */
    private Integer online;
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

    private String param;

    /**
     * 주소id
     */
    @JSONField(name = "video_id")
    private String videoId;
    /**
     * 파일주소id
     */
    @JSONField(name = "appendix_id")
    private String appendixId;

    @TableField(exist = false)
    @JSONField(name = "edit_flag")
    private Integer editFlag;

    @TableField(exist = false)
    private String category;
}