package com.iflytek.rpa.notify.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.iflytek.rpa.conf.LongJsonSerializer;
import java.io.Serializable;
import java.util.Date;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NotifySend implements Serializable {

    private static final long serialVersionUID = 3271461913224607218L;

    // 기본 키, 기본 키증가
    @JsonSerialize(using = LongJsonSerializer.class) // 프론트엔드의시사용LongJson의형식, 중지id경과길이출력
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    // 테넌트Id
    private String tenantId;

    // 사용자Id
    private String userId;

    // 메시지Id
    private String messageInfo;

    // 메시지유형, 사람메시지teamMarketInvite, 업데이트메시지teamMarketUpdate
    private String messageType;

    // 결과, 미완료1,  완료2, 완료추가입력3, 완료4
    private Integer operateResult;

    // 마켓id
    private String marketId;

    private String userType;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date updateTime;

    // 삭제
    @TableLogic(value = "0", delval = "1")
    private Integer deleted;

    private String appName;
}