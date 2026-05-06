package com.iflytek.rpa.market.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * 팀마켓-테이블(AppMarketResource)유형
 *
 * @author mjren
 * @since 2024-10-21 14:36:30
 */
@Data
public class AppMarketResource implements Serializable {
    private static final long serialVersionUID = 596242538092112354L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    /**
     * 팀마켓id
     */
    //    @NotBlank(message = "마켓id비워 둘 수 없습니다")
    private String marketId;
    /**
     * 사용id, id, 컴포넌트id
     */
    private String appId;
    /**
     * 다운로드데이터
     */
    private Long downloadNum;
    /**
     * 조회데이터
     */
    private Long checkNum;
    /**
     * 게시사람
     */
    private String creatorId;
    /**
     * 게시시간
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
    @TableLogic(value = "0", delval = "1")
    private Integer deleted;

    private String tenantId;
    /**
     * 봇id
     */
    private String robotId;
    /**
     * 이름
     */
    private String appName;

    /**
     * 비밀단계식별자: red,yellow,green
     */
    @TableField(exist = false)
    private String security_level;

    /**
     * 사용제한중지시간
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @TableField(exist = false)
    private Date expiry_date;

    @TableField(exist = false)
    private String expiry_date_str;
}