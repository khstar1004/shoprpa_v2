package com.iflytek.rpa.robot.entity;

import com.alibaba.fastjson.annotation.JSONField;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.io.Serializable;
import java.util.Date;
import javax.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.Length;

/**
 * 단말봇테이블(Robot)유형
 *
 * @author makejava
 * @since 2024-09-29 15:34:14
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class RobotDesign implements Serializable {
    private static final long serialVersionUID = 733865250569736282L;
    /**
     * 기본 키id
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    /**
     * 봇일id, 가져오기의사용id
     */
    @JSONField(name = "robot_id")
    private String robotId;
    /**
     * 봇이름
     */
    @NotBlank(message = "봇이름비워 둘 수 없습니다")
    @Length(max = 50, message = "봇이름할 수 없음초과경과50개문자기호")
    private String name;

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

    private String tenantId;
    /**
     * appmarketResource중의사용id
     */
    private String appId;
    /**
     * 가져오기의사용: 앱 마켓버전
     */
    private Integer appVersion;
    /**
     * 가져오기의사용: 마켓id
     */
    private String marketId;

    /**
     * 상태: toObtain, obtaining, obtained, toUpdate, updating
     */
    private String resourceStatus;

    // : create 생성 ; market 마켓가져오기
    private String dataSource;

    //    @TableField(exist = false)
    private Integer editEnable;

    private String transformStatus;
}